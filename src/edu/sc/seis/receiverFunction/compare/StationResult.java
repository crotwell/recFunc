package edu.sc.seis.receiverFunction.compare;

import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.StationId;

/**
 * Stores the crustal thickness and vp/vs ratio for a station from a previous study.
 * @author crotwell Created on Dec 7, 2004
 */
public class StationResult {


    public StationResult(NetworkId networkId, String stationCode, float h, float vpVs, float vp, StationResultRef ref) {
        this.h = h;
        this.vp = vp;
        this.vpVs = vpVs;
        this.networkId = networkId;
        this.ref = ref;
        this.stationCode = stationCode;
    }
    
    public float getH() {
        return h;
    }

    public float getVpVs() {
        return vpVs;
    }
    
    public float getVp() {
        return vp;
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

    private float h;

    private float vpVs;
    
    private float vp;
}