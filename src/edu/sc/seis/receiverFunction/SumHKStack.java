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

    protected Channel channel;
    protected HKStack[] individuals;
    protected HKStack sum;
    protected float minPercentMatch;
    protected float smallestH;
}

