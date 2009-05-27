package edu.sc.seis.receiverFunction.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.sod.velocity.network.VelocityStation;


public class SummaryCache {
    
    protected SummaryCache() {};
    
    static SummaryCache singleton = null;
    
    public static SummaryCache getSingleton() {
        if (singleton == null) {
            singleton = new SummaryCache();
        }
        return singleton;
    }
    
    public HashMap<VelocityStation, SumHKStack> getSummaries(Float gaussianWidth) {
        checkDataLoaded(gaussianWidth);
        return data.get(gaussianWidth);
    }
    
    synchronized void checkDataLoaded(Float gaussianWidth) {
        if (data.get(gaussianWidth) == null
                || ClockUtil.now().difference(loadtime.get(gaussianWidth)).greaterThan(CACHE_TIME)) {
            loadData(gaussianWidth);
        }
    }
    
    void loadData(float gaussianWidth) {
        HashMap<VelocityStation, SumHKStack> loadData = new HashMap<VelocityStation, SumHKStack>();
        List<SumHKStack> summaryList = RecFuncDB.getSingleton().getAllSumStack(gaussianWidth);
        for (Iterator<SumHKStack> iter = summaryList.iterator(); iter.hasNext();) {
            SumHKStack stack = (SumHKStack) iter.next();
            loadData.put(new VelocityStation(NetworkDB.getSingleton().getStationForNet(stack.getNet(), stack.getStaCode()).get(0))
                    , stack);
            stack.getNumEQ();
        }
        loadtime.put(gaussianWidth, ClockUtil.now());
        data.put(gaussianWidth, loadData);
    }
    
    

    HashMap<Float, HashMap<VelocityStation, SumHKStack>> data = new HashMap<Float, HashMap<VelocityStation, SumHKStack>>();

    HashMap<Float, MicroSecondDate> loadtime = new HashMap<Float, MicroSecondDate>();

    public static TimeInterval CACHE_TIME = new TimeInterval(24, UnitImpl.HOUR);
    
}
