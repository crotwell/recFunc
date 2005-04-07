package edu.sc.seis.receiverFunction.compare;

import java.text.DecimalFormat;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.sc.seis.fissuresUtil.bag.PoissonsRatio;

/**
 * Stores the crustal thickness and vp/vs ratio for a station from a previous study.
 * @author crotwell Created on Dec 7, 2004
 */
public class StationResult {


    public StationResult(NetworkId networkId, String stationCode, QuantityImpl h, float vpVs, float vp, StationResultRef ref) {
        this.h = h;
        h.setFormat(depthFormat);
        this.vp = vp;
        this.vpVs = vpVs;
        this.networkId = networkId;
        this.ref = ref;
        this.stationCode = stationCode;
    }

    public String formatH() {
        return getH().toString();
    }
    
    public String formatVpVs() {
        return vpvsFormat.format(getVpVs());
    }
    
    public String formatVp() {
        return vpvsFormat.format(getVp());
    }
    
    public String formatVs() {
        return vpvsFormat.format(getVs());
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
    
    public float getVp() {
        return vp;
    }
    
    public float getVs() {
        return getVp()/getVpVs();
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
    
    private float vp;
    
    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");

    private static DecimalFormat depthFormat = new DecimalFormat("0.##");
    
}