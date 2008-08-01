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
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityStation;


public class Event extends edu.sc.seis.winkle.Event {

    public Event() throws Exception {
        super();
    }
    
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        try {
            VelocityEvent event = extractEvent(req, res);
            List stationList = new ArrayList();
            HashMap resultMap = new HashMap();
            List<ReceiverFunctionResult> results = extractRF(req, res, event);
            for(ReceiverFunctionResult receiverFunctionResult : results) {
                VelocityStation sta = new VelocityStation((StationImpl)receiverFunctionResult.getChannelGroup().getChannel1().getSite().getStation());
                stationList.add(sta);
                resultMap.put(sta, receiverFunctionResult);
            }
            
            RevletContext context = new RevletContext("eventStationList.vm", Start.getDefaultContext());
            Revlet.loadStandardQueryParams(req, context);
            context.put("stationList", stationList);
            context.put("eq", event);
            context.put("recfunc", resultMap);
            return context;
        } catch(ParseException e) {
            return handleNotFound(req, res, e);
        } catch(NotFound e) {
            return handleNotFound(req, res, e);
        }
    }
    
    protected void setContentType(HttpServletRequest request,
                                  HttpServletResponse response)
    {
        response.setContentType("text/html");
    }
    
    public List<ReceiverFunctionResult> extractRF(HttpServletRequest req, HttpServletResponse resp, VelocityEvent event) throws SQLException, FileNotFoundException, FissuresException, IOException {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch());
        
        return RecFuncDB.getSingleton().getStationsByEvent(event.getCacheEvent(), gaussianWidth, minPercentMatch);
        
    }
    
}
