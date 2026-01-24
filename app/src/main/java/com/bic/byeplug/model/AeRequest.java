package com.bic.byeplug.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

public class AeRequest {

    @SerializedName("m2m:ae")
    public Ae ae;

    public AeRequest(String rn, String api, boolean rr, String rvi) {
        this.ae = new Ae(rn, api, rr, Collections.singletonList(rvi));
    }

    public static class Ae {
        public String rn;      // AE 이름(=제품번호)
        public String api;     // App ID
        public boolean rr;     // request reachable
        public List<String> srv; // supported release versions

        public Ae(String rn, String api, boolean rr, List<String> srv) {
            this.rn = rn;
            this.api = api;
            this.rr = rr;
            this.srv = srv;
        }
    }
}
