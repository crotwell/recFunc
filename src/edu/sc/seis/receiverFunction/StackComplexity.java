package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.Sampling;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.synth.SimpleSynthReceiverFunction;

/**
 * @author crotwell Created on Oct 21, 2005
 */
public class StackComplexity {

    private float gaussianWidth;

    public StackComplexity(SumHKStack stack, int num_points, float gaussianWidth) {
        this.stack = stack;
        this.samp = stack.getChannel().sampling_info;
        this.num_points = num_points;
        this.gaussianWidth = gaussianWidth;
    }

    public HKStack getSynthetic(StationResult staResult)
            throws FissuresException {
        float flatRP = sphRayParamRad / 6371;
        return getSyntheticForRayParam(staResult, flatRP);
    }

    public HKStack getSyntheticForDist(StationResult staResult, float distDeg)
            throws FissuresException, TauModelException {
        Arrival[] arrivals = TauPUtil.getTauPUtil()
                .calcTravelTimes(distDeg, 0, new String[] {"P"});
        return getSyntheticForRayParam(staResult,
                                       (float)arrivals[0].getRayParam() / 6371);
    }

    public HKStack getSyntheticForRayParam(StationResult staResult, float flatRP)
            throws FissuresException {
        SimpleSynthReceiverFunction synth = new SimpleSynthReceiverFunction(staResult,
                                                                            samp,
                                                                            num_points);
        LocalSeismogramImpl synthRF = synth.calculate(flatRP,
                                                      ClockUtil.now()
                                                              .getFissuresTime(),
                                                      RecFunc.getDefaultShift(),
                                                      stack.getChannel()
                                                              .get_id(), gaussianWidth);
        HKStack synthStack = new HKStack(stack.getSum().getAlpha(),
                                         flatRP,
                                         gaussianWidth,
                                         100,
                                         stack.getSum().minH,
                                         stack.getSum().stepH,
                                         stack.getSum().numH,
                                         stack.getSum().minK,
                                         stack.getSum().stepK,
                                         stack.getSum().numK,
                                         1 / 3f,
                                         1 / 3f,
                                         1 / 3f,
                                         synthRF,
                                         stack.getChannel(),
                                         RecFunc.getDefaultShift());
        return synthStack;
    }

    public HKStack getResidual(StationResult staResult, float distDeg)
            throws FissuresException, TauModelException {
        HKStack synthStack = getSyntheticForDist(staResult, distDeg);
        float[][] data = stack.getSum().getStack();
        float[][] synthData = synthStack.getStack();
        // scale synth data by max of data so best HK -> 0
        // hopefully this subtracts the bulk of the "mountain" around the max
        QuantityImpl smallestH = HKStack.getBestSmallestH(stack.getChannel().my_site.my_station);
        float scale = stack.getSum().getMaxValue(smallestH)
                / synthStack.getMaxValue(smallestH);
        float[][] diff = new float[stack.getSum().numH][stack.getSum().numK];
        for(int i = 0; i < diff.length; i++) {
            for(int jj = 0; jj < diff[0].length; jj++) {
                if (synthData[i][jj] > 0) {
                    diff[i][jj] = data[i][jj] - synthData[i][jj] * scale;
                } else {
                    diff[i][jj] = data[i][jj];
                }
            }
        }
        return new HKStack(stack.getSum().getAlpha(),
                           sphRayParamRad,
                           gaussianWidth,
                           -1,
                           stack.getSum().minH,
                           stack.getSum().stepH,
                           stack.getSum().numH,
                           stack.getSum().minK,
                           stack.getSum().stepK,
                           stack.getSum().numK,
                           1 / 3f,
                           1 / 3f,
                           1 / 3f,
                           diff,
                           stack.getChannel());
    }

    /**
     * s/deg for P for 60 deg distance
     */
    static final float sphRayParamRad = 6.877f;

    SumHKStack stack;

    Sampling samp;

    int num_points;
}