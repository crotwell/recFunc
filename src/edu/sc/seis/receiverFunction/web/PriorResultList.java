package edu.sc.seis.receiverFunction.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;

import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;


/**
 * @author crotwell
 * Created on Aug 9, 2005
 */
public class PriorResultList extends Revlet {
    
    public synchronized RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        VelocityContext velContext = new VelocityContext(Start.getDefaultContext());
        RevletContext context = new RevletContext("priorResultList.vm", velContext);
        Revlet.loadStandardQueryParams(req, context);
        List<StationResultRef> refs = RecFuncDB.getSingleton().getAllPriorResultsRef();
        velContext.put("priorResults", refs);
        return context;
    }
    
}
