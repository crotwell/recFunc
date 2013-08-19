package edu.sc.seis.receiverFunction.web;

import java.util.List;

import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.XYDataset;

import edu.sc.seis.receiverFunction.server.SummaryLine;

public class StationSummaryUrlGenerator implements XYURLGenerator {

    public StationSummaryUrlGenerator(SummaryLineDataset dataset,
                                      float gaussianWidth) {
        this(dataset.summary,
             dataset.xAxis,
             dataset.yAxis,
             dataset.zAxis,
             gaussianWidth);
    }

    public StationSummaryUrlGenerator(List<SummaryLine> summary,
                                      String xAxis,
                                      String yAxis,
                                      String zAxis,
                                      float gaussianWidth) {
        this.summary = summary;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
        this.gaussianWidth = gaussianWidth;
    }

    public String generateURL(XYDataset dataset, int series, int item) {
        SummaryLine sumLine = summary.get(item);
        return "station.html?netcode="
                + sumLine.getNetCodeWithYear() + "&stacode="
                + sumLine.getStaCode()+"&gaussian="+gaussianWidth;
    }

    List<SummaryLine> summary;

    String xAxis, yAxis, zAxis;

    float gaussianWidth;
}
