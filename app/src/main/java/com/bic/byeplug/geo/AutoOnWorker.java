package com.bic.byeplug.geo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bic.byeplug.model.CinRequest;
import com.bic.byeplug.network.OneM2MHeaders;
import com.bic.byeplug.network.OneM2MService;
import com.bic.byeplug.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import retrofit2.Response;

public class AutoOnWorker extends Worker {
    private static final String CSE = "Mobius";

    public AutoOnWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        if (!HomePrefs.hasHome(c)) return Result.success();

        // ✅ “집에 들어올 때 한 번만” 처리
        // 1) 이미 inside=true면 무시
        if (GeoRestorePrefs.isInside(c)) {
            android.util.Log.d("GEO", "ENTER ignored: already inside");
            return Result.success();
        }
        // 2) ENTER 흔들림 디바운스
        if (!GeoRestorePrefs.shouldHandleEnterNow(c)) {
            android.util.Log.d("GEO", "ENTER ignored: debounce");
            return Result.success();
        }

        OneM2MService api = RetrofitClient.getInstance().create(OneM2MService.class);

        Set<String> restoreDevices = GeoRestorePrefs.getRestoreDevices(c);
        for (String deviceId : restoreDevices) {
            int restoreMask = GeoRestorePrefs.getRestoreMask(c, deviceId);
            if (restoreMask == 0) {
                GeoRestorePrefs.clearRestoreMask(c, deviceId);
                continue;
            }

            // 현재 상태를 아는 게 가장 정확하므로 캐시 활용
            boolean[] last = LastStatusPrefs.load(c, deviceId);

            boolean o1 = last != null ? last[1] : false;
            boolean o2 = last != null ? last[2] : false;
            boolean o3 = last != null ? last[3] : false;
            boolean o4 = last != null ? last[4] : false;

            if ((restoreMask & (1 << 0)) != 0) o1 = true;
            if ((restoreMask & (1 << 1)) != 0) o2 = true;
            if ((restoreMask & (1 << 2)) != 0) o3 = true;
            if ((restoreMask & (1 << 3)) != 0) o4 = true;

            try {
                Response<Object> resp = sendSnapshot(api, deviceId, o1, o2, o3, o4, "GEO_ENTER");
                android.util.Log.d("GEO", "CONTROL(GEO_ENTER) device=" + deviceId + " resp=" + resp.code());
            } catch (Exception e) {
                android.util.Log.e("GEO", "AutoOnWorker send fail: " + e.getMessage());
                continue;
            }

            GeoRestorePrefs.clearRestoreMask(c, deviceId);
        }

        GeoRestorePrefs.setInside(c, true);
        return Result.success();
    }

    private Response<Object> sendSnapshot(OneM2MService api, String deviceId,
                                          boolean o1, boolean o2, boolean o3, boolean o4,
                                          String reason) throws Exception {
        Map<String, Object> con = new HashMap<>();
        con.put("cmd", "SET_OUTLETS");
        Map<String, Object> outlets = new HashMap<>();
        outlets.put("1", o1); outlets.put("2", o2); outlets.put("3", o3); outlets.put("4", o4);
        con.put("outlets", outlets);
        con.put("ts", System.currentTimeMillis());
        con.put("reason", reason);

        CinRequest body = new CinRequest(con);

        return api.postControl(
                CSE, deviceId, "CONTROL", OneM2MHeaders.withTy(4), body
        ).execute();
    }
}
