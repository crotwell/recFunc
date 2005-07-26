/**
 * SumHKStack.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.sod.status.FissuresFormatter;

public class SumHKStack {
    
    public SumHKStack(float minPercentMatch,
                      QuantityImpl smallestH,
                      HKStack sum,
                      float hVariance,
                      float kVariance) {
        this.sum = sum;
        this.minPercentMatch = minPercentMatch;
        this.smallestH = smallestH;
        this.hVariance = hVariance;
        this.kVariance = kVariance;
        this.channel = sum.chan;
    }
    
    public SumHKStack(HKStack[] individuals,
                      Channel chan,
                      float minPercentMatch,
                      QuantityImpl smallestH) {
        this.individuals = individuals;
        this.minPercentMatch = minPercentMatch;
        this.channel = chan;
        this.smallestH = smallestH;
        if (individuals.length == 0) {
            throw new IllegalArgumentException("Cannot create SumStack with empty array");
        }
        for (int i = 0; i < individuals.length; i++) {
            if (! individuals[i].getMinH().equals(individuals[0].getMinH())) {
                throw new IllegalArgumentException("Cannot create SumStack with different minH, "+individuals[i].getMinH() +"!="+ individuals[0].getMinH());
            }
            if (individuals[i].getMinK() != individuals[0].getMinK()) {
                throw new IllegalArgumentException("Cannot create SumStack with different minK, "+individuals[i].getMinK() +"!="+ individuals[0].getMinK());
            }
            // need to do more of this checking...
        }
        sum = calculate(chan, individuals, smallestH, minPercentMatch);
        calcVarianceBootstrap();
    }

    public BufferedImage createStackImage() {
        return sum.createStackImage();
    }
    
    public Channel getChannel() {
        return channel;
    }

    public HKStack getSum() {
        return sum;
    }
    
    public HKStack[] getIndividuals() {
        return individuals;
    }
    
    static HKStack calculate(Channel chan, HKStack[] individuals, QuantityImpl smallestH, float minPercentMatch) {
        int smallestHIndex = individuals[0].getHIndex(smallestH);
        Cmplx[][] analyticPs = new Cmplx[individuals[0].getStack().length-smallestHIndex][individuals[0].getStack()[0].length];
        Cmplx[][] analyticPpPs = new Cmplx[analyticPs.length][analyticPs[0].length];
        Cmplx[][] analyticPsPs = new Cmplx[analyticPs.length][analyticPs[0].length];
        for (int hIndex = 0; hIndex < analyticPs.length; hIndex++) {
            for (int kIndex = 0; kIndex < analyticPs[0].length; kIndex++) {
                Cmplx aPs = new Cmplx(0,0);
                Cmplx aPpPs = new Cmplx(0,0);
                Cmplx aPsPs = new Cmplx(0,0);
                for (int s = 0; s < individuals.length; s++) {
                    aPs = Cmplx.add(aPs, individuals[s].getAnalyticPs()[hIndex][kIndex]);
                    aPpPs = Cmplx.add(aPpPs, individuals[s].getAnalyticPpPs()[hIndex][kIndex]);
                    aPsPs = Cmplx.add(aPsPs, individuals[s].getAnalyticPsPs()[hIndex][kIndex]);
                }
                analyticPs[hIndex][kIndex] = aPs;
                analyticPpPs[hIndex][kIndex] = aPpPs;
                analyticPsPs[hIndex][kIndex] = aPsPs;
            }
        }
        return new HKStack(individuals[0].getAlpha(),
                          0f,
                          minPercentMatch,
                          individuals[0].getMinH().add(  individuals[0].getStepH().multiplyBy(smallestHIndex)  ),
                          individuals[0].getStepH(),
                          individuals[0].getNumH()-smallestHIndex,
                          individuals[0].getMinK(),
                          individuals[0].getStepK(),
                          individuals[0].getNumK(),
                          individuals[0].getWeightPs(),
                          individuals[0].getWeightPpPs(),
                          individuals[0].getWeightPsPs(),
                          analyticPs,
                          analyticPpPs,
                          analyticPsPs,
                          chan);  
    }

    public double getHVariance() {
        return hVariance;
    }

    public double getKVariance() {
        return kVariance;
    }
    
    public double getMixedVariance() {
        return mixedVariance;
    }
    
    public QuantityImpl getHStdDev() {
        return new QuantityImpl(Math.sqrt(hVariance), UnitImpl.KILOMETER) ;
    }

    public double getKStdDev() {
        return Math.sqrt(kVariance);
    }
    
    public String formatKStdDev() {
        return vpvsFormat.format(getKStdDev());
    }
    
    public double getMixedStdDev() {
        return Math.sqrt(getMixedVariance());
    }
    
    public float getMinPercentMatch() {
        return minPercentMatch;
    }
    
    public QuantityImpl getSmallestH() {
        return smallestH;
    }
    
    protected void calcVarianceBootstrap() {
        HKStack temp = individuals[0];
        Random random = new Random();
        double[] hErrors = new double[bootstrapIterations];
        double[] kErrors = new double[bootstrapIterations];
        TimeOMatic.start();
        for(int i = 0; i < bootstrapIterations; i++) {
            ArrayList sample = new ArrayList();
            for(int j = 0; j < individuals.length; j++) {
                sample.add(individuals[randomInt(individuals.length)]);
            }
            HKStack sampleStack = calculate(temp.chan, (HKStack[])sample.toArray(new HKStack[0]), smallestH, minPercentMatch);
            hErrors[i] = sampleStack.getMaxValueH().getValue(UnitImpl.KILOMETER);
            kErrors[i] = sampleStack.getMaxValueK();
            System.out.println("calcVarianceBootstrap:  "+i+" "+hErrors[i]+"  "+kErrors[i]);
        }
        
        Statistics hStat = new Statistics(hErrors);
        hVariance = (float)hStat.var();
        Statistics kStat = new Statistics(kErrors);
        kVariance = (float)kStat.var();
        TimeOMatic.print("Stat for "
                + ChannelIdUtil.toStringNoDates(temp.getChannel())
                + " h stddev=" + getHStdDev() + "  k stddev="
                + getKStdDev());
    }

    protected int randomInt(int top) {
        return (int)Math.floor(Math.random() * top);
    }
    
    protected void calcVarianceSecondDerivative() {
        float[] peakVals = new float[individuals.length];
        int[] maxIndices = sum.getMaxValueIndices();
        for (int s = 0; s < individuals.length; s++) {
            peakVals[s] = individuals[s].getStack()[maxIndices[0]][maxIndices[1]];
        }
        Statistics stat = new Statistics(peakVals);
        maxVariance = stat.var();
        // H is first index, K is second, f first difference, s second difference, abc for left, center right
        double hsa;
        double hsb;
        double hsc;
        if (maxIndices[0] == 0) {
            // off edge, shift by 1 for second diff???
            hsa = sum.getStack()[maxIndices[0]][maxIndices[1]];
            hsb = sum.getStack()[maxIndices[0]+1][maxIndices[1]];
            hsc = sum.getStack()[maxIndices[0]+2][maxIndices[1]];
        } else if (maxIndices[0] == sum.getStack().length-1) {
            // off edge, shift by 1 for second difference???
            hsa = sum.getStack()[maxIndices[0]-2][maxIndices[1]];
            hsb = sum.getStack()[maxIndices[0]-1][maxIndices[1]];
            hsc = sum.getStack()[maxIndices[0]][maxIndices[1]];
        } else {
            // normal case in interior
            hsa = sum.getStack()[maxIndices[0]-1][maxIndices[1]];
            hsb = sum.getStack()[maxIndices[0]][maxIndices[1]];
            hsc = sum.getStack()[maxIndices[0]+1][maxIndices[1]];
        }
        double ksa;
        double ksb;
        double ksc;
        if (maxIndices[1] == 0) {
            // off edge, shift by 1???
            ksa = sum.getStack()[maxIndices[0]][maxIndices[1]];
            ksb = sum.getStack()[maxIndices[0]][maxIndices[1]+1];
            ksc = sum.getStack()[maxIndices[0]][maxIndices[1]+2];
        } else if (maxIndices[1] == sum.getStack()[0].length-1) {
            // off edge, shift by 1???
            ksa = sum.getStack()[maxIndices[0]][maxIndices[1]-2];
            ksb = sum.getStack()[maxIndices[0]][maxIndices[1]-1];
            ksc = sum.getStack()[maxIndices[0]][maxIndices[1]];
        } else {
            // normal case
            ksa = sum.getStack()[maxIndices[0]][maxIndices[1]-1];
            ksb = sum.getStack()[maxIndices[0]][maxIndices[1]];
            ksc = sum.getStack()[maxIndices[0]][maxIndices[1]+1];
        }
        // for corners/sides for partial H partial K
        // 4* happens for each corner as both are simple first difference
        // 2* happens for sides as one is simple first difference, other is centered first difference
        // normal case is both centered first difference
        // see for example http://snowball.millersville.edu/~adecaria/ESCI445/esci445_03_finite_diff_I.html
        double hkaa, hkab, hkba, hkbb;
        if (maxIndices[1] == 0) {
            if (maxIndices[0] == 0) {
                hkaa = 4*sum.getStack()[maxIndices[0]][maxIndices[1]];
                hkab = 4*sum.getStack()[maxIndices[0]][maxIndices[1]+1];
                hkba = 4*sum.getStack()[maxIndices[0]+1][maxIndices[1]];
                hkbb = 4*sum.getStack()[maxIndices[0]+1][maxIndices[1]+1];
            } else if (maxIndices[0] == sum.getStack().length-1) {
                hkaa = 4*sum.getStack()[maxIndices[0]-1][maxIndices[1]];
                hkab = 4*sum.getStack()[maxIndices[0]-1][maxIndices[1]+1];
                hkba = 4*sum.getStack()[maxIndices[0]][maxIndices[1]];
                hkbb = 4*sum.getStack()[maxIndices[0]][maxIndices[1]+1];
            } else {
                hkaa = 2*sum.getStack()[maxIndices[0]-1][maxIndices[1]];
                hkab = 2*sum.getStack()[maxIndices[0]-1][maxIndices[1]+1];
                hkba = 2*sum.getStack()[maxIndices[0]+1][maxIndices[1]];
                hkbb = 2*sum.getStack()[maxIndices[0]+1][maxIndices[1]+1];
            }
        } else if (maxIndices[1] == sum.getStack()[0].length-1) {
            if (maxIndices[0] == 0) {
                hkaa = 4*sum.getStack()[maxIndices[0]][maxIndices[1]-1];
                hkab = 4*sum.getStack()[maxIndices[0]][maxIndices[1]];
                hkba = 4*sum.getStack()[maxIndices[0]+1][maxIndices[1]-1];
                hkbb = 4*sum.getStack()[maxIndices[0]+1][maxIndices[1]];
            } else if (maxIndices[0] == sum.getStack().length-1) {
                hkaa = 4*sum.getStack()[maxIndices[0]-1][maxIndices[1]-1];
                hkab = 4*sum.getStack()[maxIndices[0]-1][maxIndices[1]];
                hkba = 4*sum.getStack()[maxIndices[0]][maxIndices[1]-1];
                hkbb = 4*sum.getStack()[maxIndices[0]][maxIndices[1]];
            } else {
                hkaa = 2*sum.getStack()[maxIndices[0]-1][maxIndices[1]-1];
                hkab = 2*sum.getStack()[maxIndices[0]-1][maxIndices[1]];
                hkba = 2*sum.getStack()[maxIndices[0]+1][maxIndices[1]-1];
                hkbb = 2*sum.getStack()[maxIndices[0]+1][maxIndices[1]];
            }
        } else {
            if (maxIndices[0] == 0) {
                hkaa = 2*sum.getStack()[maxIndices[0]][maxIndices[1]-1];
                hkab = 2*sum.getStack()[maxIndices[0]][maxIndices[1]+1];
                hkba = 2*sum.getStack()[maxIndices[0]+1][maxIndices[1]-1];
                hkbb = 2*sum.getStack()[maxIndices[0]+1][maxIndices[1]+1];
            } else if (maxIndices[0] == sum.getStack().length-1) {
                hkaa = 2*sum.getStack()[maxIndices[0]-1][maxIndices[1]-1];
                hkab = 2*sum.getStack()[maxIndices[0]-1][maxIndices[1]+1];
                hkba = 2*sum.getStack()[maxIndices[0]][maxIndices[1]-1];
                hkbb = 2*sum.getStack()[maxIndices[0]][maxIndices[1]+1];
            } else {
                // normal case
                hkaa = sum.getStack()[maxIndices[0]-1][maxIndices[1]-1];
                hkab = sum.getStack()[maxIndices[0]-1][maxIndices[1]+1];
                hkba = sum.getStack()[maxIndices[0]+1][maxIndices[1]-1];
                hkbb = sum.getStack()[maxIndices[0]+1][maxIndices[1]+1];
            }
        }
        double stepH = sum.getStepH().getValue(UnitImpl.KILOMETER);
        double stepK = sum.getStepK();
        double secPartialH = (hsa-2*hsb+hsc)/(stepH*stepH);
        double secPartialK = (ksa-2*ksb+ksc)/(stepK*stepK);
        double mixedPartialHK = ((hkbb-hkba)-(hkab-hkaa))/(4*stepK*stepH);
        double denom = (secPartialH*secPartialK-mixedPartialHK*mixedPartialHK);
        if (secPartialH == 0 || secPartialK == 0 || mixedPartialHK == 0 || denom <= 0) {
            logger.error("h or k Variance is NaN: index h/k: "+maxIndices[0]+"/"+maxIndices[1]+" delta h/k: "+stepH+"/"+stepK);
            logger.error("h: a="+hsa+"  b="+hsb+"  c="+hsc+"  "+secPartialH);
            logger.error("k: a="+ksa+"  b="+ksb+"  c="+ksc+"  "+secPartialK);
            logger.error("mixed: hkaa="+hkaa+" hkab="+hkab+" hkba="+hkba+" hkbb="+hkbb+"  "+mixedPartialHK);
            logger.error("denom "+denom);
            hVariance = 9999;
            kVariance = 9999;
            mixedVariance = 9999;
            return;
        }
        
        hVariance = -2*maxVariance* secPartialK / denom;
        kVariance = -2*maxVariance* secPartialH / denom;
        mixedVariance = -2*maxVariance* mixedPartialHK / denom;
        logger.debug("partials: "+ secPartialH +" "+ secPartialK +" "+mixedPartialHK  +" "+ denom);
        logger.debug("Variances: "+hVariance+"  "+kVariance+"  "+mixedVariance);
    }

    protected int bootstrapIterations = 100;
    
    protected Channel channel;
    protected HKStack[] individuals;
    protected HKStack sum;
    protected float minPercentMatch;
    protected QuantityImpl smallestH;
    protected double maxVariance;
    protected double hVariance;
    protected double kVariance;
    protected double mixedVariance;
    
    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SumHKStack.class);
}

