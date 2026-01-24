package com.bic.byeplug.model;

import com.google.gson.annotations.SerializedName;

public class CntRequest {

    @SerializedName("m2m:cnt")
    public Cnt cnt;

    public CntRequest(String rn) {
        this.cnt = new Cnt(rn, 16384);
    }

    public static class Cnt {
        public String rn; // 컨테이너 이름
        public int mbs;   // max byte size

        public Cnt(String rn, int mbs) {
            this.rn = rn;
            this.mbs = mbs;
        }
    }
}
