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

import edu.iris.Fissures.Location;
import edu.iris.Fissures.LocationType;
import edu.iris.Fissures.IfNetwork.Station;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;


/**
 * @author crotwell
 * Created on Sep 6, 2005
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
        if (req.getServletPath().endsWith(".html")) {
            return "stationLatLonBox.vm";
        } else if (req.getServletPath().endsWith(".txt")) {
            return "stationLatLonBoxTxt.vm";
        } else {
            return "stationLatLonBox.vm";
        }
    }
    
    public ArrayList getStations(HttpServletRequest req, RevletContext context) throws SQLException,
            NotFound {
        float minLat = RevUtil.getFloat("minLat", req);
        float minLon = RevUtil.getFloat("minLon", req);
        float maxLat = RevUtil.getFloat("maxLat", req);
        float maxLon = RevUtil.getFloat("maxLon", req);
        context.put("minLat", new Float(minLat));
        context.put("minLon", new Float(minLon));
        context.put("maxLat", new Float(maxLat));
        context.put("maxLon", new Float(maxLon));
        ArrayList stationList =new ArrayList();
        Station[] stations = jdbcChannel.getStationTable().getAllStations();
        logger.info("getAllStations finished");
        for(int j = 0; j < stations.length; j++) {
            if(stations[j].my_location.latitude >= minLat && stations[j].my_location.latitude <= maxLat &&
                    stations[j].my_location.longitude >= minLon && stations[j].my_location.longitude <= maxLon) {
                stationList.add(new VelocityStation(stations[j]));
            }
        }
        return stationList;
    }
    
    public void postProcess(HttpServletRequest req, RevletContext context, ArrayList stationList, HashMap summary) {
    	JFreeChart chart = HKLatLonPlot.getChart(req, stationList, summary); 
    	try {
			String filename = ServletUtilities.saveChartAsPNG(chart, RevUtil.getInt("xdim", req, HKLatLonPlot.xdimDefault), RevUtil.getInt("ydim", req, HKLatLonPlot.ydimDefault), req.getSession());
	context.put("plotfilename", filename);	
	ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
	context.put("imagemap", ImageMapUtilities.getImageMap(filename, info));
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return;
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationLatLonBox.class);
}
