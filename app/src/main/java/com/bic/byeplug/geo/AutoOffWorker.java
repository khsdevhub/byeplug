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

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        OneM2MService api = RetrofitClient.getInstance().create(OneM2MService.class);

        // 홈이 없으면 동작하지 않게
        if (!HomePrefs.hasHome(c)) return Result.success();

        Set<String> devices = AutoOffPrefs.getDevices(c);
        for (String deviceId : devices) {
            int mask = AutoOffPrefs.getMask(c, deviceId);
            if (mask == 0) continue; // 자동OFF 설정된 콘센트 없음

            // 마지막 상태 기반으로 “선택된 콘센트만 OFF”
            boolean[] last = LastStatusPrefs.load(c, deviceId);

            // 캐시 없으면 true로 가정 → 선택된 것만 false로 내려서 OFF 보장(안전)
            boolean o1 = last != null ? last[1] : true;
            boolean o2 = last != null ? last[2] : true;
            boolean o3 = last != null ? last[3] : true;
            boolean o4 = last != null ? last[4] : true;

            if (AutoOffPrefs.isSelected(mask, 1)) o1 = false;
            if (AutoOffPrefs.isSelected(mask, 2)) o2 = false;
            if (AutoOffPrefs.isSelected(mask, 3)) o3 = false;
            if (AutoOffPrefs.isSelected(mask, 4)) o4 = false;

            Map<String, Object> con = new HashMap<>();
            con.put("cmd", "SET_OUTLETS");

            Map<String, Object> outlets = new HashMap<>();
            outlets.put("1", o1);
            outlets.put("2", o2);
            outlets.put("3", o3);
            outlets.put("4", o4);

            con.put("outlets", outlets);
            con.put("ts", System.currentTimeMillis());
            con.put("reason", "GEO_EXIT"); // 디버깅용(아두이노는 무시 가능)

            CinRequest body = new CinRequest(con);

            try {
                Call<Object> call = api.postControl(
                        CSE,
                        deviceId,
                        "CONTROL",
                        OneM2MHeaders.withTy(4),
                        body
                );
                Response<Object> resp = call.execute();
                // 실패해도 전체 실패로 처리하지 않음(원하면 Result.retry로 바꿀 수 있음)
                if (!resp.isSuccessful()) {
                    // no-op
                }
            } catch (Exception ignored) {
                // 네트워크 일시 문제면 다음 EXIT/수동 재등록 등으로 커버 가능
            }
        }

        return Result.success();
    }
}
