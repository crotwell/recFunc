/**
 * HKStack.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;



public class HKStack {
    
    public HKStack(float alpha,
                   float p,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK) {
        this.alpha = alpha;
        this.p = p;
        this.minH = minH;
        this.stepH = stepH;
        this.numH = numH;
        this.minK = minK;
        this.stepK = stepK;
        this.numK = numK;
    }
    
    public float[][] calculate(DataSetSeismogram recFunc) {
        float[][] ans = new float[numH][numK];
        float etaP = (float) Math.sqrt(1/(alpha*alpha)-p*p);
        for (int i = 0; i < numK; i++) {
            float beta = alpha/(minK + i*stepK);
            float etaS = (float) Math.sqrt(1/(beta*beta)-p*p);
            for (int j = 0; j < numH; j++) {
                float h = minH + j*stepH;
                float timePs = h * (etaS - etaP);
                float timePpPs = h * (etaS + etaP);
                float timePsPs = h * (2 * etaS);
                ans[i][j] += getAmp(recFunc, timePs)
                    + getAmp(recFunc, timePpPs)
                    - getAmp(recFunc, timePsPs);
            }
        }
        return ans;
    }
    
    float getAmp(DataSetSeismogram recFunc, float time) {
        return 1;
    }
    
    float p;
    float alpha;
    float minH;
    float stepH;
    int numH;
    float minK;
    float stepK;
    int numK;
}

