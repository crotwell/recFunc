package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.JDBCSummaryHKStack;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Feb 10, 2005
 */
public class StationList extends Revlet {

    public StationList() throws SQLException, ConfigurationException, Exception {
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
        jdbcSumHKStack = new JDBCSummaryHKStack(jdbcHKStack);
    }

    public synchronized RevletContext getContext(HttpServletRequest req,
                                                 HttpServletResponse res)
            throws Exception {
        RevletContext context = new RevletContext(getVelocityTemplate(req),
                                                  Start.getDefaultContext());
        Revlet.loadStandardQueryParams(req, context);
        ArrayList stationList = getStations(req, context);
        cleanStations(stationList);
        logger.debug("getStations done: " + stationList.size());
        HashMap summary = getSummaries(stationList, context, req);
        logger.debug("getSummaries done: " + summary.keySet().size());
        logger.debug("count successful events done");
        context.put("stationList", stationList);
        context.put("summary", summary);
        postProcess(req, context, stationList, summary);
        return context;
    }
    
    public void postProcess(HttpServletRequest req, RevletContext context, ArrayList stationList, HashMap summary) {
    	    return;
    }

    public String getVelocityTemplate(HttpServletRequest req) {
        String path = req.getServletPath();
        if(path == null) {
            path = "";
        }
        if(path.endsWith(".html")) {
            return "stationList.vm";
        } else {
            return "stationListTxt.vm";
        }
    }

    protected void setContentType(HttpServletRequest req,
                                  HttpServletResponse response) {
        String path = req.getServletPath();
        if(path.endsWith(".txt")) {
            response.setContentType("text/plain");
        } else if(path.endsWith(".xml")) {
            response.setContentType("text/xml");
        } else if(path.endsWith(".html")) {
            response.setContentType("text/html");
        } else {
            throw new RuntimeException("Unknown URL: " + req.getRequestURI());
        }
    }

    public ArrayList getStations(HttpServletRequest req, RevletContext context)
            throws SQLException, NotFound {
        VelocityNetwork net = Start.getNetwork(req, jdbcChannel.getStationTable()
                .getNetTable());
        context.put("net", net);
        Station[] stations = jdbcChannel.getSiteTable()
                .getStationTable()
                .getAllStations(net.get_id());
        ArrayList stationList = new ArrayList();
        for(int i = 0; i < stations.length; i++) {
            stationList.add(new VelocityStation(stations[i]));
        }
        return stationList;
    }

    /**
     * Populates a hashmap with keys (objects of type Station) from the list and
     * values of SumHKStack. Also populates the dbid for the stations and
     * network.
     * 
     * @throws SQLException
     * @throws IOException
     */
    public HashMap getSummaries(ArrayList stationList,
                                RevletContext context,
                                HttpServletRequest req) throws SQLException,
            IOException {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch());
        float maxComplexity = RevUtil.getFloat("maxComplexity",
                                               req,
                                               1.0f);
        float minVpvs = RevUtil.getFloat("minVpvs",
                                               req,
                                               0.0f);
        float maxVpvs = RevUtil.getFloat("maxVpvs",
                                               req,
                                               3.0f);
        float minH = RevUtil.getFloat("minH",
                                         req,
                                         -90.0f);
        float maxH = RevUtil.getFloat("maxH",
                                         req,
                                         99999.0f);
        int minEQ = RevUtil.getInt("minEQ",
                                      req,
                                      0);
        int maxEQ = RevUtil.getInt("maxEQ",
                                      req,
                                      99999);
        Iterator it = stationList.iterator();
        HashMap summary = new HashMap();
        while(it.hasNext()) {
            VelocityStation sta = (VelocityStation)it.next();
            try {
                sta.setDbId(jdbcChannel.getStationTable().getDBId(sta.get_id()));
                int netDbId = jdbcChannel.getNetworkTable()
                        .getDbId(sta.getNet().get_id());
                sta.getNet().setDbId(netDbId);
                SumHKStack sumStack = jdbcSumHKStack.getForStation(netDbId,
                                                                   sta.get_code(),
                                                                   gaussianWidth,
                                                                   minPercentMatch,
                                                                   false);
                float bestVpvs = sumStack.getComplexityResult().getBestK();
                float bestH = (float)sumStack.getComplexityResult().getBestH();
                if (sumStack.getComplexityResidual() <= maxComplexity &&
                        bestVpvs >= minVpvs && bestVpvs <= maxVpvs &&
                        bestH >= minH && bestH <= maxH &&
                        sumStack.getNumEQ() >= minEQ && sumStack.getNumEQ() <= maxEQ) {
                    summary.put(sta, sumStack);
                }
            } catch(NotFound e) {
                // oh well, skip this station
                logger.warn("not found for " + sta.getNetCode() + "."
                        + sta.get_code());
            }
        }
        logger.debug("found " + summary.size() + " summaries");
        return summary;
    }

    /** weed out stations with same net and station code to avoid duplicates
     in list. */
    public static void cleanStations(ArrayList stationList) {
        HashMap codeMap = new HashMap();
        Iterator it = stationList.iterator();
        while(it.hasNext()) {
            Station sta = (Station)it.next();
            String key = StationIdUtil.toStringNoDates(sta.get_id());
            if(codeMap.containsKey(key)) {
                Station previousSta = (Station)codeMap.get(key);
                MicroSecondDate staBegin = new MicroSecondDate(sta.effective_time.start_time);
                MicroSecondDate previousStaBegin = new MicroSecondDate(previousSta.effective_time.start_time);
                if(staBegin.after(previousStaBegin)) {
                    codeMap.put(key, sta);
                }
            } else {
                codeMap.put(key, sta);
            }
        }
        stationList.clear();
        stationList.addAll(codeMap.values());
    }

    public HashMap cleanSummaries(ArrayList stationList, HashMap summary) {
        logger.debug("before cleanSummaries stationList.size()="
                + stationList.size() + "  summary.size()=" + summary.size());
        Iterator it = stationList.iterator();
        while(it.hasNext()) {
            Object next = it.next();
            if(summary.get(next) == null) {
                summary.remove(next);
                it.remove();
            }
        }
        logger.debug("after cleanSummaries stationList.size()="
                + stationList.size() + "  summary.size()=" + summary.size());
        return summary;
    }

    String DATA_LOC;

    JDBCEventAccess jdbcEventAccess;

    JDBCChannel jdbcChannel;

    JDBCHKStack jdbcHKStack;

    JDBCRecFunc jdbcRecFunc;

    JDBCSodConfig jdbcSodConfig;

    JDBCSummaryHKStack jdbcSumHKStack;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StationList.class);
}
