package edu.sc.seis.receiverFunction;

//import edu.iris.Fissures.IfSeismogramDC.*;
import edu.sc.seis.fissuresUtil.freq.*;

/**
 * IterDecon.java
 *
 *
 * Created: Sat Mar 23 18:24:29 2002
 *
 * @author <a href="mailto:">Philip Crotwell</a>
 * @version $Id: IterDecon.java 2579 2002-09-12 15:09:54Z crotwell $
 */

public class IterDecon {
    public IterDecon (int maxBumps, 
		      boolean useAbsVal, 
		      float tol, 
		      float gwidth) {
	this.maxBumps = maxBumps;
	this.useAbsVal = useAbsVal;
	this.tol = tol;
	this.gwidth = gwidth;
	//	this.shift = shift;
    }

    public float[] process(float[] numerator, 
			   float[] denominator,
			   float dt) {
	float[] amps = new float[maxBumps];
	int[] shifts = new int[maxBumps];

	/* Now begin the cross-correlation procedure
	   Put the filter in the signals
	*/
	float[] f  = gaussianFilter(numerator, gwidth, dt);
	float[] g  = gaussianFilter(denominator, gwidth, dt);

	// compute the power in the "numerator" for error scaling
	float fPower = power(f);

	float[] residual = f;
	float[] predicted = new float[0];
	for (int bump=0; bump < maxBumps; bump++) {
	
	    // correlate the signals
	    float[] corr = correlate(residual, g);

	    //  find the peak in the correlation
	    float peak;
	    if (useAbsVal) {
		shifts[bump] = getAbsMaxIndex(corr);
	    } else {
		shifts[bump] = getMaxIndex(corr);
	    } // end of else
	    amps[bump] = corr[shifts[bump]]; // note don't normalize by dt here
	    System.out.println("Corr max is "+amps[bump]+" at index "+shifts[bump]);

	    predicted = buildDecon(amps, shifts, g.length, gwidth, dt);
	    float[] predConvolve = Cmplx.convolve(predicted, denominator);

	    residual = getResidual(f, predConvolve);
	} // end of for (int bump=0; bump < maxBumps; bump++)

	return predicted;
    }

    /** computes the correlation of f and g normalized by the zero-lag
     *  autocorrelation of g divided by dt. */
    float[] correlate(float[] fdata, float[] gdata) {
	float[] corr = Cmplx.correlate(fdata, gdata);
	float zeroLag = 0;
	for (int i=0; i<gdata.length; i++) {
	    zeroLag += gdata[i]*gdata[i];
	}
	float temp = 1 / zeroLag;
	for (int i=0; i<corr.length; i++) {
	    corr[i] *= temp;
	}
	return corr;	    
    }

    void subtractSpike(float[] data, int shift, float amp) {

    }

    float[] buildSpikes(float[] amps, int[] shifts, int n) {
	float[] p = new float[n];
	for (int i=0; i<amps.length; i++) {
	    p[shifts[i]] += amps[i];
	} // end of for (int i=0; i<amps.length; i++)
	return p;
    }

    float[] buildDecon(float[] amps, int[] shifts, int n, float gwidth, float dt) {
	return gaussianFilter(buildSpikes(amps, shifts, n), gwidth, dt);
    }

    float[] getResidual(float[] x, float[] y) {
	float[] r = new float[x.length];
	for (int i=0; i<x.length; i++) {
	    r[i] = x[i]-y[i];
	} // end of for (int i=0; i<x.length; i++)
	return r;
    }

    int getAbsMaxIndex(float[] data) {
	int minIndex = getMinIndex(data);
	int maxIndex = getMaxIndex(data);
	if (Math.abs(data[minIndex]) > Math.abs(data[maxIndex])) {
	    return minIndex;
	} // end of if (Math.abs(data[minIndex]) > Math.abs(data[maxIndex]))
	return maxIndex;
    }

    int getMinIndex(float[] data) {
	int index = 0;
	for (int i=1; i<data.length; i++) {
	    if (data[i] < data[index]) {
		index = i;
	    } 
	}
	return index;
    }	

    int getMaxIndex(float[] data) {
	int index = 0;
	for (int i=1; i<data.length; i++) {
	    if (data[i] > data[index]) {
		index = i;
	    }
	}
	return index;
    }	

    void zero(float[] data) {
	for (int i=0; i<data.length; i++) {
	    data[i] = 0;
	} // end of for (int i=0; i<data.length; i++)
    }

    float power(float[] data) {
	float power=0;
	for (int i=0; i<data.length; i++) {
	    power += data[i]*data[i];
	} // end of for (int i=0; i<data.length; i++)
	return power;
    }

    /** convolve a function with a unit-area Gaussian filter.*/
    float[] gaussianFilter(float[] x, float gwidthFactor, float dt) {
	int n2 = nextPowerTwo(x.length);
	int halfpts = n2 / 2;
	Cmplx[] forward = Cmplx.fft(x);
	//	float[] forward = realFT(x, halfpts);

	double df = 1/(n2 * dt);
	double d_omega = 2*Math.PI*df;
	double gwidth = 4*gwidthFactor*gwidthFactor;

	// Handle the nyquist frequency
	double omega = Math.PI/dt;
	double gauss = Math.exp(-omega*omega / gwidth);
	//	x[2] *= gauss;
	forward[0].i *= gauss;

	for (int i=1; i<halfpts; i++) {
	    int j=i*2;
	    omega = i*d_omega;
	    gauss = Math.exp(-omega*omega / gwidth);
	    forward[i].r *= gauss;
	    forward[i].r *= gauss;
	}
	
	float[] ans = Cmplx.fftInverse(forward, x.length);

	float scaleFactor = (float)(dt * 2 * df);
	for (int i=0; i<ans.length; i++) {
	    ans[i] *= scaleFactor;
	}
	return ans;
    }

    int nextPowerTwo(int n) {
	int i=1;
	while (i < n) {
	    i*=2;
	}
	return i;
    }
    
    int maxBumps;
    boolean useAbsVal; 
    float tol;
    float gwidth;
    float shift;

}// IterDecon
