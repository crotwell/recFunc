package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfParameterMgr.ParameterRef;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.StackMaximum;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.receiverFunction.server.SumStackWorker;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Feb 23, 2005
 */
public class Station extends Revlet {

    public Station() {
        DATA_LOC = RecFuncDB.getDataLoc();
    }

    public synchronized RevletContext getContext(HttpServletRequest req,
                                                 HttpServletResponse res)
            throws Exception {
        VelocityNetwork net;
        try {
            net = Start.getNetwork(req);
        } catch(NotFound e) {
            return handleNotFound(req, res, e);
        }
        // possible that there are multiple stations with the same code
        String staCode = req.getParameter("stacode").toUpperCase();
        TimeOMatic.start();
        List<ReceiverFunctionResult> winners = getWinnerEvents(req);
        TimeOMatic.print("successful events");
        List<ReceiverFunctionResult> losers = getLoserEvents(req);
        TimeOMatic.print("go losers");
        String fileType = RevUtil.getFileType(req);
        String vmFile = "station.vm";
        if(fileType.equals(RevUtil.MIME_CSV)
                || fileType.equals(RevUtil.MIME_TEXT)) {
            vmFile = "stationCSV.vm";
        }
        RevUtil.autoSetContentType(req, res);
        RevletContext context = new RevletContext(vmFile,
                                                  Start.getDefaultContext());
        Revlet.loadStandardQueryParams(req, context);
        SumHKStack summary = null;
        try {
            summary = getSummaryStack(req);
        } catch(NotFound e) {
            // no summary, oh well...
            logger.warn("Got a not found: ", e);
        }
        populateContext(context, net, staCode, summary, winners, losers);
        return context;
    }

    public synchronized void populateContext(RevletContext context,
                                                      VelocityNetwork net, 
                                                      String staCode, 
                                                      SumHKStack summary,
                                                      List<ReceiverFunctionResult> winners, 
                                                      List<ReceiverFunctionResult> losers) {
        ArrayList stationList = getStationList(net.getWrapped(), staCode);
        ArrayList<VelocityEvent> eventList = new ArrayList<VelocityEvent>();
        int numNinty = 0;
        int numEighty = 0;
        for(ReceiverFunctionResult result : winners) {
            eventList.add(result.createVelocityEvent());
            if(result.getRadialMatch() >= 80) {
                numEighty++;
                if(result.getRadialMatch() >= 90) {
                    numNinty++;
                }
            }
        }
        TimeOMatic.print("numEighty");
        ArrayList<VelocityEvent> eventLoserList = new ArrayList<VelocityEvent>();
        for(ReceiverFunctionResult result : losers) {
            eventLoserList.add(result.createVelocityEvent());
        }
        Collections.sort(eventList, itrMatchComparator);
        Collections.reverse(eventList);
        Collections.sort(eventLoserList, itrMatchComparator);
        Collections.reverse(eventLoserList);
        TimeOMatic.print("sort");
        VelocityStation sta = (VelocityStation)stationList.get(0);
        ArrayList markerList = new ArrayList();
        QuantityImpl smallestH = HKStack.getBestSmallestH(sta);
        String crust2Type = "";
        if(crust2 != null) {
            StationResult result = crust2.getStationResult(sta);
            markerList.add(result);
            crust2Type = result.getExtras();
        }
        TimeOMatic.print("crust2");
        List<StationResult> results = RecFuncDB.getSingleton().getPriorResults((NetworkAttrImpl)sta.getNetworkAttr(),
                                                        sta.get_code());
        markerList.addAll(results);
        TimeOMatic.print("other results");
        context.put("crust2Type", crust2Type);
        
        if (summary != null) {
            context.put("summary", summary);
            StackMaximum[] localMaxima = summary.getSum()
                    .getLocalMaxima(smallestH, 5);
            for(int i = 0; i < localMaxima.length; i++) {
                StationResultRef earsStaRef = new StationResultRef(i == 0 ? "Global Maxima"
                                                                           : "Local Maxima "
                                                                                   + i,
                                                                   "ears",
                                                                   "ears");
                String extra = "amp=" + localMaxima[i].getMaxValue();
                List<RejectedMaxima> rejects = RecFuncDB.getSingleton().getRejectedMaxima(net.getWrapped(),
                                                              staCode);
                RejectedMaxima reject = SumHKStack.inAnalystReject(localMaxima[i].getHValue(),
                                                           localMaxima[i].getKValue(),
                                                           rejects);
                if(reject != null) {
                    extra += ", reject by analyst: "+reject.getReason();
                }
                markerList.add(new StationResult(net.getWrapped(),
                                                 staCode,
                                                 localMaxima[i].getHValue(),
                                                 localMaxima[i].getKValue(),
                                                 summary.getSum().getAlpha(),
                                                 earsStaRef,
                                                 extra));
            }
            TimeInterval timePs = summary.getSum().getTimePs();
            DecimalFormat arrivalTimeFormat = new DecimalFormat("0.00");
            timePs.setFormat(arrivalTimeFormat);
            context.put("timePs", timePs);
            TimeInterval timePpPs = summary.getSum().getTimePpPs();
            timePpPs.setFormat(arrivalTimeFormat);
            context.put("timePpPs", timePpPs);
            TimeInterval timePsPs = summary.getSum().getTimePsPs();
            timePsPs.setFormat(arrivalTimeFormat);
            context.put("timePsPs", timePsPs);
        }
        TimeOMatic.print("summary and local maxima");
        context.put("stationList", stationList);
        context.put("stacode", staCode);
        context.put("net", net);
        context.put("eventList", eventList);
        context.put("eventLoserList", eventLoserList);
        context.put("numNinty", new Integer(numNinty));
        int numEvents = winners.size()+losers.size();
        context.put("numEvents", new Integer(numEvents));
        context.put("percentNinty", ""
                + new Float(numNinty * 100f / numEvents));
        context.put("numEighty", new Integer(numEighty));
        context.put("percentEighty",
                    new Float(numEighty * 100f / numEvents));
        context.put("markerList", markerList);
        context.put("smallestH", smallestH);
        context.put("comparePrefix", SumStackWorker.COMPARE_PRIOR_RESULT);
        TimeOMatic.print("done");
    }

