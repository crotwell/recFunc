package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ImageMapUtilities;
import org.jfree.chart.servlet.ServletUtilities;

import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.receiverFunction.server.SumStackWorker;
import edu.sc.seis.receiverFunction.server.SummaryLine;
import edu.sc.seis.receiverFunction.summaryFilter.SummaryLineFilter;
import edu.sc.seis.rev.RevUtil;
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
        makeChart(req, context, summary);
        return context;
    }

    public static void makeChart(HttpServletRequest req,
                            RevletContext context,
                            List<SummaryLine> summary) {
        String titleString = "Ears results";
        JFreeChart chart = HKLatLonPlot.getChart(req,
                                                 summary,
                                                 titleString);
        try {
            ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
            String filename = ServletUtilities.saveChartAsPNG(chart,
                                                              RevUtil.getInt("xdim",
                                                                             req,
                                                                             HKLatLonPlot.xdimDefault),
                                                              RevUtil.getInt("ydim",
                                                                             req,
                                                                             HKLatLonPlot.ydimDefault),
                                                              info,
                                                              req.getSession());
            context.put("plotname", filename);
            context.put("imagemap", ImageMapUtilities.getImageMap(filename,
                                                                  info));
        } catch(IOException e) {
            GlobalExceptionHandler.handle(e);
        }
        return;
    }
    
    public abstract String getVelocityTemplate(HttpServletRequest req);
    
    protected abstract SummaryLineFilter getFilter(HttpServletRequest req, RevletContext context) throws Exception;  
}
