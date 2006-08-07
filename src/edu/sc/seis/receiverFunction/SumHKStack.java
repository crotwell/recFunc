/**
 * SumHKStack.java
 * 
 * @author Created by Omnicore CodeGuide
 */
package edu.sc.seis.receiverFunction;

import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.freq.CmplxArray2D;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.receiverFunction.server.HKBox;
import edu.sc.seis.receiverFunction.server.JDBCRejectedMaxima;
import edu.sc.seis.receiverFunction.server.StackComplexityResult;

public class SumHKStack {

    public SumHKStack(float minPercentMatch,
                      QuantityImpl smallestH,
                      HKStack sum,
                      float hVariance,
                      float kVariance,
                      int numEQ,
                      HKBox[] rejects) {
        this.sum = sum;
        this.minPercentMatch = minPercentMatch;
        this.smallestH = smallestH;
        this.hVariance = hVariance;
        this.kVariance = kVariance;
        this.channel = sum.chan;
        this.numEQ = numEQ;
        this.rejects = rejects;
        best = makeStationResult(new StackMaximum(-1,
                                                  sum.getMaxValueH(),
                                                  -1,
                                                  sum.getMaxValueK(),
                                                  sum.getMaxValue(),
                                                  -1,
                                                  -1), "max", "");
    }

    public SumHKStack(HKStack[] individuals,
                      Channel chan,
                      float minPercentMatch,
                      QuantityImpl smallestH,
                      boolean doBootstrap,
                      boolean usePhaseWeight,
                      HKBox[] rejects) {
        this(individuals,
             chan,
             minPercentMatch,
             smallestH,
             doBootstrap,
             usePhaseWeight,
             DEFAULT_BOOTSTRAP_ITERATONS,
             rejects);
    }

