package edu.sc.seis.receiverFunction.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.server.CachedResultPlusDbId;
import edu.sc.seis.receiverFunction.server.HKBox;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCRejectedMaxima;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;

public class CustomStack extends Station {

    public CustomStack() throws SQLException, ConfigurationException, Exception {
        super();
    }

    public SumHKStack getSummaryStack(HttpServletRequest req)
            throws SQLException, NotFound, IOException, TauModelException,
            FissuresException {
        return calcCustomStack(req, jdbcHKStack, jdbcRejectMax);
    }

    public static SumHKStack calcCustomStack(HttpServletRequest req,
                                             JDBCHKStack jdbcHKStack,
                                             JDBCRejectedMaxima jdbcRejectMax)
            throws SQLException, NotFound, IOException, TauModelException,
            FissuresException {
        int[] dbids = parseDbIds(req);
        if(dbids.length == 0) {
            throw new RuntimeException("No dbids found in query params");
        }
        HKStack[] plots = new HKStack[dbids.length];
        if(RevUtil.exists("vp", req)) {
            // custom vp, so we need to recreate the HK stacks
            for(int i = 0; i < dbids.length; i++) {
                plots[i] = jdbcHKStack.calc(dbids[i],
                                            JDBCHKStack.DEFAULT_WEIGHT_Ps,
                                            JDBCHKStack.DEFAULT_WEIGHT_PpPs,
                                            JDBCHKStack.DEFAULT_WEIGHT_PsPs,
                                            false,
                                            new QuantityImpl(RevUtil.getFloat("vp", req), UnitImpl.KILOMETER_PER_SECOND));
                plots[i].compact();
            }
        } else {
            for(int i = 0; i < dbids.length; i++) {
                try {
                    plots[i] = jdbcHKStack.get(dbids[i]);
                } catch(NotFound n) {
                    plots[i] = jdbcHKStack.calc(dbids[i], true);
                }
                plots[i].compact();
            }
        }
        boolean doBootstrap = RevUtil.getBoolean("bootstrap", req, false);
        int netDbId = Start.getNetwork(req,
                                       jdbcHKStack.getJDBCChannel()
                                               .getNetworkTable()).getDbId();
        HKBox[] rejects = jdbcRejectMax.getForStation(netDbId,
                                                      req.getParameter("stacode"));
        SumHKStack sumStack = new SumHKStack(plots,
                                             plots[0].getChannel(),
                                             80,
                                             HKStack.getDefaultSmallestH(),
                                             doBootstrap,
                                             true,
                                             rejects);
        sumStack.calcStackComplexity();
        return sumStack;
    }

    public CacheEvent[] getWinnerEvents(HttpServletRequest req)
            throws SQLException, NotFound, FileNotFoundException,
            FissuresException, IOException {
        int[] dbids = parseDbIds(req);
        CacheEvent[] events = new CacheEvent[dbids.length];
        for(int i = 0; i < dbids.length; i++) {
            CachedResultPlusDbId result = jdbcRecFunc.get(dbids[i]);
            if(result != null) {
                events[i] = result.getEvent();
                JDBCRecFunc.addToParms(events[i].getOrigin(),
                                       result.getCachedResult().radialMatch,
                                       dbids[i]);
            } else {
                throw new RuntimeException("no receiver function found for dbid="
                        + dbids[i]);
            }
        }
        return events;
    }

    static int[] parseDbIds(HttpServletRequest req) {
        String dbidStr = RevUtil.get("recfunc_id", req);
        StringTokenizer tokenizer = new StringTokenizer(dbidStr, ",");
        ArrayList idArray = new ArrayList();
        while(tokenizer.hasMoreTokens()) {
            idArray.add(Integer.valueOf(tokenizer.nextToken().trim()));
        }
        int[] ids = new int[idArray.size()];
        Iterator it = idArray.iterator();
        int next = 0;
        while(it.hasNext()) {
            ids[next++] = ((Integer)it.next()).intValue();
        }
        return ids;
    }

    public CacheEvent[] getLoserEvents(HttpServletRequest req)
            throws SQLException, NotFound {
        return new CacheEvent[0];
    }
}
