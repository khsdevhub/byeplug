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
import com.bic.byeplug.geo.AutoOffPrefs;
import com.bic.byeplug.geo.LastStatusPrefs;


import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PowerStripDialogFragment extends DialogFragment {

    private static final String TAG = "PowerStripDialog";
    private static final String ARG_DEVICE_ID = "deviceId";
    private static final String CSE = "Mobius";

    // 폴링 정책
    private static final long POLL_IDLE_MS = 5000; // 평상시 5초
    private static final long POLL_FAST_MS = 100;
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
    private void toggleAutoOff(int outlet) {
        int mask = AutoOffPrefs.getMask(requireContext(), deviceId);
        int next = AutoOffPrefs.toggleOutlet(mask, outlet);
        AutoOffPrefs.setMask(requireContext(), deviceId, next);

        boolean selected = AutoOffPrefs.isSelected(next, outlet);
        Toast.makeText(requireContext(),
                "집 밖 자동OFF: " + outlet + "번 " + (selected ? "선택됨" : "해제됨"),
                Toast.LENGTH_SHORT).show();
    }

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

        b1.setOnLongClickListener(vw -> { toggleAutoOff(1); return true; });
        b2.setOnLongClickListener(vw -> { toggleAutoOff(2); return true; });
        b3.setOnLongClickListener(vw -> { toggleAutoOff(3); return true; });
        b4.setOnLongClickListener(vw -> { toggleAutoOff(4); return true; });

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

        boolean[] rollbackSnapshot = snapshotState();

        // 1) UI 즉시 토글(낙관적)
        state[outlet] = !state[outlet];
        refreshButtons();

        // 2) CONTROL 전송 (항상 4구 전체 스냅샷)
        sendControlSnapshot(rollbackSnapshot);
    }

    private boolean[] snapshotState() {
        boolean[] snap = new boolean[5];
        for (int i = 1; i <= 4; i++) snap[i] = state[i];
        return snap;
    }

    private void sendControlSnapshot(boolean[] rollbackSnapshot) {
        Map<String, Object> con = new HashMap<>();
        con.put("cmd", "SET_OUTLETS");

        Map<String, Object> outlets = new HashMap<>();
        outlets.put("1", state[1]);
        outlets.put("2", state[2]);
        outlets.put("3", state[3]);
        outlets.put("4", state[4]);

        con.put("outlets", outlets);
        con.put("ts", System.currentTimeMillis());

        CinRequest body = new CinRequest(con);

        api.postControl(
                CSE,
                deviceId,
                "CONTROL",
                OneM2MHeaders.withTy(4),
                body
        ).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "CONTROL(snapshot) failed HTTP=" + response.code());
                    for (int i = 1; i <= 4; i++) state[i] = rollbackSnapshot[i];
                    refreshButtons();
                    Toast.makeText(requireContext(), "제어 실패 (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchStatusAndApply();
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                Log.e(TAG, "CONTROL(snapshot) network error", t);
                for (int i = 1; i <= 4; i++) state[i] = rollbackSnapshot[i];
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
                CSE,
                deviceId,
                "STATUS",
                OneM2MHeaders.base()
        ).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                try {
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

                    Object outletsObj = con.get("outlets");
                    if (!(outletsObj instanceof Map)) return;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> outlets = (Map<String, Object>) outletsObj;

                    applyOutletStateFromMap(outlets);

                } catch (Exception e) {
                    Log.w(TAG, "STATUS parse fail: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
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

        LastStatusPrefs.save(requireContext(), deviceId, state[1], state[2], state[3], state[4]);
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
        currentPollMs = POLL_FAST_MS;
        handler.removeCallbacks(pollTask);
        handler.post(pollTask);

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
        try {
            if (isOn) {
                btn.setBackgroundResource(R.drawable.bg_circle_switch_on);
            } else {
                btn.setBackgroundResource(R.drawable.bg_circle_switch_off);
            }
        } catch (Exception ignored) {
            btn.setBackgroundColor(0x00000000);
        }
    }
}
