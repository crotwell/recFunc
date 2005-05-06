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
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.velocity.VelocityStation;
import edu.sc.seis.sod.ConfigurationException;

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

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            logger.debug("doGet called");
            int xdim = RevUtil.getInt("xdim", req, xdimDefault);
            int ydim = RevUtil.getInt("ydim", req, ydimDefault);
            RevletContext context = new RevletContext("unused");
            ArrayList stationList = stationsNearBy.getStations(req, context);
            HashMap summary = stationsNearBy.cleanSummaries(stationList,
                                                            stationsNearBy.getSummaries(stationList, context));
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
            JFreeChart chart = ChartFactory.createScatterPlot(RevUtil.get("title",
                                                                          req,
                                                                          "Ears results near "
                                                                                  + RevUtil.get(LATITUDE,
                                                                                                req)
                                                                                  + ", "
                                                                                  + RevUtil.get(LONGITUDE,
                                                                                                req)),
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
            logger.debug("chart renderer: "+chart.getXYPlot().getRenderer().getClass().getName());
            double minZ = dataset.getZValue(0, 0);
            for(int i = 0; i < dataset.getItemCount(0); i++) {
                double tmp = dataset.getZValue(0, i);
                if(minZ > tmp) {
                    minZ = tmp;
                }
            }
            double maxZ = dataset.getZValue(0, 0);
            for(int i = 0; i < dataset.getItemCount(0); i++) {
                double tmp = dataset.getZValue(0, i);
                if(maxZ < tmp) {
                    maxZ = tmp;
                }
            }
            chart.getXYPlot()
                    .setRenderer(new ZColorXYDotRenderer(dataset,
                                                         GMTColorPallete.getDefault(minZ,
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
            e.printStackTrace();
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

        ArrayList stationList;

        HashMap summary;

        String xAxis, yAxis, zAxis;
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKLatLonPlot.class);
}