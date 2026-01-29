package com.bic.byeplug.geo;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class AutoOffPrefs {
    private static final String SP = "byeplug_geo";
    private static final String KEY_DEVICE_SET = "device_set";
    private static final String KEY_MASK_PREFIX = "mask_"; // mask_<deviceId>

    public static void upsertDevice(Context c, String deviceId) {
        SharedPreferences sp = c.getSharedPreferences(SP, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(sp.getStringSet(KEY_DEVICE_SET, new HashSet<>()));
        set.add(deviceId);
        sp.edit().putStringSet(KEY_DEVICE_SET, set).apply();
    }

    public static Set<String> getDevices(Context c) {
        SharedPreferences sp = c.getSharedPreferences(SP, Context.MODE_PRIVATE);
        return new HashSet<>(sp.getStringSet(KEY_DEVICE_SET, new HashSet<>()));
    }

    public static void setMask(Context c, String deviceId, int mask) {
        upsertDevice(c, deviceId);
        c.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_MASK_PREFIX + deviceId, mask)
                .apply();
    }

    public static int getMask(Context c, String deviceId) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .getInt(KEY_MASK_PREFIX + deviceId, 0);
    }

    public static int toggleOutlet(int mask, int outlet) {
        int bit = outlet - 1;
        return mask ^ (1 << bit);
    }

    public static boolean isSelected(int mask, int outlet) {
        int bit = outlet - 1;
        return (mask & (1 << bit)) != 0;
    }
}
