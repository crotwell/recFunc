package edu.sc.seis.receiverFunction.synth;

import java.time.Duration;
import java.time.Instant;

import edu.sc.seis.TauP.ReflTransCoefficient;
import edu.sc.seis.receiverFunction.HKAlpha;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.sod.bag.IterDecon;
import edu.sc.seis.sod.model.common.SamplingImpl;
import edu.sc.seis.sod.model.common.UnitImpl;
import edu.sc.seis.sod.model.seismogram.LocalSeismogramImpl;
import edu.sc.seis.sod.model.station.ChannelId;
import edu.sc.seis.sod.util.time.ClockUtil;

public class SimpleSynthReceiverFunction {


    public SimpleSynthReceiverFunction(HKAlpha model) {
        this(model, new SamplingImpl(20, ClockUtil.durationOfSeconds(1)), 4096);
    }
    
    public SimpleSynthReceiverFunction(HKAlpha model, SamplingImpl samp, int num_points) {
        this(model, samp, num_points, 2.7, 8.0, 4.5, 3.2);
    }

    public SimpleSynthReceiverFunction(HKAlpha hka,
    		                           SamplingImpl samp,
                                       int num_points,
                                       double crustRho,
                                       double mantleVp,
                                       double mantleVs,
                                       double mantleRho) {
        this.hka = hka;
        this.samp = samp;
        this.num_points = num_points;
        downgoingRFCoeff = new ReflTransCoefficient(hka.getVp().getValue(kmps), 
                                                    hka.getVs().getValue(kmps),
                                                    crustRho,
                                                    mantleVp,
                                                    mantleVs,
                                                    mantleRho);
        upgoingRFCoeff = downgoingRFCoeff.flip();
    }

    public LocalSeismogramImpl calculate(float flatRP,
                                         Instant begin_time,
                                         Duration lagZeroOffset,
                                         ChannelId chan,
                                         float gaussianWidth) {
        if (gaussianWidth <= 0) {
            throw new IllegalArgumentException("gaussian must be >= 0: "+gaussianWidth);
        }
        float[] data = new float[num_points];
        LocalSeismogramImpl seis = new LocalSeismogramImpl("SimpleSynthReceiverFunction",
                                                           begin_time,
                                                           num_points,
                                                           samp,
                                                           UnitImpl.DIMENSONLESS,
                                                           chan,
                                                           data);
        // scale from unit area gaussian to unit amplitude gaussian
        float scale = (float)2*SQRT_PI*gaussianWidth;
        // P
        Duration timeP = ClockUtil.durationOfSeconds(0);
        double refTransP = getAmpP(flatRP);
        float index = HKStack.getDataIndex(seis, timeP.plus(lagZeroOffset));
        data[Math.round(index)] = scale * (float)refTransP;
        
        // Ps
        Duration timePs = HKStack.getTimePs(flatRP, hka.getVp(), hka.getVpVs(), hka.getH());
        double refTransPs = getAmpPs(flatRP);
        index = HKStack.getDataIndex(seis, timePs.plus(lagZeroOffset));
        data[Math.round(index)] = scale * (float)refTransPs;

        // PpPs
        Duration timePpPs = HKStack.getTimePpPs(flatRP, hka.getVp(), hka.getVpVs(), hka.getH());
        double refTransPpPs = getAmpPpPs(flatRP);
        index = HKStack.getDataIndex(seis, timePpPs.plus(lagZeroOffset));
        data[Math.round(index)] = scale * (float)refTransPpPs;

        // PsPs+PpSs
        Duration timePsPs = HKStack.getTimePsPs(flatRP, hka.getVp(), hka.getVpVs(), hka.getH());
        double refTransPsPs = getAmpPsPs(flatRP);
        index = HKStack.getDataIndex(seis, timePsPs.plus(lagZeroOffset));
        data[Math.round(index)] = scale * (float)refTransPsPs;

        float[] tmp = IterDecon.gaussianFilter(data, gaussianWidth, (float)ClockUtil.floatSeconds(((SamplingImpl)seis.sampling_info).getPeriod()));
        System.arraycopy(tmp, 0, data, 0, data.length);
        
        return seis;
    }

