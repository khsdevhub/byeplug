package com.bic.byeplug.network;

import com.bic.byeplug.model.CinRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;

import com.bic.byeplug.model.AeRequest;
import com.bic.byeplug.model.CntRequest;

public interface OneM2MService {

    // AE 조회
    @GET("{cse}/{ae}")
    Call<Object> getAe(
            @Path("cse") String cse,
            @Path("ae") String ae,
            @HeaderMap Map<String, String> headers
    );

    // AE 생성 (POST /Mobius, ty=2)
    @POST("{cse}")
    Call<Object> createAe(
            @Path("cse") String cse,
            @HeaderMap Map<String, String> headers,
            @Body AeRequest body
    );

    // CNT 조회
    @GET("{cse}/{ae}/{cnt}")
    Call<Object> getCnt(
            @Path("cse") String cse,
            @Path("ae") String ae,
            @Path("cnt") String cnt,
            @HeaderMap Map<String, String> headers
    );

    // CNT 생성 (POST /Mobius/{AE}, ty=3)
    @POST("{cse}/{ae}")
    Call<Object> createCnt(
            @Path("cse") String cse,
            @Path("ae") String ae,
            @HeaderMap Map<String, String> headers,
            @Body CntRequest body
    );

    // 상태 조회 (la = latest)
    @GET("{cse}/{ae}/{cnt}/la")
    Call<Object> getLatestCin(
            @Path("cse") String cse,
            @Path("ae") String ae,
            @Path("cnt") String cnt,
            @HeaderMap Map<String, String> headers
    );

    // 명령 전송
    @POST("{cse}/{ae}/{cnt}")
    Call<Object> postControl(
            @Path("cse") String cse,
            @Path("ae") String ae,
            @Path("cnt") String cnt,
            @HeaderMap Map<String, String> headers,
            @Body CinRequest body
    );
}
