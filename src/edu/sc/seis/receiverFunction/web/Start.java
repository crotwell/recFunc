package edu.sc.seis.receiverFunction.web;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import net.sf.ehcache.CacheManager;

import org.apache.log4j.PropertyConfigurator;
import org.apache.velocity.VelocityContext;
import org.mortbay.jetty.servlet.ServletHandler;

import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.FloatQueryParamParser;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.ServletFromSet;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Feb 10, 2005
 */
public class Start {

    public static void main(String[] args) throws Exception {
        Properties props = Initializer.loadProperties(args);
        PropertyConfigurator.configure(props);
        RecFuncDB.setDataLoc(props.getProperty("cormorant.servers.ears.dataloc", RecFuncDB.getDataLoc()));
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(props.getProperty("fissuresUtil.database.url"));
        synchronized(HibernateUtil.class) {
            RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
        }
        logger.info("connecting to database: " + ConnMgr.getURL());
        Set servletStrings = new HashSet();
        ServletHandler rootHandler = new ServletFromSet(servletStrings);
        RevUtil.populateJetty("/index.html",
                              "edu.sc.seis.receiverFunction.web.IndexPage",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/networkList.html",
                              "edu.sc.seis.receiverFunction.web.NetworkList",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/stationList.html",
                              "edu.sc.seis.receiverFunction.web.StationList",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/stationList.txt",
                              "edu.sc.seis.receiverFunction.web.StationList",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/stationCodeList.html",
                              "edu.sc.seis.receiverFunction.web.StationCodeList",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/stationsNearBy.html",
                              "edu.sc.seis.receiverFunction.web.StationsNearBy",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/stationLatLonBox.html",
                              "edu.sc.seis.receiverFunction.web.StationLatLonBox",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/stationLatLonBox.txt",
                              "edu.sc.seis.receiverFunction.web.StationLatLonBox",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/station.html",
                              "edu.sc.seis.receiverFunction.web.Station",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/customStack",
        		              "edu.sc.seis.receiverFunction.web.CustomStack",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/customStackAsXYZ",
                "edu.sc.seis.receiverFunction.web.CustomStackAsXYZ",
                servletStrings,
                rootHandler);
        RevUtil.populateJetty("/summaryHKStack.png",
                              "edu.sc.seis.receiverFunction.web.SummaryHKStackImageServlet",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/stationEvent.html",
                              "edu.sc.seis.receiverFunction.web.RFStationEvent",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/stationEventSimple.html",
                              "edu.sc.seis.receiverFunction.web.RFStationEventSimple",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/hkstackimage.png",
                              "edu.sc.seis.receiverFunction.web.HKStackImageServlet",
                              servletStrings,
                              rootHandler);
        /*
         * RevUtil.populateJetty("/hkphasestackimage.png",
         * "/hkphasestackimage.png",
         * "edu.sc.seis.receiverFunction.web.HKPhaseStackImage", servletStrings,
         * sh);
         */
        RevUtil.populateJetty("/waveforms.png",
                              "edu.sc.seis.receiverFunction.web.SeismogramImage",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/comparePriorResult.html",
                              "edu.sc.seis.receiverFunction.web.ComparePriorResult",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/comparePriorResult.txt",
                              "edu.sc.seis.receiverFunction.web.ComparePriorResultTxt",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/analyticWaveforms.png",
                              "edu.sc.seis.receiverFunction.web.AnalyticPhaseSeismogramImage",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/hklatlon.png",
                              "edu.sc.seis.receiverFunction.web.HKLatLonPlot",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/priorResultList.html",
                              "edu.sc.seis.receiverFunction.web.PriorResultList",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/earthquakeHKPlot.png",
                              "edu.sc.seis.receiverFunction.web.EarthquakeHKPlot",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/sumHKStackAsXYZ.txt",
                              "edu.sc.seis.receiverFunction.web.SumHKStackAsXYZ",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/receiverFunction.zip",
                              "edu.sc.seis.receiverFunction.web.ReceiverFunctionZip",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/overview.txt",
                              "edu.sc.seis.receiverFunction.web.Overview",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/overview.html",
                              "edu.sc.seis.receiverFunction.web.Overview",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/recordSection.png",
                              "edu.sc.seis.receiverFunction.web.RecordSectionImage",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/complexityResidualImage.png",
                              "edu.sc.seis.receiverFunction.web.ComplexityResidualImage",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/complexResidualAsXYZ.txt",
                              "edu.sc.seis.receiverFunction.web.ComplexResidualAsXYZ",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/synthHKImage.png",
                              "edu.sc.seis.receiverFunction.web.SynthHKImage",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/synthHKAsXYZ.txt",
                              "edu.sc.seis.receiverFunction.web.SynthHKAsXYZ",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/crust2TypeStats.html",
                              "edu.sc.seis.receiverFunction.web.Crust2TypeStats",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/crust2GridCompare.html",
                              "edu.sc.seis.receiverFunction.web.Crust2GridCompare",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/crust2GridCompare.txt",
                              "edu.sc.seis.receiverFunction.web.Crust2GridCompare",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/eqrate",
                              "edu.sc.seis.receiverFunction.web.EQRateCalc",
                              servletStrings,
                              rootHandler);
        // jfreechart image servlet
        RevUtil.populateJetty("/DisplayChart",
                              "org.jfree.chart.servlet.DisplayChart",
                              servletStrings,
                              rootHandler);
        RevUtil.populateJetty("/eventSearch.html",
                              "edu.sc.seis.receiverFunction.web.EventSearch",
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
        axisHandler.addServlet("/eventReceiverFunction.zip",
                               "/eventReceiverFunction.zip/*",
                               "edu.sc.seis.receiverFunction.web.EventReceiverFunctionZip");
        List handlers = new ArrayList();
        handlers.add(rootHandler);
        handlers.add(axisHandler);
        handlers.addAll(edu.sc.seis.winkle.Start.loadHandlers(WINKLE,
                                                              servletStrings,
                                                              rootHandler));
        // override winkle Event servlet
        axisHandler.addServlet(WINKLE + "/earthquakes", WINKLE
                + "/earthquakes/*", "edu.sc.seis.receiverFunction.web.Event");
        Revlet.addStandardQueryParam(new FloatQueryParamParser("gaussian",
                                                               Start.getDefaultGaussian()));
        Revlet.addStandardQueryParam(new FloatQueryParamParser("minPercentMatch",
                                                               Start.getDefaultMinPercentMatch()));
        edu.sc.seis.rev.Start.runREV(args, handlers);
    }

    public static VelocityContext getDefaultContext() {
        String warning = "<h2>\n"
                + "<p><b>WARNING:</b>I have found an error in our stacking code, and am recalculating all of the stacks. \n"
                + "Until that finishes there will likely be many stations with missing or wrong stacks. Sorry. </p>\n"
                + "</h2>";
        warning = "";
        VelocityContext context = new VelocityContext();
        context.put("header", "<a href=\""
                + RevletContext.getDefault("revBase") + "\"><img src=\""
                + RevletContext.getDefault("staticFiles")
                + "earslogo.png\"/></a><br/>" + warning);
        ArrayList knownGaussians = new ArrayList();
        if(false) {
            knownGaussians.add(new Float(5));
            knownGaussians.add(new Float(2.5f));
            knownGaussians.add(new Float(1));
            knownGaussians.add(new Float(0.7f));
            knownGaussians.add(new Float(0.4f));
        } else {
            knownGaussians.add(new Float(1.0f));
            knownGaussians.add(new Float(2.5f));
        }
        context.put("knownGaussians", knownGaussians);
        return context;
    }

    public static String getDataLoc() {
        return RecFuncDB.getDataLoc();
    }

    public static VelocityNetwork getNetwork(HttpServletRequest req)
            throws SQLException, NotFound {
        int netDbId = RevUtil.getInt("netdbid", req, -1);
        if(netDbId != -1) {
            return new VelocityNetwork(NetworkDB.getSingleton().getNetwork(netDbId));
        }
        String netCode;
        // also check for netCode to keep google happy
        if (RevUtil.get("netCode", req, null) != null) {
            netCode = RevUtil.get("netCode", req);
        } else {
            netCode = RevUtil.get("netcode", req);
        }
        netCode = netCode.toUpperCase();
        return getNetwork(netCode);
    }

    public static VelocityNetwork getNetwork(String netCode) throws SQLException, NotFound {
        int netDbId = -1;
        String netCodeNoYear = netCode;
        if(netCodeNoYear != null) {
            // check for XE05 case, but allow G and II to not change
            if(netCodeNoYear.length() > 2) {
                netCodeNoYear = netCodeNoYear.substring(0, 2);
            }
            List<NetworkAttrImpl> nets = NetworkDB.getSingleton().getNetworkByCode(netCodeNoYear);
            for(NetworkAttrImpl networkAttrImpl : nets) {
                if(NetworkIdUtil.toStringNoDates(networkAttrImpl.get_id()).equals(netCode)) {
                    return new VelocityNetwork(networkAttrImpl);
                }
            }
        }
        throw new NotFound();
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Start.class);

    public static float getDefaultGaussian() {
        return 2.5f;
    }

    public static float getDefaultMinPercentMatch() {
        return 80;
    }

    public static String WINKLE = "/winkle";
}