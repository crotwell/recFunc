package edu.sc.seis.receiverFunction.server;

import edu.iris.Fissures.model.QuantityImpl;
import edu.sc.seis.receiverFunction.HKAlpha;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/** also see macro(stationListCSV in VM_global_library.vm */

public class SummaryLine extends HKAlpha {
    
    
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
                       int numEarthquakes,
                       float complexityResidual) {
        super(crustalThickness, crustalVpVs, vp, 1, crustalThicknessStdDev, crustalVpVsStdDev);
        this.netCodeWithYear = netCodeWithYear;
        this.staCode = staCode;
        this.lat = lat;
        this.lon = lon;
        this.elevation = elevation;
        this.numEarthquakes = numEarthquakes;
        this.complexityResidual = complexityResidual;
    }
    
    String netCodeWithYear;
    String staCode;
    float lat;
    float lon;
    QuantityImpl elevation;
    int numEarthquakes;
    float complexityResidual;
    
    public String getNetCodeWithYear() {
        return netCodeWithYear;
    }
    
    public String getStaCode() {
        return staCode;
    }
    
    public float getLat() {
        return lat;
    }
    
    public String getOrientedLatitude() {
        return VelocityStation.getOrientedLatitude(getLat());
    }
    
    public float getLon() {
        return lon;
    }

    public String getOrientedLongitude() {
        return VelocityStation.getOrientedLongitude(getLon());
    }
    
    public QuantityImpl getElevation() {
        return elevation;
    }
    
    public int getNumEarthquakes() {
        return numEarthquakes;
    }
    
    public float getComplexityResidual() {
        return complexityResidual;
    }

    public String formatComplexityResidual() {
        return HKAlpha.vpvsFormat.format(getComplexityResidual());
    }
}
