package edu.sc.seis.receiverFunction;


/**
 * IterDecon.java
 *
 *
 * Created: Sat Mar 23 18:24:29 2002
 *
 * @author <a href="mailto:">Philip Crotwell</a>
 * @version $Id: IterDecon.java 12134 2005-02-16 20:48:12Z crotwell $
 */

public class IterDecon {
    public IterDecon (int maxBumps,
                      boolean useAbsVal,
                      float tol,
                      float gwidthFactor) {
        this.maxBumps = maxBumps;
        this.useAbsVal = useAbsVal;
        this.tol = tol;
        this.gwidthFactor = gwidthFactor;
    }

    public IterDeconResult process(float[] numerator,
                                   float[] denominator,
                                   float dt) throws RecFuncException {
        float[] amps = new float[maxBumps];
        int[] shifts = new int[maxBumps];

        numerator = makePowerTwo(numerator);
        denominator = makePowerTwo(denominator);

        /* Now begin the cross-correlation procedure
         Put the filter in the signals
         */
        float[] f  = gaussianFilter(numerator, gwidthFactor, dt);
        float[] g  = gaussianFilter(denominator, gwidthFactor, dt);

        // compute the power in the "numerator" for error scaling
        float fPower = power(f);
        float prevPower = fPower;
        float gPower = power(g);
        if (fPower == 0 || gPower == 0) {
            throw new RecFuncException("Power of numerator and denominator must be non-zero: num="+fPower+" denom="+gPower);
        }
        float[] residual = f;
        float[] predicted = new float[0];

        float[][] corrSave = new float[maxBumps][];
        float improvement = 100;
        int bump;
        for ( bump=0; bump < maxBumps && improvement > tol ; bump++) {

            // correlate the signals
            float[] corr = correlateNorm(residual, g);
            corrSave[bump] = corr;

            //  find the peak in the correlation
            if (useAbsVal) {
                shifts[bump] = getAbsMaxIndex(corr);
            } else {
                shifts[bump] = getMaxIndex(corr);
            } // end of else
            amps[bump] = corr[shifts[bump]]/dt; // why normalize by dt here?

            predicted = buildDecon(amps, shifts, g.length, gwidthFactor, dt);
            float[] predConvolve = NativeFFT.convolve(predicted, denominator, dt);

            residual = getResidual(f, predConvolve);
            float residualPower = power(residual);
            improvement = 100*(prevPower-residualPower)/fPower;
            prevPower = residualPower;
        } // end of for (int bump=0; bump < maxBumps; bump++)

        IterDeconResult result = new IterDeconResult(maxBumps,
                                                     useAbsVal,
                                                     tol,
                                                     gwidthFactor,
                                                     numerator,
                                                     denominator,
                                                     dt,
                                                     amps,
                                                     shifts,
                                                     residual,
                                                     predicted,
                                                     corrSave,
                                                     buildSpikes(amps, shifts, g.length),
                                                     prevPower,
                                                     fPower,
                                                     bump);
        return result;
    }

    /** computes the correlation of f and g normalized by the zero-lag
     *  autocorrelation of g. */
    public static float[] correlateNorm(float[] fdata, float[] gdata) {
        float zeroLag = power(gdata);

        float[] corr = NativeFFT.correlate(fdata, gdata);

        float temp =1 / zeroLag;
        for (int i=0; i<corr.length; i++) {
            corr[i] *= temp;
        }
        return corr;
    }

    static float[] buildSpikes(float[] amps, int[] shifts, int n) {
        float[] p = new float[n];
        for (int i=0; i<amps.length; i++) {
            p[shifts[i]] += amps[i];
        } // end of for (int i=0; i<amps.length; i++)
        return p;
    }

    static float[] buildDecon(float[] amps, int[] shifts, int n, float gwidthFactor, float dt) {
        return gaussianFilter(buildSpikes(amps, shifts, n), gwidthFactor, dt);
    }

