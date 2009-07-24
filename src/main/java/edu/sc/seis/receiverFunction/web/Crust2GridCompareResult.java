package edu.sc.seis.receiverFunction.web;

import java.text.DecimalFormat;
import java.util.List;

import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;

public class Crust2GridCompareResult {

    float[] hVals;

    float[] kVals;
    
	Statistics hStat;
	
	Statistics kStat;

    float lat;

    float lon;

    List stationSummary;

    Crust2Profile profile;

    public Crust2GridCompareResult(float[] hVals,
                                   float[] kVals,
                                   float lat,
                                   float lon,
                                   List stationSummary,
                                   Crust2Profile profile) {
        super();
        // TODO Auto-generated constructor stub
        this.hVals = hVals;
        this.kVals = kVals;
		hStat = new Statistics(hVals);
		kStat = new Statistics(kVals);
        this.lat = lat;
        this.lon = lon;
        this.stationSummary = stationSummary;
        this.profile = profile;
    }

    public float getHAvg() {
        return (float)hStat.mean();
    }
    
    public String formatHAvg() {
        return formatter.format(getHAvg());
    }

    public float getKAvg() {
        return (float)kStat.mean();
    }
    
    public String formatKAvg() {
        return formatter.format(getKAvg());
    }


    public float getHStdDev() {
        if (getStationSummary().size() >1) {
            return (float)hStat.stddev();
        }
        return 99999;
    }
    
    public String formatHStdDev() {
        return formatter.format(getHStdDev());
    }

    public float getKStdDev() {
        if (getStationSummary().size() >1) {
            return (float)kStat.stddev();
        }
        return 99999;
    }
    
    public String formatKStdDev() {
        return formatter.format(getKStdDev());
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
