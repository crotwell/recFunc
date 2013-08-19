package edu.sc.seis.receiverFunction.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;

public class CustomStack extends Station {

    public CustomStack() throws SQLException, ConfigurationException, Exception {
        super();
    }

    public SumHKStack getSummaryStack(HttpServletRequest req)
            throws SQLException, NotFound, IOException, TauModelException,
            FissuresException {
        return calcCustomStack(req);
    }

    public static SumHKStack calcCustomStack(HttpServletRequest req)
            throws SQLException, NotFound, IOException, TauModelException,
            FissuresException {
        int[] dbids = parseDbIds(req);
        if(dbids.length == 0) {
            throw new RuntimeException("No dbids found in query params");
        }
        HKStack[] plots = new HKStack[dbids.length];
        List<ReceiverFunctionResult> rf = new ArrayList<ReceiverFunctionResult>();
        RecFuncDB rfdb = RecFuncDB.getSingleton();
        if(RevUtil.exists("vp", req)) {
            // custom vp, so we need to recreate the HK stacks
            for(int i = 0; i < dbids.length; i++) {
                ReceiverFunctionResult result = rfdb.getReceiverFunctionResult(dbids[i]);
                rf.add(result);
                plots[i] = HKStack.create(result,
                                            HKStack.DEFAULT_WEIGHT_Ps,
                                            HKStack.DEFAULT_WEIGHT_PpPs,
                                            HKStack.DEFAULT_WEIGHT_PsPs,
                                            new QuantityImpl(RevUtil.getFloat("vp", req), UnitImpl.KILOMETER_PER_SECOND));
                plots[i].compact();
            }
        } else {
            for(int i = 0; i < dbids.length; i++) {
                ReceiverFunctionResult result = rfdb.getReceiverFunctionResult(dbids[i]);
                rf.add(result);
                    plots[i] = result.getHKstack();
                if (plots[i] == null) {
                    plots[i] = HKStack.create(result,
                                               HKStack.DEFAULT_WEIGHT_Ps,
                                               HKStack.DEFAULT_WEIGHT_PpPs,
                                               HKStack.DEFAULT_WEIGHT_PsPs);
                }
                plots[i].compact();
            }
        }
        boolean doBootstrap = RevUtil.getBoolean("bootstrap", req, false);
        NetworkAttrImpl net = Start.getNetwork(req).getWrapped();
        Set<RejectedMaxima> rejects = new HashSet<RejectedMaxima>();
        rejects.addAll(rfdb.getRejectedMaxima(net, req.getParameter("stacode")));
        SumHKStack sumStack = SumHKStack.calculateForPhase(rf, 
                                                           HKStack.getDefaultSmallestH(), 
                                                           0, true, rejects, doBootstrap, 
                                                           SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS, "all");
        sumStack.calcStackComplexity();
        return sumStack;
    }

    public List<ReceiverFunctionResult> getWinnerEvents(HttpServletRequest req)
            throws NotFound, FileNotFoundException,
            FissuresException, IOException {
        int[] dbids = parseDbIds(req);
        List<ReceiverFunctionResult> out = new ArrayList<ReceiverFunctionResult>();
        RecFuncDB rfdb = RecFuncDB.getSingleton();
        for(int i = 0; i < dbids.length; i++) {
            ReceiverFunctionResult result = rfdb.getReceiverFunctionResult(dbids[i]);
            if(result != null) {
                out.add(result);
            } else {
                throw new RuntimeException("no receiver function found for dbid="
                        + dbids[i]);
            }
        }
        return out;
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

    public List<ReceiverFunctionResult> getLoserEvents(HttpServletRequest req)
            throws SQLException, NotFound {
        return new ArrayList<ReceiverFunctionResult>();
    }
}
