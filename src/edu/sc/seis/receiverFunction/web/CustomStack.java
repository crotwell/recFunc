package edu.sc.seis.receiverFunction.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.FissuresException;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.StackComplexity;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.server.CachedResultPlusDbId;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;


public class CustomStack extends Station {
    
    public CustomStack() throws SQLException, ConfigurationException, Exception {
        super();
    }

    public SumHKStack getSummaryStack(HttpServletRequest req) throws SQLException, NotFound, IOException, TauModelException {
        int[] dbids = parseDbIds(req);
        HKStack[] plots = new HKStack[dbids.length];
        for(int i = 0; i < dbids.length; i++) {
            plots[i] = jdbcHKStack.get(dbids[i]);
            plots[i].compact();
        }
        SumHKStack sumStack = new SumHKStack(plots,
                                             plots[0].getChannel(),
                                             80,
                                             HKStack.getDefaultSmallestH(),
                                             false,
                                             true);
        sumStack.calcStackComplexity();
        return sumStack;
    }

    public CacheEvent[] getWinnerEvents(HttpServletRequest req) throws SQLException, NotFound, FileNotFoundException, FissuresException, IOException {
        int[] dbids = parseDbIds(req);
        
        CacheEvent[] events = new CacheEvent[dbids.length];
        for(int i = 0; i < dbids.length; i++) {
            CachedResultPlusDbId result = jdbcRecFunc.get(dbids[i]);
            if (result != null) {
                events[i] = result.getEvent();
                JDBCRecFunc.addToParms(events[i].getOrigin(), result.getCachedResult().radialMatch, dbids[i]);
            } else {
                throw new RuntimeException("no receiver function found for dbid="+dbids[i]);
            }
        }
        return events;
        
    }
    
    int[] parseDbIds(HttpServletRequest req) {
        String dbidStr = RevUtil.get("recfunc_id", req);
        StringTokenizer tokenizer = new StringTokenizer(dbidStr, 
                                                        ",");
        ArrayList idArray = new ArrayList();
        while(tokenizer.hasMoreTokens()) {
            idArray.add(Integer.valueOf(tokenizer.nextToken()));
        }
        int[] ids = new int[idArray.size()];
        Iterator it = idArray.iterator();
        int next = 0;
        while(it.hasNext()) {
            ids[next++] = ((Integer)it.next()).intValue();
        }
        return ids;
    }

    public CacheEvent[] getLoserEvents(HttpServletRequest req) throws SQLException, NotFound {
        return new CacheEvent[0];
    }
    
}