    public double getAmpP(double flatRP) {
        calcBasicTerms(flatRP);
        return rpz0;
    }

    public double getRawAmpPs(double flatRP) {
        calcBasicTerms(flatRP);
        return upgoingRFCoeff.getPtoSVTrans(flatRP) / upgoingRFCoeff.getPtoPTrans(flatRP);
    }

    public double getAmpPs(double flatRP) {
        calcBasicTerms(flatRP);
        return getRawAmpPs(flatRP) * rpz0 * (rsr0 - zsz0);
    }

    public double getAmpPpPs(double flatRP) {
        calcBasicTerms(flatRP);
        double refTransPpPs = downgoingRFCoeff.getFreePtoPRefl(flatRP)
                * (downgoingRFCoeff.getPtoSVRefl(flatRP) - 
                        getRawAmpPs(flatRP) * downgoingRFCoeff.getPtoPRefl(flatRP));
        refTransPpPs *= rpz0 * (rsr0 - zsz0);
        return refTransPpPs;
    }

    public double getAmpPsPs(double flatRP) {
        calcBasicTerms(flatRP);
        double refTransPsPs = downgoingRFCoeff.getFreePtoSVRefl(flatRP)
                * downgoingRFCoeff.getSVtoSVRefl(flatRP)
                - getRawAmpPs(flatRP)
                * (downgoingRFCoeff.getFreePtoPRefl(flatRP) * downgoingRFCoeff.getPtoSVRefl(flatRP)
                        + downgoingRFCoeff.getFreePtoSVRefl(flatRP) * downgoingRFCoeff.getSVtoPRefl(flatRP) +
                        upgoingRFCoeff.getPtoSVTrans(flatRP)* downgoingRFCoeff.getFreeSVtoPRefl(flatRP)
                        * downgoingRFCoeff.getPtoPRefl(flatRP)
                        / upgoingRFCoeff.getPtoPTrans(flatRP)) 
                        + downgoingRFCoeff.getFreePtoPRefl(flatRP)
                * downgoingRFCoeff.getPtoSVRefl(flatRP) * upgoingRFCoeff.getPtoSVTrans(flatRP)
                / upgoingRFCoeff.getPtoPTrans(flatRP);
        refTransPsPs *= rpz0 * (rsr0 - zsz0);
        return refTransPsPs;
    }

    void calcBasicTerms(double flatRP) {
        double Vs = hka.getVs().getValue(UnitImpl.KILOMETER_PER_SECOND);
        double Vp = hka.getVp().getValue(UnitImpl.KILOMETER_PER_SECOND);
        double etas0 = Math.sqrt(1 / (Vs * Vs) - flatRP * flatRP);
        double etap0 = Math.sqrt(1 / (Vp * Vp) - flatRP * flatRP);
        double c1 = (1 / (Vs * Vs) - 2 * flatRP * flatRP);
        //double c2 = 4 * flatRP * flatRP * etap0 * etas0;
        rsr0 = Vs * c1 / 2. / Vp / flatRP / etap0;
        zsz0 = -2. * flatRP * etas0 * Vs / c1 / Vp;
        rpz0 = 2. * flatRP * etas0 / (1. / Vs / Vs - 2. * flatRP * flatRP);
    }

    double rsr0;

    double zsz0;

    double rpz0;

    ReflTransCoefficient downgoingRFCoeff, upgoingRFCoeff;
    
    HKAlpha hka;
    
    SamplingImpl samp;

    int num_points;

    static UnitImpl kmps = UnitImpl.KILOMETER_PER_SECOND;
    
    static final float SQRT_PI = (float)Math.sqrt(Math.PI);

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SimpleSynthReceiverFunction.class);
}