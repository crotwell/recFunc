package edu.sc.seis.receiverFunction.summaryFilter;

import edu.sc.seis.receiverFunction.server.SummaryLine;


public interface SummaryLineFilter {
    
    public boolean accept(SummaryLine line);
    
}
