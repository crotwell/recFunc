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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComponent;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.freq.CmplxArray2D;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.web.GMTColorPalette;

public class HKStackImage extends JComponent {

    HKStackImage(HKStack stack) {
        this(stack, "all", 0);
    }

    HKStackImage(HKStack stack, String phase, int smallestHindex) {
        this.stack = stack;
        this.smallestHindex = smallestHindex;
        if(phase.equals("all")) {
            stackOut = stack.getStack();
        } else {
            CmplxArray2D analytic = null;
            if(phase.equals("Ps")) {
                analytic = stack.getAnalyticPs();
            } else if(phase.equals("PpPs")) {
                analytic = stack.getAnalyticPpPs();
            } else if(phase.equals("PsPs")) {
                analytic = stack.getAnalyticPsPs();
            }
            stackOut = new float[analytic.getXLength()][analytic.getYLength()];
            for(int i = 0; i < analytic.getXLength(); i++) {
                for(int j = 0; j < analytic.getYLength(); j++) {
                    stackOut[i][j] = analytic.getReal(i, j);
                }
            }
        }
        Dimension imageSize = new Dimension(2 * stackOut[0].length,
                                            2 * (stackOut.length - smallestHindex));
        setMinimumSize(imageSize);
        setPreferredSize(imageSize);
        // color map max and min
        int[] xyMin = stack.getMinValueIndices();
        StackMaximum xyMax = stack.getGlobalMaximum();
        // colorMapMin = stack.stack[xyMin[0]][xyMin[1]];
        colorMapMin = 0;
        colorMapMax = xyMax.getMaxValue();
    }

    public void addMarker(StationResult result, Color color) {
        markers.add(new Marker(result, color));
    }

    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;
        Color origColor = g.getColor();
        int[] xyMin = stack.getMinValueIndices();
        StackMaximum xyMax = stack.getGlobalMaximum();
        float min = stack.stack[xyMin[0]][xyMin[1]];
        float max = xyMax.getMaxValue();
        try {
            BufferedReader buf = new BufferedReader(new InputStreamReader(getCPTFile().openStream()));
            colorPallete = GMTColorPalette.load(buf).renormalize(colorMapMin,
                                                                 colorMapMax,
                                                                 Color.BLACK,
                                                                 Color.MAGENTA,
                                                                 Color.CYAN);
            buf.close();
        } catch(IOException e) {
            logger.warn(e);
            colorPallete = GMTColorPalette.getDefault(colorMapMin, colorMapMax);
        }
        for(int j = smallestHindex; j < stackOut.length; j++) {
            for(int k = 0; k < stackOut[j].length; k++) {
                if(j == xyMax.getHIndex() && k == xyMax.getKIndex()) {
                    g.setColor(Color.red);
                } else {
                    Color color = colorPallete.getColor(stackOut[j][k]);
                    g.setColor(color);
                }
                g.fillRect(2 * k, 2 * (j - smallestHindex), 2, 2);
            }
        }
        // tradeoff curves
        /*
         * g.setColor(Color.CYAN); TradeoffCurve tradeoff = new
         * TradeoffCurve(stack); float[] curve = tradeoff.getH_Ps(); for (int k =
         * 0; k < stackOut[0].length-1; k++) { g.drawLine(2*k,
         * Math.round(2*stack.getHIndexFloat(new QuantityImpl(curve[k],
         * UnitImpl.KILOMETER))), 2*(k+1), 2*stack.getHIndex(new
         * QuantityImpl(curve[k+1], UnitImpl.KILOMETER))); } curve =
         * tradeoff.getH_PpPs(); for (int k = 0; k < stackOut[0].length-1; k++) {
         * g.drawLine(2*k, Math.round(2*stack.getHIndexFloat(new
         * QuantityImpl(curve[k], UnitImpl.KILOMETER))), 2*(k+1),
         * 2*stack.getHIndex(new QuantityImpl(curve[k+1], UnitImpl.KILOMETER))); }
         * curve = tradeoff.getH_PsPs(); for (int k = 0; k <
         * stackOut[0].length-1; k++) { g.drawLine(2*k,
         * Math.round(2*stack.getHIndexFloat(new QuantityImpl(curve[k],
         * UnitImpl.KILOMETER))), 2*(k+1), 2*stack.getHIndex(new
         * QuantityImpl(curve[k+1], UnitImpl.KILOMETER))); }
         */
        Iterator it = markers.iterator();
        while(it.hasNext()) {
            Marker mark = (Marker)it.next();
            g.setColor(mark.getColor());
            g.fillRect(Math.round(2 * stack.getKIndexFloat(mark.getVpvs())),
                       Math.round(2 * stack.getHIndexFloat(mark.getDepth())),
                       2,
                       2);
        }
        g.setColor(origColor);
    }

    public GMTColorPalette getColorPallete() {
        return colorPallete;
    }

    public float getColorMapMax() {
        return colorMapMax;
    }

    public void setColorMapMax(float colorMapMax) {
        this.colorMapMax = colorMapMax;
    }

    public float getColorMapMin() {
        return colorMapMin;
    }

    public void setColorMapMin(float colorMapMin) {
        this.colorMapMin = colorMapMin;
    }

    public static URL getCPTFile() {
        if(CPT_FILE == null) {
            String prop = System.getProperty("gmt.cpt_file");
            if(prop != null && prop != "") {
                try {
                    CPT_FILE = new URL(prop);
                } catch(MalformedURLException e) {
                    throw new RuntimeException("Shouldn't happen", e);
                }
            } else {
                CPT_FILE = DEFAULT_CPT_FILE;
            }
        }
        return CPT_FILE;
    }

    static URL CPT_FILE;

    static final URL DEFAULT_CPT_FILE = HKStackImage.class.getClassLoader()
            .getResource("edu/sc/seis/receiverFunction/GMT_wysiwyg.cpt");

    GMTColorPalette colorPallete;

    protected HKStack stack;

    float[][] stackOut;

    float colorMapMax, colorMapMin;

    protected ArrayList markers = new ArrayList();

    protected int smallestHindex = 0;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKStackImage.class);
}
