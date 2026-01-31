package com.bic.byeplug.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    // GeofenceBroadcastReceiver.java
    @Override
    public void onReceive(Context context, Intent intent) {
        android.util.Log.d("GEO", "GeofenceBroadcastReceiver triggered");

        com.google.android.gms.location.GeofencingEvent event =
                com.google.android.gms.location.GeofencingEvent.fromIntent(intent);

        if (event == null) {
            android.util.Log.e("GEO", "GeofencingEvent is null");
            return;
        }
        if (event.hasError()) {
            android.util.Log.e("GEO", "Geofence error: " + event.getErrorCode());
            return;
        }

        int transition = event.getGeofenceTransition();
        android.util.Log.d("GEO", "transition=" + transition);

        if (transition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT) {
            androidx.work.OneTimeWorkRequest work =
                    new androidx.work.OneTimeWorkRequest.Builder(AutoOffWorker.class).build();

            androidx.work.WorkManager.getInstance(context)
                    .enqueueUniqueWork("geo_exit", androidx.work.ExistingWorkPolicy.REPLACE, work);

        } else if (transition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER) {
            androidx.work.OneTimeWorkRequest work =
                    new androidx.work.OneTimeWorkRequest.Builder(AutoOnWorker.class).build();

            androidx.work.WorkManager.getInstance(context)
                    .enqueueUniqueWork("geo_enter", androidx.work.ExistingWorkPolicy.REPLACE, work);
        }
    }
}
