package edu.sc.seis.receiverFunction.server;

import java.sql.Timestamp;

public class RecFuncQCResult {

    public RecFuncQCResult(int recFunc_id, boolean keep, boolean manualOverride, float transRadialRatio, float pMaxAmpRatio, String reason, Timestamp insertTime) {
        super();
        // TODO Auto-generated constructor stub
        this.recFunc_id = recFunc_id;
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

    public int getRecFunc_id() {
        return recFunc_id;
    }

    public float getPMaxAmpRatio() {
        return pMaxAmpRatio;
    }

    
    public float getTransRadialRatio() {
        return transRadialRatio;
    }
    
    private int recFunc_id;

    private boolean keep;

    private boolean manualOverride;

    private float transRadialRatio;
    
    private float pMaxAmpRatio;
    
    private String reason;

    private Timestamp insertTime;

    
    
}