    /** returns the residual, ie x-y */
    public static float[] getResidual(float[] x, float[] y) {
        float[] r = new float[x.length];
        for (int i=0; i<x.length; i++) {
            r[i] = x[i]-y[i];
        } // end of for (int i=0; i<x.length; i++)
        return r;
    }

    public static int getAbsMaxIndex(float[] data) {
        int minIndex = getMinIndex(data);
        int maxIndex = getMaxIndex(data);
        if (Math.abs(data[minIndex]) > Math.abs(data[maxIndex])) {
            return minIndex;
        } // end of if (Math.abs(data[minIndex]) > Math.abs(data[maxIndex]))
        return maxIndex;
    }

    public static final int getMinIndex(float[] data) {
        int index = 0;
        for (int i=1; i<data.length/2; i++) {
            if (data[i] < data[index]) {
                index = i;
            }
        }
        return index;
    }

    public static final int getMaxIndex(float[] data) {
        int index = 0;
        for (int i=1; i<data.length/2; i++) {
            if (data[i] > data[index]) {
                index = i;
            }
        }
        return index;
    }


    public static final float power(float[] data) {
        float power=0;
        for (int i=0; i<data.length; i++) {
            power += data[i]*data[i];
        } // end of for (int i=0; i<data.length; i++)
        return power;
    }

    /** convolve a function with a unit-area Gaussian filter.
     *   G(w) = exp(-w^2 / (4 a^2))
     *  The 1D gaussian is: f(x) = 1/(2*PI*sigma) e^(-x^2/(q * sigma^2))
     *  and the impluse response is: g(x) = 1/(2*PI)e^(-sigma^2 * u^2 / 2)
     *
     * If gwidthFactor is zero, does not filter.
     */
    public static float[] gaussianFilter(float[] x,
                                         float gwidthFactor,
                                         float dt) {
        // gwidthFactor of zero means no filter
        if (gwidthFactor == 0) {
            return x;
        }
        float[] forward = new float[x.length];
        System.arraycopy(x, 0, forward, 0, x.length);
        NativeFFT.forward(forward);

        double df = 1/(forward.length * dt);
        double d_omega = 2*Math.PI*df;
        double gwidth = 4*gwidthFactor*gwidthFactor;
        double gauss;
        double omega;

        // Handle the nyquist frequency
        omega = Math.PI/dt; // eliminate 2 / 2
        gauss = Math.exp(-omega*omega / gwidth);
        forward[1] *= gauss;

        int j;
        for (int i=1; i<forward.length/2; i++) {
            j  = i*2;
            omega = i*d_omega;
            gauss = Math.exp(-omega*omega / gwidth);
            forward[j] *= gauss;
            forward[j+1] *= gauss;
        }

        NativeFFT.inverse(forward);
        return forward;
    }

    public static float[] phaseShift(float[] x, float shift, float dt) {

        float[] forward = new float[x.length];
        System.arraycopy(x, 0, forward, 0, x.length);
        NativeFFT.forward(forward);

        double df = 1/(forward.length * dt);
        double d_omega = 2*Math.PI*df;

        double omega;
        //Handle the nyquist frequency
        omega = Math.PI/dt;
        forward[1] *= (float)Math.cos(omega*shift);

        double a,b,c,d;
        for (int j=2; j<forward.length-1; j+=2) {
            omega = (j/2)*d_omega;
            a = forward[j];
            b = forward[j+1];
            c = Math.cos(omega*shift);
            d = Math.sin(omega*shift);

            forward[j] = (float)(a*c-b*d);
            forward[j+1] = (float)(a*d+b*c);
        }

        NativeFFT.inverse(forward);
        return forward;
    }

    public static float[] makePowerTwo(float[] data) {
        float[] out = new float[nextPowerTwo(data.length)];
        System.arraycopy(data, 0, out, 0, data.length);
        return out;
    }

    public static int nextPowerTwo(int n) {
        int i=1;
        while (i < n) {
            i*=2;
        }
        return i;
    }

    protected int maxBumps;
    protected boolean useAbsVal;
    protected float tol;
    protected float gwidthFactor;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IterDecon.class);

}// IterDecon

