package com.bic.byeplug.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) return;

        if (event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_EXIT) {
            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(AutoOffWorker.class).build();
            WorkManager.getInstance(context).enqueue(work);
        }
    }
}
