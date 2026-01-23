package com.bic.byeplug.network;

import com.bic.byeplug.model.CinRequest;
import retrofit2.Call;
import retrofit2.http.*;

public interface OneM2MService {

    // 상태 조회 (la = latest)
    @GET("{cse}/{ae}/{cnt}/la")
    Call<Object> getLatestCin(
            @Path("cse") String cse,
            @Path("ae") String ae,
            @Path("cnt") String container,
            @Header("X-M2M-Origin") String origin,
            @Header("X-M2M-RI") String ri,
            @Header("X-M2M-RVI") String rvi,
            @Header("X-API-KEY") String apiKey,
            @Header("X-AUTH-CUSTOM-LECTURE") String lecture,
            @Header("X-AUTH-CUSTOM-CREATOR") String creator
    );

    // 명령 전송
    @POST("{cse}/{ae}/{cnt}")
    Call<Object> postControl(
            @Path("cse") String cse,
            @Path("ae") String ae,
            @Path("cnt") String container,
            @Header("X-M2M-Origin") String origin,
            @Header("X-M2M-RI") String ri,
            @Header("X-M2M-RVI") String rvi,
            @Header("X-API-KEY") String apiKey,
            @Header("X-AUTH-CUSTOM-LECTURE") String lecture,
            @Header("X-AUTH-CUSTOM-CREATOR") String creator,
            @Header("Content-Type") String contentType,
            @Body CinRequest body
    );
}
