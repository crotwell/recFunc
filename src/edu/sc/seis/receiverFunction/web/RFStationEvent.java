package edu.sc.seis.receiverFunction.web;

import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.VelocityContext;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.StationLocator;
import edu.sc.seis.rev.velocity.VelocityEvent;
import edu.sc.seis.rev.velocity.VelocityStation;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelCookieJar;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelStatus;


/**
 * @author crotwell
 * Created on Feb 21, 2005
 */
public class RFStationEvent extends Revlet {

    public RFStationEvent() throws SQLException, ConfigurationException, Exception {
        Connection conn = ConnMgr.createConnection();
        jdbcCookies = new JDBCEventChannelCookieJar(conn);
        jdbcEvent = new JDBCEventAccess(conn);
        jdbcECStatus = new JDBCEventChannelStatus(conn);
        
        JDBCChannel jdbcChannel  = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn, jdbcEvent, jdbcChannel, jdbcSodConfig, RecFuncCacheImpl.getDataLoc());
        hkStack = new JDBCHKStack(conn, jdbcEvent, jdbcChannel, jdbcSodConfig, jdbcRecFunc);

        
    }
    /**
     *
     */
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {

        try {
            logger.debug("doGet called");
            res.setContentType("image/png");
            if(req.getParameter("rf") == null) { throw new Exception("rf param not set"); }
            CachedResult result = hkStack.getJDBCRecFunc().get(new Integer(req.getParameter("rf")).intValue());
            CacheEvent eq = new CacheEvent(result.event_attr, result.prefOrigin);
            VelocityEvent velEvent = new VelocityEvent(eq);
            VelocityStation sta = staLoc.locate(req);
            
            VelocityContext vContext = new VelocityContext();
            vContext.put("station", sta);
            vContext.put("eq", velEvent);
            RevletContext context = new RevletContext("RFStationEvent", vContext);
            return context;
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private StationLocator staLoc;
    
    private JDBCEventAccess jdbcEvent;

    private JDBCEventChannelCookieJar jdbcCookies;

    private JDBCEventChannelStatus jdbcECStatus;
    
    private JDBCHKStack hkStack;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RFStationEvent.class);
}
