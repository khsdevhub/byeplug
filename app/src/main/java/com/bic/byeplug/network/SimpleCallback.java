package com.bic.byeplug.network;

import android.util.Log;

import androidx.annotation.Nullable;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * oneM2M에서 흔히 쓰는 "존재하면 OK / 없으면 생성" 패턴을 깔끔하게 처리하기 위한 콜백.
 *
 * - onOk: 200/201/409 등 "성공으로 취급"할 때 실행
 * - onNotOk: 404/403 등 "없거나 접근 불가로 취급"할 때 실행
 *
 * 나머지 코드는 로그만 찍고 종료(필요하면 확장 가능).
 */
public class SimpleCallback implements Callback<Object> {

    private static final String TAG = "SimpleCallback";

    private final @Nullable Runnable onOk;
    private final @Nullable Runnable onNotOk;

    public SimpleCallback(@Nullable Runnable onOk, @Nullable Runnable onNotOk) {
        this.onOk = onOk;
        this.onNotOk = onNotOk;
    }

    @Override
    public void onResponse(Call<Object> call, Response<Object> response) {
        int code = response.code();

        // ✅ oneM2M에서 "성공"으로 자주 보는 코드들
        // 200 OK, 201 Created, 409 Conflict(이미 존재)
        if (code == 200 || code == 201 || code == 409) {
            if (onOk != null) onOk.run();
            return;
        }

        // ✅ "없음/거부"로 취급하고 생성 로직으로 넘어가는 경우
        // iotcoss/mobius 환경에서 403이 "없음처럼" 나오는 경우가 있어 같이 처리
        if (code == 404 || code == 403) {
            if (onNotOk != null) onNotOk.run();
            return;
        }

        // 그 외는 일단 로그만 (원하면 Toast/에러콜백 추가 가능)
        Log.e(TAG, "Unhandled HTTP code=" + code);
    }

    @Override
    public void onFailure(Call<Object> call, Throwable t) {
        Log.e(TAG, "Network failure: " + t.getMessage(), t);
    }
}
