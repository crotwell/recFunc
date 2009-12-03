package edu.sc.seis.receiverFunction.web;

import java.util.List;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

import edu.sc.seis.receiverFunction.server.SummaryLine;


public class StationSummaryTooltipGenerator implements XYToolTipGenerator {
    
    public StationSummaryTooltipGenerator(SummaryLineDataset dataset) {
         this(dataset.summary, dataset.xAxis, dataset.yAxis, dataset.zAxis);
    }
    
    public StationSummaryTooltipGenerator(List<SummaryLine> summary, String xAxis,
                                String yAxis, String zAxis) {
                            this.summary = summary;
                            this.xAxis = xAxis;
                            this.yAxis = yAxis;
                            this.zAxis = zAxis;
                        }
    
    public String generateToolTip(XYDataset dataset, int series, int item) {
        SummaryLine sumLine = summary.get(item);
        return sumLine.getNetCodeWithYear()+"."+sumLine.getStaCode();
    }

    List<SummaryLine> summary;

    String xAxis, yAxis, zAxis;
}
