package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.Hilbert;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.receiverFunction.compare.StationResult;

/**
 * @author crotwell Created on Apr 28, 2005
 */
public class HKPhaseStack extends HKStack {

    /**
     *
     */
    public HKPhaseStack(QuantityImpl alpha, float p, float percentMatch,
            QuantityImpl minH, QuantityImpl stepH, int numH, float minK,
            float stepK, int numK, float weightPs, float weightPpPs,
            float weightPsPs) {
        super(alpha,
              p,
              percentMatch,
              minH,
              stepH,
              numH,
              minK,
              stepK,
              numK,
              weightPs,
              weightPpPs,
              weightPsPs);
        // TODO Auto-generated constructor stub
    }

    /**
     *
     */
    public HKPhaseStack(QuantityImpl alpha, float p, float percentMatch,
            QuantityImpl minH, QuantityImpl stepH, int numH, float minK,
            float stepK, int numK, float weightPs, float weightPpPs,
            float weightPsPs, DataSetSeismogram recFunc)
            throws FissuresException {
        super(alpha,
              p,
              percentMatch,
              minH,
              stepH,
              numH,
              minK,
              stepK,
              numK,
              weightPs,
              weightPpPs,
              weightPsPs,
              recFunc);
        // TODO Auto-generated constructor stub
    }

    /**
     *
     */
    public HKPhaseStack(QuantityImpl alpha, float p, float percentMatch,
            QuantityImpl minH, QuantityImpl stepH, int numH, float minK,
            float stepK, int numK, float weightPs, float weightPpPs,
            float weightPsPs, LocalSeismogramImpl recFuncSeis, Channel chan,
            TimeInterval shift) throws FissuresException {
        super(alpha,
              p,
              percentMatch,
              minH,
              stepH,
              numH,
              minK,
              stepK,
              numK,
              weightPs,
              weightPpPs,
              weightPsPs,
              recFuncSeis,
              chan,
              shift);
        // TODO Auto-generated constructor stub
    }

    /**
     *
     */
    public HKPhaseStack(QuantityImpl alpha, float p, float percentMatch,
            QuantityImpl minH, QuantityImpl stepH, int numH, float minK,
            float stepK, int numK, float weightPs, float weightPpPs,
            float weightPsPs, float[][] stack) {
        super(alpha,
              p,
              percentMatch,
              minH,
              stepH,
              numH,
              minK,
              stepK,
              numK,
              weightPs,
              weightPpPs,
              weightPsPs,
              stack);
        // TODO Auto-generated constructor stub
    }

    /**
     *
     */
    public HKPhaseStack(QuantityImpl alpha, float p, float percentMatch,
            QuantityImpl minH, QuantityImpl stepH, int numH, float minK,
            float stepK, int numK, float weightPs, float weightPpPs,
            float weightPsPs, float[][] stack, DataSetSeismogram recFunc) {
        super(alpha,
              p,
              percentMatch,
              minH,
              stepH,
              numH,
              minK,
              stepK,
              numK,
              weightPs,
              weightPpPs,
              weightPsPs,
              stack,
              recFunc);
        // TODO Auto-generated constructor stub
    }

    /**
     *
     */
    public HKPhaseStack(QuantityImpl alpha, float p, float percentMatch,
            QuantityImpl minH, QuantityImpl stepH, int numH, float minK,
            float stepK, int numK, float weightPs, float weightPpPs,
            float weightPsPs, float[][] stack, Channel chan) {
        super(alpha,
              p,
              percentMatch,
              minH,
              stepH,
              numH,
              minK,
              stepK,
              numK,
              weightPs,
              weightPpPs,
              weightPsPs,
              stack,
              chan);
        // TODO Auto-generated constructor stub
    }

    public static HKStack create(CachedResult cachedResult,
                                 float weightPs,
                                 float weightPpPs,
                                 float weightPsPs) throws TauModelException, FissuresException {
        return create(cachedResult, weightPs, weightPpPs, weightPsPs, 
                      crust2.getStationResult(cachedResult.channels[0].my_site.my_station));
    }
    
    public static HKStack create(CachedResult cachedResult,
                                 float weightPs,
                                 float weightPpPs,
                                 float weightPsPs,
                                 StationResult staResult) throws TauModelException, FissuresException {
        String[] pPhases = {"P"};

        TauPUtil tauPTime = TauPUtil.getTauPUtil(modelName);
        Arrival[] arrivals = tauPTime.calcTravelTimes(cachedResult.channels[0].my_site.my_station,
                                                      cachedResult.prefOrigin,
                                                      pPhases);
        // convert radian per sec ray param into km per sec
        float kmRayParam = (float)(arrivals[0].getRayParam() / tauPTime.getTauModel()
                .getRadiusOfEarth());
        QuantityImpl smallestH = new QuantityImpl(25, UnitImpl.KILOMETER);
        HKPhaseStack stack = new HKPhaseStack(staResult.getVp(),
                                    kmRayParam,
                                    cachedResult.radialMatch,
                                    getBestSmallestH(cachedResult.channels[0].my_site.my_station, smallestH),
                                    new QuantityImpl(.25f, UnitImpl.KILOMETER),
                                    240,
                                    1.6f,
                                    .0025f,
                                    200,
                                    weightPs,
                                    weightPpPs,
                                    weightPsPs,
                                    (LocalSeismogramImpl)cachedResult.radial,
                                    cachedResult.channels[0],
                                    RecFunc.getDefaultShift());
        return stack;
    }
    
    void calculate(LocalSeismogramImpl seis, QuantityImpl shift)
            throws FissuresException {
        //set up analytic signal
        Hilbert hilbert = new Hilbert();
        Cmplx[] analytic = hilbert.analyticSignal(seis);
        double[] dblPhase = hilbert.phase(analytic);
        float[] fltPhase = new float[dblPhase.length];
        for(int i = 0; i < fltPhase.length; i++) {
            fltPhase[i] = (float)dblPhase[i];
        }
        phase = new LocalSeismogramImpl(seis, fltPhase);
        super.calculate(seis, shift);
    }

    public float calcForStack(LocalSeismogramImpl seis,
                              double timePs,
                              double timePpPs,
                              double timePsPs) throws FissuresException {
        return super.calcForStack(seis, timePs, timePpPs, timePsPs) * 
        (float)Math.pow(Math.abs(weightPs * getAmp(phase, timePs) + 
                                 weightPpPs * getAmp(phase, timePpPs) - 
                                 weightPsPs * getAmp(phase, timePsPs)), 2);
    }

    private LocalSeismogramImpl phase;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKPhaseStack.class);
}