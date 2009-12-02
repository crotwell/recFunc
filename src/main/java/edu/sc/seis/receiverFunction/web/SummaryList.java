package edu.sc.seis.receiverFunction.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.sc.seis.receiverFunction.server.SumStackWorker;
import edu.sc.seis.receiverFunction.server.SummaryLine;
import edu.sc.seis.receiverFunction.summaryFilter.SummaryLineFilter;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;


public abstract class SummaryList extends Revlet {

    public SummaryList() {
    // TODO Auto-generated constructor stub
    }

    @Override
    public RevletContext getContext(HttpServletRequest req, HttpServletResponse res) throws Exception {
        RevletContext context = new RevletContext(getVelocityTemplate(req),
                                                  Start.getDefaultContext());
        Revlet.loadStandardQueryParams(req, context);
        SummaryLineFilter filter = getFilter(req, context);
        List<SummaryLine> summary = SumStackWorker.loadSummaryFromCSV(filter);
        context.put("summary", summary);
        context.put("filter", filter);
        return context;
    }
    
    public abstract String getVelocityTemplate(HttpServletRequest req);
    
    protected abstract SummaryLineFilter getFilter(HttpServletRequest req, RevletContext context) throws Exception;  
}
