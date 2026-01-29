package com.bic.byeplug.network;

import com.bic.byeplug.model.AeRequest;
import com.bic.byeplug.model.CntRequest;
import com.bic.byeplug.secret.OneM2MSecret;

public class DeviceProvisioner {

    private final OneM2MService api;

    public DeviceProvisioner(OneM2MService api) {
        this.api = api;
    }

    /**
     * deviceId(AE)가 존재하고 CONTROL/STATUS가 준비됐음을 보장
     */
    public void ensureDeviceTree(String deviceId, Runnable onReady) {
        api.getAe("Mobius", deviceId, OneM2MHeaders.base())
                .enqueue(new SimpleCallback(
                        // AE 있음
                        () -> ensureContainers(deviceId, onReady),
                        // AE 없음 → 생성
                        () -> createAeThenContainers(deviceId, onReady)
                ));
    }

    private void createAeThenContainers(String deviceId, Runnable onReady) {
        AeRequest body = new AeRequest(deviceId, "NByePlug", true, OneM2MSecret.RVI);

        api.createAe(
                "Mobius",
                OneM2MHeaders.withTy(2),
                body
        ).enqueue(new SimpleCallback(
                () -> ensureContainers(deviceId, onReady),
                () -> ensureContainers(deviceId, onReady) // 409 포함
        ));
    }

    private void ensureContainers(String deviceId, Runnable onReady) {
        ensureOneContainer(deviceId, "CONTROL",
                () -> ensureOneContainer(deviceId, "STATUS", onReady));
    }

    private void ensureOneContainer(String deviceId, String cnt, Runnable next) {
        api.getCnt(
                "Mobius",
                deviceId,
                cnt,
                OneM2MHeaders.base()
        ).enqueue(new SimpleCallback(
                next,
                () -> api.createCnt(
                        "Mobius",
                        deviceId,
                        OneM2MHeaders.withTy(3),
                        new CntRequest(cnt)
                ).enqueue(new SimpleCallback(next, null))
        ));
    }
}
