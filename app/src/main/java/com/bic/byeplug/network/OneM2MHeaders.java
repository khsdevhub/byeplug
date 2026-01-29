package com.bic.byeplug.network;

import com.bic.byeplug.secret.OneM2MSecret;
import java.util.HashMap;
import java.util.Map;

public class OneM2MHeaders {

    public static Map<String, String> base() {
        Map<String, String> h = new HashMap<>();
        h.put("X-M2M-Origin", OneM2MSecret.ORIGIN);
        h.put("X-M2M-RI", "app-" + System.currentTimeMillis());
        h.put("X-M2M-RVI", OneM2MSecret.RVI);
        h.put("X-API-KEY", OneM2MSecret.API_KEY);
        h.put("X-AUTH-CUSTOM-LECTURE", OneM2MSecret.LECTURE);
        h.put("X-AUTH-CUSTOM-CREATOR", OneM2MSecret.CREATOR);
        h.put("Accept", "application/json");
        return h;
    }

    public static Map<String, String> withTy(int ty) {
        Map<String, String> h = base();
        h.put("Content-Type", "application/json;ty=" + ty);
        return h;
    }
}
