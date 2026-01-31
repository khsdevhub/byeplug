package com.bic.byeplug.geo;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class GeoRestorePrefs {
    private static final String SP = "byeplug_geo_restore";
    private static final String KEY_DEVICE_SET = "restore_device_set";
    private static final String KEY_RESTORE_PREFIX = "restore_"; // restore_<deviceId>
    private static final String KEY_LAST_ENTER_MS = "last_enter_ms";
    private static final String KEY_INSIDE = "inside";

    // 지오펜스 흔들림 대비: ENTER 중복 실행 최소화(예: 30초 이내면 무시)
    private static final long ENTER_DEBOUNCE_MS = 30_000L;

    public static void setInside(Context c, boolean inside) {
        c.getSharedPreferences(SP, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_INSIDE, inside)
                .apply();
    }

    public static boolean isInside(Context c) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .getBoolean(KEY_INSIDE, true); // 초기값은 inside로 가정
    }

    public static boolean shouldHandleEnterNow(Context c) {
        SharedPreferences sp = c.getSharedPreferences(SP, Context.MODE_PRIVATE);
        long last = sp.getLong(KEY_LAST_ENTER_MS, 0L);
        long now = System.currentTimeMillis();
        if (now - last < ENTER_DEBOUNCE_MS) return false;
        sp.edit().putLong(KEY_LAST_ENTER_MS, now).apply();
        return true;
    }

    public static void addRestoreMask(Context c, String deviceId, int mask) {
        if (mask == 0) return;

        SharedPreferences sp = c.getSharedPreferences(SP, Context.MODE_PRIVATE);

        Set<String> set = new HashSet<>(sp.getStringSet(KEY_DEVICE_SET, new HashSet<>()));
        set.add(deviceId);

        // 누적(OR) 저장: 여러 번 EXIT가 와도 복구 목록이 유지됨
        int prev = sp.getInt(KEY_RESTORE_PREFIX + deviceId, 0);
        int next = prev | mask;

        sp.edit()
                .putStringSet(KEY_DEVICE_SET, set)
                .putInt(KEY_RESTORE_PREFIX + deviceId, next)
                .apply();
    }

    public static Set<String> getRestoreDevices(Context c) {
        SharedPreferences sp = c.getSharedPreferences(SP, Context.MODE_PRIVATE);
        return new HashSet<>(sp.getStringSet(KEY_DEVICE_SET, new HashSet<>()));
    }

    public static int getRestoreMask(Context c, String deviceId) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .getInt(KEY_RESTORE_PREFIX + deviceId, 0);
    }

    public static void clearRestoreMask(Context c, String deviceId) {
        SharedPreferences sp = c.getSharedPreferences(SP, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(sp.getStringSet(KEY_DEVICE_SET, new HashSet<>()));
        set.remove(deviceId);

        sp.edit()
                .putStringSet(KEY_DEVICE_SET, set)
                .remove(KEY_RESTORE_PREFIX + deviceId)
                .apply();
    }
}
