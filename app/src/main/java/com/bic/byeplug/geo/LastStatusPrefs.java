package com.bic.byeplug.geo;

import android.content.Context;
import android.content.SharedPreferences;

public class LastStatusPrefs {
    private static final String SP = "byeplug_status_cache";
    private static final String KEY_PREFIX = "outlets_"; // outlets_<deviceId> = "1,0,1,0"

    public static void save(Context c, String deviceId, boolean o1, boolean o2, boolean o3, boolean o4) {
        String v = (o1 ? "1" : "0") + "," + (o2 ? "1" : "0") + "," + (o3 ? "1" : "0") + "," + (o4 ? "1" : "0");
        c.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PREFIX + deviceId, v)
                .apply();
    }

    public static boolean[] load(Context c, String deviceId) {
        SharedPreferences sp = c.getSharedPreferences(SP, Context.MODE_PRIVATE);
        String v = sp.getString(KEY_PREFIX + deviceId, null);
        if (v == null) return null;

        String[] p = v.split(",");
        if (p.length != 4) return null;

        boolean[] s = new boolean[5];
        s[1] = "1".equals(p[0]);
        s[2] = "1".equals(p[1]);
        s[3] = "1".equals(p[2]);
        s[4] = "1".equals(p[3]);
        return s;
    }
}
