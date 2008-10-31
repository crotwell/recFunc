package edu.sc.seis.receiverFunction.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import edu.iris.Fissures.Area;
import edu.iris.Fissures.Time;
import edu.iris.Fissures.model.BoxAreaImpl;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.bag.AreaUtil;
import edu.sc.seis.fissuresUtil.cache.WorkerThreadPool;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.hibernate.AbstractHibernateDB;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.sod.hibernate.SodDB;

public class DateRangeResults {

    public DateRangeResults(float minPercentMatch,
                            boolean usePhaseWeight,
                            MicroSecondDate begin,
                            MicroSecondDate end,
                            Area area,
                            float gaussianWidth) {
        super();
        this.minPercentMatch = minPercentMatch;
        this.usePhaseWeight = usePhaseWeight;
        this.begin = begin;
        this.end = end;
        this.area = area;
        this.gaussianWidth = gaussianWidth;
    }

    public void run() {
        RecFuncDB rfdb = RecFuncDB.getSingleton();
        NetworkDB netdb = NetworkDB.getSingleton();
        List<Integer> inArea = new ArrayList();
        for(Iterator iterator = rfdb.getAllSumStack(gaussianWidth).iterator(); iterator.hasNext();) {
            SumHKStack sumHKStack = (SumHKStack)iterator.next();
            StationImpl sta = netdb.getStationForNet(sumHKStack.getNet(),
                                                     sumHKStack.getStaCode())
                    .get(0);
            if(AreaUtil.inArea(area, sta.getLocation())) {
                inArea.add(new Integer(sumHKStack.getDbid()));
            }
        }
        for(Iterator iterator = inArea.iterator(); iterator.hasNext();) {
            DateRangeWorker worker = new DateRangeWorker((Integer)iterator.next());
        }
        do {
            try {
                Thread.sleep(5000);
            } catch(InterruptedException e) {
            }
        } while( pool.isEmployed());
    }
    
    WorkerThreadPool pool = new WorkerThreadPool("dateRange", 4);

    boolean doBootstrap = false;

    float minPercentMatch;

    boolean usePhaseWeight;

    MicroSecondDate begin;

    MicroSecondDate end;

    Area area;

    float gaussianWidth;

    public static void main(String[] args) {
        float minPercentMatch = 80f;
        boolean bootstrap = true;
        boolean usePhaseWeight = true;
        Properties props = StackSummary.loadProps(args);
        RecFuncDB.setDataLoc("Data");
        // ConnMgr.setURL("jdbc:hsqldb:hsql://localhost:9003/ears");
        ConnMgr.setURL("jdbc:postgresql:ears");
        ConnMgr.installDbProperties(props, new String[0]);
        logger.debug("before set up hibernate");
        synchronized(HibernateUtil.class) {
            HibernateUtil.setUpFromConnMgr(props);
            SodDB.configHibernate(HibernateUtil.getConfiguration());
            RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
        }
        AbstractHibernateDB.deploySchema();
        MicroSecondDate begin = new MicroSecondDate(new Time("19900101T00:00:00Z",
                                                             -1));
        MicroSecondDate end = new MicroSecondDate(new Time("20090101T00:00:00Z",
                                                           -1));
        DateRangeResults worker = new DateRangeResults(minPercentMatch,
                                                       usePhaseWeight,
                                                       begin,
                                                       end,
                                                       new BoxAreaImpl(25.0f,
                                                                       50f,
                                                                       -126f,
                                                                       -66f),
                                                       2.5f);
        worker.run();
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DateRangeResults.class);

    class DateRangeWorker implements Runnable {

        DateRangeWorker(Integer i) {
         this.dbid = i;   
        }

        int dbid;

        public void run() {
            SumHKStack sumHKStack = (SumHKStack)RecFuncDB.getSession()
            .get(SumHKStack.class, dbid);
            StationImpl sta = (StationImpl)sumHKStack.getIndividuals()
                    .get(0)
                    .getChannelGroup()
                    .getStation();
            List<ReceiverFunctionResult> individuals = sumHKStack.getIndividuals();
            MicroSecondDate stepEnd = begin;
            TimeInterval step = new TimeInterval(365, UnitImpl.DAY);
            for(stepEnd = begin.add(step); stepEnd.before(end); stepEnd = stepEnd.add(step)) {
                List<ReceiverFunctionResult> byDate = new ArrayList<ReceiverFunctionResult>();
                for(ReceiverFunctionResult rf : individuals) {
                    if(rf.getEvent().getOrigin().getTime().after(begin)
                            && rf.getEvent()
                                    .getOrigin()
                                    .getTime()
                                    .before(stepEnd)) {
                        byDate.add(rf);
                    }
                }
                if(byDate.size() == 0) {
                    continue;
                }
                QuantityImpl smallestH = HKStack.getBestSmallestH(sta,
                                                                  HKStack.getDefaultSmallestH());
                SumHKStack sumStack = SumHKStack.calculateForPhase(byDate,
                                                                   smallestH,
                                                                   minPercentMatch,
                                                                   usePhaseWeight,
                                                                   // sumHKStack.getRejectedMaxima(),
                                                                   new HashSet<RejectedMaxima>(),
                                                                   doBootstrap,
                                                                   SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS,
                                                                   "all");
                System.out.println(stepEnd + " " + sta.getLocation().longitude
                        + " " + sta.getLocation().latitude + " "
                        + sumStack.getBest().formatH() + " "
                        + StationIdUtil.toStringNoDates(sta));
                RecFuncDB.getSession().evict(sumHKStack);
            }
        }
    }
}