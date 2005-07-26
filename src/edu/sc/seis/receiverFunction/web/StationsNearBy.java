package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.VelocityContext;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.LocationType;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.JDBCSummaryHKStack;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
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

    public StationsNearBy(String databaseURL, String dataloc)
            throws SQLException, ConfigurationException, Exception {
        super(databaseURL, dataloc);
    }

    public String getVelocityTemplate() {
        return "stationsNearBy.vm";
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
        ArrayList stationList =new ArrayList();
        Station[] stations = jdbcChannel.getStationTable().getAllStations();
        logger.info("getAllStations finished");
        for(int j = 0; j < stations.length; j++) {
            DistAz distAz = new DistAz(stations[j].my_location, loc);
            System.out.println(stations[j].my_location.latitude+","+stations[j].my_location.longitude+" -> "+distAz.getDelta());
            if(distAz.getDelta() < delta) {
                stationList.add(new VelocityStation(stations[j]));
            }
        }
        return stationList;
    }
    
    QuantityImpl ZERO_KM = new QuantityImpl(0, UnitImpl.KILOMETER);
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationsNearBy.class);
}