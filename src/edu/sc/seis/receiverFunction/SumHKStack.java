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
import java.util.LinkedList;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.sod.status.FissuresFormatter;

public class SumHKStack {
    public SumHKStack(HKStack[] individuals,
                      Channel chan,
                      float minPercentMatch,
                      float smallestH) {
        this.individuals = individuals;
        this.minPercentMatch = minPercentMatch;
        this.channel = chan;
        this.smallestH = smallestH;
        if (individuals.length == 0) {
            throw new IllegalArgumentException("Cannot create SumStack with empty array");
        }
        for (int i = 0; i < individuals.length; i++) {
            if (individuals[i].getMinH() != individuals[0].getMinH()) {
                throw new IllegalArgumentException("Cannot create SumStack with different minH, "+individuals[i].getMinH() +"!="+ individuals[0].getMinH());
            }
            if (individuals[i].getMinK() != individuals[0].getMinK()) {
                throw new IllegalArgumentException("Cannot create SumStack with different minK, "+individuals[i].getMinK() +"!="+ individuals[0].getMinK());
            }
            // need to do more of this checking...
        }
        calculate(chan);
    }

    public BufferedImage createStackImage() {
        return sum.createStackImage();
    }

    public void write(DataOutputStream out)  throws IOException {
        sum.write(out);
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
    
    void calculate(Channel chan) {
        int smallestHIndex = (int)Math.round((smallestH-individuals[0].getMinH())/individuals[0].getStepH());
        float[][] stack = new float[individuals[0].getStack().length-smallestHIndex][individuals[0].getStack()[0].length];
        for (int i = 0; i < stack.length; i++) {
            for (int j = 0; j < stack[0].length; j++) {
                for (int s = 0; s < individuals.length; s++) {
                    stack[i][j] += individuals[s].getStack()[i+smallestHIndex][j];
                }
            }
        }
        sum = new HKStack(individuals[0].getAlpha(),
                          0f,
                          minPercentMatch,
                          individuals[0].getMinH()+smallestHIndex*individuals[0].getStepH(),
                          individuals[0].getStepH(),
                          individuals[0].getNumH()-smallestHIndex,
                          individuals[0].getMinK(),
                          individuals[0].getStepK(),
                          individuals[0].getNumK(),
                          stack,
                          chan);
        calcVariance();   
    }

    public static SumHKStack load(File parentDir, Channel chan, String prefix, String postfix, float minPercentMatch) throws IOException {
        File[] subdir = parentDir.listFiles();
        LinkedList stacks = new LinkedList();
        for (int i = 0; i < subdir.length; i++) {
            if ( ! subdir[i].isDirectory()) {
                continue;
            }
            File stackFile = new File(subdir[i],
                                      FissuresFormatter.filize(prefix+ChannelIdUtil.toStringNoDates(chan.get_id())+postfix));
            if ( ! stackFile.exists()) {
                continue;
            }
            // found a file with the correct name, load it
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(stackFile)));
            HKStack individual = HKStack.read(dis);
            if (individual.getPercentMatch() > minPercentMatch) {
                stacks.add(individual);
            }
        }
        if (stacks.size() != 0) {
            return new SumHKStack((HKStack[])stacks.toArray(new HKStack[0]),
                                  chan,
                                  minPercentMatch,
                                  ((HKStack)stacks.get(0)).getMinH());
        } else {
            return null;
        }
    }
    
    public double getHError() {
        return HError;
    }

    public double getKError() {
        return KError;
    }
    
    protected void calcVariance() {
        float[] peakVals = new float[individuals.length];
        int[] maxIndices = sum.getMaxValueIndices();
        for (int s = 0; s < individuals.length; s++) {
            peakVals[s] = individuals[s].getStack()[maxIndices[0]][maxIndices[1]];
        }
        Statistics stat = new Statistics(peakVals);
        maxVariance = stat.var();
        // H is first index, K is second
        double a;
        double b;
        double c;
        if (maxIndices[0] == 0) {
            // off edge, shift by 1???
            a = sum.getStack()[maxIndices[0]][maxIndices[1]];
            b = sum.getStack()[maxIndices[0]+1][maxIndices[1]];
            c = sum.getStack()[maxIndices[0]+2][maxIndices[1]];
        } else if (maxIndices[0] == sum.getStack().length-1) {
            //          off edge, shift by 1???
            a = sum.getStack()[maxIndices[0]-2][maxIndices[1]];
            b = sum.getStack()[maxIndices[0]-1][maxIndices[1]];
            c = sum.getStack()[maxIndices[0]][maxIndices[1]];
        } else {
            // normal case in interior
            a = sum.getStack()[maxIndices[0]-1][maxIndices[1]];
            b = sum.getStack()[maxIndices[0]][maxIndices[1]];
            c = sum.getStack()[maxIndices[0]+1][maxIndices[1]];
        }
        HError = -2*maxVariance/((a-2*b+c)/sum.getStepH()*sum.getStepH());
        
        if (maxIndices[1] == 0) {
            // off edge, shift by 1???
            a = sum.getStack()[maxIndices[0]][maxIndices[1]];
            b = sum.getStack()[maxIndices[0]][maxIndices[1]+1];
            c = sum.getStack()[maxIndices[0]][maxIndices[1]+2];
        } else if (maxIndices[1] == sum.getStack()[0].length-1) {
            // off edge, shift by 1???
            a = sum.getStack()[maxIndices[0]][maxIndices[1]-2];
            b = sum.getStack()[maxIndices[0]][maxIndices[1]-1];
            c = sum.getStack()[maxIndices[0]][maxIndices[1]];
        } else {
            // normal case
            a = sum.getStack()[maxIndices[0]][maxIndices[1]-1];
            b = sum.getStack()[maxIndices[0]][maxIndices[1]];
            c = sum.getStack()[maxIndices[0]][maxIndices[1]+1];
        }
        KError = -2*maxVariance/((a-2*b+c)/sum.getStepK()*sum.getStepK());
    }

    protected Channel channel;
    protected HKStack[] individuals;
    protected HKStack sum;
    protected float minPercentMatch;
    protected float smallestH;
    protected double maxVariance;
    protected double HError;
    protected double KError;
}

