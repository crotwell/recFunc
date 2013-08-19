package edu.sc.seis.receiverFunction.summaryFilter;

import java.util.ArrayList;
import java.util.List;

import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.server.SummaryLine;


public class PriorResultsFilter implements SummaryLineFilter {

    public PriorResultsFilter(String name) {
        this.name = name;
        if( ! name.equalsIgnoreCase("crust2.0")) {
            results = RecFuncDB.getSingleton().getPriorResults(name);
        }
    }
    
    public boolean accept(SummaryLine line) {
        if(name.equalsIgnoreCase("crust2.0")) {
            return true;
        } else {
            for(StationResult stationResult : results) {
                if (line.getNetCodeWithYear().equals(NetworkIdUtil.toStringNoDates(stationResult.getNet()))
                        && line.getStaCode().equals(stationResult.getStaCode())) {
                    return true;
                }
            }
            return false;
        }
    }
    
    String name;
    
    List<StationResult> results = new ArrayList<StationResult>();
}
