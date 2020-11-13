package edu.sc.seis.receiverFunction.hibernate;

import java.sql.Timestamp;
import java.time.Instant;

import edu.sc.seis.sod.util.time.ClockUtil;

public class RejectedMaxima {

    
    /** hibernate */
    protected RejectedMaxima() {}
    
    public RejectedMaxima(String net,
                          String staCode,
                          float hMin,
                          float hMax,
                          float kMin,
                          float kMax,
                          String reason) {
        super();
        this.net = net;
        this.staCode = staCode;
        this.hMin = hMin;
        this.hMax = hMax;
        this.kMin = kMin;
        this.kMax = kMax;
        this.reason = reason;
        this.insertTime = ClockUtil.now();
    }

    public boolean inside(float h, float k) {
        return h >= hMin && h <= hMax && k >= kMin && k <= kMax;
    }

    public String getNet() {
        return net;
    }

    public void setNet(String net) {
        this.net = net;
    }

    public String getStaCode() {
        return staCode;
    }

    public void setStaCode(String staCode) {
        this.staCode = staCode;
    }

    public float getHMin() {
        return hMin;
    }

    public void setHMin(float min) {
        hMin = min;
    }

    public float getHMax() {
        return hMax;
    }

    public void setHMax(float max) {
        hMax = max;
    }

    public float getKMin() {
        return kMin;
    }

    public void setKMin(float min) {
        kMin = min;
    }

    public float getKMax() {
        return kMax;
    }

    public void setKMax(float max) {
        kMax = max;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getDbid() {
        return dbid;
    }

    protected void setDbid(int dbid) {
        this.dbid = dbid;
    }

    public Instant getInsertTime() {
        return insertTime;
    }

    protected void setInsertTime(Instant insertTime) {
        this.insertTime = insertTime;
    }

    int dbid;

    String net;

    String staCode;

    float hMin;

    float hMax;

    float kMin;

    float kMax;

    String reason;

    Instant insertTime;
}
