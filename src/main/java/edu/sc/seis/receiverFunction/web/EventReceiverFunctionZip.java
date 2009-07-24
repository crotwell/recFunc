package edu.sc.seis.receiverFunction.web;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.server.CachedResultPlusDbId;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.event.VelocityEvent;


public class EventReceiverFunctionZip extends ReceiverFunctionZip {

    public EventReceiverFunctionZip() throws SQLException,
            ConfigurationException, Exception {
        super();
        eventServlet = new Event();
    }
    
    protected synchronized void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, FileNotFoundException, IOException {
        try {
            List<ReceiverFunctionResult> results;
                VelocityEvent event = eventServlet.extractEvent(req, res);
                results = eventServlet.extractRF(req, res, event);
            
            res.addHeader("Content-Disposition", "inline; filename="+"ears_"+event.getFilizedTime()+".zip");
            processResults(results, req, res);
        } catch(EOFException e) {
            // client has closed the connection, so not much we can do...
            return;
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new ServletException(e);
        }
    }
    
    Event eventServlet;
}
