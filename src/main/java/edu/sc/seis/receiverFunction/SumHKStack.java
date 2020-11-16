/**
 * SumHKStack.java
 * 
 * @author Created by Omnicore CodeGuide
 */
package edu.sc.seis.receiverFunction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.hkstack.CmplxArray2D;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.receiverFunction.server.StackComplexityResult;
import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.sod.bag.Cmplx;
import edu.sc.seis.sod.bag.Statistics;
import edu.sc.seis.sod.model.common.Location;
import edu.sc.seis.sod.model.common.QuantityImpl;
import edu.sc.seis.sod.model.common.UnitImpl;

public class SumHKStack {
    
    private boolean viaHibernate = false;
    
    /** for hibernate */
    protected SumHKStack(){viaHibernate=true;}

    public SumHKStack(float minPercentMatch,
                      QuantityImpl smallestH,
                      HKStack sum,
                      float hVariance,
                      float kVariance,
                      int numEQ,
                      List<ReceiverFunctionResult> individuals,
                      Set<RejectedMaxima> rejects) {
        this.sum = sum;
        this.minPercentMatch = minPercentMatch;
        this.smallestH = smallestH;
        this.hVariance = hVariance;
        this.kVariance = kVariance;
        this.individuals = individuals;
        this.setGaussianWidth(individuals.get(0).getGwidth());
        Channel chan = individuals.iterator()
                .next()
                .getChannelGroup()
                .getChannel1();

        net = chan.getNetworkCode();
        staCode = chan.getStationCode();
        this.rejects = rejects;
        best = makeStationResult(new StackMaximum(-1,
                                                  sum.getMaxValueH(),
                                                  -1,
                                                  sum.getMaxValueK(),
                                                  sum.getMaxValue(),
                                                  -1,
                                                  -1), "max", "");
    }

    public HKStack getSum() {
        if (sum != null) {
            sum.setGaussianWidth(getGaussianWidth());// g width not stored in db for hkstack
        }
        return sum;
    }

    public List<ReceiverFunctionResult> getIndividuals() {
        return individuals;
    }
    
    protected void setIndividuals(List<ReceiverFunctionResult> individuals) {
        this.individuals = individuals;
    }

    private int numEQ = -1;
    
    public int getNumEQ() {
    	/*
        if (viaHibernate) {
            if (numEQ < 0) {
                numEQ = ((Long) RecFuncDB.getSession().createFilter( getIndividuals(), "select count(*)" ).uniqueResult()).intValue();
            }
            return numEQ;
        }
        */
        return getIndividuals().size();
    }
    
    public Set<RejectedMaxima> getRejectedMaxima() {
        return rejects;
    }
    
    protected void setRejectedMaxima(Set<RejectedMaxima> rejects) {
        this.rejects = rejects;
    }
    
    protected void setSum(HKStack sum) {
        this.sum = sum;
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
        StationResult result = new StationResult(net,
                                                 staCode,
                                                 getSum().getMaxValueH(getSmallestH()),
                                                 getSum().getMaxValueK(getSmallestH()),
                                                 getSum().getAlpha(),
                                                 earsStaRef);
        StackComplexity complexity = new StackComplexity(getSum(),
                                                         getGaussianWidth());
        return complexity.getResidual(result, 60);
    }

