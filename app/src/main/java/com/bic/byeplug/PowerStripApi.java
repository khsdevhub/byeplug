package com.bic.byeplug;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

class OneM2MHeaders {
    public static Map<String, String> create() {
        Map<String, String> h = new HashMap<>();
        h.put("X-M2M-Origin", "SOrigin_BIC_t1");   // ORIGIN
        h.put("X-M2M-RI", "app-" + System.currentTimeMillis());
        h.put("X-M2M-RVI", "2a");
        h.put("X-API-KEY", "secret");
        h.put("X-AUTH-CUSTOM-LECTURE", "secret");
        h.put("X-AUTH-CUSTOM-CREATOR", "secret");
        h.put("Accept", "application/json");
        return h;
    }
}

public interface PowerStripApi {
    static final String BASE_URL =
            "https://onem2m.iotcoss.ac.kr:433/";

    @GET("api/powerstrip/{id}")
    Call<PowerStrip> getStatus(@Path("id") String id);

    @POST("api/powerstrip/{id}/outlet/{num}")
    Call<Void> setOutlet(
            @Path("id") String id,
            @Path("num") int outletNum,
            @Body Map<String, Boolean> body
    );
}
