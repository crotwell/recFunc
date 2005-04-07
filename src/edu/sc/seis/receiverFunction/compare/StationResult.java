package edu.sc.seis.receiverFunction.compare;

import java.text.DecimalFormat;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.sc.seis.fissuresUtil.bag.PoissonsRatio;
import edu.sc.seis.sod.status.FissuresFormatter;

/**
 * Stores the crustal thickness and vp/vs ratio for a station from a previous study.
 * @author crotwell Created on Dec 7, 2004
 */
public class StationResult {


    public StationResult(NetworkId networkId, String stationCode, QuantityImpl h, float vpVs, QuantityImpl vp, StationResultRef ref) {
        this.h = h;
        h.setFormat(depthFormat);
        this.vp = vp;
        this.vpVs = vpVs;
        this.networkId = networkId;
        this.ref = ref;
        this.stationCode = stationCode;
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

    private String stationCode;
    
    private StationResultRef ref;

    private NetworkId networkId;

    private QuantityImpl h;

    private float vpVs;
    
    private QuantityImpl vp;
    
    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");

    private static DecimalFormat depthFormat = new DecimalFormat("0.##");
    
}