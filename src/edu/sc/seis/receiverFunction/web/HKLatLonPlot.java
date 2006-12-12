package edu.sc.seis.receiverFunction.web;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStackImage;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on May 4, 2005
 */
public class HKLatLonPlot extends HttpServlet {

    /**
     * @throws Exception
     * @throws ConfigurationException
     * @throws SQLException
     */
    public HKLatLonPlot() throws SQLException, ConfigurationException,
            Exception {
        stationsNearBy = new StationsNearBy();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doGet(req, res);
    }

    public static JFreeChart getChart(HttpServletRequest req,
                                      ArrayList stationList,
                                      HashMap summary,
                                      String titleString) {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        StationSummaryDataset dataset = new StationSummaryDataset(stationList,
                                                       summary,
                                                       RevUtil.get("xAxis",
                                                                   req,
                                                                   LONGITUDE),
                                                       RevUtil.get("yAxis",
                                                                   req,
                                                                   THICKNESS),
                                                       RevUtil.get("zAxis",
                                                                   req,
                                                                   VPVS));
        boolean legend = false;
        boolean tooltips = true;
        boolean urls = true;
        if(summary.size() == 0) {
            titleString = "No " + titleString;
        }
        JFreeChart chart = ChartFactory.createScatterPlot(RevUtil.get("title",
                                                                      req,
                                                                      titleString),
                                                          RevUtil.get("xAxisLabel",
                                                                      req,
                                                                      getLabel(RevUtil.get("xAxis",
                                                                                           req,
                                                                                           LONGITUDE))),
                                                          RevUtil.get("yAxisLabel",
                                                                      req,
                                                                      getLabel(RevUtil.get("yAxis",
                                                                                           req,
                                                                                           THICKNESS))),
                                                          dataset,
                                                          PlotOrientation.VERTICAL,
                                                          legend,
                                                          tooltips,
                                                          urls);
        double minZ, maxZ;
        if(dataset.getItemCount(0) != 0) {
            if(RevUtil.exists("minZ", req)) {
                minZ = RevUtil.getFloat("minZ", req);
            } else {
                minZ = dataset.getZValue(0, 0);
                for(int i = 0; i < dataset.getItemCount(0); i++) {
                    double tmp = dataset.getZValue(0, i);
                    if(minZ > tmp) {
                        minZ = tmp;
                    }
                }
            }
            if(RevUtil.exists("maxZ", req)) {
                maxZ = RevUtil.getFloat("maxZ", req);
            } else {
                maxZ = dataset.getZValue(0, 0);
                for(int i = 0; i < dataset.getItemCount(0); i++) {
                    double tmp = dataset.getZValue(0, i);
                    if(maxZ < tmp) {
                        maxZ = tmp;
                    }
                }
            }
        } else {
            minZ = 0;
            maxZ = 0;
        }
        GMTColorPalette colorPallete;
        try {
            BufferedReader buf = new BufferedReader(new FileReader(HKStackImage.getCPTFile()));
            colorPallete = GMTColorPalette.load(buf).renormalize(minZ, maxZ, Color.BLACK, Color.MAGENTA, Color.CYAN);
            buf.close();
        } catch (IOException e) {
            logger.warn(e);
            colorPallete = GMTColorPalette.getDefault(minZ, maxZ);
        }
        ZColorXYDotRenderer renderer = new ZColorXYDotRenderer(dataset,
                                                               colorPallete);
        renderer.setToolTipGenerator(new StationSummaryTooltipGenerator(dataset));
        renderer.setURLGenerator(new StationSummaryUrlGenerator(dataset, gaussianWidth));
        chart.getXYPlot().setRenderer(renderer);
        return chart;
    }

    protected synchronized void doGet(HttpServletRequest req,
                                      HttpServletResponse res)
            throws ServletException, IOException {
        try {
            logger.debug("doGet called");
            int xdim = RevUtil.getInt("xdim", req, xdimDefault);
            int ydim = RevUtil.getInt("ydim", req, ydimDefault);
            RevletContext context = new RevletContext("unused");
            ArrayList stationList = stationsNearBy.getStations(req, context);
            logger.debug(stationList.size() + " stations nearby");
            HashMap summary = stationsNearBy.cleanSummaries(stationList,
                                                            stationsNearBy.getSummaries(stationList,
                                                                                        context,
                                                                                        req));
            String titleString = "Ears results near "
                    + RevUtil.get(LATITUDE, req) + ", "
                    + RevUtil.get(LONGITUDE, req);
            JFreeChart chart = getChart(req, stationList, summary, titleString);
            OutputStream out = res.getOutputStream();
            BufferedImage image = chart.createBufferedImage(xdim, ydim);
            res.setContentType("image/png");
            ImageIO.write(image, "png", out);
            out.close();
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            writer.write("<html><body><p>No HK stack foundfor id "
                    + req.getParameter("rf") + "</p></body></html>");
            writer.flush();
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        }
    }

    static String getLabel(String key) {
        if(THICKNESS.equals(key)) {
            return "Thickness";
        } else if(VPVS.equals(key)) {
            return "Vp/Vs";
        } else if(LATITUDE.equals(key)) {
            return "Latitude";
        } else if(LONGITUDE.equals(key)) {
            return "Longitude";
        }
        return "UNKNOWN";
    }

    static final String THICKNESS = "H";

    static final String VPVS = "vpvs";

    static final String LATITUDE = "lat";

    static final String LONGITUDE = "lon";

    StationsNearBy stationsNearBy;

    public static final int xdimDefault = 600;

    public static final int ydimDefault = 600;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKLatLonPlot.class);
}