package edu.sc.seis.receiverFunction.hibernate;

import java.sql.Timestamp;

import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;

public class RejectedMaxima {

    public RejectedMaxima(NetworkAttrImpl net,
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
        this.insertTime = ClockUtil.now().getTimestamp();
    }

    public boolean inside(float h, float k) {
        return h >= hMin && h <= hMax && k >= kMin && k <= kMax;
    }

    public NetworkAttrImpl getNet() {
        return net;
    }

    public void setNet(NetworkAttrImpl net) {
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

    public Timestamp getInsertTime() {
        return insertTime;
    }

    protected void setInsertTime(Timestamp insertTime) {
        this.insertTime = insertTime;
    }

    int dbid;

    NetworkAttrImpl net;

    String staCode;

    float hMin;

    float hMax;

    float kMin;

    float kMax;

    String reason;

    Timestamp insertTime;
}
