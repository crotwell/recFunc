package edu.sc.seis.receiverFunction.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.LocationType;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
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
import edu.sc.seis.rev.velocity.VelocityStation;
import edu.sc.seis.sod.ConfigurationException;

/**
 * @author crotwell Created on Mar 16, 2005
 */
public class StationsNearBy extends Revlet {

    public StationsNearBy() throws SQLException, ConfigurationException,
            Exception {
        this("jdbc:postgresql:ears", System.getProperty("user.home")
                + "/CacheServer/Ears/Data");
    }

    public StationsNearBy(String databaseURL, String dataloc)
            throws SQLException, ConfigurationException, Exception {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(databaseURL);
        DATA_LOC = dataloc;
        Connection conn = ConnMgr.createConnection();
        jdbcEventAccess = new JDBCEventAccess(conn);
        jdbcChannel = new JDBCChannel(conn);
        jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      DATA_LOC);
        jdbcHKStack = new JDBCHKStack(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      jdbcRecFunc);
        jdbcSumHKStack = new JDBCSummaryHKStack(jdbcHKStack);
    }

    /**
     *
     */
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        logger.info("StationsNearBy.getContext");
        float lat = RevUtil.getFloat("lat", req);
        float lon = RevUtil.getFloat("lon", req);
        float delta = RevUtil.getFloat("delta", req);
        Location loc = new Location(lat,
                                    lon,
                                    ZERO_KM,
                                    ZERO_KM,
                                    LocationType.GEOGRAPHIC);
        ArrayList stationList = new ArrayList();
        Station[] stations = jdbcChannel.getStationTable().getAllStations();
        logger.info("getAllStations finished");
        for(int j = 0; j < stations.length; j++) {
            DistAz distAz = new DistAz(stations[j].my_location, loc);
            if(distAz.getDelta() < delta) {
                stationList.add(new VelocityStation(stations[j]));
            }
        }
        logger.info("distance check finished");
        Iterator it = stationList.iterator();
        HashMap summary = new HashMap();
        while(it.hasNext()) {
            VelocityStation sta = (VelocityStation)it.next();
            int dbid = jdbcSumHKStack.getDbIdForStation(sta.my_network.get_id(), sta.get_code());
            SumHKStack sumStack = jdbcSumHKStack.get(dbid);
            summary.put(sta, sumStack);
        }
        RevletContext context = new RevletContext("stationsNearBy.vm");
        context.put("stationList", stationList);
        context.put("lat", lat + "");
        context.put("lon", lon + "");
        context.put("delta", delta + "");
        context.put("summary", summary);
        return context;
    }

    QuantityImpl ZERO_KM = new QuantityImpl(0, UnitImpl.KILOMETER);

    String DATA_LOC;

    JDBCEventAccess jdbcEventAccess;

    JDBCChannel jdbcChannel;

    JDBCHKStack jdbcHKStack;
    
    JDBCSummaryHKStack jdbcSumHKStack;

    JDBCRecFunc jdbcRecFunc;

    JDBCSodConfig jdbcSodConfig;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationsNearBy.class);
}