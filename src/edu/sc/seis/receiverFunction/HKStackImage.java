/**
 * HKStackImage.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;

public class HKStackImage extends JComponent {

    HKStackImage(HKStack stack) {
        this.stack = stack;
        float[][] stackOut = stack.getStack();
        Dimension imageSize = new Dimension(2*stackOut[0].length, 2*stackOut.length);
        setMinimumSize(imageSize);
        setPreferredSize(imageSize);
    }

    public void addMarker(int vpvsIndex, int depthIndex) {
        markerH = depthIndex;
        markerV = vpvsIndex;
    }


    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;

        float[][] stackOut = stack.getStack();

        int[] xyMin = stack.getMinValueIndices();
        int[] xyMax = stack.getMaxValueIndices();

        float min = stack.stack[xyMin[0]][xyMin[1]];
        float max = stack.stack[xyMax[0]][xyMax[1]];

        for (int j = 0; j < stackOut.length; j++) {
            //System.out.print(j+" : ");
            for (int k = 0; k < stackOut[j].length; k++) {
                if (j== xyMax[0] && k==xyMax[1]) {
                    g.setColor(Color.red);
                } else if (j== markerH && k==markerV) {
                    g.setColor(Color.blue);
                } else {
                    int colorVal = makeImageable(0, max, stackOut[j][k]);
                    g.setColor(new Color(colorVal, colorVal, colorVal));
                }
                g.fillRect( 2*k, 2*j, 2, 2);
                //System.out.print(colorVal+" ");
            }
            //System.out.println("");
        }

    }

    static int makeImageable(float min, float max, float val) {
        if (val > max) { return makeImageable(min, max, max); }
        if (val < min) { return makeImageable(min, max, min); }
        return (int)SimplePlotUtil.linearInterp(min, 255, max, 0, val);
    }

    protected HKStack stack;

    protected int markerH = -1;
    protected int markerV = -1;

}