    public ArrayList getStationList(NetworkAttrImpl attr, String staCode) {
        List<StationImpl> stations = NetworkDB.getSingleton().getStationForNet(attr, staCode);
        ArrayList stationList = new ArrayList();
        for(StationImpl sta : stations) {
            stationList.add(new VelocityStation(sta));
        }
        return stationList;
    }

    public SumHKStack getSummaryStack(HttpServletRequest req)
            throws SQLException, NotFound, IOException, TauModelException, FissuresException {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch());
        VelocityNetwork net = Start.getNetwork(req);
        String staCode = req.getParameter("stacode");
        return RecFuncDB.getSingleton().getSumStack(net.getWrapped(), staCode, gaussianWidth);
    }

    public List<ReceiverFunctionResult> getWinnerEvents(HttpServletRequest req)
            throws SQLException, NotFound, FileNotFoundException,
            FissuresException, IOException {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        VelocityNetwork net = Start.getNetwork(req);
        String staCode = req.getParameter("stacode");
        return RecFuncDB.getSingleton().getSuccessful(net.getWrapped(),
                                               staCode,
                                               gaussianWidth);
    }

    public List<ReceiverFunctionResult> getLoserEvents(HttpServletRequest req)
            throws SQLException, NotFound {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        VelocityNetwork net = Start.getNetwork(req);
        String staCode = req.getParameter("stacode");
        return RecFuncDB.getSingleton().getUnsuccessful(net.getWrapped(),
                                                      staCode,
                                                      gaussianWidth);
    }

    static File tempDir = new File(System.getProperty("java.io.tmpdir"));

    static final QuantityImpl TEN_KM = new QuantityImpl(10, UnitImpl.KILOMETER);

    transient static Crust2 crust2 = HKStack.getCrust2();

    static ITRMatchComparator itrMatchComparator = new ITRMatchComparator();

    static class ITRMatchComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if(o1.equals(o2)) {
                return 0;
            }
            if(!(o1 instanceof VelocityEvent) || !(o2 instanceof VelocityEvent)) {
                return 0;
            }
            VelocityEvent v1 = (VelocityEvent)o1;
            VelocityEvent v2 = (VelocityEvent)o2;
            float f1 = new Float(v1.getParam("itr_match")).floatValue();
            float f2 = new Float(v2.getParam("itr_match")).floatValue();
            if(f1 > f2)
                return 1;
            if(f1 < f2)
                return -1;
            return 0;
        }
    }

    String DATA_LOC;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Station.class);
}