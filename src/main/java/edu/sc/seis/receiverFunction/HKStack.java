/**
 * HKStack.java
 * 
 * @author Created by Omnicore CodeGuide
 */
package edu.sc.seis.receiverFunction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.WilsonRistra;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.sod.bag.Cmplx;
import edu.sc.seis.sod.bag.Hilbert;
import edu.sc.seis.sod.bag.PoissonsRatio;
import edu.sc.seis.sod.bag.TauPUtil;
import edu.sc.seis.sod.model.common.FissuresException;
import edu.sc.seis.sod.model.common.Location;
import edu.sc.seis.sod.model.common.QuantityImpl;
import edu.sc.seis.sod.model.common.UnitImpl;
import edu.sc.seis.sod.model.event.NoPreferredOrigin;
import edu.sc.seis.sod.model.seismogram.LocalSeismogramImpl;
import edu.sc.seis.sod.model.station.ChannelId;
import edu.sc.seis.sod.model.station.ChannelIdUtil;
import edu.sc.seis.sod.util.display.SimplePlotUtil;
import edu.sc.seis.sod.util.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.sod.util.time.ClockUtil;

public class HKStack  {

    public static final String ALL = "all";

    /** for hibernate */
    protected HKStack() {}
    
    protected HKStack(QuantityImpl alpha,
                      float p,
                      float gwidth,
                      float percentMatch,
                      QuantityImpl minH,
                      QuantityImpl stepH,
                      int numH,
                      float minK,
                      float stepK,
                      int numK,
                      float weightPs,
                      float weightPpPs,
                      float weightPsPs) {
        if(alpha.getValue() <= 0) {
            throw new IllegalArgumentException("alpha must be positive: "
                    + alpha);
        }
        if(p < 0) {
            throw new IllegalArgumentException("p must be nonnegative: " + p);
        }
        if(minK <= 0) {
            throw new IllegalArgumentException("minK must be positive: " + minK);
        }
        this.alpha = alpha;
        this.p = p;
        this.gwidth = gwidth;
        this.percentMatch = percentMatch;
        this.minH = minH.convertTo(UnitImpl.KILOMETER);
        this.stepH = stepH;
        this.numH = numH;
        this.minK = minK;
        this.stepK = stepK;
        this.numK = numK;
        this.weightPs = weightPs;
        this.weightPpPs = weightPpPs;
        this.weightPsPs = weightPsPs;
    }


    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   LocalSeismogramImpl recFuncSeis,
                   Duration shift) throws FissuresException {
        this(alpha,
             p,
             gwidth,
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
        this.recFunc = recFuncSeis;
        analyticPs = new CmplxArray2D(numH, numK);
        analyticPpPs = new CmplxArray2D(numH, numK);
        analyticPsPs = new CmplxArray2D(numH, numK);
        calculate(recFuncSeis, shift);
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   CmplxArray2D analyticPs,
                   CmplxArray2D analyticPpPs,
                   CmplxArray2D analyticPsPs) {
        this(alpha,
             p,
             gwidth,
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
        this.recFunc = null;
        this.analyticPs = analyticPs;
        this.analyticPpPs = analyticPpPs;
        this.analyticPsPs = analyticPsPs;
        calcPhaseStack();
    }


    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   float[][] stack) {
        this(alpha,
             p,
             gwidth,
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
        this.recFunc = null;
        this.stack = stack;
        this.compactAnalyticPhase = null;
        this.analyticPs = null;
        this.analyticPpPs = null;
        this.analyticPsPs = null;
    }

    /**
     * returns the x and y indices for the max value in the stack. The min x
     * value is in index 0 and the y in index 1. The max x value is in 2 and the
     * y in 3.
     */
    public int[] getMinValueIndices() {
        float[][] stackOut = getStack();
        float min = stackOut[0][0];
        int minIndexX = 0;
        int minIndexY = 0;
        for(int j = 0; j < stackOut.length; j++) {
            for(int k = 0; k < stackOut[j].length; k++) {
                if(stackOut[j][k] < min) {
                    min = stackOut[j][k];
                    minIndexX = j;
                    minIndexY = k;
                }
            }
        }
        int[] xy = new int[2];
        xy[0] = minIndexX;
        xy[1] = minIndexY;
        return xy;
    }

    /**
     * returns the x and y indices for the max value in the stack. The max x
     * value is in index 0 and the y in index 1. 
     */
    public int[] getMaxValueIndices() {
        return getMaxValueIndices(0);
    }

    /**
     * returns the x and y indices for the max value in the stack for a depth
     * grater than minH.
     */
    public int[] getMaxValueIndices(int startHIndex) {
        float[][] stackOut = getStack();
        float max = stackOut[startHIndex][0];
        int maxIndexX = 0;
        int maxIndexY = 0;
        for(int j = startHIndex; j < stackOut.length; j++) {
            for(int k = 0; k < stackOut[j].length; k++) {
                if(stackOut[j][k] > max) {
                    max = stackOut[j][k];
                    maxIndexX = j;
                    maxIndexY = k;
                }
            }
        }
        return new int[] {maxIndexX, maxIndexY};
    }

    private StackMaximum maxCache = null;
    
    public StackMaximum getGlobalMaximum() {
        if (maxCache == null) {
            maxCache = getLocalMaxima(0, 1)[0];
            peakH = maxCache.getHValue();
            peakK = new Float(maxCache.getKValue());
        }
    	return maxCache;
    }

    public StackMaximum[] getLocalMaxima(QuantityImpl startH, int num) {
        return getLocalMaxima(getHIndex(startH), num);
    }

    
    private StackMaximum[] stackMaximumCache = new StackMaximum[0];
    /**
     * Finds the top num local maxuma that are not within minDeltaH and
     * minDeltaK of another local maxima.
     */
    public StackMaximum[] getLocalMaxima(int startHIndex, int num) {
        if (stackMaximumCache.length >= num) {
            StackMaximum[] out = new StackMaximum[num];
            System.arraycopy(stackMaximumCache, 0, out, 0, out.length);
            return out;
        }
        int[] maxIndices = getMaxValueIndices(startHIndex);
        StackMaximum[] out = new StackMaximum[num];
        int maxIndexX = maxIndices[0];
        int maxIndexY = maxIndices[1];
        float max = getStack()[maxIndexX][maxIndexY];
        
        // do twice as we need StackMaximum to calc power to populate StackMaximum
        StackMaximum dumb = new StackMaximum(maxIndexX,
                                  getHFromIndex(maxIndexX),
                                  maxIndexY,
                                  getKFromIndex(maxIndexY),
                                  max,
                                  -1,
                                  -1);
        HKAlpha hka = new HKAlpha(dumb.getHValue(),
                                  dumb.getKValue(),
                                  getAlpha());
        StackComplexity complexity = new StackComplexity(this,
                                                         getGaussianWidth());
        try {
            HKStack residualStack = complexity.getResidual(hka, 80);
            out[0] = new StackMaximum(maxIndexX,
                                      getHFromIndex(maxIndexX),
                                      maxIndexY,
                                      getKFromIndex(maxIndexY),
                                      max,
                                      getPower(),
                                      residualStack.getPower());
            if(num > 1) {
                StackMaximum[] recursion = residualStack.getLocalMaxima(startHIndex,
                                                                        num - 1);
                System.arraycopy(recursion, 0, out, 1, recursion.length);
            }
        } catch(TauModelException e) {
            throw new RuntimeException("problem getting residual for local maxima",
                                       e);
        }
        stackMaximumCache = out;
        return out;
    }

    private boolean isLocalMaxima(int j, int k, float[][] stackOut) {
        if(j != 0 && stackOut[j - 1][k] > stackOut[j][k]) {
            return false;
        }
        if(j != stackOut.length - 1 && stackOut[j + 1][k] > stackOut[j][k]) {
            return false;
        }
        if(k != 0 && stackOut[j][k - 1] > stackOut[j][k]) {
            return false;
        }
        if(k != stackOut[0].length - 1 && stackOut[j][k + 1] > stackOut[j][k]) {
            return false;
        }
        // check corners
        if(j != 0) {
            if(k != 0 && stackOut[j - 1][k - 1] > stackOut[j][k]) {
                return false;
            }
            if(k != stackOut[0].length - 1
                    && stackOut[j - 1][k + 1] > stackOut[j][k]) {
                return false;
            }
        }
        if(j != stackOut.length - 1) {
            if(k != 0 && stackOut[j + 1][k - 1] > stackOut[j][k]) {
                return false;
            }
            if(k != stackOut[0].length - 1
                    && stackOut[j + 1][k + 1] > stackOut[j][k]) {
                return false;
            }
        }
        // must be a maxima
        return true;
    }

    public int getHIndex(QuantityImpl h) {
        return Math.round(getHIndexFloat(h));
    }

    public int getKIndex(float k) {
        return Math.round(getKIndexFloat(k));
    }

    public float getHIndexFloat(QuantityImpl h) {
        if(h.greaterThan(getMinH())) {
            float f = (float)(h.subtract(getMinH()).divideBy(getStepH())).getValue();
            return f;
        } else {
            return 0;
        }
    }

    public float getKIndexFloat(double k) {
        return (float)((k - getMinK()) / getStepK());
    }

    public QuantityImpl getMaxValueH() {
        if(peakH != null) {
            return peakH;
        }
        try {
            StackMaximum indicies = getGlobalMaximum();
            QuantityImpl peakH = indicies.getHValue();
            return peakH;
        } catch(Throwable e) {
            GlobalExceptionHandler.handle(e);
            return new QuantityImpl(0, UnitImpl.METER);
        }
    }

    public QuantityImpl getMaxValueH(QuantityImpl smallestH) {
        try {
            StackMaximum[] indicies = getLocalMaxima(getHIndex(smallestH), 1);
            QuantityImpl peakH = indicies[0].getHValue();
            return peakH;
        } catch(Throwable e) {
            GlobalExceptionHandler.handle(e);
            return new QuantityImpl(0, UnitImpl.METER);
        }
    }

    public QuantityImpl getHFromIndex(int index) {
        return getMinH().add(getStepH().multipliedByDbl(index));
    }

    public String formatMaxValueH() {
        return getGlobalMaximum().formatHValue();
    }

    public float getMaxValueK() {
        if(peakK != null) {
            return peakK.floatValue();
        }
        StackMaximum indicies = getGlobalMaximum();
        float peakK = getMinK() + getStepK() * indicies.getKIndex();
        return peakK;
    }

    public float getMaxValueK(QuantityImpl smallestH) {
        StackMaximum[] indicies = getLocalMaxima(smallestH, 1);
        return indicies[0].getKValue();
    }

    public float getKFromIndex(int index) {
        return getMinK() + getStepK() * index;
    }

    public String formatMaxValueK() {
        return getGlobalMaximum().formatKValue();
    }

    public float getMaxValue() {
        if(peakVal != null) {
            return peakVal.floatValue();
        }
        StackMaximum indicies = getGlobalMaximum();
        return indicies.getMaxValue();
    }

    public float getMaxValue(QuantityImpl smallestH) {
        StackMaximum[] indicies = getLocalMaxima(smallestH, 1);
        return indicies[0].getMaxValue();
    }

    public String formatMaxValue() {
        return maxValueFormat.format(getMaxValue());
    }

    public QuantityImpl getVs() {
        return getAlpha().dividedByDbl(getMaxValueK());
    }

    public String formatVs() {
        return getVs().formatValue("0.00");
    }

    public float getPoissonsRatio() {
        return (float)PoissonsRatio.calcPoissonsRatio(getMaxValueK());
    }

    public String formatPoissonsRatio() {
        return vpvsFormat.format(getPoissonsRatio());
    }

/*
    public static float getPercentMatch(DataSetSeismogram recFunc) {
        String percentMatch = "-9999";
        if(recFunc != null) {
            Element e = (Element)recFunc.getAuxillaryData("recFunc.percentMatch");
            percentMatch = SodUtil.getNestedText(e);
        }
        return Float.parseFloat(percentMatch);
    }
*/
    
    /**
     * Compacts the complex values for the three phases, Ps, PpPs, PsPs, into a
     * single Complex array, thereby cutting memory usage by factor 3, and likely
     * CPU by the same during stacking.
     * 
     * Note that the PsPs phase is opposite polarity, so subtract. Also this is
     * complex subtraction of unit vectors, so sub is correct. It is not phase 
     * subtraction, in which case minus would be wrong as need a 180 phase shift.
     * Lucky thing complex math takes care of this!
     */
    public void compact() {
        compactAnalyticPhase = new CmplxArray2D(analyticPs.getXLength(),
                                                analyticPs.getYLength());
        for(int i = 0; i < getNumH(); i++) {
            for(int k = 0; k < getNumK(); k++) {
                Cmplx ps = Cmplx.mul(analyticPs.get(i, k).zeroOrUnitVector(),
                                     getWeightPs());
                Cmplx ppps = Cmplx.mul(analyticPpPs.get(i, k)
                        .zeroOrUnitVector(), getWeightPpPs());
                Cmplx psps = Cmplx.mul(analyticPsPs.get(i, k)
                        .zeroOrUnitVector(), getWeightPsPs());
                Cmplx val = Cmplx.sub(Cmplx.add(ps, ppps), psps);
                compactAnalyticPhase.set(i, k, val);
            }
        }
        analyticPs = null;
        analyticPpPs = null;
        analyticPsPs = null;
    }

    public static float[][] createArray(int numH, int numK) {
        return new float[numH][numK];
    }

    void calculate(LocalSeismogramImpl seis, Duration shift)
            throws FissuresException {
        // set up analytic signal
        Hilbert hilbert = new Hilbert();
        Cmplx[] analytic = hilbert.analyticSignal(seis);
        float[] imagFloats = new float[analytic.length];
        for(int i = 0; i < analytic.length; i++) {
            imagFloats[i] = (float)analytic[i].imag();
        }
        LocalSeismogramImpl imag = new LocalSeismogramImpl(seis, imagFloats);
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        if(Float.isNaN(etaP)) {
            throw new RuntimeException("Warning: Eta P is NaN alpha=" + alpha
                    + "  p=" + p);
        } else if(etaP <= 0) {
            throw new RuntimeException("EtaP should never be negative: " + etaP
                    + "  a=" + a + "  p=" + p);
        }
        for(int kIndex = 0; kIndex < numK; kIndex++) {
            float beta = a / (minK + kIndex * stepK);
            float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
            if(Float.isNaN(etaS)) {
                throw new RuntimeException("Warning: Eta S is NaN " + kIndex
                        + "  beta=" + beta + "  p=" + p);
            } else if(etaS <= 0) {
                throw new RuntimeException("EtaS should never be negative: "
                        + etaS + "   etaP=" + etaP + "  beta=" + beta);
            }
            for(int hIndex = 0; hIndex < numH; hIndex++) {
                float h = (float)(minH.getValue(UnitImpl.KILOMETER) + hIndex
                        * stepH.getValue(UnitImpl.KILOMETER));
                Duration timePs = ClockUtil.durationOfSeconds(h * (etaS - etaP)
                        + ClockUtil.doubleSeconds(shift));
                Duration timePpPs = ClockUtil.durationFrom(h * (etaS + etaP)
                        + ClockUtil.doubleSeconds(shift), UnitImpl.SECOND);
                Duration timePsPs = ClockUtil.durationFrom(h * (2 * etaS)
                        + ClockUtil.doubleSeconds(shift), UnitImpl.SECOND);
                calcForStack(seis,
                             imag,
                             timePs,
                             timePpPs,
                             timePsPs,
                             hIndex,
                             kIndex);
            }
        }
        calcPhaseStack();
    }

    private void calcForStack(LocalSeismogramImpl seis,
                              LocalSeismogramImpl imag,
                              Duration timePs,
                              Duration timePpPs,
                              Duration timePsPs,
                              int hIndex,
                              int kIndex) throws FissuresException {
        analyticPs.setReal(hIndex, kIndex, getAmp(seis, timePs));
        analyticPs.setImag(hIndex, kIndex, getAmp(imag, timePs));
        analyticPpPs.setReal(hIndex, kIndex, getAmp(seis, timePpPs));
        analyticPpPs.setImag(hIndex, kIndex, getAmp(imag, timePpPs));
        analyticPsPs.setReal(hIndex, kIndex, getAmp(seis, timePsPs));
        analyticPsPs.setImag(hIndex, kIndex, getAmp(imag, timePsPs));
    }

    public static HKStack create(ReceiverFunctionResult cachedResult,
                                 float weightPs,
                                 float weightPpPs,
                                 float weightPsPs) throws TauModelException,
            FissuresException {
    	Channel chan = cachedResult.getChannelGroup().getChannel1();
    	StationResult c2Result = crust2.getStationResult(chan.getNetworkCode(), chan.getStationCode(), Location.of(chan));
        return create(cachedResult,
                      weightPs,
                      weightPpPs,
                      weightPsPs,
                      c2Result.getVp());
    }

    public static HKStack create(ReceiverFunctionResult cachedResult,
                                 float weightPs,
                                 float weightPpPs,
                                 float weightPsPs,
                                 QuantityImpl vp)
            throws TauModelException, FissuresException {
        String[] pPhases = {"P"};
        TauPUtil tauPTime = TauPUtil.getTauPUtil(modelName);
        List<Arrival> arrivals;
        try {
            arrivals = tauPTime.calcTravelTimes(cachedResult.getChannelGroup().getChannel1(),
                                                          cachedResult.getEvent().getPreferred(),
                                                          pPhases);
        } catch(NoPreferredOrigin e) {
            throw new RuntimeException("No Preferred Origin, should never happen but it jut did!");
        }
        // convert radian per sec ray param into km per sec
        float kmRayParam = (float)(arrivals.get(0).getRayParam() / tauPTime.getTauModel()
                .getRadiusOfEarth());
        HKStack stack = new HKStack(vp,
                                    kmRayParam,
                                    cachedResult.getGwidth(),
                                    cachedResult.getRadialMatch(),
                                    getDefaultMinH(),
                                    new QuantityImpl(.25f, UnitImpl.KILOMETER),
                                    240,
                                    1.6f,
                                    .0025f,
                                    200,
                                    weightPs,
                                    weightPpPs,
                                    weightPsPs,
                                    (LocalSeismogramImpl)cachedResult.getRadial(),
                                    RecFunc.getDefaultShift());
        return stack;
    }

    public Duration getTimePs() {
        return getTimePs(p, alpha, getMaxValueK(), getMaxValueH());
    }

    public static Duration getTimePs(float p,
                                         QuantityImpl alpha,
                                         float k,
                                         QuantityImpl h) {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        float beta = a / k;
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        return ClockUtil.durationOfSeconds(h.getValue(UnitImpl.KILOMETER) * (etaS - etaP));
    }

    public Duration getTimePpPs() {
        return getTimePpPs(p, alpha, getMaxValueK(), getMaxValueH());
    }

    public static Duration getTimePpPs(float p,
                                           QuantityImpl alpha,
                                           float k,
                                           QuantityImpl h) {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        float beta = a / k;
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        return ClockUtil.durationOfSeconds(h.getValue(UnitImpl.KILOMETER) * (etaS + etaP));
    }

    public Duration getTimePsPs() {
        return getTimePsPs(p, alpha, getMaxValueK(), getMaxValueH());
    }

    public static Duration getTimePsPs(float p,
                                           QuantityImpl alpha,
                                           float k,
                                           QuantityImpl h) {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float beta = a / k;
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        return ClockUtil.durationOfSeconds(h.getValue(UnitImpl.KILOMETER) * (2 * etaS));
    }

    /**
     * Gets the sample corresponding to the time. Return is a float so the
     * relative position between the nearest samples can be determined.
     */
    public static float getDataIndex(LocalSeismogramImpl seis, Duration time) {
        double sampOffset = ClockUtil.doubleSeconds(time) /
        		ClockUtil.doubleSeconds(seis.getSampling().getPeriod());
        return (float)sampOffset;
    }

    /** gets the amp at the given time offset from the start of the seismogram. */
    public static float getAmp(LocalSeismogramImpl seis, Duration time)
            throws FissuresException {
        float sampOffset = getDataIndex(seis, time);
        int offset = (int)Math.floor(sampOffset);
        if(sampOffset < 0 || offset > seis.getNumPoints() - 2) {
            logger.warn("time " + time
                    + " is outside of seismogram, returning 0: "
                    + seis.getBeginTime() + " - " + seis.getEndTime()
                    + " sampOffset=" + sampOffset + " npts="
                    + seis.getNumPoints());
            return 0;
        }
        float valA = seis.get_as_floats()[offset];
        float valB = seis.get_as_floats()[offset + 1];
        // linear interp
        float retVal = (float)SimplePlotUtil.linearInterp(offset,
                                                          valA,
                                                          offset + 1,
                                                          valB,
                                                          sampOffset);
        if(Float.isNaN(retVal)) {
            logger.error("Got a NaN for HKStack.getAmp() at " + time + " chan="
                    + ChannelIdUtil.toStringNoDates(seis.channel_id));
        }
        return retVal;
    }

    public ChannelId getChannelId() {
        if(recFunc != null) {
            return getRecFunc().channel_id;
        } else {
            return null;
        }
    }

    public LocalSeismogramImpl getRecFunc() {
        return recFunc;
    }
    
    public void setRecFunc(LocalSeismogramImpl dss) {
        this.recFunc = dss;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public Origin getOrigin() {
        return origin;
    }

    /**
     * Optional
     */
    Origin origin = null;

    public float[][] getStack() {
        return stack;
    }

    public float getP() {
        return p;
    }

    public String formatP() {
        return vpvsFormat.format(getP());
    }

    /**
     * calculates the power (sqrt of sum of squares) over the stack for only
     * positive entries, ie negative values are set = 0.
     */
    public float getPower() {
        return getPower(0);
    }

    /**
     * calculates the power (sqrt of sum of squares) over the stack for only
     * entries > floor, ie values < floor are set = 0.
     */
    public float getPower(float floor) {
        float power = 0;
        float[][] s = getStack();
        for(int i = 0; i < s.length; i++) {
            for(int j = 0; j < s[0].length; j++) {
                if(s[i][j] > floor) {
                    power += s[i][j] * s[i][j];
                }
            }
        }
        return (float)Math.sqrt(power);
    }

    public QuantityImpl getAlpha() {
        return alpha;
    }

    public float getAlphaKmps() {
        return (float)alpha.getValue(UnitImpl.KILOMETER_PER_SECOND);
    }

    public String formatAlpha() {
        return getAlpha().formatValue("0.00");
    }

    protected float getGaussianWidth() {
        return gwidth;
    }

    public float getPercentMatch() {
        return percentMatch;
    }

    public void setPercentMatch(float percentMatch) {
        this.percentMatch = percentMatch;
    }

    public String formatPercentMatch() {
        return vpvsFormat.format(getPercentMatch());
    }

    public QuantityImpl getMinH() {
        return minH;
    }

    public QuantityImpl getStepH() {
        return stepH;
    }

    public int getNumH() {
        return numH;
    }

    public float getMinK() {
        return minK;
    }
    
    protected void setMinK(float minK) {
        this.minK = minK;
    }

    public float getStepK() {
        return stepK;
    }

    public int getNumK() {
        return numK;
    }

    public float getWeightPpPs() {
        return weightPpPs;
    }

    public String formatWeightPpPs() {
        return vpvsFormat.format(getWeightPpPs());
    }

    public float getWeightPs() {
        return weightPs;
    }

    public String formatWeightPs() {
        return vpvsFormat.format(getWeightPs());
    }

    public float getWeightPsPs() {
        return weightPsPs;
    }

    public String formatWeightPsPs() {
        return vpvsFormat.format(getWeightPsPs());
    }

    /**
     * Writes the HKStack report to a string.
     */
    public void writeReport(BufferedWriter out) throws IOException {
        out.write("p=" + p);
        out.newLine();
        out.write("alpha=" + alpha);
        out.newLine();
        StackMaximum xyMax = getGlobalMaximum();
        float max = xyMax.getMaxValue();
        out.write("Max H="
                + xyMax.getHValue());
        out.write("    K=" + xyMax.getKValue());
        out.write("  max=" + max);
        out.write(" alpha=" + alpha);
        out.newLine();
        out.write("percentMatch=" + percentMatch);
        out.newLine();
        out.write("minH=" + minH);
        out.newLine();
        out.write("stepH=" + stepH);
        out.newLine();
        out.write("numH=" + numH);
        out.newLine();
        out.write("maxH=" + minH.add(stepH.multipliedByDbl(numH-1)));
        out.newLine();
        out.write("minK=" + minK);
        out.newLine();
        out.write("stepK=" + stepK);
        out.newLine();
        out.write("numK=" + numK);
        out.newLine();
        out.write("maxK=" + minK + stepK * (numK-1));
        out.newLine();
        out.write("stack.length=" + stack.length);
        out.newLine();
        out.write("stack[0].length=" + stack[0].length);
        out.newLine();
    }

    float calcPhaseStack(int hIndex, int kIndex) {
        return (float)(calcRegStack(hIndex, kIndex) * calcPhaseWeight(hIndex,
                                                                      kIndex));
        // return (float)(calcRegStack(hIndex, kIndex));
    }

    double calcRegStack(int hIndex, int kIndex) {
        return weightPs * analyticPs.getReal(hIndex, kIndex) + weightPpPs
                * analyticPpPs.getReal(hIndex, kIndex) - weightPsPs
                * analyticPsPs.getReal(hIndex, kIndex);
    }

    double calcPhaseWeight(int hIndex, int kIndex) {
        Cmplx ps, ppps, psps;
        double magPs = analyticPs.mag(hIndex, kIndex);
        double magPpPs = analyticPpPs.mag(hIndex, kIndex);
        double magPsPs = analyticPsPs.mag(hIndex, kIndex);
        if(magPs == 0 || magPpPs == 0 || magPsPs == 0) {
            return 0;
        }
        ps = Cmplx.div(analyticPs.get(hIndex, kIndex), magPs);
        ppps = Cmplx.div(analyticPpPs.get(hIndex, kIndex), magPpPs);
        psps = Cmplx.div(analyticPsPs.get(hIndex, kIndex), magPsPs);
        Cmplx out = Cmplx.sub(Cmplx.add(ps, ppps), psps);
        if(Double.isNaN(out.mag())) {
            System.out.println("calcPhaseWeight: NaN  " + " mag" + magPs + "\n"
                    + ps + "\n" + ppps + "\n" + psps);
        }
        return Math.pow(out.mag(), 2);
    }

    void calcPhaseStack() {
        stack = createArray(numH, numK);
        for(int i = 0; i < stack.length; i++) {
            for(int j = 0; j < stack[i].length; j++) {
                stack[i][j] = calcPhaseStack(i, j);
            }
        }
    }

    float[][] stack;

    public CmplxArray2D getAnalyticPpPs() {
        return analyticPpPs;
    }

    public CmplxArray2D getAnalyticPs() {
        return analyticPs;
    }

    public CmplxArray2D getAnalyticPsPs() {
        return analyticPsPs;
    }

    public CmplxArray2D getCompactAnalyticPhase() {
        return compactAnalyticPhase;
    }
    
    //hibernate
    protected String stackFile;
    
    public void setStackFile(String f) {this.stackFile = f;}
    public String getStackFile() {return stackFile;}
    
    protected void setAlphaKmps(float v) {
        alpha = new QuantityImpl(v, UnitImpl.KILOMETER_PER_SECOND);
    }
    
    /** not actually stored in hibernate as is in ReceiverFunctionResult, passed down from RFR on call to getHKStack */
    public void setGaussianWidth(float g) {
        this.gwidth = g;
    }
    
    protected void setP(float p) {
        this.p = p;
    }
    
    protected void setMinHKm(float v) {
        this.minH = new QuantityImpl(v, UnitImpl.KILOMETER);
    }
    
    protected float getMinHKm() { return (float)minH.getValue(UnitImpl.KILOMETER);}
    
    protected void setStepHKm(float v) {
        this.stepH = new QuantityImpl(v, UnitImpl.KILOMETER);
    }
    protected float getStepHKm() { return (float)stepH.getValue(UnitImpl.KILOMETER);}
    
    protected void setNumH(int v) {
        this.numH = v;
    }
    
    protected void setStepK(float v) {
        this.stepK = v;
    }
    
    protected void setNumK(int v) {
        this.numK = v;
    }
    
    protected void setWeightPs(float v) {
        this.weightPs = v;
    }
    
    protected void setWeightPpPs(float v) {
        this.weightPpPs = v;
    }
    
    protected void setWeightPsPs(float v) {
        this.weightPsPs = v;
    }
    
    CmplxArray2D compactAnalyticPhase = null;

    float[][] realStack;

    CmplxArray2D analyticPs;

    CmplxArray2D analyticPpPs;

    CmplxArray2D analyticPsPs;

    float p;

    QuantityImpl alpha;

    float gwidth;

    float percentMatch;

    QuantityImpl minH;

    QuantityImpl stepH;

    int numH;

    float minK;

    float stepK;

    int numK;

    float weightPs = 1;

    float weightPpPs = 1;

    float weightPsPs = 1;

    QuantityImpl peakH;

    Float peakK;

    Float peakVal;

    private static final QuantityImpl DEFAULT_MIN_H = new QuantityImpl(10,
                                                                       UnitImpl.KILOMETER);

    private static final QuantityImpl DEFAULT_SMALLEST_H = new QuantityImpl(25,
                                                                            UnitImpl.KILOMETER);

    public static String modelName = "iasp91";

    transient static Crust2 crust2 = new Crust2();

    transient static WilsonRistra wilson = null;

    public static Crust2 getCrust2() {
        return crust2;
    }

    public static QuantityImpl getDefaultMinH() {
        return DEFAULT_MIN_H;
    }

    public static QuantityImpl getDefaultSmallestH() {
        return DEFAULT_SMALLEST_H;
    }

    // don't serialize the DSS
    transient LocalSeismogramImpl recFunc;

    public static final float DEFAULT_WEIGHT_Ps = 1 / 3f;
    
    public static final float DEFAULT_WEIGHT_PpPs = 1 / 3f;
    
    public static final float DEFAULT_WEIGHT_PsPs = 1 - DEFAULT_WEIGHT_Ps
    - DEFAULT_WEIGHT_PpPs;


    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");

    private static DecimalFormat maxValueFormat = new DecimalFormat("0.0000");

    private static DecimalFormat depthFormat = new DecimalFormat("0.##");


    
    static public final Duration DEFAULT_SHIFT = Duration.ofSeconds(10);

    public static Duration getDefaultShift() {
        return DEFAULT_SHIFT;   
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKStack.class);
}
