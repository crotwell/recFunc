package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
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
import edu.sc.seis.receiverFunction.server.HKStackIterator;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
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
        jdbcHKStack = station.jdbcHKStack;
    }

    protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {
        doGet(arg0, arg1);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            boolean legend = false;
            boolean tooltips = true;
            boolean urls = true;
            int netDbId = RevUtil.getInt("netdbid", req);
            VelocityNetwork net = station.getNetwork(netDbId);
            String staCode = req.getParameter("stacode");
            ArrayList stationList = station.getStationList(netDbId, staCode);

            float gaussianWidth = RevUtil.getFloat("gaussian", req, Start.getDefaultGaussian());
            float minPercentMatch = RevUtil.getFloat("minPercentMatch", req, Start.getDefaultMinPercentMatch());QuantityImpl smallestH = HKStack.getBestSmallestH((VelocityStation)stationList.get(0));
            ArrayList stackList = new ArrayList();
            synchronized(jdbcHKStack.getConnection()) {
                HKStackIterator it = jdbcHKStack.getIteratorForStation(net.getCode(),
                                                                       staCode,
                                                                       gaussianWidth,
                                                                       minPercentMatch,
                                                                       true);
                while(it.hasNext()) {
                    HKStack stack = (HKStack)it.next();
                    stackList.add(new HKMax(stack, smallestH));
                }
                it.close();
            }
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

    Station station;

    JDBCHKStack jdbcHKStack;

    int xdimDefault = 600;

    int ydimDefault = 600;
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

    private ArrayList items;

    public HKXYDataset(ArrayList items) {
        this.items = items;
    }

    public int getItemCount(int series) {
        System.out.println("getItemCount: " + items.size());
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
