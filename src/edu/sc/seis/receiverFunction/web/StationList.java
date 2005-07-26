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
import edu.iris.Fissures.Time;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
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
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;


/**
 * @author crotwell
 * Created on Feb 10, 2005
 */
public class StationList extends Revlet {
    
    public StationList() throws SQLException, ConfigurationException, Exception {
        this("jdbc:postgresql:ears", System.getProperty("user.home")+"/CacheRecFunc/Ears/Data");
    }
    
    public StationList(String databaseURL, String dataloc) throws SQLException, ConfigurationException, Exception {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(databaseURL);
        DATA_LOC = dataloc;
        Connection conn = ConnMgr.createConnection();
        jdbcEventAccess = new JDBCEventAccess(conn);
        jdbcChannel  = new JDBCChannel(conn);
        jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn, jdbcEventAccess, jdbcChannel, jdbcSodConfig, DATA_LOC);
        jdbcHKStack = new JDBCHKStack(conn, jdbcEventAccess, jdbcChannel, jdbcSodConfig, jdbcRecFunc);
        jdbcSumHKStack = new JDBCSummaryHKStack(jdbcHKStack);
    }

    /**
     *
     */
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        RevletContext context = new RevletContext(getVelocityTemplate(), Start.getDefaultContext());
        ArrayList stationList = getStations(req, context);
        logger.debug("getStations done");
        HashMap summary = getSummaries(stationList, context);
        logger.debug("getSummaries done");
        HashMap numEQ = new HashMap();
        Iterator it = stationList.iterator();
        while(it.hasNext()) {
            VelocityStation sta = (VelocityStation)it.next();
            numEQ.put(sta, new Integer(jdbcRecFunc.countSeccessfulEvents(sta.getNet().getDbId(), sta.get_code(), 80.0f)));
        }
        logger.debug("count successful events done");
        context.put("stationList", stationList);
        context.put("summary", summary);
        context.put("numEQ", numEQ);
        return context;
    }
    
    public String getVelocityTemplate() {
        return "stationList.vm";
    }
    
    public ArrayList getStations(HttpServletRequest req, RevletContext context) throws SQLException, NotFound {
        int netDbId = RevUtil.getInt("netdbid", req);
        VelocityNetwork net = new VelocityNetwork(jdbcChannel.getStationTable().getNetTable().get(netDbId), netDbId);
        context.put("net", net);
        Station[] stations = jdbcChannel.getSiteTable().getStationTable().getAllStations(net.get_id());
        ArrayList stationList = new ArrayList();
        for(int i = 0; i < stations.length; i++) {
            stationList.add(new VelocityStation(stations[i]));
        }
        return stationList;
    }
    
    /** 
     * Populates a hashmap with keys (objects of type Station) from the list
     * and values of SumHKStack. Also populates the dbid for the stations and network.
     * @param context TODO
     * @throws SQLException
     * @throws IOException
     */
    public HashMap getSummaries(ArrayList stationList, RevletContext context) throws SQLException, IOException {
        Iterator it = stationList.iterator();
        HashMap summary = new HashMap();
        while(it.hasNext()) {
            VelocityStation sta = (VelocityStation)it.next();
            try {
            sta.setDbId(jdbcChannel.getStationTable().getDBId(sta.get_id()));
            int netDbId = jdbcChannel.getNetworkTable().getDbId(sta.getNet().get_id());
            sta.getNet().setDbId(netDbId);
            SumHKStack sumStack = jdbcSumHKStack.getForStation(netDbId, sta.get_code());
            summary.put(sta, sumStack);
            } catch (NotFound e) {
                // oh well, skip this station
                logger.warn("not found for "+sta.getNetCode()+"."+sta.get_code());
            }
        }
        logger.debug("found "+summary.size()+" summaries");
        return summary;
    }
    
    public HashMap cleanSummaries(ArrayList stationList, HashMap summary) {
        logger.debug("before cleanSummaries stationList.size()="+stationList.size()+"  summary.size()="+summary.size());
        Iterator it = stationList.iterator();
        while(it.hasNext()) {
            Object next = it.next();
            if (summary.get(next) == null) {
                summary.remove(next);
                it.remove();
            }
        }
        logger.debug("after cleanSummaries stationList.size()="+stationList.size()+"  summary.size()="+summary.size());
        return summary;
    }
    
    String DATA_LOC;
    
    JDBCEventAccess jdbcEventAccess;
    
    JDBCChannel jdbcChannel;
    
    JDBCHKStack jdbcHKStack;
    
    JDBCRecFunc jdbcRecFunc;
    
    JDBCSodConfig jdbcSodConfig;
    
    JDBCSummaryHKStack jdbcSumHKStack;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationList.class);
    
}
