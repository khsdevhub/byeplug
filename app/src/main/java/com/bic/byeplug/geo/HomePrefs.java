package com.bic.byeplug.geo;

import android.content.Context;

public class HomePrefs {
    private static final String SP = "byeplug_home";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";
    private static final String KEY_RADIUS = "radius_m";

    public static void save(Context c, double lat, double lng, float radiusM) {
        c.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAT, Double.doubleToRawLongBits(lat))
                .putLong(KEY_LNG, Double.doubleToRawLongBits(lng))
                .putFloat(KEY_RADIUS, radiusM)
                .apply();
    }

    public static boolean hasHome(Context c) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE).contains(KEY_LAT);
    }

    public static double lat(Context c) {
        long v = c.getSharedPreferences(SP, Context.MODE_PRIVATE).getLong(KEY_LAT, 0L);
        return Double.longBitsToDouble(v);
    }

    public static double lng(Context c) {
        long v = c.getSharedPreferences(SP, Context.MODE_PRIVATE).getLong(KEY_LNG, 0L);
        return Double.longBitsToDouble(v);
    }

    public static float radius(Context c) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE).getFloat(KEY_RADIUS, 100f);
    }

    public static void clear(Context c) {
        c.getSharedPreferences(SP, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
