package edu.sc.seis.receiverFunction.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.PropertyConfigurator;
import org.apache.velocity.VelocityContext;
import org.mortbay.jetty.servlet.ServletHandler;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.rev.FloatQueryParamParser;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.ServletFromSet;

/**
 * @author crotwell Created on Feb 10, 2005
 */
public class Start {

    public static void main(String[] args) throws Exception {
        Properties props = Initializer.loadProperties(args);
        PropertyConfigurator.configure(props);
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(props.getProperty("fissuresUtil.database.url"));
        logger.info("connecting to database: "+ConnMgr.getURL());
        Set servletStrings = new HashSet();
        ServletHandler rootHandler = new ServletFromSet(servletStrings);
        edu.sc.seis.rev.RevUtil.populateJetty("/index.html",
                                              "edu.sc.seis.receiverFunction.web.IndexPage",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/networkList.html",
                                              "edu.sc.seis.receiverFunction.web.NetworkList",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationList.html",
                                              "edu.sc.seis.receiverFunction.web.StationList",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationList.txt",
                                              "edu.sc.seis.receiverFunction.web.StationList",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationsNearBy.html",
                                              "edu.sc.seis.receiverFunction.web.StationsNearBy",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationLatLonBox.html",
                                              "edu.sc.seis.receiverFunction.web.StationLatLonBox",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationLatLonBox.txt",
                                              "edu.sc.seis.receiverFunction.web.StationLatLonBox",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/station.html",
                                              "edu.sc.seis.receiverFunction.web.Station",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/summaryHKStack.png",
                                              "edu.sc.seis.receiverFunction.web.SummaryHKStackImageServlet",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/stationEvent.html",
                                              "edu.sc.seis.receiverFunction.web.RFStationEvent",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/hkstackimage.png",
                                              "edu.sc.seis.receiverFunction.web.HKStackImageServlet",
                                              servletStrings,
                                              rootHandler);
        /*
         * edu.sc.seis.rev.RevUtil.populateJetty("/hkphasestackimage.png",
         * "/hkphasestackimage.png",
         * "edu.sc.seis.receiverFunction.web.HKPhaseStackImage", servletStrings,
         * sh);
         */
        edu.sc.seis.rev.RevUtil.populateJetty("/waveforms.png",
                                              "edu.sc.seis.receiverFunction.web.SeismogramImage",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/comparePriorResult.html",
                                              "edu.sc.seis.receiverFunction.web.ComparePriorResult",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/comparePriorResult.txt",
                                              "edu.sc.seis.receiverFunction.web.ComparePriorResultTxt",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/analyticWaveforms.png",
                                              "edu.sc.seis.receiverFunction.web.AnalyticPhaseSeismogramImage",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/hklatlon.png",
                                              "edu.sc.seis.receiverFunction.web.HKLatLonPlot",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/priorResultList.html",
                                              "edu.sc.seis.receiverFunction.web.PriorResultList",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/earthquakeHKPlot.png",
                                              "edu.sc.seis.receiverFunction.web.EarthquakeHKPlot",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/sumHKStackAsXYZ.txt",
                                              "edu.sc.seis.receiverFunction.web.SumHKStackAsXYZ",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/receiverFunction.zip",
                                              "edu.sc.seis.receiverFunction.web.ReceiverFunctionZip",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/overview.txt",
                                              "edu.sc.seis.receiverFunction.web.Overview",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/overview.html",
                                              "edu.sc.seis.receiverFunction.web.Overview",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/recordSection.png",
                                              "edu.sc.seis.receiverFunction.web.RecordSectionImage",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/complexityResidualImage.png",
                                              "edu.sc.seis.receiverFunction.web.ComplexityResidualImage",
                                              servletStrings,
                                              rootHandler);
        edu.sc.seis.rev.RevUtil.populateJetty("/synthHKImage.png",
                                              "edu.sc.seis.receiverFunction.web.SynthHKImage",
                                              servletStrings,
                                              rootHandler);
        ServletHandler axisHandler = new ServletHandler();
        axisHandler.addServlet("Earthquakes",
                               "/earthquakes/*",
                               "edu.sc.seis.rev.servlets.EarthquakeServlet");
        axisHandler.addServlet("Network",
                               "/network/*",
                               "edu.sc.seis.receiverFunction.rest.Network");
        axisHandler.addServlet("EarthquakeStation",
                               "/es/*",
                               "edu.sc.seis.rev.servlets.EarthquakeStationServlet");
        List handlers = new ArrayList();
        handlers.add(rootHandler);
        handlers.add(axisHandler);
        Revlet.addStandardQueryParam(new FloatQueryParamParser("gaussian", Start.getDefaultGaussian()));
        Revlet.addStandardQueryParam(new FloatQueryParamParser("minPercentMatch", Start.getDefaultMinPercentMatch()));
        edu.sc.seis.rev.Start.runREV(args, handlers);
    }

    public static VelocityContext getDefaultContext() {
        String warning = "<h2>\n"
                + "<p><b>WARNING:</b>I have found an error in our stacking code, and am recalculating all of the stacks. \n"
                + "Until that finishes there will likely be many stations with missing or wrong stacks. Sorry. </p>\n"
                + "</h2>";
        warning = "";
        
        VelocityContext context = new VelocityContext();
        context.put("header",
                    "<a href=\""+edu.sc.seis.rev.Start.getVisibleURL()+"\"><img src=\"earslogo.png\"/></a><br/>"
                            + warning);
        ArrayList knownGaussians = new ArrayList();
        knownGaussians.add(new Float(5));
        knownGaussians.add(new Float(2.5f));
        knownGaussians.add(new Float(1));
        knownGaussians.add(new Float(0.7f));
        knownGaussians.add(new Float(0.4f));
        context.put("knownGaussians", knownGaussians);
        return context;
    }
    
    public static String getDataLoc() {
        return "../Ears";
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Start.class);

    public static float getDefaultGaussian() {
        return 2.5f;
    }

    public static float getDefaultMinPercentMatch() {
        return 80;
    }
}