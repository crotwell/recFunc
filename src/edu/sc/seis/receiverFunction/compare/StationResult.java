package edu.sc.seis.receiverFunction.compare;

import edu.iris.Fissures.IfNetwork.StationId;

/**
 * Stores the crustal thickness and vp/vs ratio for a station from a previous study.
 * @author crotwell Created on Dec 7, 2004
 */
public class StationResult {

    public StationResult(StationId stationId, float h, float vpVs) {
        this.h = h;
        this.vpVs = vpVs;
        this.stationId = stationId;
    }
    
    public float getH() {
        return h;
    }

    public float getVpVs() {
        return vpVs;
    }

    public StationId getStationId() {
        return stationId;
    }

    private StationId stationId;

    private float h;

    private float vpVs;
}