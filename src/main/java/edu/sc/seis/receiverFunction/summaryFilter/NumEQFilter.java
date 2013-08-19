package edu.sc.seis.receiverFunction.summaryFilter;

import edu.sc.seis.receiverFunction.server.SummaryLine;


public class NumEQFilter implements SummaryLineFilter {

    public NumEQFilter(int minEQ) {
        this.minEQ = minEQ;
    }
    
    public boolean accept(SummaryLine line) {
        return line.getNumEarthquakes() >= minEQ;
    }
    
    int minEQ;
}
