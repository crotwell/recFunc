package edu.sc.seis.receiverFunction.server;

import edu.iris.Fissures.model.QuantityImpl;


public class SummaryLine {
    
    
    public SummaryLine(String netCodeWithYear,
                       String staCode,
                       float lat,
                       float lon,
                       QuantityImpl elevation,
                       QuantityImpl crustalThickness,
                       QuantityImpl crustalThicknessStdDev,
                       float crustalVpVs,
                       float crustalVpVsStdDev,
                       QuantityImpl vp,
                       QuantityImpl vs,
                       float poissonsRatio,
                       int numEarthquakes,
                       float complexity) {
        super();
        this.netCodeWithYear = netCodeWithYear;
        this.staCode = staCode;
        this.lat = lat;
        this.lon = lon;
        this.elevation = elevation;
        this.crustalThickness = crustalThickness;
        this.crustalThicknessStdDev = crustalThicknessStdDev;
        this.crustalVpVs = crustalVpVs;
        this.crustalVpVsStdDev = crustalVpVsStdDev;
        this.vp = vp;
        this.vs = vs;
        this.poissonsRatio = poissonsRatio;
        this.numEarthquakes = numEarthquakes;
        this.complexity = complexity;
    }
    
    String netCodeWithYear;
    String staCode;
    float lat;
    float lon;
    QuantityImpl elevation;
    QuantityImpl crustalThickness;
    QuantityImpl crustalThicknessStdDev;
    float crustalVpVs;
    float crustalVpVsStdDev;
    QuantityImpl vp;
    QuantityImpl vs;
    float poissonsRatio;
    int numEarthquakes;
    float complexity;
}
