package edu.sc.seis.receiverFunction;

import junit.framework.TestCase;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;

public class HKStackTest extends TestCase {

    public static HKStack getMockHKStack() {
        return getMockHKStack(new int[][] { {40, 20}, {60, 40}, {40, 40}});
    }

    public static HKStack getMockHKStack(int[][] inMaxima) {
        QuantityImpl alpha = new QuantityImpl(6.0, UnitImpl.KILOMETER_PER_SECOND);
        float p = 0.1f;
        float gwidth = 2.5f;
        float percentMatch = 80;
        QuantityImpl minH = new QuantityImpl(10, UnitImpl.KILOMETER);
        QuantityImpl stepH = new QuantityImpl(.25, UnitImpl.KILOMETER);
        int numH = 200;
        float minK = 1.6f;
        float stepK = .01f;
        int numK = 50;
        float weightPs = 1 / 3f;
        float weightPpPs = 1 / 3f;
        float weightPsPs = 1 / 3f;
        float[][] stack = new float[numH][numK];
        for(int i = 0; i < inMaxima.length; i++) {
            stack[inMaxima[i][0]][inMaxima[i][1]] = inMaxima.length - i;
        }
        // smooth
        float[][] smoothStack = new float[stack.length][stack[0].length];
        for(int i = 1; i < stack.length - 1; i++) {
            for(int j = 1; j < stack[i].length - 1; j++) {
                smoothStack[i][j] = stack[i - 1][j] * .1f + stack[i + 1][j] * .1f + stack[i][j + 1] * .1f
                        + stack[i][j - 1] * .1f + stack[i][j] * .6f + stack[i - 1][j + 1] * .05f + stack[i + 1][j - 1]
                        * .05f + stack[i + 1][j + 1] * .05f + stack[i - 1][j - 1] * .05f;
            }
        }
        // stack = smoothStack;
        return new HKStack(alpha,
                           p,
                           gwidth,
                           percentMatch,
                           minH,
                           stepH,
                           numH,
                           minK,
                           stepK,
                           numK,
                           weightPs,
                           weightPpPs,
                           weightPsPs,
                           stack);
    }

    public void testGetLocalMaxima() {
        int[][] inMaxima = new int[][] { {40, 20}, {60, 30}, {80, 40}};
        int startHIndex = 5;
        int num = 3;
        HKStack in = getMockHKStack(inMaxima);
        StackMaximum[] out = in.getLocalMaxima(startHIndex, num);
        for(int i = 0; i < inMaxima.length; i++) {
            assertEquals("local max x " + i, inMaxima[i][0], out[i].getHIndex());
            assertEquals("local max y " + i, inMaxima[i][1], out[i].getKIndex());
        }
    }
}
