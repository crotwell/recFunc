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
        int[] peakIndices = stack.getMaxValueIndices();
        float peakH = stack.getMaxValueH();
        float peakK = stack.getMaxValueK();
        calc();
    }
    
    public float[] getPs() {
        return outK_Ps;
    }
    
    void calc() {
        outK_Ps = new float[stack.getNumK()];
        for(int i = 0; i < stack.getNumK(); i++) {
            float ki = stack.getMinK()+i*stack.getStepK();
            float vsi = stack.getAlpha()/ki;
            float vso = stack.getAlpha()/peakK;
            // Ps
            outK_Ps[i] = (float)(peakH*Math.sqrt(1/vso-1/stack.getAlpha())/
                    Math.sqrt(1/vsi-1/stack.getAlpha()));
        }
    }
    
    float peakH, peakK;
    
    HKStack stack;
    
    float[] outK_Ps;
}
