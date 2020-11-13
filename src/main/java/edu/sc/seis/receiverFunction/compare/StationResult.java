package edu.sc.seis.receiverFunction.compare;

import edu.sc.seis.receiverFunction.HKAlpha;
import edu.sc.seis.sod.model.common.QuantityImpl;
import edu.sc.seis.sod.model.common.UnitImpl;

/**
 * Stores the crustal thickness and vp/vs ratio for a station from a previous
 * study.
 * 
 * @author crotwell Created on Dec 7, 2004
 */
public class StationResult extends HKAlpha {

    
    /** for hibernate */
    protected StationResult() {}
    
    public StationResult(String networkId,
                         String stationCode,
                         QuantityImpl h,
                         float vpVs,
                         QuantityImpl vp,
                         StationResultRef ref) {
        this(networkId,
             stationCode,
             h,
             vpVs,
             vp,
             0,
             new QuantityImpl(0, UnitImpl.KILOMETER),
             0,
             ref,
             "");
    }
     
    public StationResult(String networkId,
                         String stationCode,
                         QuantityImpl h,
                         float vpVs,
                         QuantityImpl vp,
                         StationResultRef ref,
                         String extras) {
        this(networkId,
             stationCode,
             h,
             vpVs,
             vp,
             0,
             new QuantityImpl(0, UnitImpl.KILOMETER),
             0,
             ref,
             extras);
    }

    public StationResult(String networkId,
                         String stationCode,
                         QuantityImpl h,
                         float vpVs,
                         QuantityImpl vp,
                         float amp,
                         QuantityImpl hStdDev,
                         float kStdDev,
                         StationResultRef ref) {
        this(networkId,
             stationCode,
             h,
             vpVs,
             vp,
             amp,
             hStdDev,
             kStdDev,
             ref,
             "");
    }

    public StationResult(String networkId,
                         String stationCode,
                         QuantityImpl h,
                         float vpVs,
                         QuantityImpl vp,
                         float amp,
                         QuantityImpl hStdDev,
                         float kStdDev,
                         StationResultRef ref,
                         String extras) {
        super(h, vpVs, vp, amp, hStdDev, kStdDev);
        this.networkId = networkId;
        this.ref = ref;
        this.stationCode = stationCode;
        this.extras = extras;
    }

    public String getNet() {
        return networkId;
    }

    public StationResultRef getRef() {
        return ref;
    }

    public String getStaCode() {
        return stationCode;
    }

    public String getExtras() {
        return extras;
    }

    /**
     * Same as getExtras() except all commas are removed. Mainly for use in CSV
     * output.
     */
    public String formatExtras() {
        return (getExtras()!=null)?getExtras().replaceAll(",", ""):"";
    }

    private String stationCode;

    private StationResultRef ref;

    private String networkId;

    private String extras;

    private int dbid;

    public int getDbid() {
        return dbid;
    }
    
    public void setHStdDev(QuantityImpl stdDev) {
        hStdDev = stdDev;
    }

    /** dumb hibernate can't find getKStdDev() */
    public float getkStdDev() {
        return getKStdDev();
    }
    
    /** dumb hibernate can't find setKStdDev() */
    public void setkStdDev(float stdDev) {
        setKStdDev(stdDev);
    }
    
    public void setKStdDev(float stdDev) {
        this.kStdDev = stdDev;
    }

    
    public void setStaCode(String stationCode) {
        this.stationCode = stationCode;
    }

    
    protected void setRef(StationResultRef ref) {
        this.ref = ref;
    }

    
    public void setNet(String networkId) {
        this.networkId = networkId;
    }
    
    protected void setHkm(float h) {
        this.h = new QuantityImpl(h, UnitImpl.KILOMETER);
    }
    
    protected void setH(QuantityImpl h) {
        this.h = h;
    }

    
    protected void setVpVs(float vpVs) {
        this.vpVs = vpVs;
    }

    
    public void setVp(QuantityImpl vp) {
        this.vp = vp;
    }
    
    public void setVpkm(float vp) {
        setVp(new QuantityImpl(vp, UnitImpl.KILOMETER_PER_SECOND));
    }
    
    protected void setAmp(float amp) {
        this.amp = amp;
    }

    
    protected void setExtras(String extras) {
        this.extras = extras;
    }

    
    protected void setDbid(int dbid) {
        this.dbid = dbid;
    }
    
    
}