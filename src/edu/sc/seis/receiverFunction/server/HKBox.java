package edu.sc.seis.receiverFunction.server;

import java.sql.Timestamp;

public class HKBox {

    public HKBox(float hMin, float hMax, float kMin, float kMax, String reason, Timestamp insertTime) {
        this.hMin = hMin;
        this.hMax = hMax;
        this.kMin = kMin;
        this.kMax = kMax;
        this.reason = reason;
        this.insertTime = insertTime;
    }
    
    public boolean inside(float h, float k) {
        return h >= hMin && h <= hMax && k >= kMin && k <= kMax;
    }

    public float getHMax() {
        return hMax;
    }

    public float getHMin() {
        return hMin;
    }

    public float getKMax() {
        return kMax;
    }

    public float getKMin() {
        return kMin;
    }
    
    public Timestamp getInsertTime() {
        return insertTime;
    }
    
    public String getReason() {
        return reason;
    }

    private float hMin, hMax, kMin, kMax;
    private String reason;
    private Timestamp insertTime;
}
