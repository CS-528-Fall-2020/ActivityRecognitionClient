package edu.wpi.activityrecognition;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.wpi.activityrecognition.database.DBManager;
import edu.wpi.activityrecognition.database.DatabaseHelper;
import edu.wpi.activityrecognition.database.MyBroadcastReceiver;

public class MainActivity extends AppCompatActivity
        implements StepListener, SensorEventListener, OnMapReadyCallback {
    private Context mContext;
    private DBManager dbManager;

    private String TAG = MainActivity.class.getSimpleName();

    private TextView txtActivity, txtConfidence;
    private ImageView imgActivity;
    MediaPlayer mediaPlayer;
    Boolean beatsPlaying = false;

    private Map<String, TextView> counts = new HashMap<>();
    private TextView address;
    private GoogleMap map;
    private Geocoder geocoder;
    private GeofencingClient client;
    private List<Geofence> geofences = new ArrayList<>();
    private PendingIntent geofenceIntent;
    private MyBroadcastReceiver myBroadcastReceiver;

    private TextView stepCount;
    private TextView stepCountNeil;
    private StepDetector simpleStepDetector;
    private NeilStepDetector neilStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private int numSteps = 0;
    private int numStepsNeil = 0;

    private String lastActivity = "";
    private long start;
    private long end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        dbManager = new DBManager(mContext);
        dbManager.open();
        Cursor cursor = dbManager.fetchAllFenceCounts();

        if (cursor.getCount() > 0) {
            do {
                Log.e(cursor.getString(cursor.getColumnIndex(DatabaseHelper.FENCE_NAME)), cursor.getInt(cursor.getColumnIndex(DatabaseHelper.FENCE_CNT)) + "");
            } while (cursor.moveToNext());
            cursor.close();
        }

        Cursor cursor2 = dbManager.fetchActivities();

        if (cursor2.getCount() > 0) {
            do {
                Log.e(cursor2.getString(cursor2.getColumnIndex(DatabaseHelper.CREATED_AT1)), cursor2.getString(cursor2.getColumnIndex(DatabaseHelper.DESC1)));
            } while (cursor2.moveToNext());
            cursor2.close();
        }


        // get an instance of SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        sensorManager.registerListener(MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

        neilStepDetector = new NeilStepDetector();
        neilStepDetector.registerListener(this);

        //get the TextView widgets
        stepCount = findViewById(R.id.step_counter_count);
        stepCountNeil = findViewById(R.id.step_counter_count_neil);

        txtActivity = findViewById(R.id.txt_activity);
        txtConfidence = findViewById(R.id.txt_confidence);
        imgActivity = findViewById(R.id.img_activity);

        Button btnStartTrcking = findViewById(R.id.btn_start_tracking);
        Button btnStopTracking = findViewById(R.id.btn_stop_tracking);

        //Get the map information
        counts.put(Constants.FULLER_LABS, (TextView) findViewById(R.id.geo_fence_1_count));
        counts.put(Constants.GORDON_LIBRARY, (TextView) findViewById(R.id.geo_fence_2_count));
        for (Map.Entry<String, TextView> entry : counts.entrySet()) {
            entry.getValue().setText("" + (dbManager.fetchGeoFenceCount(entry.getKey())));
        }
        address = findViewById(R.id.current_address);
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        client = LocationServices.getGeofencingClient(this);
        geofences.add(new Geofence.Builder()
                .setRequestId(Constants.FULLER_LABS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(42.275010, -71.806408, 35.0f)
                .setLoiteringDelay(15000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .build());
        geofences.add(new Geofence.Builder()
                .setRequestId(Constants.GORDON_LIBRARY)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(42.274231, -71.806383, 32.0f)
                .setLoiteringDelay(15000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .build());
        myBroadcastReceiver = new MyBroadcastReceiver(this);

        mediaPlayer = MediaPlayer.create(this, R.raw.beat_02);
        mediaPlayer.setLooping(true);

        btnStartTrcking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTracking();
            }
        });

        btnStopTracking.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                stopTracking();
            }
        });

        startTracking();
    }

    private static int type = -1;
    @Override
    public void handleUserActivity(int type, int confidence) {
        String label = getString(R.string.activity_unknown);
        if (confidence > Constants.CONFIDENCE && type != this.type) {
            setDefaults();
            this.type = type;
            showLastActivityStats(true);
            switch (type) {
                case DetectedActivity.IN_VEHICLE:
                    label = getString(R.string.activity_in_vehicle);
                    imgActivity.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.in_vehicle));
                    dbManager.insertActivity("IN_VEHICLE");
                    lastActivity = "IN_VEHICLE";
                    break;

//                case DetectedActivity.ON_BICYCLE:
//                    label = getString(R.string.activity_on_bicycle);
//                    dbManager.insertActivity("ON_BICYCLE");
//                    lastActivity = "ON_BICYCLE";
//                    break;
//                case DetectedActivity.ON_FOOT:
//                    label = getString(R.string.activity_on_foot);
//                    dbManager.insertActivity("ON_FOOT");
//                    lastActivity = "ON_FOOT";
//                    break;
                case DetectedActivity.RUNNING:
                    label = getString(R.string.activity_running);
                    dbManager.insertActivity("RUNNING");
                    imgActivity.setImageResource(R.drawable.running);
                    lastActivity = "RUNNING";
                    playBeats();
                    break;
                case DetectedActivity.STILL:
                    label = getString(R.string.activity_still);
                    dbManager.insertActivity("STILL");
                    imgActivity.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.still));
                    lastActivity = "STILL";
                    //playBeats(); //Testing purpose only
                    break;

