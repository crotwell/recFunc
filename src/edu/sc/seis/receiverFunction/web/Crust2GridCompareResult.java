package edu.sc.seis.receiverFunction.web;

import java.text.DecimalFormat;
import java.util.List;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;

public class Crust2GridCompareResult {

    float hAvg;

    float kAvg;

    float lat;

    float lon;

    List stationSummary;

    Crust2Profile profile;

    public Crust2GridCompareResult(float hAvg,
                                   float kAvg,
                                   float lat,
                                   float lon,
                                   List stationSummary,
                                   Crust2Profile profile) {
        super();
        // TODO Auto-generated constructor stub
        this.hAvg = hAvg;
        this.kAvg = kAvg;
        this.lat = lat;
        this.lon = lon;
        this.stationSummary = stationSummary;
        this.profile = profile;
    }

    public float getHAvg() {
        return hAvg;
    }
    
    public String formatHAvg() {
        return formatter.format(getHAvg());
    }

    public float getKAvg() {
        return kAvg;
    }
    
    public String formatKAvg() {
        return formatter.format(getKAvg());
    }

    public Crust2Profile getProfile() {
        return profile;
    }

    public List getStationSummary() {
        return stationSummary;
    }

    public float getLat() {
        return lat;
    }

    public float getLon() {
        return lon;
    }
    
    DecimalFormat formatter = new DecimalFormat("0.##");
}
