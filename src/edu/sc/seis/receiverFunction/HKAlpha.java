package edu.sc.seis.receiverFunction;

import java.text.DecimalFormat;

import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.bag.PoissonsRatio;
import edu.sc.seis.sod.status.FissuresFormatter;

public class HKAlpha {

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
        return FissuresFormatter.formatDepth(getH());
    }

    public String formatVpVs() {
        return vpvsFormat.format(getVpVs());
    }

    public String formatVp() {
        return FissuresFormatter.formatQuantity(getVp());
    }

    public String formatVs() {
        return FissuresFormatter.formatQuantity(getVs());
    }

    public String formatPoissonsRatio() {
        return vpvsFormat.format(PoissonsRatio.calcPoissonsRatio(getVpVs()));
    }

    public String formatAmp() {
        return vpvsFormat.format(getAmp());
    }

    public String formatHStdDev() {
        return FissuresFormatter.formatDepth(getHStdDev());
    }

    public String formatKStdDev() {
        return vpvsFormat.format(getKStdDev());
    }

    public QuantityImpl getH() {
        return h;
    }

    public float getVpVs() {
        return vpVs;
    }

    public QuantityImpl getVp() {
        return vp;
    }

    public QuantityImpl getVs() {
        return getVp().divideBy(getVpVs());
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

    protected static DecimalFormat vpvsFormat = new DecimalFormat("0.00");

    protected static DecimalFormat depthFormat = new DecimalFormat("0.##");

    private static final QuantityImpl ZERO_KM = new QuantityImpl(0, UnitImpl.KILOMETER);
}
