package edu.sc.seis.receiverFunction.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.hibernate.RFInsertion;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;

public class SumStackWorker implements Runnable {

    public SumStackWorker(QuantityImpl smallestH,
                          float minPercentMatch,
                          boolean usePhaseWeight,
                          boolean doBootstrap,
                          int bootstrapIterations) {
        this.smallestH = smallestH;
        this.minPercentMatch = minPercentMatch;
        this.usePhaseWeight = usePhaseWeight;
        this.doBootstrap = doBootstrap;
        this.bootstrapIterations = bootstrapIterations;
    }

    public void run() {
        while(true) {
            RFInsertion insertion = getNext();
            if (insertion != null) {
            processNext(insertion);
            } else {
                try {
                    Thread.sleep(5*60*1000);
                } catch(InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    void processNext(RFInsertion insertion) {
        List<ReceiverFunctionResult> individualHK = RecFuncDB.getSingleton()
                .getSuccessful(insertion.getNet(),
                               insertion.getStaCode(),
                               insertion.getGaussianWidth());
        List<StationImpl> stations = NetworkDB.getSingleton()
                .getStationForNet(insertion.getNet(), insertion.getStaCode());
        Set<RejectedMaxima> rejects = new HashSet<RejectedMaxima>();
        rejects.addAll(RecFuncDB.getSingleton()
                .getRejectedMaxima((NetworkAttrImpl)stations.get(0)
                                           .getNetworkAttr(),
                                   insertion.getStaCode()));
        logger.info("in sum for " + insertion.getNet().get_code() + "."
                + insertion.getStaCode() + " numeq=" + individualHK.size());
        System.out.println("in sum for " + insertion.getNet().get_code() + "."
                + insertion.getStaCode() + " numeq=" + individualHK.size());
        // if there is only 1 eq that matches, then we can't really do a stack
        if(individualHK.size() > 1) {
            SumHKStack sumStack = SumHKStack.calculateForPhase(individualHK,
                                                               smallestH,
                                                               minPercentMatch,
                                                               usePhaseWeight,
                                                               rejects,
                                                               doBootstrap,
                                                               SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS,
                                                               "all");
            TimeOMatic.print("sum for " + insertion.getNet().get_code() + "."
                    + insertion.getStaCode());
            SumHKStack sum = RecFuncDB.getSingleton()
                    .getSumStack(insertion.getNet(),
                                 insertion.getStaCode(),
                                 insertion.getGaussianWidth());
            try {
                if(sum != null) {
                    System.out.println("Old sumstack in db, deleting...");
                    RecFuncDB.getSession().delete(sum);
                    RecFuncDB.getSession().flush();
                }
                calcComplexity(sumStack);
                RecFuncDB.getSingleton().put(sumStack);
                RecFuncDB.getSession().delete(insertion);
                RecFuncDB.commit();
            } catch(TauModelException e) {
                GlobalExceptionHandler.handle(e);
                RecFuncDB.rollback();
                RecFuncDB.getSession().saveOrUpdate(insertion);
                RecFuncDB.commit();
            }
        }
    }
    
    public synchronized RFInsertion getNext() {
        RFInsertion insertion = (RFInsertion)RecFuncDB.getSingleton().getOlderInsertion(RF_AGE_TIME, 2.5f);
        if (insertion == null) {
            return null;
        }
        // side effect in case of lazy loading of fields by hibernate
        insertion.getGaussianWidth();
        insertion.getNet();
        insertion.getInsertTime();
        insertion.getStaCode();
        RecFuncDB.getSession().delete(insertion);
        RecFuncDB.commit();
        return insertion;
    }

    public static float calcComplexity(SumHKStack sumStack)
            throws TauModelException {
        Channel chan = sumStack.getIndividuals()
                .get(0)
                .getChannelGroup()
                .getChannel1();
        HKStack residual = sumStack.getResidual();
        float complex = sumStack.getResidualPower();
        float complex25 = sumStack.getResidualPower(.25f);
        float complex50 = sumStack.getResidualPower(.50f);
        float bestH = (float)sumStack.getSum()
                .getMaxValueH(sumStack.getSmallestH())
                .getValue(UnitImpl.KILOMETER);
        float bestHStdDev = (float)sumStack.getHStdDev()
                .getValue(UnitImpl.KILOMETER);
        float bestK = sumStack.getSum().getMaxValueK(sumStack.getSmallestH());
        float bestKStdDev = (float)sumStack.getKStdDev();
        float bestVal = sumStack.getSum().getMaxValue(sumStack.getSmallestH());
        float hkCorrelation = (float)sumStack.getMixedVariance();
        float nextH = (float)residual.getMaxValueH(sumStack.getSmallestH())
                .getValue(UnitImpl.KILOMETER);
        float nextK = residual.getMaxValueK(sumStack.getSmallestH());
        float nextVal = residual.getMaxValue(sumStack.getSmallestH());
        StationResult crust2Result = HKStack.getCrust2()
                .getStationResult(chan.getSite().getStation());
        float crust2diff = bestH
                - (float)crust2Result.getH().getValue(UnitImpl.KILOMETER);
        sumStack.setComplexityResult(new StackComplexityResult(complex,
                                                               complex25,
                                                               complex50,
                                                               bestH,
                                                               bestHStdDev,
                                                               bestK,
                                                               bestKStdDev,
                                                               bestVal,
                                                               hkCorrelation,
                                                               nextH,
                                                               nextK,
                                                               nextVal,
                                                               crust2diff));
        System.out.println(NetworkIdUtil.toStringNoDates(chan.get_id().network_id)
                + "." + chan.get_id().station_code + " Complexity: " + complex);
        return complex;
    }

    QuantityImpl smallestH;

    float minPercentMatch;

    boolean usePhaseWeight;

    boolean doBootstrap;

    int bootstrapIterations;

    public static final TimeInterval RF_AGE_TIME = new TimeInterval(1, UnitImpl.HOUR);

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SumStackWorker.class);
    
    public static void main(String[] args) {
        
    }
}
