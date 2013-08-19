package edu.sc.seis.receiverFunction.summaryFilter;

import edu.sc.seis.receiverFunction.server.SummaryLine;


public class LatLonBoxFilter implements SummaryLineFilter {

    public LatLonBoxFilter(float minLat, float maxLat, float minLon, float maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }
    
    public boolean accept(SummaryLine line) {
        return (line.getLat() >= minLat
                && line.getLat() <= maxLat
                && line.getLon() >= minLon
                && line.getLon() <= maxLon);
    }
    
    float minLat;
    float maxLat;
    float minLon;
    float maxLon;
}