//                case DetectedActivity.TILTING:
//                    label = getString(R.string.activity_tilting);
//                    dbManager.insertActivity("TILTING");
//                    lastActivity = "TILTING";
//
//                    break;
                case DetectedActivity.WALKING:
                    label = getString(R.string.activity_walking);
                    dbManager.insertActivity("WALKING");
                    imgActivity.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.walking));
                    lastActivity = "WALKING";
                    playBeats();
                    break;
                case DetectedActivity.UNKNOWN:
                    label = getString(R.string.activity_unknown);
                    imgActivity.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.unknown));
                    lastActivity = "UNKNOWN";
                    break;
            }

            Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);
            txtActivity.setText(label);
            txtConfidence.setText("Confidence: " + confidence);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        dbManager = new DBManager(this);
        dbManager.open();
        IntentFilter filters = new IntentFilter();
        filters.addAction(Constants.BROADCAST_DETECTED_ACTIVITY);
        filters.addAction(Constants.BROADCAST_GEOFENCE);
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, filters);
        client.addGeofences(getGeofencingRequest(), getGeofenceIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        dbManager.close();
        lastActivity = "";
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadcastReceiver);
        client.removeGeofences(getGeofenceIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (beatsPlaying) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        pauseIfPlaying();
        lastActivity = "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void startTracking() {
        Intent intent = new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopTracking() {
        Intent intent = new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
        type = -1;

        beatsPlaying = false;
        pauseIfPlaying();
        showLastActivityStats(false);
        lastActivity = "";
    }

    public void playBeats() {
        mediaPlayer.start();
        beatsPlaying = true;
    }

    public void pauseIfPlaying() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void setDefaults() {
        pauseIfPlaying();
        beatsPlaying = false;
        imgActivity.setImageResource(0);
    }

    public void showLastActivityStats(Boolean restart) {
        if (lastActivity != "") {
            end = System.currentTimeMillis();
            long time = (end - start) / 1000;

            String units = "";
            long hours = time / 3600;
            long mins = time / 60;
            long secs = time % 60;

            if (hours > 0) units += hours + "hrs";
            if (mins > 0) units += mins + "mins";
            if (secs > 0) units += secs + "secs";
            Toast.makeText(getApplicationContext(), lastActivity + " lasted for " + units, Toast.LENGTH_SHORT).show();
        }
        if (restart) {
            start = System.currentTimeMillis();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
            neilStepDetector.updateAccel(sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        stepCount.setText("" + numSteps);
    }

    @Override
    public void neilStep(int updateCount) {
        numStepsNeil += updateCount;
        stepCountNeil.setText("" + numStepsNeil);
    }

    @Override
    public void geoFenceTrigger(String fenceName) {
        dbManager.insertGeoFence(fenceName, counts.get(fenceName));
        Toast.makeText(this, "You have been inside the " + fenceName + " geofence for 15 seconds, incrementing counter.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            return;
        }
        setupMap();
    }

    @SuppressLint("MissingPermission")
    private void setupMap() {
        if (map != null) {
            geocoder = new Geocoder(this);
            map.setMyLocationEnabled(true);
            map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                    map.addMarker(new MarkerOptions().position(loc));
                    map.moveCamera(CameraUpdateFactory.newLatLng(loc));
                    try {
                        Address addr = geocoder.getFromLocation(loc.latitude, loc.longitude, 1).get(0);
                        address.setText(addr.getAddressLine(0) + ", " + addr.getLocality() + ", " + addr.getAdminArea());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(geofences)
                .build();
    }

    private PendingIntent getGeofenceIntent() {
        if (geofenceIntent != null) return geofenceIntent;
        Intent intent = new Intent(Constants.BROADCAST_GEOFENCE);
        geofenceIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofenceIntent;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1001) {
            for (int i : grantResults) {
                if (i == PackageManager.PERMISSION_DENIED)
                    throw new RuntimeException("Couldn't get the valid permissions");
            }
            setupMap();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
