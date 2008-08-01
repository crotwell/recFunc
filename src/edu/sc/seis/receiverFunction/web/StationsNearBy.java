package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.LocationType;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Mar 16, 2005
 */
public class StationsNearBy extends StationList {

    public StationsNearBy() throws SQLException, ConfigurationException,
            Exception {
        super();
    }
    
    public String getVelocityTemplate(HttpServletRequest req) {
        String fileType = RevUtil.getFileType(req);
        if (fileType.equals(RevUtil.MIME_HTML)) {
            return "stationsNearBy.vm";
        } else {
            return super.getVelocityTemplate(req);
        }
    }
    
    public ArrayList getStations(HttpServletRequest req, RevletContext context) throws SQLException,
            NotFound {
        float lat = RevUtil.getFloat("lat", req);
        float lon = RevUtil.getFloat("lon", req);
        float delta = RevUtil.getFloat("delta", req);
        context.put("lat", new Float(lat));
        context.put("lon", new Float(lon));
        context.put("delta", new Float(delta));
        Location loc = new Location(lat,
                                    lon,
                                    ZERO_KM,
                                    ZERO_KM,
                                    LocationType.GEOGRAPHIC);
        ArrayList<VelocityStation> stationList =new ArrayList<VelocityStation>();
        Station[] stations;
            stations = NetworkDB.getSingleton().getAllStations();
        logger.info("getAllStations finished");
        for(int j = 0; j < stations.length; j++) {
            DistAz distAz = new DistAz(stations[j].getLocation(), loc);
            if(distAz.getDelta() < delta) {
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
        StationLatLonBox.makeChart(req, context, stationList, summary);
    }
    
    public static final QuantityImpl ZERO_KM = new QuantityImpl(0, UnitImpl.KILOMETER);
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationsNearBy.class);
}