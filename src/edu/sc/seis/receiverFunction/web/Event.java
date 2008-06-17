package edu.sc.seis.receiverFunction.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.FissuresException;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.server.CachedResultPlusDbId;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityStation;


public class Event extends edu.sc.seis.winkle.Event {

    public Event(JDBCRecFunc jdbcRecFunc) throws Exception {
        super(jdbcRecFunc.getConnection());
        this.jdbcRecFunc = jdbcRecFunc;
    }

    public Event() throws Exception {
        super();
        this.jdbcRecFunc = new JDBCRecFunc(jdbcEventAccess.getConnection(), Start.getDataLoc());
    }
    
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        try {
            VelocityEvent event = extractEvent(req, res);
            CachedResultPlusDbId[] resultsWithDbId = extractRF(req, res, event, false);
            List stationList = new ArrayList();
            HashMap resultMap = new HashMap();
            for(int i = 0; i < resultsWithDbId.length; i++) {
                VelocityStation sta = new VelocityStation(resultsWithDbId[i].getCachedResult().channels[0].getSite().getStation());
                stationList.add(sta);
                resultMap.put(sta, resultsWithDbId[i]);
            }
            
            RevletContext context = new RevletContext("eventStationList.vm", Start.getDefaultContext());
            Revlet.loadStandardQueryParams(req, context);
            context.put("stationList", stationList);
            context.put("eq", event);
            context.put("recfunc", resultMap);
            return context;
        } catch(ParseException e) {
            return handleNotFound(res);
        } catch(NotFound e) {
            return handleNotFound(res);
        }
    }
    
    protected void setContentType(HttpServletRequest request,
                                  HttpServletResponse response)
    {
        response.setContentType("text/html");
    }
    
    public CachedResultPlusDbId[] extractRF(HttpServletRequest req, HttpServletResponse resp, VelocityEvent event, boolean withSeismograms) throws SQLException, FileNotFoundException, FissuresException, IOException {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch());
        try {
            return jdbcRecFunc.getStationsByEvent(event.getCacheEvent(), gaussianWidth, minPercentMatch, withSeismograms);
        } catch(NotFound e) {
            return new CachedResultPlusDbId[0];
        }
    }
    
    JDBCRecFunc jdbcRecFunc;
}
