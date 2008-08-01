package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;

import edu.iris.Fissures.event.OriginImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.StationImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.bag.LongShortTrigger;
import edu.sc.seis.fissuresUtil.bag.SimplePhaseStoN;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.server.CachedResultPlusDbId;
import edu.sc.seis.receiverFunction.server.SyntheticFactory;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.locator.StationLocator;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.FissuresFormatter;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityStation;


/**
 * @author crotwell
 * Created on Feb 21, 2005
 */
public class RFStationEvent extends Revlet {

    public RFStationEvent() throws SQLException, ConfigurationException, Exception {
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
    public synchronized RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {

        try {
            logger.debug("doGet called");
            res.setContentType("image/png");
            ReceiverFunctionResult result;
            if (RevUtil.get("rf", req).equals("synth")) {
                result = SyntheticFactory.getReceiverFunctionResult();
            } else {
                int rfid = RevUtil.getInt("rf", req);
                result = RecFuncDB.getSingleton().getReceiverFunctionResult(rfid);
            }
            CacheEvent eq = result.getEvent();
            VelocityEvent velEvent = new VelocityEvent(eq);
            VelocityStation sta = new VelocityStation((StationImpl)result.getChannelGroup().getStation());
            
            VelocityContext vContext = new VelocityContext( Start.getDefaultContext());
            vContext.put("sta", sta);
            vContext.put("eq", velEvent);
            vContext.put("result", new VelocityCachedResult(result));
            vContext.put("rf", req.getParameter("rf"));
                HKStack stack;
                if (req.getParameter("rf").equals("synth")) {
                    stack = SyntheticFactory.getHKStack();
                } else {
                    stack = result.getHKstack();
                }
                if (stack != null) {
                vContext.put("stack", stack);
                vContext.put("rayparam", ""+stack.getP());
                TimeInterval timePs = stack.getTimePs();
                timePs.setFormat(FissuresFormatter.getDepthFormat());
                vContext.put("timePs", timePs);
                TimeInterval timePpPs = stack.getTimePpPs();
                timePpPs.setFormat(FissuresFormatter.getDepthFormat());
                vContext.put("timePpPs", timePpPs);
                TimeInterval timePsPs = stack.getTimePsPs();
                timePsPs.setFormat(FissuresFormatter.getDepthFormat());
                vContext.put("timePsPs", timePsPs);
                }
            
            LocalSeismogramImpl[] seis = new LocalSeismogramImpl[]{result.getOriginal1(),result.getOriginal2(),result.getOriginal3()};
            ArrayList triggers = new ArrayList();
            for(int i = 0; i < seis.length; i++) {
                LongShortTrigger trigger = ston.process(sta.getLocation(), velEvent.get_preferred_origin(), seis[i]);
                triggers.add(trigger);
            }
            vContext.put("ston", triggers);
            RevUtil.copy("H", req, vContext);
            RevUtil.copy("vpvs", req, vContext);
            
            RevletContext context = new RevletContext(velocityFile, vContext);
            Revlet.loadStandardQueryParams(req, context);
            return context;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected String velocityFile = "rfStationEvent.vm";

    private SimplePhaseStoN ston;
    
    private StationLocator staLoc;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RFStationEvent.class);
}
