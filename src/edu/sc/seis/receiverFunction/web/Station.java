package edu.sc.seis.receiverFunction.web;

import java.awt.Color;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.Time;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.Marker;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.WilsonRistra;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
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
        int numNinty = 0;
        int numEighty = 0;
        for(int i = 0; i < events.length; i++) {
            VelocityEvent ve = new VelocityEvent(events[i]);
            eventList.add(ve);
            float match = new Float(ve.getParam("itr_match")).floatValue();
            if (match >= 80) {
                numEighty++;
                if (match >= 90) {
                    numNinty++;
                }
            }
        }
        Collections.sort(eventList, itrMatchComparator);
        Collections.reverse(eventList);
        
        VelocityStation sta = (VelocityStation)stationList.get(0);
        ArrayList markerList = new ArrayList();
        float smallestH = 20;
        if (crust2 != null) {
            Crust2Profile profile = crust2.getClosest(sta.my_location.longitude,
                                                      sta.my_location.latitude);
            double vpvs = profile.getPWaveAvgVelocity() / profile.getSWaveAvgVelocity();
            markerList.add(new Marker("Crust2", vpvs, profile.getCrustThickness(), Color.blue));
            if (profile.getCrustThickness() < smallestH + 10) {
                smallestH = (float)profile.getCrustThickness() - 10;
            }
        }
        if (wilson != null) {
            StationResult result = wilson.getResult(sta.get_id());
            if (result != null) {
                double vpvs = result.getVpVs();
                markerList.add(new Marker("Wilson", result.getVpVs(), result.getH(), Color.GREEN));
            }
        }
        
        RevletContext context = new RevletContext("station.vm");
        context.put("stationList", stationList);
        context.put("stacode", staCode);
        context.put("net", net);
        context.put("eventList", eventList);
        context.put("numNinty", ""+numNinty);
        context.put("numEighty", ""+numEighty);
        context.put("markerList", markerList);
        context.put("smallestH", smallestH+"");
        return context;
    }

    transient static Crust2 crust2 = HKStack.getCrust2();
    transient static WilsonRistra wilson = HKStack.getWilsonRistra();
    
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