    public SumHKStack(HKStack[] individuals,
                      Channel chan,
                      float minPercentMatch,
                      QuantityImpl smallestH,
                      boolean doBootstrap,
                      boolean usePhaseWeight,
                      int bootstrapIterations,
                      HKBox[] rejects) {
        this.individuals = individuals;
        this.minPercentMatch = minPercentMatch;
        this.channel = chan;
        this.smallestH = smallestH;
        this.numEQ = individuals.length;
        this.rejects = rejects;
        if(individuals.length == 0) {
            throw new IllegalArgumentException("Cannot create SumStack with empty array");
        }
        for(int i = 0; i < individuals.length; i++) {
            if(!individuals[i].getMinH().equals(individuals[0].getMinH())) {
                throw new IllegalArgumentException("Cannot create SumStack with different minH, "
                        + individuals[i].getMinH()
                        + "!="
                        + individuals[0].getMinH());
            }
            if(individuals[i].getMinK() != individuals[0].getMinK()) {
                throw new IllegalArgumentException("Cannot create SumStack with different minK, "
                        + individuals[i].getMinK()
                        + "!="
                        + individuals[0].getMinK());
            }
            // need to do more of this checking...
        }
        sum = SumHKStack.calculate(chan,
                        individuals,
                        smallestH,
                        minPercentMatch,
                        usePhaseWeight,
                        rejects);
        recalcBest();
        if(doBootstrap) {
            calcVarianceBootstrap(usePhaseWeight, bootstrapIterations);
        } else {
            hVariance = -1;
            kVariance = -1;
        }
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

    public int getNumEQ() {
        return numEQ;
    }

    public float getResidualPower() throws TauModelException {
        return getResidualPower(0);
    }

    public float getResidualPower(float percentFloor) throws TauModelException {
        float floor = percentFloor * getSum().getMaxValue(getSmallestH());
        float residualPower = getResidual().getPower(floor)
                / getSum().getPower(floor);
        return residualPower;
    }

    public HKStack getResidual() throws TauModelException {
        StationResultRef earsStaRef = new StationResultRef("Global Maxima",
                                                           "ears",
                                                           "ears");
        StationResult result = new StationResult(getChannel().get_id().network_id,
                                                 getChannel().get_id().station_code,
                                                 getSum().getMaxValueH(getSmallestH()),
                                                 getSum().getMaxValueK(getSmallestH()),
                                                 getSum().getAlpha(),
                                                 earsStaRef);
        StackComplexity complexity = new StackComplexity(getSum(),
                                                         4096,
                                                         getSum().getGaussianWidth());
        return complexity.getResidual(result, 60);
    }

    static HKStack calculate(Channel chan,
                             HKStack[] individuals,
                             QuantityImpl smallestH,
                             float minPercentMatch,
                             boolean usePhaseWeight,
                             HKBox[] rejects) {
        if(individuals.length == 0) {
            throw new IllegalArgumentException("individual HKStack cannot be length 0");
        }
        int smallestHIndex = individuals[0].getHIndex(smallestH);
        float[][] sumStack = new float[individuals[0].getStack().length
                - smallestHIndex][individuals[0].getStack()[0].length];
        for(int hIndex = 0; hIndex < sumStack.length; hIndex++) {
            int shiftHIndex = smallestHIndex + hIndex;
            for(int kIndex = 0; kIndex < sumStack[0].length; kIndex++) {
                Cmplx phaseWeight = new Cmplx(0, 0);
                float realStack = 0;
                for(int s = 0; s < individuals.length; s++) {
                    if(usePhaseWeight) {
                        phaseWeight = Cmplx.add(phaseWeight,
                                                individuals[s].getCompactAnalyticPhase()
                                                        .get(shiftHIndex,
                                                             kIndex));
                    }
                    realStack += individuals[s].getStack()[shiftHIndex][kIndex];
                }
                realStack /= individuals.length;
                if(usePhaseWeight) {
                    sumStack[hIndex][kIndex] = (float)(realStack * Math.pow(phaseWeight.mag()
                                                                                    / individuals.length
                                                                                    / 3,
                                                                            2));
                } else {
                    sumStack[hIndex][kIndex] = realStack;
                }
            }
        }
        HKStack hkStack = new HKStack(individuals[0].getAlpha(),
                                      0f,
                                      individuals[0].getGaussianWidth(),
                                      minPercentMatch,
                                      individuals[0].getMinH()
                                              .add(individuals[0].getStepH()
                                                      .multiplyBy(smallestHIndex)),
                                      individuals[0].getStepH(),
                                      individuals[0].getNumH() - smallestHIndex,
                                      individuals[0].getMinK(),
                                      individuals[0].getStepK(),
                                      individuals[0].getNumK(),
                                      individuals[0].getWeightPs(),
                                      individuals[0].getWeightPpPs(),
                                      individuals[0].getWeightPsPs(),
                                      sumStack,
                                      chan);
        return hkStack;
    }

    public static SumHKStack calculateForPhase(Iterator iterator,
                                               QuantityImpl smallestH,
                                               float minPercentMatch,
                                               boolean usePhaseWeight,
                                               String phase) {
        Channel chan = null;
        int numStacks = 0;
        float[][] sumStack = null;
        CmplxArray2D phaseWeight = null;
        HKStack individual;
        HKStack first = null;
        int smallestHIndex = 0;
        while(iterator.hasNext()) {
            individual = (HKStack)iterator.next();
            if(numStacks == 0) {
                first = individual;
                chan = first.chan;
                smallestHIndex = first.getHIndex(smallestH);
                sumStack = new float[first.getStack().length - smallestHIndex][first.getStack()[0].length];
                phaseWeight = new CmplxArray2D(sumStack.length,
                                               sumStack[0].length);
            }
            for(int hIndex = 0; hIndex < sumStack.length; hIndex++) {
                int shiftHIndex = smallestHIndex + hIndex;
                for(int kIndex = 0; kIndex < sumStack[0].length; kIndex++) {
                    if(usePhaseWeight) {
                        Cmplx val = new Cmplx(0, 0);
                        if(phase.equals("Ps") || phase.equals("all")) {
                            val = individual.getAnalyticPs().get(shiftHIndex,
                                                                 kIndex);
                            sumStack[hIndex][kIndex] += val.real();
                            phaseWeight.set(hIndex,
                                            kIndex,
                                            Cmplx.add(phaseWeight.get(hIndex,
                                                                      kIndex),
                                                      val.unitVector()));
                        }
                        if(phase.equals("PpPs") || phase.equals("all")) {
                            val = individual.getAnalyticPpPs().get(shiftHIndex,
                                                                   kIndex);
                            sumStack[hIndex][kIndex] += val.real();
                            phaseWeight.set(hIndex,
                                            kIndex,
                                            Cmplx.add(phaseWeight.get(hIndex,
                                                                      kIndex),
                                                      val.unitVector()));
                        }
                        if(phase.equals("PsPs") || phase.equals("all")) {
                            val = individual.getAnalyticPsPs().get(shiftHIndex,
                                                                   kIndex);
                            sumStack[hIndex][kIndex] -= val.real();
                            phaseWeight.set(hIndex,
                                            kIndex,
                                            Cmplx.sub(phaseWeight.get(hIndex,
                                                                      kIndex),
                                                      val.unitVector()));
                        }
                    }
                }
            }
            numStacks++;
        }
        for(int hIndex = 0; hIndex < sumStack.length; hIndex++) {
            for(int kIndex = 0; kIndex < sumStack[0].length; kIndex++) {
                sumStack[hIndex][kIndex] /= numStacks;
                if(phase.equals("all")) {
                    sumStack[hIndex][kIndex] /= 3;
                }
                if(usePhaseWeight) {
                    if(phase.equals("all")) {
                        sumStack[hIndex][kIndex] = (float)(sumStack[hIndex][kIndex] * Math.pow(phaseWeight.get(hIndex,
                                                                                                               kIndex)
                                                                                                       .mag()
                                                                                                       / numStacks
                                                                                                       / 3,
                                                                                               2));
                    } else {
                        sumStack[hIndex][kIndex] = (float)(sumStack[hIndex][kIndex] * Math.pow(phaseWeight.get(hIndex,
                                                                                                               kIndex)
                                                                                                       .mag()
                                                                                                       / numStacks,
                                                                                               2));
                    }
                }
            }
        }
        HKStack hkStack = new HKStack(first.getAlpha(),
                                      0f,
                                      first.getGaussianWidth(),
                                      minPercentMatch,
                                      first.getMinH().add(first.getStepH()
                                              .multiplyBy(smallestHIndex)),
                                      first.getStepH(),
                                      first.getNumH() - smallestHIndex,
                                      first.getMinK(),
                                      first.getStepK(),
                                      first.getNumK(),
                                      first.getWeightPs(),
                                      first.getWeightPpPs(),
                                      first.getWeightPsPs(),
                                      sumStack,
                                      chan);
        return new SumHKStack(minPercentMatch,
                              smallestH,
                              hkStack,
                              -1,
                              -1,
                              numStacks,
                              new HKBox[0]);
    }

    public StackComplexityResult calcStackComplexity() throws TauModelException {
        StackComplexity complexity = new StackComplexity(getSum(),
                                                         4096,
                                                         getSum().getGaussianWidth());
        StationResult model = new StationResult(getChannel().get_id().network_id,
                                                getChannel().get_id().station_code,
                                                getSum().getMaxValueH(getSmallestH()),
                                                getSum().getMaxValueK(getSmallestH()),
                                                getSum().getAlpha(),
                                                null);
        HKStack residual = getResidual();
        float complex = getResidualPower();
        float complex25 = getResidualPower(.25f);
        float complex50 = getResidualPower(.50f);
        float bestH = (float)getSum().getMaxValueH(getSmallestH())
                .getValue(UnitImpl.KILOMETER);
        float bestHStdDev = (float)getHStdDev().getValue(UnitImpl.KILOMETER);
        float bestK = getSum().getMaxValueK(getSmallestH());
        float bestKStdDev = (float)getKStdDev();
        float bestVal = getSum().getMaxValue(getSmallestH());
        float hkCorrelation = (float)getMixedVariance();
        float nextH = (float)residual.getMaxValueH(getSmallestH())
                .getValue(UnitImpl.KILOMETER);
        float nextK = residual.getMaxValueK(getSmallestH());
        float nextVal = residual.getMaxValue(getSmallestH());
        StationResult crust2Result = HKStack.getCrust2()
                .getStationResult(getChannel().my_site.my_station);
        float crust2diff = bestH
                - (float)crust2Result.getH().getValue(UnitImpl.KILOMETER);
        setComplexityResult(new StackComplexityResult(getDbid(),
                                                      complex,
                                                      complex25,
                                                      complex50,
                                                      bestH,
                                                      bestHStdDev,
                                                      bestK,
                                                      bestKStdDev,
                                                      bestVal,
                                                      hkCorrelation,
                                                      nextH,
                                                      nextK,
                                                      nextVal,
                                                      crust2diff));
        return getComplexityResult();
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
        return new QuantityImpl(Math.sqrt(hVariance), UnitImpl.KILOMETER);
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

    protected void calcVarianceBootstrap(boolean usePhaseWeight,
                                         int bootstrapIterations) {
        HKStack temp = individuals[0];
        double[] hErrors = new double[bootstrapIterations];
        double[] kErrors = new double[bootstrapIterations];
        TimeOMatic.start();
        for(int i = 0; i < bootstrapIterations; i++) {
            ArrayList sample = new ArrayList();
            for(int j = 0; j < individuals.length; j++) {
                sample.add(individuals[randomInt(individuals.length)]);
            }
            HKStack sampleStack = calculate(temp.chan,
                                            (HKStack[])sample.toArray(new HKStack[0]),
                                            smallestH,
                                            minPercentMatch,
                                            usePhaseWeight,
                                            rejects);
            StationResult bestBoot = calcBest(sampleStack);
            hErrors[i] = bestBoot.getH().getValue(UnitImpl.KILOMETER);
            kErrors[i] = bestBoot.getVpVs();
            if(i % 10 == 0) {
                System.out.println("calcVarianceBootstrap:  " + i + " "
                        + hErrors[i] + "  " + kErrors[i]);
            }
        }
        Statistics hStat = new Statistics(hErrors);
        hVariance = (float)hStat.var();
        Statistics kStat = new Statistics(kErrors);
        kVariance = (float)kStat.var();
        mixedVariance = hStat.correlation(kErrors);
        TimeOMatic.print("Stat for "
                + ChannelIdUtil.toStringNoDates(temp.getChannel())
                + " h stddev=" + getHStdDev() + "  k stddev=" + getKStdDev());
        best.setHStdDev(getHStdDev());
        best.setKStdDev((float)getKStdDev());
    }

    protected int randomInt(int top) {
        return (int)Math.floor(Math.random() * top);
    }

    protected void calcVarianceSecondDerivative() {
        float[] peakVals = new float[individuals.length];
        StackMaximum maxIndices = sum.getGlobalMaximum();
        for(int s = 0; s < individuals.length; s++) {
            peakVals[s] = maxIndices.getMaxValue();
        }
        Statistics stat = new Statistics(peakVals);
        maxVariance = stat.var();
        // H is first index, K is second, f first difference, s second
        // difference, abc for left, center right
        double hsa;
        double hsb;
        double hsc;
        if(maxIndices.getHIndex() == 0) {
            // off edge, shift by 1 for second diff???
            hsa = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
            hsb = sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex()];
            hsc = sum.getStack()[maxIndices.getHIndex() + 2][maxIndices.getKIndex()];
        } else if(maxIndices.getHIndex() == sum.getStack().length - 1) {
            // off edge, shift by 1 for second difference???
            hsa = sum.getStack()[maxIndices.getHIndex() - 2][maxIndices.getKIndex()];
            hsb = sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex()];
            hsc = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
        } else {
            // normal case in interior
            hsa = sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex()];
            hsb = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
            hsc = sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex()];
        }
        double ksa;
        double ksb;
        double ksc;
        if(maxIndices.getKIndex() == 0) {
            // off edge, shift by 1???
            ksa = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
            ksb = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() + 1];
            ksc = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() + 2];
        } else if(maxIndices.getKIndex() == sum.getStack()[0].length - 1) {
            // off edge, shift by 1???
            ksa = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() - 2];
            ksb = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() - 1];
            ksc = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
        } else {
            // normal case
            ksa = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() - 1];
            ksb = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
            ksc = sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() + 1];
        }
        // for corners/sides for partial H partial K
        // 4* happens for each corner as both are simple first difference
        // 2* happens for sides as one is simple first difference, other is
        // centered first difference
        // normal case is both centered first difference
        // see for example
        // http://snowball.millersville.edu/~adecaria/ESCI445/esci445_03_finite_diff_I.html
        double hkaa, hkab, hkba, hkbb;
        if(maxIndices.getKIndex() == 0) {
            if(maxIndices.getHIndex() == 0) {
                hkaa = 4 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
                hkab = 4 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() + 1];
                hkba = 4 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex()];
                hkbb = 4 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex() + 1];
            } else if(maxIndices.getHIndex() == sum.getStack().length - 1) {
                hkaa = 4 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex()];
                hkab = 4 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex() + 1];
                hkba = 4 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
                hkbb = 4 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() + 1];
            } else {
                hkaa = 2 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex()];
                hkab = 2 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex() + 1];
                hkba = 2 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex()];
                hkbb = 2 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex() + 1];
            }
        } else if(maxIndices.getKIndex() == sum.getStack()[0].length - 1) {
            if(maxIndices.getHIndex() == 0) {
                hkaa = 4 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() - 1];
                hkab = 4 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
                hkba = 4 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex() - 1];
                hkbb = 4 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex()];
            } else if(maxIndices.getHIndex() == sum.getStack().length - 1) {
                hkaa = 4 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex() - 1];
                hkab = 4 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex()];
                hkba = 4 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() - 1];
                hkbb = 4 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex()];
            } else {
                hkaa = 2 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex() - 1];
                hkab = 2 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex()];
                hkba = 2 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex() - 1];
                hkbb = 2 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex()];
            }
        } else {
            if(maxIndices.getHIndex() == 0) {
                hkaa = 2 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() - 1];
                hkab = 2 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() + 1];
                hkba = 2 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex() - 1];
                hkbb = 2 * sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex() + 1];
            } else if(maxIndices.getHIndex() == sum.getStack().length - 1) {
                hkaa = 2 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex() - 1];
                hkab = 2 * sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex() + 1];
                hkba = 2 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() - 1];
                hkbb = 2 * sum.getStack()[maxIndices.getHIndex()][maxIndices.getKIndex() + 1];
            } else {
                // normal case
                hkaa = sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex() - 1];
                hkab = sum.getStack()[maxIndices.getHIndex() - 1][maxIndices.getKIndex() + 1];
                hkba = sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex() - 1];
                hkbb = sum.getStack()[maxIndices.getHIndex() + 1][maxIndices.getKIndex() + 1];
            }
        }
        double stepH = sum.getStepH().getValue(UnitImpl.KILOMETER);
        double stepK = sum.getStepK();
        double secPartialH = (hsa - 2 * hsb + hsc) / (stepH * stepH);
        double secPartialK = (ksa - 2 * ksb + ksc) / (stepK * stepK);
        double mixedPartialHK = ((hkbb - hkba) - (hkab - hkaa))
                / (4 * stepK * stepH);
        double denom = (secPartialH * secPartialK - mixedPartialHK
                * mixedPartialHK);
        if(secPartialH == 0 || secPartialK == 0 || mixedPartialHK == 0
                || denom <= 0) {
            logger.error("h or k Variance is NaN: index h/k: "
                    + maxIndices.getHIndex() + "/" + maxIndices.getKIndex()
                    + " delta h/k: " + stepH + "/" + stepK);
            logger.error("h: a=" + hsa + "  b=" + hsb + "  c=" + hsc + "  "
                    + secPartialH);
            logger.error("k: a=" + ksa + "  b=" + ksb + "  c=" + ksc + "  "
                    + secPartialK);
            logger.error("mixed: hkaa=" + hkaa + " hkab=" + hkab + " hkba="
                    + hkba + " hkbb=" + hkbb + "  " + mixedPartialHK);
            logger.error("denom " + denom);
            hVariance = 9999;
            kVariance = 9999;
            mixedVariance = 9999;
            return;
        }
        hVariance = -2 * maxVariance * secPartialK / denom;
        kVariance = -2 * maxVariance * secPartialH / denom;
        mixedVariance = -2 * maxVariance * mixedPartialHK / denom;
        logger.debug("partials: " + secPartialH + " " + secPartialK + " "
                + mixedPartialHK + " " + denom);
        logger.debug("Variances: " + hVariance + "  " + kVariance + "  "
                + mixedVariance);
    }

    public int getDbid() {
        return dbid;
    }

    public void setDbid(int dbid) {
        this.dbid = dbid;
    }

    public StackComplexityResult getComplexityResult() {
        return complexityResult;
    }

    public float getComplexityResidual() {
        return complexityResult.getComplexity();
    }

    public void setComplexityResult(StackComplexityResult complexityResidual) {
        this.complexityResult = complexityResidual;
    }

    public String formatComplexityResidual() {
        return vpvsFormat.format(getComplexityResidual());
    }

    StationResult calcBest(HKStack sum) {
        StackMaximum[] localMaxima = sum.getLocalMaxima(smallestH, 5);
        for(int i = 0; i < localMaxima.length; i++) {
            if(-1 != inAnalystReject(localMaxima[i].getHValue(),
                                  localMaxima[i].getKValue(),
                                  rejects)) {
                String extra = "amp=" + localMaxima[i].getMaxValue();
                String name = i == 0 ? "Global Maxima" : "Local Maxima " + i;
                return makeStationResult(localMaxima[i], name, extra);
            }
        }
        // this is bad
        return null;
    }

    public StationResult makeStationResult(StackMaximum max,
                                           String name,
                                           String extra) {
        StationResultRef earsStaRef = new StationResultRef(name, "ears", "ears");
        return new StationResult(getChannel().get_id().network_id,
                                 getChannel().get_id().station_code,
                                 max.getHValue(),
                                 max.getKValue(),
                                 sum.getAlpha(),
                                 max.getMaxValue(),
                                 getHStdDev(),
                                 (float)getKStdDev(),
                                 earsStaRef,
                                 extra);
    }

    public static int inAnalystReject(QuantityImpl hQuantity,
                                            float k,
                                            HKBox[] rejects) {
        float h = (float)hQuantity.getValue(UnitImpl.KILOMETER);
        for(int i = 0; i < rejects.length; i++) {
            if(rejects[i].inside(h, k)) {
                return i;
            }
        }
        return -1;
    }

    public StationResult getBest() {
        return best;
    }
    
    public void recalcBest() {
        best = calcBest(sum);
    }

    protected Channel channel;

    protected HKStack[] individuals;

    protected int numEQ;

    protected HKStack sum;

    protected float minPercentMatch;

    protected QuantityImpl smallestH;

    protected double maxVariance;

    protected double hVariance;

    protected double kVariance;

    protected StationResult best;

    protected HKBox[] rejects;

    protected double mixedVariance;

    protected int dbid = -1;

    public static int DEFAULT_BOOTSTRAP_ITERATONS = 100;

    protected StackComplexityResult complexityResult = null;

    private static DecimalFormat vpvsFormat = new DecimalFormat("0.000");

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SumHKStack.class);
}
