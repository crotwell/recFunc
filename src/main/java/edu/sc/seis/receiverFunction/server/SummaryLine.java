package edu.sc.seis.receiverFunction.server;

import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.receiverFunction.HKAlpha;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/** also see macro(stationListCSV in VM_global_library.vm */

public class SummaryLine extends HKAlpha {
    
    public SummaryLine(SumHKStack stack) {
        super(stack.getBest().getH(), 
              stack.getBest().getVpVs(), 
              stack.getBest().getVp(), 
              1, 
              stack.getBest().getHStdDev(), 
              stack.getBest().getKStdDev());
        VelocityStation sta = new VelocityStation(NetworkDB.getSingleton().getStationByCodes(stack.getNet().get_code(), stack.getStaCode()).get(0));
        this.netCodeWithYear = sta.getNet().getCodeWithYear();
        this.staCode = sta.getCode();
        this.lat = sta.getFloatLatitude();
        this.lon = sta.getFloatLongitude();
        this.elevation = QuantityImpl.createQuantityImpl(sta.getLocation().elevation);
        this.numEarthquakes = stack.getNumEQ();
        this.complexityResidual = stack.getComplexityResidual();
    }
    
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
    StationResult prior;
    
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
    
    public StationResult getPrior() {
        return prior;
    }
    
    public void setPrior(StationResult r) {
        this.prior = r;
    }
}
