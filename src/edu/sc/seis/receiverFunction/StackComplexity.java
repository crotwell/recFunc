package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.Sampling;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.display.DisplayUtils;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.fissuresUtil.mockFissures.MockLocation;
import edu.sc.seis.fissuresUtil.mockFissures.IfNetwork.MockChannel;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.synth.SimpleSynthReceiverFunction;
import edu.sc.seis.sod.SodConfig;

/**
 * @author crotwell Created on Oct 21, 2005
 */
public class StackComplexity {

    private float gaussianWidth;

    public StackComplexity(HKStack hkplot, int num_points, float gaussianWidth) {
        this.hkplot = hkplot;
        this.samp = hkplot.getChannel().getSamplingInfo();
        this.num_points = num_points;
        this.gaussianWidth = gaussianWidth;
    }

    public ReceiverFunctionResult getSynthetic(StationResult staResult)
            throws FissuresException {
        float flatRP = sphRayParamRad / 6371;
        return getSyntheticForRayParam(staResult, flatRP);
    }

    public ReceiverFunctionResult getSyntheticForDist(HKAlpha staResult,
                                                      float distDeg)
            throws TauModelException {
        Arrival[] arrivals = TauPUtil.getTauPUtil()
                .calcTravelTimes(distDeg, 0, new String[] {"P"});
        return getSyntheticForRayParam(staResult,
                                       (float)arrivals[0].getRayParam() / 6371);
    }

    public ReceiverFunctionResult getSyntheticForRayParam(HKAlpha staResult,
                                                          float flatRP) {
        for(int i = 0; i < cache.length; i++) {
            if(cache[i] != null && cache[i].flatRP == flatRP
                    && cache[i].staResult.equals(staResult)) {
                return cache[i].synthStack;
            }
        }
        try {
            SimpleSynthReceiverFunction synth = new SimpleSynthReceiverFunction(staResult,
                                                                                samp,
                                                                                num_points);
            LocalSeismogramImpl synthRF = synth.calculate(flatRP,
                                                          ClockUtil.now()
                                                                  .getFissuresTime(),
                                                          RecFunc.getDefaultShift(),
                                                          hkplot.getChannel()
                                                                  .get_id(),
                                                          gaussianWidth);
            HKStack synthStack = new HKStack(hkplot.getAlpha(),
                                             flatRP,
                                             gaussianWidth,
                                             100,
                                             hkplot.minH,
                                             hkplot.stepH,
                                             hkplot.numH,
                                             hkplot.minK,
                                             hkplot.stepK,
                                             hkplot.numK,
                                             1 / 3f,
                                             1 / 3f,
                                             1 / 3f,
                                             synthRF,
                                             hkplot.getChannel(),
                                             RecFunc.getDefaultShift());
            synthStack.compact();
            ReceiverFunctionResult result = new ReceiverFunctionResult(null,
                                                                       MockChannel.createGroup(),
                                                                       SimplePlotUtil.createSpike(),
                                                                       SimplePlotUtil.createSpike(),
                                                                       SimplePlotUtil.createSpike(),
                                                                       synthRF,
                                                                       synthRF,
                                                                       100,
                                                                       1,
                                                                       100,
                                                                       1,
                                                                       gaussianWidth,
                                                                       1,
                                                                       0,
                                                                       new SodConfig(""));
            result.setHKstack(synthStack);
            for(int i = cache.length - 1; i > 0; i--) {
                cache[i] = cache[i - 1];
            }
            cache[0] = new CachedStackComplexity(staResult, flatRP, result);
            return result;
        } catch(FissuresException e) {
            throw new RuntimeException("should never happen as synth data should always be good and convertible to floats",
                                       e);
        }
    }

    public HKStack getResidual(HKAlpha staResult, float distDeg)
            throws TauModelException {
        HKStack synthStack = getSyntheticForDist(staResult, distDeg).getHKstack();
        return getResidual(hkplot, synthStack);
    }

    public static HKStack getResidual(HKStack real, HKStack synth) {
        float[][] data = real.getStack();
        float[][] synthData = synth.getStack();
        // scale synth data by max of data so best HK -> 0
        // hopefully this subtracts the bulk of the "mountain" around the max
        int[] maxIndex = real.getMaxValueIndices();
        float scale = data[maxIndex[0]][maxIndex[1]]
                / synthData[maxIndex[0]][maxIndex[1]];
        float[][] diff = new float[real.numH][real.numK];
        for(int i = 0; i < diff.length; i++) {
            for(int jj = 0; jj < diff[0].length; jj++) {
                if(synthData[i][jj] > 0) {
                    diff[i][jj] = data[i][jj] - synthData[i][jj] * scale;
                } else {
                    diff[i][jj] = data[i][jj];
                }
            }
        }
        return new HKStack(real.getAlpha(),
                           sphRayParamRad,
                           real.gwidth,
                           -1,
                           real.minH,
                           real.stepH,
                           real.numH,
                           real.minK,
                           real.stepK,
                           real.numK,
                           1 / 3f,
                           1 / 3f,
                           1 / 3f,
                           diff,
                           real.getChannel());
    }

    class CachedStackComplexity {

        public CachedStackComplexity(HKAlpha staResult2,
                                     float flatRP2,
                                     ReceiverFunctionResult synthStack2) {
            this.staResult = staResult2;
            this.flatRP = flatRP2;
            this.synthStack = synthStack2;
        }

        HKAlpha staResult;

        float flatRP;

        ReceiverFunctionResult synthStack;
    }

    CachedStackComplexity[] cache = new CachedStackComplexity[4];

    /**
     * s/deg for P for 60 deg distance
     */
    static final float sphRayParamRad = 6.877f;

    SumHKStack stack;

    HKStack hkplot;

    Sampling samp;

    int num_points;
}