package edu.sc.seis.receiverFunction;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * @author crotwell
 * Created on Nov 12, 2004
 */
public class TradeoffCurve  {

    public TradeoffCurve(HKStack stack) {
        this.stack = stack;
        peakIndices = stack.getMaxValueIndices();
        peakH = stack.getMaxValueH();
        peakK = stack.getMaxValueK();
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
        for(int i = 0; i < stack.getNumK(); i++) {
            float ki = stack.getMinK()+i*stack.getStepK();
            float vsi = stack.getAlpha()/ki;
            float vso = stack.getAlpha()/peakK;
            // Ps
            outH_Ps[i] = (float)(peakH*Math.sqrt(1/vso-1/stack.getAlpha())/
                    Math.sqrt(1/vsi-1/stack.getAlpha()));
            // PpPs
            outH_PpPs[i] = (float)(peakH*Math.sqrt(1/vso+1/stack.getAlpha())/
                    Math.sqrt(1/vsi+1/stack.getAlpha()));
            //PsPs
            outH_PsPs[i] = (float)(peakH*Math.sqrt(1/vso)/
                    Math.sqrt(1/vsi));
        }
    }
    
    float peakH, peakK;
    int[] peakIndices;
    HKStack stack;

    float[] outH_Ps;
    float[] outH_PpPs;
    float[] outH_PsPs;
}
