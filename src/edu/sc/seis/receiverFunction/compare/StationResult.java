package edu.sc.seis.receiverFunction.compare;

import java.text.DecimalFormat;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.bag.PoissonsRatio;
import edu.sc.seis.sod.status.FissuresFormatter;

/**
 * Stores the crustal thickness and vp/vs ratio for a station from a previous
 * study.
 * 
 * @author crotwell Created on Dec 7, 2004
 */
public class StationResult {

    public StationResult(NetworkId networkId,
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
     
    public StationResult(NetworkId networkId,
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

    public StationResult(NetworkId networkId,
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

    public StationResult(NetworkId networkId,
                         String stationCode,
                         QuantityImpl h,
                         float vpVs,
                         QuantityImpl vp,
                         float amp,
                         QuantityImpl hStdDev,
                         float kStdDev,
                         StationResultRef ref,
                         String extras) {
        this.h = h;
        h.setFormat(depthFormat);
        this.vp = vp;
        this.vpVs = vpVs;
        this.amp = amp;
        this.hStdDev = hStdDev;
        this.kStdDev = kStdDev;
        this.networkId = networkId;
        this.ref = ref;
        this.stationCode = stationCode;
        this.extras = extras;
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

    public NetworkId getNetworkId() {
        return networkId;
    }

    public StationResultRef getRef() {
        return ref;
    }

    public String getStationCode() {
        return stationCode;
    }

    public String getExtras() {
        return extras;
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

    /**
     * Same as getExtras() except all commas are removed. Mainly for use in CSV
     * output.
     */
    public String formatExtras() {
        return extras.replaceAll(",", "");
    }

    public String toString() {
        return "H=" + formatH() + " Vp=" + formatVp() + " Vs=" + formatVs()
                + " Vp/Vs=" + formatVpVs() + " pr=" + formatPoissonsRatio();
    }

    private String stationCode;

    private StationResultRef ref;

    private NetworkId networkId;

    private QuantityImpl h;

    private float vpVs;

    private QuantityImpl vp;

    private float amp;

    private QuantityImpl hStdDev;

    private float kStdDev;

    private String extras;

    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");

    private static DecimalFormat depthFormat = new DecimalFormat("0.##");

    public void setHStdDev(QuantityImpl stdDev) {
        hStdDev = stdDev;
    }

    public void setKStdDev(float stdDev) {
        this.kStdDev = stdDev;
    }
}