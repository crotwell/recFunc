package edu.sc.seis.receiverFunction.web;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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
        ConnMgr.setURL(props.getProperty("sod.dburl"));
        Set servletStrings = new HashSet();
        ServletHandler sh = new ServletFromSet(servletStrings);
        edu.sc.seis.rev.Start.populateJetty("/networkList.html",
                                            "/networkList.html",
                                            "edu.sc.seis.receiverFunction.web.NetworkList",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.Start.populateJetty("/stationList.html",
                                            "/stationList.html",
                                            "edu.sc.seis.receiverFunction.web.StationList",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.Start.populateJetty("/station.html",
                                            "/station.html",
                                            "edu.sc.seis.receiverFunction.web.Station",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.Start.populateJetty("/summaryHKStack.png",
                                            "/summaryHKStack.png",
                                            "edu.sc.seis.receiverFunction.web.SummaryHKStackImageServlet",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.Start.populateJetty("/stationEvent.html",
                                            "/stationEvent.html",
                                            "edu.sc.seis.receiverFunction.web.RFStationEvent",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.Start.populateJetty("/hkstackimage.png",
                                            "/hkstackimage.png",
                                            "edu.sc.seis.receiverFunction.web.HKStackImageServlet",
                                            servletStrings,
                                            sh);
        edu.sc.seis.rev.Start.runREV(args, sh);
    }
}