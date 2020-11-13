package edu.sc.seis.receiverFunction;

import edu.sc.seis.sod.model.common.QuantityImpl;
import edu.sc.seis.sod.model.common.UnitImpl;


/**
 * @author crotwell
 * Created on Nov 12, 2004
 */
public class TradeoffCurve  {

    public TradeoffCurve(HKStack stack) {
        this.stack = stack;
        peakIndices = stack.getGlobalMaximum();
        peakH = peakIndices.getHValue();
        peakK = peakIndices.getKValue();
        calc();
    }

    public float[] getH_PpPs() {
        return outH_PpPs;
    }
    public float[] getH_Ps() {
        return outH_Ps;
    }
    public float[] getH_PsPs() {
        return outH_PsPs;
    }
    
    void calc() {
        outH_Ps = new float[stack.getNumK()];
        outH_PpPs = new float[stack.getNumK()];
        outH_PsPs = new float[stack.getNumK()];
        float a = (float)stack.getAlpha().getValue(UnitImpl.KILOMETER_PER_SECOND);
        for(int i = 0; i < stack.getNumK(); i++) {
            float ki = stack.getMinK()+i*stack.getStepK();
            float vsi = a/ki;
            float vso = a/peakK;
            // Ps
            outH_Ps[i] = (float)(peakH.getValue()*Math.sqrt(1/vso-1/a)/
                    Math.sqrt(1/vsi-1/a));
            // PpPs
            outH_PpPs[i] = (float)(peakH.getValue()*Math.sqrt(1/vso+1/a)/
                    Math.sqrt(1/vsi+1/a));
            //PsPs
            outH_PsPs[i] = (float)(peakH.getValue()*Math.sqrt(1/vso)/
                    Math.sqrt(1/vsi));
        }
    }
    
    QuantityImpl peakH;
    float peakK;
    StackMaximum peakIndices;
    HKStack stack;

    float[] outH_Ps;
    float[] outH_PpPs;
    float[] outH_PsPs;
}
