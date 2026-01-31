package com.bic.byeplug.geo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Collections;

public class GeofenceManager {
    public static final String GEOFENCE_ID_HOME = "HOME";
    private final Context appContext;
    private final GeofencingClient client;

    public GeofenceManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.client = LocationServices.getGeofencingClient(appContext);
    }

    public PendingIntent geofencePendingIntent() {
        Intent intent = new Intent(appContext, GeofenceBroadcastReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        // Android 12(API 31)+: 반드시 MUTABLE/IMMUTABLE 중 하나 명시 필요
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        return PendingIntent.getBroadcast(
                appContext,
                0,
                intent,
                flags
        );

    }

    // GeofenceManager.java
    public void registerHomeGeofence() {
        double lat = HomePrefs.lat(appContext);
        double lng = HomePrefs.lng(appContext);
        float radius = HomePrefs.radius(appContext);

        Geofence geofence = new Geofence.Builder()
                .setRequestId(GEOFENCE_ID_HOME)
                .setCircularRegion(lat, lng, radius)
                .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_ENTER
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();

        GeofencingRequest req = new GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofence(geofence)
                .build();

        client.addGeofences(req, geofencePendingIntent())
                .addOnSuccessListener(unused -> android.util.Log.d("GEO", "Geofence registered OK"))
                .addOnFailureListener(e -> android.util.Log.e("GEO", "Geofence register FAIL: " + e));
    }

    public void unregisterHomeGeofence() {
        client.removeGeofences(geofencePendingIntent());
    }
}
