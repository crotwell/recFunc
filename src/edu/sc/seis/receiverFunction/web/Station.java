package edu.sc.seis.receiverFunction.web;

import java.awt.Color;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.Time;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.Marker;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.JDBCStationResult;
import edu.sc.seis.receiverFunction.compare.JDBCStationResultRef;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.receiverFunction.compare.WilsonRistra;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.JDBCSummaryHKStack;
import edu.sc.seis.receiverFunction.server.StackSummary;
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
        Connection conn = ConnMgr.createConnection();
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
        jdbcStationResult = new JDBCStationResult(jdbcChannel.getNetworkTable(),
                                                  new JDBCStationResultRef(conn));
    }

    /**
     *
     */
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        int netDbId = RevUtil.getInt("netdbid", req, -1);
        if(netDbId == -1) {
            String netCode = RevUtil.get("netCode", req);
            if(netCode != null
                    && (netCode.length() == 2 || netCode.length() == 4)) {
                NetworkId[] netIds = jdbcChannel.getNetworkTable()
                        .getByCode(netCode);
                for(int i = 0; i < netIds.length; i++) {
                    if(NetworkIdUtil.toStringNoDates(netIds[i]).equals(netCode)) {
                        netDbId = jdbcChannel.getNetworkTable()
                                .getDbId(netIds[0]);
                        break;
                    }
                }
            }
        }
        VelocityNetwork net = getNetwork(netDbId);
        // possible that there are multiple stations with the same code
        String staCode = req.getParameter("stacode");
        ArrayList stationList = getStationList(netDbId, staCode);
        TimeOMatic.start();
        CacheEvent[] events = jdbcRecFunc.getSuccessfulEvents(netDbId, staCode);
        TimeOMatic.print("successful events");
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
        Collections.sort(eventList, itrMatchComparator);
        Collections.reverse(eventList);
        TimeOMatic.print("sort");
        VelocityStation sta = (VelocityStation)stationList.get(0);
        ArrayList markerList = new ArrayList();
        QuantityImpl smallestH = HKStack.getBestSmallestH(sta);
        if(crust2 != null) {
            StationResult result = crust2.getStationResult(sta);
            markerList.add(result);
        }
        TimeOMatic.print("crust2");
        StationResult[] results = jdbcStationResult.get(sta.my_network.get_id(),
                                                        sta.get_code());
        for(int i = 0; i < results.length; i++) {
            markerList.add(results[i]);
        }
        TimeOMatic.print("other results");
        RevletContext context = new RevletContext("station.vm",
                                                  Start.getDefaultContext());
        try {
            int summaryDbId = jdbcSummaryHKStack.getDbIdForStation(net.get_id(),
                                                                   staCode);
            SumHKStack summary = jdbcSummaryHKStack.get(summaryDbId);
            context.put("summary", summary);
            int[][] localMaxima = summary.getSum().getLocalMaxima(smallestH, 5);
            for(int i = 0; i < localMaxima.length; i++) {
                StationResultRef earsStaRef = new StationResultRef(i == 0 ? "Global Maxima"
                                                                           : "Local Maxima "
                                                                                   + i,
                                                                   "ears",
                                                                   "ears");
                markerList.add(new StationResult(net.get_id(),
                                                 staCode,
                                                 summary.getSum()
                                                         .getHFromIndex(localMaxima[i][0]),
                                                 summary.getSum()
                                                         .getKFromIndex(localMaxima[i][1]),
                                                 summary.getSum().getAlpha(),
                                                 earsStaRef));
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
        }
        TimeOMatic.print("summary and local maxima");
        context.put("stationList", stationList);
        context.put("stacode", staCode);
        context.put("net", net);
        context.put("eventList", eventList);
        context.put("numNinty", new Integer(numNinty));
        context.put("numEighty", new Integer(numEighty));
        context.put("markerList", markerList);
        context.put("smallestH", smallestH);
        TimeOMatic.print("done");
        return context;
    }

    public VelocityNetwork getNetwork(int netDbId) throws SQLException,
            NotFound {
        return new VelocityNetwork(jdbcChannel.getStationTable()
                .getNetTable()
                .get(netDbId), netDbId);
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

    static final QuantityImpl TEN_KM = new QuantityImpl(10, UnitImpl.KILOMETER);

    transient static Crust2 crust2 = HKStack.getCrust2();

    static ITRMatchComparator itrMatchComparator = new ITRMatchComparator();

    static class ITRMatchComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            if(o1.equals(o2)) { return 0; }
            if(!(o1 instanceof VelocityEvent) || !(o2 instanceof VelocityEvent)) { return 0; }
            VelocityEvent v1 = (VelocityEvent)o1;
            VelocityEvent v2 = (VelocityEvent)o2;
            float f1 = new Float(v1.getParam("itr_match")).floatValue();
            float f2 = new Float(v2.getParam("itr_match")).floatValue();
            if(f1 > f2) return 1;
            if(f1 < f2) return -1;
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
}