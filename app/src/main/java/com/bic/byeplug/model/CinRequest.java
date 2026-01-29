package com.bic.byeplug.model;

import com.google.gson.annotations.SerializedName;

public class CinRequest {

    @SerializedName("m2m:cin")
    public M2mCin m2mCin;

    public CinRequest(Object con) {
        this.m2mCin = new M2mCin(con);
    }

    public static class M2mCin {
        public Object con;
        public M2mCin(Object con) { this.con = con; }
    }
}
