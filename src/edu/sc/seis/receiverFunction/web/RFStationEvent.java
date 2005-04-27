package edu.sc.seis.receiverFunction.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.VelocityContext;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.bag.LongShortTrigger;
import edu.sc.seis.fissuresUtil.bag.SimplePhaseStoN;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.locator.StationLocator;
import edu.sc.seis.rev.velocity.VelocityEvent;
import edu.sc.seis.rev.velocity.VelocityStation;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelCookieJar;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelStatus;
import edu.sc.seis.sod.process.waveform.PhaseSignalToNoise;
import edu.sc.seis.sod.status.FissuresFormatter;


/**
 * @author crotwell
 * Created on Feb 21, 2005
 */
public class RFStationEvent extends Revlet {

    public RFStationEvent() throws SQLException, ConfigurationException, Exception {
        Connection conn = ConnMgr.createConnection();
        jdbcEvent = new JDBCEventAccess(conn);
        
        JDBCChannel jdbcChannel  = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn, jdbcEvent, jdbcChannel, jdbcSodConfig, RecFuncCacheImpl.getDataLoc());
        hkStack = new JDBCHKStack(conn, jdbcEvent, jdbcChannel, jdbcSodConfig, jdbcRecFunc);
        staLoc = new StationLocator();
        ston = new SimplePhaseStoN("P",
                                   new TimeInterval(-1, UnitImpl.SECOND),
                                   new TimeInterval(5, UnitImpl.SECOND),
                                   new TimeInterval(-30, UnitImpl.SECOND),
                                   new TimeInterval(-5, UnitImpl.SECOND),
                                   TauPUtil.getTauPUtil("prem"));
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
            int rfid = new Integer(req.getParameter("rf")).intValue();
            CachedResult result = hkStack.getJDBCRecFunc().get(rfid);
            CacheEvent eq = new CacheEvent(result.event_attr, result.prefOrigin);
            HKStack stack = hkStack.get(rfid);
            VelocityEvent velEvent = new VelocityEvent(new CacheEvent(result.event_attr, result.prefOrigin));
            VelocityStation sta = new VelocityStation(result.channels[0].my_site.my_station);
            
            VelocityContext vContext = new VelocityContext();
            vContext.put("sta", sta);
            vContext.put("eq", velEvent);
            vContext.put("result", new VelocityCachedResult(result));
            vContext.put("rf", req.getParameter("rf"));
            vContext.put("stack", stack);
            TimeInterval timePs = stack.getTimePs();
            timePs.setFormat(FissuresFormatter.getDepthFormat());
            vContext.put("timePs", timePs);
            TimeInterval timePpPs = stack.getTimePpPs();
            timePpPs.setFormat(FissuresFormatter.getDepthFormat());
            vContext.put("timePpPs", timePpPs);
            TimeInterval timePsPs = stack.getTimePsPs();
            timePsPs.setFormat(FissuresFormatter.getDepthFormat());
            vContext.put("timePsPs", timePsPs);
            
            LocalSeismogramImpl[] seis = (LocalSeismogramImpl[])result.original;
            ArrayList triggers = new ArrayList();
            for(int i = 0; i < seis.length; i++) {
                LongShortTrigger trigger = ston.process(sta.my_location, velEvent.get_preferred_origin(), seis[i]);
                triggers.add(trigger);
            }
            vContext.put("ston", triggers);
            
            RevletContext context = new RevletContext("rfStationEvent.vm", vContext);
            return context;
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private SimplePhaseStoN ston;
    
    private StationLocator staLoc;
    
    private JDBCEventAccess jdbcEvent;
    
    private JDBCHKStack hkStack;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RFStationEvent.class);
}
