package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
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
        // TODO Auto-generated constructor stub
    }

    /**
     *
     */
    public StationLatLonBox(String databaseURL, String dataloc)
            throws SQLException, ConfigurationException, Exception {
        super(databaseURL, dataloc);
        // TODO Auto-generated constructor stub
    }
    

    public String getVelocityTemplate() {
        return "stationLatLonBox.vm";
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
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationLatLonBox.class);
}
