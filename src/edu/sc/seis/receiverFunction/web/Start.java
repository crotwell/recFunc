package edu.sc.seis.receiverFunction.web;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.mortbay.jetty.servlet.ServletHandler;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.rev.ServletFromSet;


/**
 * @author crotwell
 * Created on Feb 10, 2005
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
        
        String netHTML = "/networks.html";
        String staForNet = "/stations.html";
        String rfstationEvent = "/rfStationEvent.html";
        String rfstationEventImage = "/hkstackimage.png";
        
        Set servletStrings = new HashSet();
        servletStrings.add(netHTML);
        servletStrings.add(staForNet);
        servletStrings.add(rfstationEventImage);
        servletStrings.add(rfstationEvent);
        
        ServletHandler sh = new ServletFromSet(servletStrings);
//        sh.addServlet("Networks",
//                      netHTML,
//                      "edu.sc.seis.viewResult.NetworkList");
//        sh.addServlet("StationEqViewer",
//                      staForNet,
//                      "edu.sc.seis.viewResult.StationList");
//        sh.addServlet("rfStationEvent",
//                      rfstationEvent,
//                      "edu.sc.seis.receiverFunction.web.RFStationEvent");
        sh.addServlet(rfstationEventImage,
                      rfstationEventImage,
                      "edu.sc.seis.receiverFunction.web.HKStackImageServlet");
        edu.sc.seis.rev.Start.runREV(args, sh);}
}