    public static SumHKStack calculateForPhase(List<ReceiverFunctionResult> stackList,
                                               QuantityImpl smallestH,
                                               float minPercentMatch,
                                               boolean usePhaseWeight,
                                               Set<RejectedMaxima> rejects,
                                               boolean doBootstrap,
                                               int bootstrapIterations,
                                               String phase) {
        Channel chan = null;
        int numStacks = 0;
        float[][] sumStack = null;
        CmplxArray2D phaseWeight = null;
        HKStack individual;
        ReceiverFunctionResult first = null;
        int smallestHIndex = 0;
        Iterator<ReceiverFunctionResult> iterator = stackList.iterator();
        if(!iterator.hasNext()) {
            throw new IllegalArgumentException("individual HKStack cannot be length 0");
        }
        if (phase == null || phase.length() == 0) {
        	phase = "all";
        }
        while(iterator.hasNext()) {
            ReceiverFunctionResult result = iterator.next();
            if (result.getHKstack().getStack() == null) {
                throw new RuntimeException("hkstack.stack is null");
            }
            individual = result.getHKstack();
            if(first == null) {
                first = result;
                smallestHIndex = first.getHKstack().getHIndex(smallestH);
                sumStack = new float[first.getHKstack().getStack().length - smallestHIndex][first.getHKstack().getStack()[0].length];
                phaseWeight = new CmplxArray2D(sumStack.length,
                                               sumStack[0].length);
            }
            if(!individual.getMinH().equals(first.getHKstack().getMinH())) {
                throw new IllegalArgumentException("Cannot create SumStack with different minH, "
                        + individual.getMinH() + "!=" + first.getHKstack().getMinH());
            }
            if(individual.getMinK() != first.getHKstack().getMinK()) {
                throw new IllegalArgumentException("Cannot create SumStack with different minK, "
                        + individual.getMinK() + "!=" + first.getHKstack().getMinK());
            }
            // need to do more of this checking...
            for(int hIndex = 0; hIndex < sumStack.length; hIndex++) {
                int shiftHIndex = smallestHIndex + hIndex;
                for(int kIndex = 0; kIndex < sumStack[0].length; kIndex++) {
                    if(usePhaseWeight) {
                        Cmplx val = new Cmplx(0, 0);
                        if(phase.equals("Ps") || phase.equalsIgnoreCase("all")) {
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

        HKStack s = first.getHKstack();
        HKStack hkStack = new HKStack(s.getAlpha(),
                                      0f,
                                      first.getGwidth(),
                                      minPercentMatch,
                                      s.getMinH().add(s.getStepH()
                                              .multipliedByDbl(smallestHIndex)),
                                      s.getStepH(),
                                      s.getNumH() - smallestHIndex,
                                      s.getMinK(),
                                      s.getStepK(),
                                      s.getNumK(),
                                      s.getWeightPs(),
                                      s.getWeightPpPs(),
                                      s.getWeightPsPs(),
                                      sumStack);
        float hVariance = -1;
        float kVariance = -1;
        SumHKStack out = new SumHKStack(minPercentMatch,
                                        smallestH,
                                        hkStack,
                                        hVariance,
                                        kVariance,
                                        numStacks,
                                        stackList,
                                        rejects);
        out.recalcBest();
        if(doBootstrap) {
            out.calcVarianceBootstrap(usePhaseWeight, bootstrapIterations);
        }
        return out;
    }

    public StackComplexityResult calcStackComplexity() throws TauModelException {
        StackComplexity complexity = new StackComplexity(getSum(),
                                                         getGaussianWidth());
        StationResult model = new StationResult(net,
                                                staCode,
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
        Channel chan = getIndividuals().iterator()
                .next()
                .getChannelGroup()
                .getChannel1();
        StationResult crust2Result = HKStack.getCrust2()
                .getStationResult(chan.getNetworkCode(), chan.getStationCode(), Location.of(chan));
        float crust2diff = bestH
                - (float)crust2Result.getH().getValue(UnitImpl.KILOMETER);
        setComplexityResult(new StackComplexityResult(complex,
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

    public float getHVariance() {
        return hVariance;
    }
    
    protected void setHVariance(float var) {
        this.hVariance = var;
    }

    public float getKVariance() {
        return kVariance;
    }
    
    protected void setKVariance(float var) {
        this.kVariance = var;
    }

    public float getMixedVariance() {
        return mixedVariance;
    }
    
    protected void setMixedVariance(float var) {
        this.mixedVariance = var;
    }

    public QuantityImpl getHStdDev() {
        return new QuantityImpl(Math.sqrt(hVariance), UnitImpl.KILOMETER);
    }

    public float getKStdDev() {
        return (float)Math.sqrt(kVariance);
    }

    public String formatKStdDev() {
        return vpvsFormat.format(getKStdDev());
    }

    public float getMixedStdDev() {
        return (float)Math.sqrt(getMixedVariance());
    }

    public float getMinPercentMatch() {
        return minPercentMatch;
    }

    public QuantityImpl getSmallestH() {
        return smallestH;
    }

    protected void calcVarianceBootstrap(boolean usePhaseWeight,
                                         int bootstrapIterations) {
        List<ReceiverFunctionResult> individualList = new ArrayList<ReceiverFunctionResult>();
        individualList.addAll(getIndividuals());
        double[] hErrors = new double[bootstrapIterations];
        double[] kErrors = new double[bootstrapIterations];
        for(int i = 0; i < bootstrapIterations; i++) {
            ArrayList sample = new ArrayList();
            for(ReceiverFunctionResult result : getIndividuals()) {
                sample.add(individualList.get(randomInt(individualList.size())));
            }
            SumHKStack sampleStack = calculateForPhase(sample,
                                                    smallestH,
                                                    minPercentMatch,
                                                    usePhaseWeight,
                                                    getRejectedMaxima(),
                                                    false,
                                                    0,
                                                    "all");
            StationResult bestBoot = sampleStack.getBest();
            hErrors[i] = (float)bestBoot.getH().getValue(UnitImpl.KILOMETER);
            kErrors[i] = bestBoot.getVpVs();
            if(i % 10 == 0) {
                logger.info("calcVarianceBootstrap:  " + i + " "+getNet()+"."+getStaCode()+" "
                        + hErrors[i] + "  " + kErrors[i]);
            }
        }
        Statistics hStat = new Statistics(hErrors);
        hVariance = (float)hStat.var();
        Statistics kStat = new Statistics(kErrors);
        kVariance = (float)kStat.var();
        mixedVariance = (float)hStat.correlation(kErrors);
        best.setHStdDev(getHStdDev());
        best.setKStdDev((float)getKStdDev());
        hBootstrap = hErrors;
        kBootstrap = kErrors;
    }

    protected int randomInt(int top) {
        return (int)Math.floor(Math.random() * top);
    }

    protected void calcVarianceSecondDerivative() {
        float[] peakVals = new float[individuals.size()];
        StackMaximum maxIndices = sum.getGlobalMaximum();
        for(int s = 0; s < individuals.size(); s++) {
            peakVals[s] = maxIndices.getMaxValue();
        }
        Statistics stat = new Statistics(peakVals);
        maxVariance = (float)stat.var();
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
        hVariance = (float)(-2 * maxVariance * secPartialK / denom);
        kVariance = (float)(-2 * maxVariance * secPartialH / denom);
        mixedVariance = (float)(-2 * maxVariance * mixedPartialHK / denom);
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
            if(inAnalystReject(localMaxima[i].getHValue(),
                                     localMaxima[i].getKValue(),
                                     getRejectedMaxima()) == null) {
                String extra = "amp=" + localMaxima[i].getMaxValue();
                String name = i == 0 ? "Global Maxima" : "Local Maxima " + i;
                return makeStationResult(localMaxima[i], name, extra);
            }
        }
        // this is bad
        throw new RuntimeException("didn't find good maxima in top 5 local max");
    }

    public StationResult makeStationResult(StackMaximum max,
                                           String name,
                                           String extra) {
        StationResultRef earsStaRef = new StationResultRef(name, "ears", "ears");
        return new StationResult(net,
                                 staCode,
                                 max.getHValue(),
                                 max.getKValue(),
                                 sum.getAlpha(),
                                 max.getMaxValue(),
                                 getHStdDev(),
                                 (float)getKStdDev(),
                                 earsStaRef,
                                 extra);
    }

    public static RejectedMaxima inAnalystReject(QuantityImpl hQuantity,
                                      float k,
                                      Collection<RejectedMaxima> rejects) {
        float h = (float)hQuantity.getValue(UnitImpl.KILOMETER);
        for(RejectedMaxima box : rejects) {
            if(box.inside(h, k)) {
                return box;
            }
        }
        return null;
    }

    public StationResult getBest() {
        if(best == null) {
            logger.error("best is null", new NullPointerException());
        }
        return best;
    }
    
    protected void setBest(StationResult sr) {
        this.best = sr;
        // these are not part of the StationResult stored in the db to avoid duplication
        best.setNet(getNet());
        best.setStaCode(getStaCode());
        best.setVp(getSum().getAlpha());
        best.setHStdDev(getHStdDev());
        best.setKStdDev(getKStdDev());
    }

    public void recalcBest() {
        best = calcBest(sum);
    }

    public double[] getHBootstrap() {
        return hBootstrap;
    }

    public double[] getKBootstrap() {
        return kBootstrap;
    }
    
    public float getGaussianWidth() {
        if (viaHibernate && gaussianWidth <= 0) {
            setGaussianWidth(getIndividuals().get(0).getGwidth());
        }
        return gaussianWidth;
    }
    
    protected void setGaussianWidth(float gw) {
        this.gaussianWidth = gw;
    }
    
    protected float gaussianWidth;

    protected List<ReceiverFunctionResult> individuals;

    protected HKStack sum;

    protected float minPercentMatch;

    protected QuantityImpl smallestH;

    protected float maxVariance;

    protected float hVariance;
    
    protected double[] hBootstrap;
    
    protected float kVariance;

    protected double[] kBootstrap;

    protected StationResult best;

    protected Set<RejectedMaxima> rejects;

    protected float mixedVariance;

    protected int dbid = -1;

    protected String net;

    protected String staCode;

    public static int DEFAULT_BOOTSTRAP_ITERATONS = 100;

    protected StackComplexityResult complexityResult = null;

    private static DecimalFormat vpvsFormat = new DecimalFormat("0.000");

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SumHKStack.class);

    
    public String getNet() {
        return net;
    }

    
    public void setNet(String net) {
        this.net = net;
    }

    
    public String getStaCode() {
        return staCode;
    }

    
    public void setStaCode(String stationCode) {
        this.staCode = stationCode;
    }
}
