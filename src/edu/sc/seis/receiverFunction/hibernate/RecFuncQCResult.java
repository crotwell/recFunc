package edu.sc.seis.receiverFunction.hibernate;

import java.sql.Timestamp;

public class RecFuncQCResult {

    public RecFuncQCResult(boolean keep,
                           boolean manualOverride,
                           float transRadialRatio,
                           float pMaxAmpRatio,
                           String reason,
                           Timestamp insertTime) {
        this.keep = keep;
        this.manualOverride = manualOverride;
        this.transRadialRatio = transRadialRatio;
        this.pMaxAmpRatio = pMaxAmpRatio;
        this.reason = reason;
        this.insertTime = insertTime;
    }

    public Timestamp getInsertTime() {
        return insertTime;
    }

    public boolean isKeep() {
        return keep;
    }

    public boolean isManualOverride() {
        return manualOverride;
    }

    public String getReason() {
        return reason;
    }

    public float getPMaxAmpRatio() {
        return pMaxAmpRatio;
    }

    public float getTransRadialRatio() {
        return transRadialRatio;
    }

    private boolean keep;

    private boolean manualOverride;

    private float transRadialRatio;

    private float pMaxAmpRatio;

    private String reason;

    private Timestamp insertTime;

    public void setKeep(boolean keep) {
        this.keep = keep;
    }

    public void setManualOverride(boolean manualOverride) {
        this.manualOverride = manualOverride;
    }

    public void setTransRadialRatio(float transRadialRatio) {
        this.transRadialRatio = transRadialRatio;
    }

    public void setPMaxAmpRatio(float maxAmpRatio) {
        pMaxAmpRatio = maxAmpRatio;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setInsertTime(Timestamp insertTime) {
        this.insertTime = insertTime;
    }
}
