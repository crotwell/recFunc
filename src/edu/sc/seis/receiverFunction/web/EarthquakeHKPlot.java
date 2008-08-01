package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.AbstractXYDataset;

import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Aug 23, 2005
 */
public class EarthquakeHKPlot extends HttpServlet {

    /**
     * @throws Exception
     * @throws ConfigurationException
     * @throws SQLException
     */
    public EarthquakeHKPlot() throws Exception {
        station = new Station();
    }

    protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {
        doGet(arg0, arg1);
    }

    protected synchronized void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            boolean legend = false;
            boolean tooltips = true;
            boolean urls = true;
            VelocityNetwork net = Start.getNetwork(req);
            String staCode = req.getParameter("stacode");
            ArrayList stationList = station.getStationList(net.getWrapped(), staCode);

            float gaussianWidth = RevUtil.getFloat("gaussian", req, Start.getDefaultGaussian());
            float minPercentMatch = RevUtil.getFloat("minPercentMatch", req, Start.getDefaultMinPercentMatch());QuantityImpl smallestH = HKStack.getBestSmallestH((VelocityStation)stationList.get(0));
            List<ReceiverFunctionResult> stackList = RecFuncDB.getSingleton().getSuccessful(net.getWrapped(), staCode, gaussianWidth, minPercentMatch);
            
            HKXYDataset dataset = new HKXYDataset(stackList);
            String title = "Maxima for Earthquakes at " + net.getCode() + "."
                    + staCode;
            /*
             * the following code is copied from the impl of this method, with
             * only the yAxis.setInverted(true); line added. JFreeChart chart =
             * ChartFactory.createScatterPlot(RevUtil.get("title", req, title),
             * RevUtil.get("xAxisLabel", req, "Vp/Vs"),
             * RevUtil.get("yAxisLabel", req, "H"), dataset,
             * PlotOrientation.VERTICAL, legend, tooltips, urls);
             */
            NumberAxis xAxis = new NumberAxis(RevUtil.get("xAxisLabel",
                                                          req,
                                                          "Vp/Vs"));
            xAxis.setAutoRangeIncludesZero(false);
            NumberAxis yAxis = new NumberAxis(RevUtil.get("yAxisLabel",
                                                          req,
                                                          "H"));
            yAxis.setInverted(true);
            yAxis.setAutoRangeIncludesZero(false);
            XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
            XYToolTipGenerator toolTipGenerator = null;
            if(tooltips) {
                toolTipGenerator = new StandardXYToolTipGenerator();
            }
            XYURLGenerator urlGenerator = null;
            if(urls) {
                urlGenerator = new StandardXYURLGenerator();
            }
            StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES,
                                                                         toolTipGenerator,
                                                                         urlGenerator);
            renderer.setShapesFilled(Boolean.TRUE);
            plot.setRenderer(renderer);
            plot.setOrientation(PlotOrientation.VERTICAL);
            JFreeChart chart = new JFreeChart(RevUtil.get("title", req, title),
                                              JFreeChart.DEFAULT_TITLE_FONT,
                                              plot,
                                              legend);
            OutputStream out = res.getOutputStream();
            BufferedImage image = chart.createBufferedImage(xdimDefault,
                                                            ydimDefault);
            res.setContentType("image/png");
            ImageIO.write(image, "png", out);
            out.close();
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            writer.write("<html><body><p>No HK stack foundfor id "
                    + req.getParameter("rf") + "</p></body></html>");
            writer.flush();
        } catch(IIOException e) {
            if (e.getCause() instanceof EOFException) {
                // usually means remote client has closed connection
                // nothing to be done...
            } else {
                logger.debug("Got IIOException, cause is a "+e.getCause().getClass());
                throw e;
            }
        } catch(EOFException e) {
            // usually means remote client has closed connection
            // nothing to be done...
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        }
    }

    Station station;

    int xdimDefault = 600;

    int ydimDefault = 600;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EarthquakeHKPlot.class);
}

class HKMax {

    HKMax(HKStack stack, QuantityImpl smallestH) {
        this(stack.getMaxValueK(smallestH),
             (float)stack.getMaxValueH(smallestH).getValue(UnitImpl.KILOMETER));
    }

    HKMax(float maxValueK, float maxValueH) {
        this.maxValueK = maxValueK;
        this.maxValueH = maxValueH;
    }

    float maxValueK;

    float maxValueH;

    public float getMaxValueH() {
        return maxValueH;
    }

    public float getMaxValueK() {
        return maxValueK;
    }
}

class HKXYDataset extends AbstractXYDataset {

    private List items;

    public HKXYDataset(List items) {
        this.items = items;
    }

    public int getItemCount(int series) {
        return items.size();
    }

    public Number getX(int series, int item) {
        HKMax stack = (HKMax)items.get(item);
        return new Float(stack.getMaxValueK());
    }

    public Number getY(int series, int item) {
        HKMax stack = (HKMax)items.get(item);
        return new Float(stack.getMaxValueH());
    }

    public int getSeriesCount() {
        return 1;
    }

    public String getSeriesName(int series) {
        return " eq stack max";
    }

    public Comparable getSeriesKey(int arg0) {
        return null;
    }
}
