package edu.sc.seis.receiverFunction.web;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.receiverFunction.server.CachedResultPlusDbId;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.event.VelocityEvent;


public class EventReceiverFunctionZip extends ReceiverFunctionZip {

    public EventReceiverFunctionZip() throws SQLException,
            ConfigurationException, Exception {
        super();
        eventServlet = new Event(jdbcRecFunc);
    }
    
    protected synchronized void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, FileNotFoundException, IOException {
        try {
            CachedResultPlusDbId[] resultsWithDbId;
            synchronized(jdbcRecFunc.getConnection()) {
                VelocityEvent event = eventServlet.extractEvent(req, res);
                resultsWithDbId = eventServlet.extractRF(req, res, event, true);
            }
            CachedResult[] result = new CachedResult[resultsWithDbId.length];
            for(int i = 0; i < result.length; i++) {
                result[i] = resultsWithDbId[i].getCachedResult();
            }
            processResults(result, req, res);
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
