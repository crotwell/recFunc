package edu.sc.seis.receiverFunction.synth;

import edu.iris.Fissures.Sampling;
import edu.iris.Fissures.Time;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.ReflTransCoefficient;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.IterDecon;
import edu.sc.seis.receiverFunction.compare.StationResult;

public class SimpleSynthReceiverFunction {

    public SimpleSynthReceiverFunction(StationResult model, Sampling samp,
            int num_points) {
        this.model = model;
        this.samp = samp;
        this.num_points = num_points;
        logger.info("calc for model: "+model);
        downgoingRFCoeff = new ReflTransCoefficient(model.getVp()
                                                            .getValue(kmps),
                                                    model.getVs()
                                                            .getValue(kmps),
                                                    2.7,
                                                    8.0,
                                                    4.5,
                                                    3.2);
        upgoingRFCoeff = downgoingRFCoeff.flip();
    }

    public LocalSeismogramImpl calculate(float flatRP,
                                         Time begin_time,
                                         TimeInterval lagZeroOffset,
                                         ChannelId chan) {
        
        float[] data = new float[num_points];
        
        LocalSeismogramImpl seis = new LocalSeismogramImpl("SimpleSynthReceiverFunction",
                                                           begin_time,
                                                           num_points,
                                                           samp,
                                                           UnitImpl.COUNT,
                                                           chan,
                                                           data);
        double freeSurfaceEffect = 1+upgoingRFCoeff.getFreeSVtoPRefl(flatRP)+upgoingRFCoeff.getFreeSVtoSVRefl(flatRP);
        double etas1 = Math.sqrt(model.getVs().getValue(UnitImpl.KILOMETER_PER_SECOND)*model.getVs().getValue(UnitImpl.KILOMETER_PER_SECOND) -1/(flatRP*flatRP));
        double etap1 = Math.sqrt(model.getVp().getValue(UnitImpl.KILOMETER_PER_SECOND)*model.getVp().getValue(UnitImpl.KILOMETER_PER_SECOND) -1/(flatRP*flatRP));
        double jordiFreeSurfaceEffect = model.getVs().getValue(UnitImpl.KILOMETER_PER_SECOND)*etas1/model.getVp().getValue(UnitImpl.KILOMETER_PER_SECOND)/etap1;
        freeSurfaceEffect = 1;
        // Ps
        TimeInterval timePs = HKStack.getTimePs(flatRP,
                                                model.getVp(),
                                                model.getVpVs(),
                                                model.getH());
        double refTransPs = upgoingRFCoeff.getPtoSVTrans(flatRP);
        refTransPs *= freeSurfaceEffect;
        float index = HKStack.getDataIndex(seis, timePs.add(lagZeroOffset));
        data[Math.round(index)] = (float)refTransPs;
        System.out.println("Ps: "+timePs+"  "+index+"  "+refTransPs);
        //PpPs
        TimeInterval timePpPs = HKStack.getTimePpPs(flatRP,
                                                  model.getVp(),
                                                  model.getVpVs(),
                                                  model.getH());
//        double refTransPpPs = upgoingRFCoeff.getPtoPTrans(flatRP)
//                * upgoingRFCoeff.getFreePtoPRefl(flatRP)
//                * downgoingRFCoeff.getPtoSVRefl(flatRP);
        double refTransPpPs = upgoingRFCoeff.getFreePtoPRefl(flatRP)
        * downgoingRFCoeff.getPtoSVRefl(flatRP);
        refTransPpPs *=  freeSurfaceEffect;
        index = HKStack.getDataIndex(seis, timePpPs.add(lagZeroOffset));
        data[Math.round(index)] = (float)refTransPpPs;
         System.out.println("PpPs: "+timePpPs+"  "+index+" "+refTransPpPs);
        //PsPs+PpSs
        TimeInterval timePsPs = HKStack.getTimePsPs(flatRP,
                                                model.getVp(),
                                                model.getVpVs(),
                                                model.getH());
//        double refTransPsPs = upgoingRFCoeff.getPtoSVTrans(flatRP) *
//        upgoingRFCoeff.getFreeSVtoPRefl(flatRP) *
//        downgoingRFCoeff.getPtoSVRefl(flatRP) +
//        upgoingRFCoeff.getPtoPTrans(flatRP) *
//        upgoingRFCoeff.getFreePtoSVRefl(flatRP) *
//        downgoingRFCoeff.getSVtoSVRefl(flatRP);
        double refTransPsPs = 
        upgoingRFCoeff.getFreePtoSVRefl(flatRP) *
        downgoingRFCoeff.getSVtoSVRefl(flatRP);
        refTransPsPs *= freeSurfaceEffect;
        index = HKStack.getDataIndex(seis, timePsPs.add(lagZeroOffset));
        data[Math.round(index)] = (float)refTransPsPs;
        System.out.println("PsPs: "+timePsPs+"  "+index+"  "+refTransPsPs);
        System.out.println((float)((SamplingImpl)seis.sampling_info).getPeriod().getValue(UnitImpl.SECOND));
        float[] tmp = IterDecon.gaussianFilter(data, 2.5f, (float)((SamplingImpl)seis.sampling_info).getPeriod().getValue(UnitImpl.SECOND));
        System.arraycopy(tmp, 0, data, 0, data.length);
        return seis;
    }

    ReflTransCoefficient downgoingRFCoeff, upgoingRFCoeff;

    StationResult model;

    Sampling samp;

    int num_points;

    static UnitImpl kmps = UnitImpl.KILOMETER_PER_SECOND;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SimpleSynthReceiverFunction.class);
}