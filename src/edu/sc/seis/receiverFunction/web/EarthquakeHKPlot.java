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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;


/**
 * @author crotwell
 * Created on Aug 23, 2005
 */
public class EarthquakeHKPlot  extends HttpServlet {

    /**
     * @throws Exception
     * @throws ConfigurationException
     * @throws SQLException
     *
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
            
            float match = RevUtil.getFloat("percentMatch", req, 80);
            ArrayList stackList = jdbcHKStack.getForStation(net.getCode(), staCode, match);
            

            QuantityImpl smallestH = HKStack.getBestSmallestH((VelocityStation)stationList.get(0));
            HKXYDataset dataset = new HKXYDataset(stackList, smallestH);
            String title = "Maxima for Earthquakes at "+net.getCode()+"."+staCode;
            JFreeChart chart = ChartFactory.createScatterPlot(RevUtil.get("title",
                                                                          req,
                                                                          title),
                                                              RevUtil.get("xAxisLabel",
                                                                          req,
                                                                          "Vp/Vs"),
                                                              RevUtil.get("yAxisLabel",
                                                                          req,
                                                                          "H"),
                                                              dataset,
                                                              PlotOrientation.VERTICAL,
                                                              legend,
                                                              tooltips,
                                                              urls);
            OutputStream out = res.getOutputStream();
            BufferedImage image = chart.createBufferedImage(xdimDefault, ydimDefault);
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
    
    Station station;
    
    JDBCHKStack jdbcHKStack;

    int xdimDefault = 600;

    int ydimDefault = 600;
    
}

class HKXYDataset extends AbstractXYDataset {
    
    private ArrayList items;
    private QuantityImpl smallestH;
    
    public HKXYDataset(ArrayList items, QuantityImpl smallestH) {
        this.items = items;
        this.smallestH = smallestH;
    }
    
    public int getItemCount(int series) {
        return items.size();
    }
    public Number getX(int series, int item) {
        HKStack stack = (HKStack)items.get(item);
        return new Float(stack.getMaxValueK(smallestH));
    }
    public Number getY(int series, int item) {
        HKStack stack = (HKStack)items.get(item);
        return new Float(stack.getMaxValueH(smallestH).getValue(UnitImpl.KILOMETER));
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
