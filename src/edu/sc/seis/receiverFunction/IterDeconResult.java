package edu.sc.seis.receiverFunction;
import edu.iris.Fissures.model.TimeInterval;



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
                           float[] predicted,
                           float[][] corrSave) {
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
        this.corrSave = corrSave;
    }
    
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
    
    public float[][] getCorrSave() {
        return corrSave;
    }
            
    /**
     * Sets AlignShift
     *
     * @param    AlignShift          a  TimeInterval
     */
    public void setAlignShift(TimeInterval alignShift) {
        this.alignShift = alignShift;
    }
    
    /**
     * Returns AlignShift
     *
     * @return    a  TimeInterval
     */
    public TimeInterval getAlignShift() {
        return alignShift;
    }
    
    TimeInterval alignShift;
    
    float[][] corrSave;
    
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
