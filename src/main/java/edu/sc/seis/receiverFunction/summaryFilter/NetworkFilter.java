package edu.sc.seis.receiverFunction.summaryFilter;

import edu.sc.seis.receiverFunction.server.SummaryLine;


public class NetworkFilter implements SummaryLineFilter {

    public NetworkFilter(String netCodeWithYear) {
        this.netCodeWithYear = netCodeWithYear;
    }
    
    public boolean accept(SummaryLine line) {
        return line.getNetCodeWithYear().equals(netCodeWithYear);
    }
    
    String netCodeWithYear;
}
