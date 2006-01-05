package edu.sc.seis.receiverFunction.web;

import java.util.ArrayList;
import java.util.HashMap;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.sod.velocity.network.VelocityStation;


public class StationSummaryTooltipGenerator implements XYToolTipGenerator {
    
    public StationSummaryTooltipGenerator(StationSummaryDataset dataset) {
         this(dataset.stationList, dataset.summary, dataset.xAxis, dataset.yAxis, dataset.zAxis);
    }
    
    public StationSummaryTooltipGenerator(ArrayList stationList, HashMap summary, String xAxis,
                                String yAxis, String zAxis) {
                            this.stationList = stationList;
                            this.summary = summary;
                            this.xAxis = xAxis;
                            this.yAxis = yAxis;
                            this.zAxis = zAxis;
                        }
    
    public String generateToolTip(XYDataset dataset, int series, int item) {
        VelocityStation sta = (VelocityStation) stationList.get(item);
        return StationIdUtil.toStringNoDates(sta);
    }

    ArrayList stationList;

    HashMap summary;

    String xAxis, yAxis, zAxis;
}
