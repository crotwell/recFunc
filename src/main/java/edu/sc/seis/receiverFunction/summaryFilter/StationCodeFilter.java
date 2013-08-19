package edu.sc.seis.receiverFunction.summaryFilter;

import edu.sc.seis.receiverFunction.server.SummaryLine;


public class StationCodeFilter implements SummaryLineFilter {


    public StationCodeFilter(String staCode) {
        this.staCode = staCode;
    }
    
    public boolean accept(SummaryLine line) {
        return line.getStaCode().equals(staCode);
    }
    
    private String staCode;
}
