package edu.sc.seis.receiverFunction;

/**
 * IterDeconResult.java
 *
 *
 * Created: Wed Mar  5 08:44:01 2003
 *
 * @author <a href="mailto:crotwell@maple.local.">Philip Crotwell</a>
 * @version 1.0
 */
public class IterDeconResult {
    public IterDeconResult(int maxBumps,
			   boolean useAbsVal,
			   float tol,
			   float gwidth,
			   float[] numerator,
			   float[] denominator,
			   float dt,
			   float[] amps,
			   int[] shifts,
			   float[] residual,
			   float[] predicted) {
	this.maxBumps = maxBumps;
	this.useAbsVal = useAbsVal;
	this.tol = tol;
	this.gwidth = gwidth;
	this.numerator = numerator;
	this.denominator = denominator;
	this.dt = dt;
	this.amps = amps;
	this.shifts = shifts;
	this.residual = residual;
	this.predicted = predicted;
    } // IterDeconResult constructor

    public int getMaxBumps() {
	return maxBumps;
    }

    public boolean isUseAbsVal() {
	return useAbsVal;
    }

    public float getTol() {
	return tol;
    }

    public float getGWidth() {
	return gwidth;
    }

    public float[] getNumerator() {
	return numerator;
    }

    public float[] getDenominator() {
	return denominator;
    }

    public float getDelta() {
	return dt;
    }

    public float[] getAmps() {
	return amps;
    }

    public int[] getShifts() {
	return shifts;
    }

    public float[] getResidual() {
	return residual;
    }

    public float[] getPredicted() {
	return predicted;
    }

    int maxBumps;

    boolean useAbsVal;

    float tol;

    float gwidth;

    float[] numerator;

    float[] denominator;

    float dt;

    float[] amps;

    int[] shifts;

    float[] residual;

    float[] predicted;

} // IterDeconResult
