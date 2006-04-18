package edu.sc.seis.receiverFunction.web;

import java.util.ArrayList;
import java.util.HashMap;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.XYDataset;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class StationSummaryUrlGenerator implements XYURLGenerator {

    public StationSummaryUrlGenerator(StationSummaryDataset dataset,
                                      float gaussianWidth) {
        this(dataset.stationList,
             dataset.summary,
             dataset.xAxis,
             dataset.yAxis,
             dataset.zAxis,
             gaussianWidth);
    }

    public StationSummaryUrlGenerator(ArrayList stationList,
                                      HashMap summary,
                                      String xAxis,
                                      String yAxis,
                                      String zAxis,
                                      float gaussianWidth) {
        this.stationList = stationList;
        this.summary = summary;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
        this.gaussianWidth = gaussianWidth;
    }

    public String generateURL(XYDataset dataset, int series, int item) {
        VelocityStation sta = (VelocityStation)stationList.get(item);
        return "station.html?netcode="
                + NetworkIdUtil.toStringNoDates(sta.my_network) + "&stacode="
                + sta.get_code()+"&gaussian="+gaussianWidth;
    }

    ArrayList stationList;

    HashMap summary;

    String xAxis, yAxis, zAxis;

    float gaussianWidth;
}
