package com.bic.byeplug.model;

public class DeviceItem {
    public final String deviceId; // = AE 이름 (제품 번호)
    public final String name;     // 표시 이름
    public final String status;   // ONLINE/OFFLINE 등

    public DeviceItem(String deviceId, String name, String status) {
        this.deviceId = deviceId;
        this.name = name;
        this.status = status;
    }
}
