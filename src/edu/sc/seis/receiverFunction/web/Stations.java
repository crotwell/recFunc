package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.Time;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.velocity.VelocityStation;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Feb 10, 2005
 */
public class Stations extends Revlet {
    
    public Stations() throws SQLException, ConfigurationException, Exception {
        this("jdbc:postgresql:ears", System.getProperty("user.home")+"/CacheServer/Ears/Data");
    }
    
    public Stations(String databaseURL, String dataloc) throws SQLException, ConfigurationException, Exception {
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

        String networkcode = req.getParameter("nwcode");
        Integer integer = new Integer(req.getParameter("leapsecondsversion"));
        int leapsecondsversion = integer.intValue();
        String datetime = req.getParameter("datetime");
        NetworkId id = new NetworkId(networkcode,new Time(datetime,leapsecondsversion));
        Station[] stations = jdbcChannel.getSiteTable().getStationTable().getAllStations(id);
        ArrayList stationList = new ArrayList();
        for(int i = 0; i < stations.length; i++) {
            stationList.add(new VelocityStation(stations[i]));
        }
        RevletContext context = new RevletContext("stationList.vm");
        context.put("staionList", stationList);
        return context;
    }
    
    String DATA_LOC;
    
    JDBCEventAccess jdbcEventAccess;
    
    JDBCChannel jdbcChannel;
    
    JDBCHKStack jdbcHKStack;
    
    JDBCRecFunc jdbcRecFunc;
    
    JDBCSodConfig jdbcSodConfig;
}
