package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.StackMaximum;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.JDBCStationResult;
import edu.sc.seis.receiverFunction.compare.JDBCStationResultRef;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.server.HKBox;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCRejectedMaxima;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.JDBCSummaryHKStack;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.FissuresFormatter;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Feb 23, 2005
 */
public class Station extends Revlet {

    public Station() throws SQLException, ConfigurationException, Exception {
        DATA_LOC = Start.getDataLoc();
        Connection conn = getConnection();
        jdbcEventAccess = new JDBCEventAccess(conn);
        jdbcChannel = new JDBCChannel(conn);
        jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      DATA_LOC);
        jdbcHKStack = new JDBCHKStack(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      jdbcRecFunc);
        jdbcSummaryHKStack = new JDBCSummaryHKStack(jdbcHKStack);
        jdbcRejectMax = new JDBCRejectedMaxima(conn);
        jdbcStationResult = new JDBCStationResult(jdbcChannel.getNetworkTable(),
                                                  new JDBCStationResultRef(conn));
    }

    /**
     * 
     */
    public synchronized RevletContext getContext(HttpServletRequest req,
                                                 HttpServletResponse res)
            throws Exception {
        VelocityNetwork net;
        try {
            net = Start.getNetwork(req, jdbcChannel.getNetworkTable());
        } catch(NotFound e) {
            return handleNotFound(res);
        }
        // possible that there are multiple stations with the same code
        String staCode = req.getParameter("stacode");
        ArrayList stationList = getStationList(net.getDbId(), staCode);
        TimeOMatic.start();
        CacheEvent[] events = getWinnerEvents(req);
        TimeOMatic.print("successful events");
        CacheEvent[] loserEvents = getLoserEvents(req);
        TimeOMatic.print("go losers");
        ArrayList eventList = new ArrayList();
        int numNinty = 0;
        int numEighty = 0;
        for(int i = 0; i < events.length; i++) {
            VelocityEvent ve = new VelocityEvent(events[i]);
            eventList.add(ve);
            float match = new Float(ve.getParam("itr_match")).floatValue();
            if(match >= 80) {
                numEighty++;
                if(match >= 90) {
                    numNinty++;
                }
            }
        }
        TimeOMatic.print("numEighty");
        ArrayList eventLoserList = new ArrayList();
        for(int i = 0; i < loserEvents.length; i++) {
            VelocityEvent ve = new VelocityEvent(loserEvents[i]);
            eventLoserList.add(ve);
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
        StationResult[] results = jdbcStationResult.get(sta.my_network.get_id(),
                                                        sta.get_code());
        for(int i = 0; i < results.length; i++) {
            markerList.add(results[i]);
        }
        TimeOMatic.print("other results");
        String fileType = RevUtil.getFileType(req);
        String vmFile = "station.vm";
        if(fileType.equals(RevUtil.MIME_CSV)
                || fileType.equals(RevUtil.MIME_TEXT)) {
            vmFile = "stationCSV.vm";
        }
        RevUtil.autoSetContentType(req, res);
        RevletContext context = new RevletContext(vmFile,
                                                  Start.getDefaultContext());
        context.put("crust2Type", crust2Type);
        Revlet.loadStandardQueryParams(req, context);
        try {
            SumHKStack summary = getSummaryStack(req);
            context.put("summary", summary);
            String sumHKPlot = makeHKPlot(summary, req.getSession());
            context.put("sumHKPlot", sumHKPlot);
            StackMaximum[] localMaxima = summary.getSum()
                    .getLocalMaxima(smallestH, 5);
            for(int i = 0; i < localMaxima.length; i++) {
                StationResultRef earsStaRef = new StationResultRef(i == 0 ? "Global Maxima"
                                                                           : "Local Maxima "
                                                                                   + i,
                                                                   "ears",
                                                                   "ears");
                String extra = "amp=" + localMaxima[i].getMaxValue();
                HKBox[] rejects = jdbcRejectMax.getForStation(net.getDbId(),
                                                              staCode);
                int rejectNum = SumHKStack.inAnalystReject(localMaxima[i].getHValue(),
                                                           localMaxima[i].getKValue(),
                                                           rejects);
                if(-1 != rejectNum) {
                    extra += ", reject by analyst: "+rejects[rejectNum].getReason();
                }
                markerList.add(new StationResult(net.get_id(),
                                                 staCode,
                                                 localMaxima[i].getHValue(),
                                                 localMaxima[i].getKValue(),
                                                 summary.getSum().getAlpha(),
                                                 earsStaRef,
                                                 extra));
            }
            TimeInterval timePs = summary.getSum().getTimePs();
            timePs.setFormat(FissuresFormatter.getDepthFormat());
            context.put("timePs", timePs);
            TimeInterval timePpPs = summary.getSum().getTimePpPs();
            timePpPs.setFormat(FissuresFormatter.getDepthFormat());
            context.put("timePpPs", timePpPs);
            TimeInterval timePsPs = summary.getSum().getTimePsPs();
            timePsPs.setFormat(FissuresFormatter.getDepthFormat());
            context.put("timePsPs", timePsPs);
        } catch(NotFound e) {
            // no summary, oh well...
            logger.warn("Got a not found: ", e);
        }
        TimeOMatic.print("summary and local maxima");
        String azPlotname = AzimuthPlot.plot((VelocityStation)stationList.get(0),
                                             (VelocityEvent[])eventList.toArray(new VelocityEvent[0]),
                                             req.getSession());
        context.put("azPlot", azPlotname);
        context.put("stationList", stationList);
        context.put("stacode", staCode);
        context.put("net", net);
        context.put("eventList", eventList);
        context.put("eventLoserList", eventLoserList);
        context.put("numNinty", new Integer(numNinty));
        context.put("percentNinty", ""
                + new Float(numNinty * 100f / events.length));
        context.put("numEighty", new Integer(numEighty));
        context.put("percentEighty",
                    new Float(numEighty * 100f / events.length));
        context.put("markerList", markerList);
        context.put("smallestH", smallestH);
        TimeOMatic.print("done");
        return context;
    }

    public ArrayList getStationList(int netDbId, String staCode)
            throws SQLException, NotFound {
        int[] dbids = jdbcChannel.getSiteTable()
                .getStationTable()
                .getDBIds(netDbId, staCode);
        ArrayList stationList = new ArrayList();
        for(int i = 0; i < dbids.length; i++) {
            stationList.add(new VelocityStation(jdbcChannel.getSiteTable()
                    .getStationTable()
                    .get(dbids[i]), dbids[i]));
        }
        return stationList;
    }

    public SumHKStack getSummaryStack(HttpServletRequest req)
            throws SQLException, NotFound, IOException, TauModelException {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch());
        VelocityNetwork net = Start.getNetwork(req,
                                               jdbcChannel.getNetworkTable());
        String staCode = req.getParameter("stacode");
        int summaryDbId = jdbcSummaryHKStack.getDbIdForStation(net.get_id(),
                                                               staCode,
                                                               gaussianWidth,
                                                               minPercentMatch);
        return jdbcSummaryHKStack.get(summaryDbId);
    }

    public CacheEvent[] getWinnerEvents(HttpServletRequest req)
            throws SQLException, NotFound, FileNotFoundException,
            FissuresException, IOException {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        VelocityNetwork net = Start.getNetwork(req,
                                               jdbcChannel.getNetworkTable());
        String staCode = req.getParameter("stacode");
        return jdbcRecFunc.getSuccessfulEvents(net.getDbId(),
                                               staCode,
                                               gaussianWidth,
                                               80);
    }

    public CacheEvent[] getLoserEvents(HttpServletRequest req)
            throws SQLException, NotFound {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        VelocityNetwork net = Start.getNetwork(req,
                                               jdbcChannel.getNetworkTable());
        String staCode = req.getParameter("stacode");
        return jdbcRecFunc.getUnsuccessfulEvents(net.getDbId(),
                                                 staCode,
                                                 gaussianWidth,
                                                 80);
    }

    public static String makeHKPlot(SumHKStack sumStack, HttpSession session)
            throws IOException {
        String prefix = "sumHKPlot-";
        File pngFile = File.createTempFile(prefix,
                                           ".png",
                                           AzimuthPlot.getTempDir());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(pngFile));
        BufferedImage image = sumStack.createStackImage();
        ImageIO.write(image, "png", out);
        out.close();
        if(session != null) {
            JFreeChartServletUtilities.registerForDeletion(pngFile, session);
        }
        return AzimuthPlot.makeDisplayFilename(pngFile.getName());
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

    JDBCEventAccess jdbcEventAccess;

    JDBCChannel jdbcChannel;

    JDBCHKStack jdbcHKStack;

    JDBCSummaryHKStack jdbcSummaryHKStack;

    JDBCRecFunc jdbcRecFunc;

    JDBCSodConfig jdbcSodConfig;

    JDBCStationResult jdbcStationResult;

    JDBCRejectedMaxima jdbcRejectMax;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Station.class);
}