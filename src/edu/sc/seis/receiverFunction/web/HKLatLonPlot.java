package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
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

    protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {
        doGet(arg0, arg1);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            logger.debug("doGet called");
            int xdim = RevUtil.getInt("xdim", req, xdimDefault);
            int ydim = RevUtil.getInt("ydim", req, ydimDefault);
            RevletContext context = new RevletContext("unused");
            ArrayList stationList = stationsNearBy.getStations(req, context);
            logger.debug(stationList.size()+" stations nearby");
            HashMap summary = stationsNearBy.cleanSummaries(stationList,
                                                            stationsNearBy.getSummaries(stationList,
                                                                                        context, req));
            XYZDataset dataset = new StationSummaryDataset(stationList,
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
            String titleString = 
                "Ears results near "
                + RevUtil.get(LATITUDE,
                              req)
                + ", "
                + RevUtil.get(LONGITUDE,
                              req);
            if (summary.size() == 0) {
                titleString = "No "+titleString;
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
            chart.getXYPlot()
                    .setRenderer(new ZColorXYDotRenderer(dataset,
                                                         GMTColorPalette.getDefault(minZ,
                                                                                    maxZ)));
            System.out.println(chart.getXYPlot().getClass().getName());
            OutputStream out = res.getOutputStream();
            BufferedImage image = chart.createBufferedImage(xdim, ydim);
            res.setContentType("image/png");
            ImageIO.write(image, "png", out);
            out.close();
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            System.out.println("No HKStack found for id "
                    + req.getParameter("rf"));
            writer.write("<html><body><p>No HK stack foundfor id "
                    + req.getParameter("rf") + "</p></body></html>");
            writer.flush();
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        }
    }

    String getLabel(String key) {
        if(THICKNESS.equals(key)) {
            return "Thickness";
        } else if(VPVS.equals(key)) {
            return "Vp/Vs";
        } else if(LATITUDE.equals(key)) {
            return "Latitude";
        } else if(LONGITUDE.equals(key)) { return "Longitude"; }
        return "UNKNOWN";
    }

    static final String THICKNESS = "H";

    static final String VPVS = "vpvs";

    static final String LATITUDE = "lat";

    static final String LONGITUDE = "lon";

    StationsNearBy stationsNearBy;

    int xdimDefault = 600;

    int ydimDefault = 600;

    class StationSummaryDataset extends AbstractXYZDataset {

        StationSummaryDataset(ArrayList stationList, HashMap summary,
                String xAxis, String yAxis, String zAxis) {
            this.stationList = stationList;
            this.summary = summary;
            this.xAxis = xAxis;
            this.yAxis = yAxis;
            this.zAxis = zAxis;
        }

        public int getItemCount(int series) {
            return stationList.size();
        }

        Number get(int series, int item, String key) {
            VelocityStation sta = (VelocityStation)stationList.get(item);
            if(sta == null) {
                logger.warn("getXValue station is NULL: " + series + "  "
                        + item + "  " + stationList.size());
                return new Float(0);
            }
            if(LATITUDE.equals(key)) {
                return new Float(sta.my_location.latitude);
            } else if(LONGITUDE.equals(key)) { return new Float(sta.my_location.longitude); }
            SumHKStack stack = (SumHKStack)summary.get(stationList.get(item));
            if(THICKNESS.equals(key)) {
                return new Float(stack.getSum()
                        .getMaxValueH()
                        .getValue(UnitImpl.KILOMETER));
            } else if(VPVS.equals(key)) { return new Float(stack.getSum()
                    .getMaxValueK()); }
            // default
            logger.warn("default get: " + series + " " + item + " '" + key
                    + "'");
            return new Integer(0);
        }

        public Number getX(int series, int item) {
            return get(series, item, xAxis);
        }

        public Number getY(int series, int item) {
            return get(series, item, yAxis);
        }

        public Number getZ(int series, int item) {
            return get(series, item, zAxis);
        }

        public int getSeriesCount() {
            return 1;
        }

        public String getSeriesName(int series) {
            return getLabel(yAxis);
        }

        public Comparable getSeriesKey(int arg0) {
            return null;
        }
        ArrayList stationList;

        HashMap summary;

        String xAxis, yAxis, zAxis;
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKLatLonPlot.class);
}