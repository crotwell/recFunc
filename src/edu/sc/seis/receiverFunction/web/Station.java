package edu.sc.seis.receiverFunction.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.Time;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.velocity.VelocityEvent;
import edu.sc.seis.rev.velocity.VelocityNetwork;
import edu.sc.seis.rev.velocity.VelocityStation;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Feb 23, 2005
 */
public class Station extends Revlet {

    public Station() throws SQLException, ConfigurationException, Exception {
        this("jdbc:postgresql:ears", System.getProperty("user.home")+"/CacheServer/Ears/Data");
    }
    
    public Station(String databaseURL, String dataloc) throws SQLException, ConfigurationException, Exception {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(databaseURL);
        DATA_LOC = dataloc;
        Connection conn = ConnMgr.createConnection();
        jdbcEventAccess = new JDBCEventAccess(conn);
        jdbcChannel  = new JDBCChannel(conn);
        jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn, jdbcEventAccess, jdbcChannel, jdbcSodConfig, DATA_LOC);
        jdbcHKStack = new JDBCHKStack(conn, jdbcEventAccess, jdbcChannel, jdbcSodConfig, jdbcRecFunc);
    }
    /**
     *
     */
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        int netDbId = new Integer(req.getParameter("netdbid")).intValue();
        VelocityNetwork net = new VelocityNetwork(jdbcChannel.getStationTable().getNetTable().get(netDbId), netDbId);
        
        // possible that there are multiple stations with the same code
        String staCode = req.getParameter("stacode");
        int[] dbids = jdbcChannel.getSiteTable().getStationTable().getDBIds(netDbId, staCode);
        ArrayList stationList = new ArrayList();
        for(int i = 0; i < dbids.length; i++) {
            stationList.add(new VelocityStation(jdbcChannel.getSiteTable().getStationTable().get(dbids[i]), dbids[i]));
        }
        
        CacheEvent[] events = jdbcRecFunc.getSuccessfulEvents(netDbId, staCode);
        ArrayList eventList = new ArrayList();
        for(int i = 0; i < events.length; i++) {
            eventList.add(new VelocityEvent(events[i]));
        }
        Collections.sort(eventList, itrMatchComparator);
        Collections.reverse(eventList);
        
        RevletContext context = new RevletContext("station.vm");
        context.put("stationList", stationList);
        context.put("stacode", staCode);
        context.put("net", net);
        context.put("eventList", eventList);
        return context;
    }
    
    static ITRMatchComparator itrMatchComparator = new ITRMatchComparator();
    
    static class ITRMatchComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if (o1.equals(o2)) { return 0;}
            if ( ! (o1 instanceof VelocityEvent) || ! (o2 instanceof VelocityEvent)) {
                return 0;
            }
            VelocityEvent v1 = (VelocityEvent)o1;
            VelocityEvent v2 = (VelocityEvent)o2;
            float f1 = new Float(v1.getParam("itr_match")).floatValue();
            float f2 = new Float(v2.getParam("itr_match")).floatValue();
            if (f1 > f2) return 1;
            if (f1 < f2) return -1;
            return 0;
        } 
        
    }

    String DATA_LOC;
    
    JDBCEventAccess jdbcEventAccess;
    
    JDBCChannel jdbcChannel;
    
    JDBCHKStack jdbcHKStack;
    
    JDBCRecFunc jdbcRecFunc;
    
    JDBCSodConfig jdbcSodConfig;
}
