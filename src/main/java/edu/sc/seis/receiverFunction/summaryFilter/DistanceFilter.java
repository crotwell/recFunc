package edu.sc.seis.receiverFunction.summaryFilter;

import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.receiverFunction.server.SummaryLine;


public class DistanceFilter implements SummaryLineFilter {

    public DistanceFilter(float lat, float lon, float degrees) {
        this.lat = lat;
        this.lon = lon;
        this.degrees = degrees;
    }
    public boolean accept(SummaryLine line) {
        DistAz distAz = new DistAz(lat, lon, line.getLat(), line.getLon());
        return distAz.getDelta() <= degrees;
    }
    
    float lat;
    float lon;
    float degrees;
}
