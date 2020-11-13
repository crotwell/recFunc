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
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import edu.sc.seis.hkstack.CmplxArray2D;

public class HKStackImage extends JComponent {

    public HKStackImage(HKStack stack) {
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


    public void paintComponent(Graphics graphics) {
    	Dimension size = getPreferredSize();
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
        int squareWidth = size.width/stackOut[0].length;
        int squareHeight = size.height/stackOut.length;
        
        for(int j = smallestHindex; j < stackOut.length; j++) {
            for(int k = 0; k < stackOut[j].length; k++) {
                if(j == xyMax.getHIndex() && k == xyMax.getKIndex()) {
                    g.setColor(Color.red);
                } else {
                    Color color = colorPallete.getColor(stackOut[j][k]);
                    g.setColor(color);
                }
                g.fillRect(squareWidth * k, squareHeight * (j - smallestHindex), squareWidth, squareHeight);
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
    

    public BufferedImage createImage(int width, int height) {
        BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Dimension imageSize = new Dimension(width, height);
        setMinimumSize(imageSize);
        setPreferredSize(imageSize);
        this.paintComponent(bufImg.createGraphics());
        return bufImg;
    }
    
    public void saveImage(String filename, int width, int height) throws IOException {
        ImageIO.write(createImage(width, height), "png", new File(filename));
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
