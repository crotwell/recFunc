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
        
        String netHTML = "/networkList.html";
        String staForNet = "/stationList.html";
        String station = "/station.html";
        String summaryHKStack = "/summaryHKStack.png";
        String rfstationEventImage = "/hkstackimage.png";
        
        Set servletStrings = new HashSet();
        servletStrings.add(netHTML);
        servletStrings.add(staForNet);
        servletStrings.add(station);
        servletStrings.add(rfstationEventImage);
        servletStrings.add(summaryHKStack);
        
        ServletHandler sh = new ServletFromSet(servletStrings);
        sh.addServlet(netHTML,
                      netHTML,
                      "edu.sc.seis.receiverFunction.web.NetworkList");
        sh.addServlet(staForNet,
                      staForNet,
                      "edu.sc.seis.receiverFunction.web.StationList");
        sh.addServlet(station,
                      station,
                      "edu.sc.seis.receiverFunction.web.Station");
        sh.addServlet(summaryHKStack,
                      summaryHKStack,
                      "edu.sc.seis.receiverFunction.web.SummaryHKStackImageServlet");
//        sh.addServlet("rfStationEvent",
//                      rfstationEvent,
//                      "edu.sc.seis.receiverFunction.web.RFStationEvent");
        sh.addServlet(rfstationEventImage,
                      rfstationEventImage,
                      "edu.sc.seis.receiverFunction.web.HKStackImageServlet");
        edu.sc.seis.rev.Start.runREV(args, sh);}
}
