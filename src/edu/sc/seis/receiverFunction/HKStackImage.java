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
import java.awt.Dimension;

public class HKStackImage extends JComponent {

    HKStackImage(HKStack stack) {
        this.stack = stack;
        float[][] stackOut = stack.getStack();
        Dimension imageSize = new Dimension(2*stackOut[0].length, 2*stackOut.length);
        setMinimumSize(imageSize);
        setPreferredSize(imageSize);
    }


    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;

        float[][] stackOut = stack.getStack();

        int[] xyMin = stack.getMinValueIndices();
        int[] xyMax = stack.getMaxValueIndices();

        float min = stack.stack[xyMin[0]][xyMin[1]];
        float max = stack.stack[xyMax[2]][xyMax[3]];

        for (int j = 0; j < stackOut.length; j++) {
            //System.out.print(j+" : ");
            for (int k = 0; k < stackOut[j].length; k++) {
                if (j== xyMax[2] && k==xyMax[3]) {
                    g.setColor(Color.red);
                } else {
                    int colorVal = makeImageable(min, max, stackOut[j][k]);
                    g.setColor(new Color(colorVal, colorVal, colorVal));
                }
                g.fillRect( 2*k, 2*(stackOut.length-j-1), 2, 2);
                //System.out.print(colorVal+" ");
            }
            //System.out.println("");
        }
        g.setColor(Color.green);
        g.drawRect(0, 0, 2*stackOut[0].length-1, 2*stackOut.length-1);
    }

    static int makeImageable(float min, float max, float val) {
        if (val > max || val < min) {
            throw new IllegalArgumentException("val must be between min and max val="+val+ "("+min+" , "+max+")");
        }
        float absMax = Math.max(Math.abs(min), Math.abs(max));
        return (int)SimplePlotUtil.linearInterp(-1*absMax, 0, absMax, 255, val);
    }

    protected HKStack stack;

}

