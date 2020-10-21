package edu.wpi.activityrecognition.database;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import edu.wpi.activityrecognition.Constants;
import edu.wpi.activityrecognition.StepListener;

public class MyBroadcastReceiver extends BroadcastReceiver {

    private StepListener listener;

    public  MyBroadcastReceiver(StepListener listener){
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
            int type = intent.getIntExtra("type", -1);
            int confidence = intent.getIntExtra("confidence", 0);
            listener.handleUserActivity(type, confidence);
        }
        if (intent.getAction().equals(Constants.BROADCAST_GEOFENCE)) {
            GeofencingEvent event = GeofencingEvent.fromIntent(intent);
            if (event.hasError()) {
                String errorMessage = GeofenceStatusCodes
                        .getStatusCodeString(event.getErrorCode());
                Log.e("Broadcast Receiver", errorMessage);
                return;
            }
            int transition = event.getGeofenceTransition();
            if (transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                List<Geofence> triggered = event.getTriggeringGeofences();
                for (Geofence fence : triggered) {
                    listener.geoFenceTrigger(fence.getRequestId());
                }
            }
        }
    }
}
