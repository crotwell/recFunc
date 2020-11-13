package edu.sc.seis.receiverFunction;

import java.text.DecimalFormat;

import edu.sc.seis.sod.bag.PoissonsRatio;
import edu.sc.seis.sod.model.common.QuantityImpl;
import edu.sc.seis.sod.model.common.UnitImpl;

public class HKAlpha {

    /** for hibernate */
    protected HKAlpha() {}
    
    public HKAlpha(QuantityImpl h,
                   float vpVs,
                   QuantityImpl vp) {
        this(h, vpVs, vp, 0, ZERO_KM, 0);
    }
    
    public HKAlpha(QuantityImpl h,
                   float vpVs,
                   QuantityImpl vp,
                   float amp,
                   QuantityImpl hStdDev,
                   float kStdDev) {
        super();
        this.h = h;
        h.setFormat(depthFormat);
        this.vpVs = vpVs;
        this.vp = vp;
        this.amp = amp;
        this.hStdDev = hStdDev;
        this.kStdDev = kStdDev;
    }

    public String formatH() {
        return getH().formatValue("0.00");
    }

    public String formatVpVs() {
        return vpvsFormat.format(getVpVs());
    }

    public String formatVp() {
        return getVp().formatValue("0.00");
    }

    public String formatVs() {
        return getVs().formatValue("0.00");
    }

    public String formatPoissonsRatio() {
        return vpvsFormat.format(PoissonsRatio.calcPoissonsRatio(getVpVs()));
    }

    public String formatAmp() {
        return vpvsFormat.format(getAmp());
    }

    public String formatHStdDev() {
        return getHStdDev().formatValue("0.0000");
    }

    public String formatKStdDev() {
        return vpvsFormat.format(getKStdDev());
    }

    public QuantityImpl getH() {
        if ( h == null) {
            // side effect, this forces hibernate to load hkm from the database and call setHkm which sets h
            getHkm();
        }
        return h;
    }

    public float getHkm() {
        return (float)h.getValue(UnitImpl.KILOMETER);
    }

    public float getVpVs() {
        return vpVs;
    }

    public QuantityImpl getVp() {
        return vp;
    }

    public QuantityImpl getVs() {
        if (getVp() == null) {throw new NullPointerException("getVp is null");}
        return getVp().dividedByDbl((double)getVpVs());
    }

    public float getAmp() {
        return amp;
    }

    public QuantityImpl getHStdDev() {
        return hStdDev;
    }

    public float getKStdDev() {
        return kStdDev;
    }
    
    public String toString() {
        return "H=" + formatH() + " Vp=" + formatVp() + " Vs=" + formatVs()
                + " Vp/Vs=" + formatVpVs() + " pr=" + formatPoissonsRatio();
    }
    
    protected QuantityImpl h;

    protected float vpVs;

    protected QuantityImpl vp;

    protected float amp;

    protected QuantityImpl hStdDev;

    protected float kStdDev;

    public static final DecimalFormat vpvsFormat = new DecimalFormat("0.00");

    public static final DecimalFormat depthFormat = new DecimalFormat("0.##");

    private static final QuantityImpl ZERO_KM = new QuantityImpl(0, UnitImpl.KILOMETER);
}
