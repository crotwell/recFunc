package edu.sc.seis.receiverFunction.web;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.apache.velocity.VelocityContext;
import org.mortbay.jetty.servlet.ServletHandler;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.rev.ServletFromSet;

/**
 * @author crotwell Created on Feb 10, 2005
 */
public class Start {

    /**
     *
     */
    public Start() {
        super();
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args) throws Exception {
        Properties props = Initializer.loadProperties(args);
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(props.getProperty("fissuresUtil.database.url"));
        Set servletStrings = new HashSet();
        ServletHandler sh = new ServletFromSet(servletStrings);
        edu.sc.seis.rev.RevUtil.populateJetty("/networkList.html",
                                            "/networkList.html",
                                            "edu.sc.seis.receiverFunction.web.NetworkList",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationList.html",
                                            "/stationList.html",
                                            "edu.sc.seis.receiverFunction.web.StationList",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationsNearBy.html",
                                              "/stationsNearBy.html",
                                              "edu.sc.seis.receiverFunction.web.StationsNearBy",
                                              servletStrings,
                                              sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/station.html",
                                            "/station.html",
                                            "edu.sc.seis.receiverFunction.web.Station",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/summaryHKStack.png",
                                            "/summaryHKStack.png",
                                            "edu.sc.seis.receiverFunction.web.SummaryHKStackImageServlet",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationEvent.html",
                                            "/stationEvent.html",
                                            "edu.sc.seis.receiverFunction.web.RFStationEvent",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/hkstackimage.png",
                                              "/hkstackimage.png",
                                              "edu.sc.seis.receiverFunction.web.HKStackImageServlet",
                                              servletStrings,
                                              sh);
       /* edu.sc.seis.rev.RevUtil.populateJetty("/hkphasestackimage.png",
                                              "/hkphasestackimage.png",
                                              "edu.sc.seis.receiverFunction.web.HKPhaseStackImage",
                                              servletStrings,
                                              sh);*/
        edu.sc.seis.rev.RevUtil.populateJetty("/waveforms.png",
                                              "/waveforms.png",
                                              "edu.sc.seis.receiverFunction.web.SeismogramImage",
                                              servletStrings,
                                              sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/comparePriorResult.html",
                                              "/comparePriorResult.html",
                                              "edu.sc.seis.receiverFunction.web.ComparePriorResult",
                                              servletStrings,
                                              sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/comparePriorResult.txt",
                                              "/comparePriorResult.txt",
                                              "edu.sc.seis.receiverFunction.web.ComparePriorResultTxt",
                                              servletStrings,
                                              sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/analyticWaveforms.png",
                                              "/analyticWaveforms.png",
                                              "edu.sc.seis.receiverFunction.web.AnalyticPhaseSeismogramImage",
                                              servletStrings,
                                              sh);
        
        edu.sc.seis.rev.RevUtil.populateJetty("/hklatlon.png",
                                              "/hklatlon.png",
                                              "edu.sc.seis.receiverFunction.web.HKLatLonPlot",
                                              servletStrings,
                                              sh);
        edu.sc.seis.rev.RevUtil.populateJetty("/priorResultList.html",
                                              "/priorResultList.html",
                                              "edu.sc.seis.receiverFunction.web.PriorResultList",
                                              servletStrings,
                                              sh);
        edu.sc.seis.rev.Start.runREV(args, sh);
    }
    
    public static VelocityContext getDefaultContext() {
        VelocityContext context = new VelocityContext();
        context.put("header", "<a href=\"/ears_tmp\"><img src=\"earslogo.png\"/></a><br/>");
        return context;
    }
}