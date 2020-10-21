package edu.wpi.activityrecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;

import edu.wpi.activityrecognition.database.DBManager;
import edu.wpi.activityrecognition.database.DatabaseHelper;

public class MainActivity extends AppCompatActivity
        implements StepListener, SensorEventListener {
    private Context mContext;
    private DBManager dbManager;

    private String TAG = MainActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;

    private TextView txtActivity, txtConfidence;
    private ImageView imgActivity;
    MediaPlayer mediaPlayer;
    Boolean beatsPlaying = false;

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

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };

        startTracking();
    }

    private void handleUserActivity(int type, int confidence) {
        String label = getString(R.string.activity_unknown);
        setDefaults();
        if (confidence > Constants.CONFIDENCE) {
            showLastActivityStats(true);
        }

        switch (type) {
            case DetectedActivity.IN_VEHICLE:
                label = getString(R.string.activity_in_vehicle);
                imgActivity.setImageResource(R.drawable.in_vehicle);
                dbManager.insertActivity("IN_VEHICLE");
                lastActivity = "IN_VEHICLE";
                break;

            case DetectedActivity.ON_BICYCLE:
                label = getString(R.string.activity_on_bicycle);
                dbManager.insertActivity("ON_BICYCLE");
                lastActivity = "ON_BICYCLE";
                break;
            case DetectedActivity.ON_FOOT:
                label = getString(R.string.activity_on_foot);
                dbManager.insertActivity("ON_FOOT");
                lastActivity = "ON_FOOT";
                break;
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
                imgActivity.setImageResource(R.drawable.still);
                lastActivity = "STILL";
                //playBeats(); //Testing purpose only
                break;

            case DetectedActivity.TILTING:
                label = getString(R.string.activity_tilting);
                dbManager.insertActivity("TILTING");
                lastActivity = "TILTING";

                break;
            case DetectedActivity.WALKING:
                label = getString(R.string.activity_walking);
                dbManager.insertActivity("WALKING");
                imgActivity.setImageResource(R.drawable.walking);
                lastActivity = "WALKING";
                playBeats();
                break;
            case DetectedActivity.UNKNOWN:
                label = getString(R.string.activity_unknown);
                lastActivity = "UNKNOWN";
                break;
        }

        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);
        txtActivity.setText(label);
        if (confidence > Constants.CONFIDENCE) {
//            txtActivity.setText(label);
            txtConfidence.setText("Confidence: " + confidence);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dbManager = new DBManager(this);
        dbManager.open();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    protected void onPause() {
        super.onPause();
        dbManager.close();
        lastActivity = "";
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
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
        startService(intent);
    }

    private void stopTracking() {
        Intent intent = new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);

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

    public void showLastActivityStats(Boolean restart){
        if (lastActivity != ""){
            end = System.currentTimeMillis();
            long time = (end - start)/1000;

            String units = "";
            long hours = time / 3600;
            long mins = time / 60;
            long secs = time % 60;

            if (hours > 0) units += hours+"hrs";
            if (mins > 0) units += mins+"mins";
            if (secs > 0) units += secs+"secs";
            Toast.makeText(getApplicationContext(), lastActivity +" lasted for "+ units, Toast.LENGTH_SHORT).show();
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
        dbManager.insertGeoFence(fenceName);
        // code to update the UI component goes here
//        String name1 = this.getString(R.string.geofence1);
//        String name2 = this.getString(R.string.geofence2);
        switch(fenceName){
            case "GeoFence1":// Fuller Labs
                // update UI here
                break;
            case "GeoFence2":// Gordon Library
                // update UI here
                break;
        }

    }


}