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

import retrofit2.Call;
import retrofit2.Response;

public class AutoOffWorker extends Worker {

    private static final String CSE = "Mobius";

    public AutoOffWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        OneM2MService api = RetrofitClient.getInstance().create(OneM2MService.class);

        if (!HomePrefs.hasHome(c)) return Result.success();

        // ✅ EXIT 처리로 inside=false
        GeoRestorePrefs.setInside(c, false);

        Set<String> devices = AutoOffPrefs.getDevices(c);
        for (String deviceId : devices) {
            int mask = AutoOffPrefs.getMask(c, deviceId);
            if (mask == 0) continue;

            boolean[] last = LastStatusPrefs.load(c, deviceId);

            // last 없으면 “현재 ON 여부”를 판단 못하므로,
            // 복구 마스크는 보수적으로 0 처리(원하면 true 가정 가능)
            int currentlyOnMask = 0;
            if (last != null) {
                if (last[1]) currentlyOnMask |= 1 << 0;
                if (last[2]) currentlyOnMask |= 1 << 1;
                if (last[3]) currentlyOnMask |= 1 << 2;
                if (last[4]) currentlyOnMask |= 1 << 3;
            }

            // ✅ 우리가 “실제로 끈 것만” 복구 대상으로 기록
            int maskToRestore = mask & currentlyOnMask;
            GeoRestorePrefs.addRestoreMask(c, deviceId, maskToRestore);

            // OFF 적용 상태 계산
            boolean o1 = last != null ? last[1] : true;
            boolean o2 = last != null ? last[2] : true;
            boolean o3 = last != null ? last[3] : true;
            boolean o4 = last != null ? last[4] : true;

            if (AutoOffPrefs.isSelected(mask, 1)) o1 = false;
            if (AutoOffPrefs.isSelected(mask, 2)) o2 = false;
            if (AutoOffPrefs.isSelected(mask, 3)) o3 = false;
            if (AutoOffPrefs.isSelected(mask, 4)) o4 = false;

            try {
                sendSnapshot(api, deviceId, o1, o2, o3, o4, "GEO_EXIT");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return Result.success();
    }

    private void sendSnapshot(OneM2MService api, String deviceId,
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

        Response<Object> resp = api.postControl(
                "Mobius", deviceId, "CONTROL", OneM2MHeaders.withTy(4), body
        ).execute();

        android.util.Log.d("GEO", "CONTROL(" + reason + ") device=" + deviceId + " resp=" + resp.code());
    }

}
