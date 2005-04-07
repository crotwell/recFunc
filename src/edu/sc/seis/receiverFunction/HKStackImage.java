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
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComponent;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.receiverFunction.compare.StationResult;

public class HKStackImage extends JComponent {

    HKStackImage(HKStack stack) {
        this(stack, 0);
    }

    HKStackImage(HKStack stack, int smallestHindex) {
        this.stack = stack;
        this.smallestHindex = smallestHindex;
        float[][] stackOut = stack.getStack();
        Dimension imageSize = new Dimension(2*stackOut[0].length, 2*(stackOut.length-smallestHindex));
        setMinimumSize(imageSize);
        setPreferredSize(imageSize);
    }

    public void addMarker(StationResult result, Color color) {
        markers.add(new Marker(result, color));
    }

    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;
        Color origColor = g.getColor();

        float[][] stackOut = stack.getStack();

        int[] xyMin = stack.getMinValueIndices();
        int[] xyMax = stack.getMaxValueIndices();

        float min = stack.stack[xyMin[0]][xyMin[1]];
        float max = stack.stack[xyMax[0]][xyMax[1]];

        for (int j = smallestHindex; j < stackOut.length; j++) {
            //System.out.print(j+" : ");
            for (int k = 0; k < stackOut[j].length; k++) {
                if (j== xyMax[0] && k==xyMax[1]) {
                    g.setColor(Color.red);
                } else {
                    int colorVal = makeImageable(0, max, stackOut[j][k]);
                    g.setColor(new Color(colorVal, colorVal, colorVal));
                }
                g.fillRect( 2*k, 2*(j-smallestHindex), 2, 2);
                //System.out.print(colorVal+" ");
            }
            //System.out.println("");
        }
        g.setColor(Color.CYAN);
        TradeoffCurve tradeoff = new TradeoffCurve(stack);
        float[] curve = tradeoff.getH_Ps();
        for (int k = 0; k < stackOut[0].length-1; k++) {
            g.drawLine(2*k, Math.round(2*stack.getHIndexFloat(new QuantityImpl(curve[k], UnitImpl.KILOMETER))),
                       2*(k+1), 2*stack.getHIndex(new QuantityImpl(curve[k+1], UnitImpl.KILOMETER)));
        }
        curve = tradeoff.getH_PpPs();
        for (int k = 0; k < stackOut[0].length-1; k++) {
            g.drawLine(2*k, Math.round(2*stack.getHIndexFloat(new QuantityImpl(curve[k], UnitImpl.KILOMETER))),
                       2*(k+1), 2*stack.getHIndex(new QuantityImpl(curve[k+1], UnitImpl.KILOMETER)));
        }
        curve = tradeoff.getH_PsPs();
        for (int k = 0; k < stackOut[0].length-1; k++) {
            g.drawLine(2*k, Math.round(2*stack.getHIndexFloat(new QuantityImpl(curve[k], UnitImpl.KILOMETER))),
                       2*(k+1), 2*stack.getHIndex(new QuantityImpl(curve[k+1], UnitImpl.KILOMETER)));
        }
        
        Iterator it = markers.iterator();
        while(it.hasNext()) {
            Marker mark = (Marker)it.next();
            System.out.println("Marker "+mark.getName()+" "+mark.getVpvs()+"-->"+Math.round(2*stack.getKIndexFloat(mark.getVpvs()))+"  "+mark.getDepth()+"-->"+ Math.round(2*stack.getHIndexFloat(mark.getDepth())));
            g.setColor(mark.getColor());
            g.fillRect(Math.round(2*stack.getKIndexFloat(mark.getVpvs())), Math.round(2*stack.getHIndexFloat(mark.getDepth())), 2, 2);
        }
        g.setColor(origColor);
    }
    

    static int makeImageable(float min, float max, float val) {
        if (val > max) { return makeImageable(min, max, max); }
        if (val < min) { return makeImageable(min, max, min); }
        return (int)SimplePlotUtil.linearInterp(min, 255, max, 0, val);
    }

    protected HKStack stack;

    protected ArrayList markers = new ArrayList();

    protected int smallestHindex = 0;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKStackImage.class);
}

