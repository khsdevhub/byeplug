package com.bic.byeplug.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bic.byeplug.R;
import com.bic.byeplug.model.CinRequest;
import com.bic.byeplug.network.DeviceProvisioner;
import com.bic.byeplug.network.OneM2MHeaders;
import com.bic.byeplug.network.OneM2MService;
import com.bic.byeplug.network.RetrofitClient;
import com.bic.byeplug.secret.OneM2MSecret;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PowerStripDialogFragment extends DialogFragment {

    private static final String TAG = "PowerStripDialog";
    private static final String ARG_DEVICE_ID = "deviceId";

    // 폴링 정책
    private static final long POLL_IDLE_MS = 5000; // 평상시 5초
    private static final long POLL_FAST_MS = 1000; // 조작 직후 1초
    private static final long POLL_FAST_WINDOW_MS = 8000; // 8초 동안만 빠르게

    public static PowerStripDialogFragment newInstance(String deviceId) {
        PowerStripDialogFragment f = new PowerStripDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_DEVICE_ID, deviceId);
        f.setArguments(b);
        return f;
    }

    private String deviceId;
    private OneM2MService api;

    // 1~4 사용 (편하게 5칸)
    private final boolean[] state = new boolean[5];

    private ImageButton b1, b2, b3, b4;
    private boolean isReady = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long currentPollMs = POLL_IDLE_MS;

    private final Runnable pollTask = new Runnable() {
        @Override public void run() {
            fetchStatusAndApply();
            handler.postDelayed(this, currentPollMs);
        }
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        deviceId = (getArguments() != null) ? getArguments().getString(ARG_DEVICE_ID) : null;
        if (deviceId == null) deviceId = "UNKNOWN";

        api = RetrofitClient.getInstance().create(OneM2MService.class);

        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_powerstrip, null);

        TextView title = v.findViewById(R.id.tvDeviceTitle);
        if (title != null) title.setText(deviceId);

        b1 = v.findViewById(R.id.btnOutlet1);
        b2 = v.findViewById(R.id.btnOutlet2);
        b3 = v.findViewById(R.id.btnOutlet3);
        b4 = v.findViewById(R.id.btnOutlet4);

        if (b1 == null || b2 == null || b3 == null || b4 == null) {
            throw new IllegalStateException("dialog_powerstrip.xml에 btnOutlet1~4 id가 필요합니다.");
        }

        // 초기 상태는 일단 OFF로 두고, STATUS 수신하면 덮어씀
        for (int i = 1; i <= 4; i++) state[i] = false;
        refreshButtons();

        // 준비 전에는 버튼 잠금
        setButtonsEnabled(false);

        // 클릭 리스너
        b1.setOnClickListener(view -> onOutletClicked(1));
        b2.setOnClickListener(view -> onOutletClicked(2));
        b3.setOnClickListener(view -> onOutletClicked(3));
        b4.setOnClickListener(view -> onOutletClicked(4));

        // AE/CNT 준비 보장 후 STATUS 동기화 시작
        DeviceProvisioner provisioner = new DeviceProvisioner(api);
        provisioner.ensureDeviceTree(deviceId, () -> {
            isReady = true;
            handler.post(() -> {
                setButtonsEnabled(true);
                // 첫 동기화 + 폴링 시작
                startPolling(POLL_IDLE_MS);
                fetchStatusAndApply();
            });
        });

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPolling();
    }

    private void onOutletClicked(int outlet) {
        if (!isReady) {
            Toast.makeText(requireContext(), "준비 중입니다. 잠시 후 다시 시도하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 조작 직후 폴링 버스트
        boostPollingTemporarily();

        boolean prev = state[outlet];
        boolean next = !prev;

        // 1) UI 즉시 토글(낙관적)
        state[outlet] = next;
        refreshButtons();

        // 2) CONTROL 전송, 실패하면 롤백
        sendControl(outlet, next, prev);
    }

    private void sendControl(int outlet, boolean isOn, boolean rollbackTo) {
        Map<String, Object> con = new HashMap<>();
        con.put("outlet", outlet);
        con.put("isOn", isOn);

        CinRequest body = new CinRequest(con);

        api.postControl(
                "Mobius",
                deviceId,
                "CONTROL",
                OneM2MHeaders.withTy(4), // Content-Type: application/json;ty=4 포함
                body
        ).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "CONTROL failed HTTP=" + response.code());
                    state[outlet] = rollbackTo;
                    refreshButtons();
                    Toast.makeText(requireContext(), "제어 실패 (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 성공해도, 실제 확정은 STATUS가 해줌 → 바로 한번 당겨서 확인
                fetchStatusAndApply();
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                Log.e(TAG, "CONTROL network error", t);
                state[outlet] = rollbackTo;
                refreshButtons();
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================
    // STATUS 동기화
    // =========================
    private void fetchStatusAndApply() {
        api.getLatestCin(
                "Mobius",
                deviceId,
                "STATUS",
                OneM2MHeaders.base()
        ).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                try {
                    // Gson이 Map 구조로 내려주는 경우가 많음(LinkedTreeMap)
                    @SuppressWarnings("unchecked")
                    Map<String, Object> root = (Map<String, Object>) response.body();

                    Object cinObj = root.get("m2m:cin");
                    if (!(cinObj instanceof Map)) return;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> cin = (Map<String, Object>) cinObj;

                    Object conObj = cin.get("con");
                    if (!(conObj instanceof Map)) return;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> con = (Map<String, Object>) conObj;

                    // ✅ 약속한 포맷: { outlets: { "1": true, ... } }
                    Object outletsObj = con.get("outlets");
                    if (!(outletsObj instanceof Map)) return;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> outlets = (Map<String, Object>) outletsObj;

                    applyOutletStateFromMap(outlets);

                } catch (Exception e) {
                    // 아두이노 포맷이 아직 변하는 중이면 조용히 무시
                    Log.w(TAG, "STATUS parse fail: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                // 폴링 실패는 조용히 (필요하면 로그/토스트)
                Log.w(TAG, "STATUS poll fail: " + t.getMessage());
            }
        });
    }

    private void applyOutletStateFromMap(Map<String, Object> outlets) {
        for (int i = 1; i <= 4; i++) {
            Object v = outlets.get(String.valueOf(i));
            state[i] = toBoolean(v, state[i]);
        }
        refreshButtons();
    }

    private boolean toBoolean(Object v, boolean fallback) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) {
            String s = ((String) v).trim().toLowerCase();
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue() != 0;
        }
        return fallback;
    }

    // =========================
    // 폴링 제어
    // =========================
    private void startPolling(long intervalMs) {
        currentPollMs = intervalMs;
        handler.removeCallbacks(pollTask);
        handler.post(pollTask);
    }

    private void stopPolling() {
        handler.removeCallbacks(pollTask);
    }

    private void boostPollingTemporarily() {
        // 빠른 폴링으로 전환
        currentPollMs = POLL_FAST_MS;
        handler.removeCallbacks(pollTask);
        handler.post(pollTask);

        // 8초 뒤 원복 (중복 예약 방지 위해 한번 더 remove)
        handler.removeCallbacks(resetPollRunnable);
        handler.postDelayed(resetPollRunnable, POLL_FAST_WINDOW_MS);
    }

    private final Runnable resetPollRunnable = new Runnable() {
        @Override public void run() {
            currentPollMs = POLL_IDLE_MS;
        }
    };

    // =========================
    // UI
    // =========================
    private void setButtonsEnabled(boolean enabled) {
        b1.setEnabled(enabled);
        b2.setEnabled(enabled);
        b3.setEnabled(enabled);
        b4.setEnabled(enabled);

        float a = enabled ? 1.0f : 0.45f;
        b1.setAlpha(a); b2.setAlpha(a); b3.setAlpha(a); b4.setAlpha(a);
    }

    private void refreshButtons() {
        setBtnState(b1, state[1]);
        setBtnState(b2, state[2]);
        setBtnState(b3, state[3]);
        setBtnState(b4, state[4]);
    }

    private void setBtnState(ImageButton btn, boolean isOn) {
        // 네가 만든 drawable이 있으면 사용
        // 없으면 alpha만으로도 구분 가능
        try {
            if (isOn) {
                btn.setBackgroundResource(R.drawable.bg_circle_switch_on);
            } else {
                btn.setBackgroundResource(R.drawable.bg_circle_switch_off);
            }
        } catch (Exception ignored) {
            // drawable 없을 때 대비
            btn.setBackgroundColor(0x00000000);
        }
    }
}
