package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.servlet.ServletUtilities;

import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Sep 6, 2005
 */
public class StationLatLonBox extends StationList {

    /**
     * 
     */
    public StationLatLonBox() throws SQLException, ConfigurationException,
            Exception {
        super();
    }

    public String getVelocityTemplate(HttpServletRequest req) {
        if(req.getServletPath().endsWith(".html")) {
            return "stationLatLonBox.vm";
        } else if(req.getServletPath().endsWith(".txt")) {
            return "stationLatLonBoxTxt.vm";
        } else {
            return "stationLatLonBox.vm";
        }
    }

    public ArrayList getStations(HttpServletRequest req, RevletContext context)
            throws SQLException, NotFound {
        float minLat = RevUtil.getFloat("minLat", req);
        float minLon = RevUtil.getFloat("minLon", req);
        float maxLat = RevUtil.getFloat("maxLat", req);
        float maxLon = RevUtil.getFloat("maxLon", req);
        context.put("minLat", new Float(minLat));
        context.put("minLon", new Float(minLon));
        context.put("maxLat", new Float(maxLat));
        context.put("maxLon", new Float(maxLon));
        ArrayList<VelocityStation> stationList = new ArrayList<VelocityStation>();
        Station[] stations = NetworkDB.getSingleton().getAllStations();
        logger.info("getAllStations finished");
        for(int j = 0; j < stations.length; j++) {
            if(stations[j].getLocation().latitude >= minLat
                    && stations[j].getLocation().latitude <= maxLat
                    && stations[j].getLocation().longitude >= minLon
                    && stations[j].getLocation().longitude <= maxLon) {
                stationList.add(new VelocityStation((StationImpl)stations[j]));
            }
        }
        return stationList;
    }

    public void postProcess(HttpServletRequest req,
                            RevletContext context,
                            ArrayList<VelocityStation> stationList,
                            HashMap summary) {
        summary = cleanSummaries(stationList, summary);
        makeChart(req, context, stationList, summary);
    }

    public static void makeChart(HttpServletRequest req,
                            RevletContext context,
                            ArrayList stationList,
                            HashMap summary) {
        String titleString = "Ears results";
        JFreeChart chart = HKLatLonPlot.getChart(req,
                                                 stationList,
                                                 summary,
                                                 titleString);
        try {
            ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
            String filename = ServletUtilities.saveChartAsPNG(chart,
                                                              RevUtil.getInt("xdim",
                                                                             req,
                                                                             HKLatLonPlot.xdimDefault),
                                                              RevUtil.getInt("ydim",
                                                                             req,
                                                                             HKLatLonPlot.ydimDefault),
                                                              info,
                                                              req.getSession());
            context.put("plotname", filename);
            context.put("imagemap", ImageMapUtilities.getImageMap(filename,
                                                                  info));
        } catch(IOException e) {
            GlobalExceptionHandler.handle(e);
        }
        return;
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationLatLonBox.class);
}
