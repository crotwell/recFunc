/**
 * HKStackImage.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

public class HKStackImage extends JComponent {

    HKStackImage(HKStack stack) {
        this.stack = stack;
    }


    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;

        float[][] stackOut = stack.getStack();

        int[] xy = stack.getMinMaxValueIndices();

        float min = stack.stack[xy[0]][xy[1]];
        float max = stack.stack[xy[2]][xy[3]];

        for (int j = 0; j < stackOut.length; j++) {
            //System.out.print(j+" : ");
            for (int k = 0; k < stackOut[j].length; k++) {
                int colorVal = makeImageable(min, max, stackOut[j][k]);
                g.setColor(new Color(colorVal, colorVal, colorVal));
                g.fillRect( 2*k, 2*j, 2, 2);
                //System.out.print(colorVal+" ");
            }
            //System.out.println("");
        }
    }

    static int makeImageable(float min, float max, float val) {
        float absMax = Math.max(Math.abs(min), Math.abs(max));
        return (int)SimplePlotUtil.linearInterp(-1*absMax, 0, absMax, 255, val);
    }

    protected HKStack stack;

}

