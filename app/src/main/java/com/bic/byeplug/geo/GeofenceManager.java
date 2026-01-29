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
        return PendingIntent.getBroadcast(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public void registerHomeGeofence() {
        double lat = HomePrefs.lat(appContext);
        double lng = HomePrefs.lng(appContext);
        float radius = HomePrefs.radius(appContext);

        Geofence geofence = new Geofence.Builder()
                .setRequestId(GEOFENCE_ID_HOME)
                .setCircularRegion(lat, lng, radius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT) // 집 밖으로 나갈 때
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();

        GeofencingRequest req = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofences(Collections.singletonList(geofence))
                .build();

        // 권한(FINE + BACKGROUND) 전제
        client.addGeofences(req, geofencePendingIntent());
    }

    public void unregisterHomeGeofence() {
        client.removeGeofences(geofencePendingIntent());
    }
}
