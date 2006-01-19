/**
 * HKStack.java
 * 
 * @author Created by Omnicore CodeGuide
 */
package edu.sc.seis.receiverFunction;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.LinkedList;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.w3c.dom.Element;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.model.UnitRangeImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.Hilbert;
import edu.sc.seis.fissuresUtil.bag.PoissonsRatio;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.display.BorderedDisplay;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.fissuresUtil.display.borders.Border;
import edu.sc.seis.fissuresUtil.display.borders.UnitRangeBorder;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.freq.CmplxArray2D;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.fissuresUtil.xml.XMLQuantity;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.WilsonRistra;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.receiverFunction.web.GMTColorPalette;
import edu.sc.seis.sod.SodUtil;
import edu.sc.seis.sod.status.FissuresFormatter;

public class HKStack implements Serializable {

    public static final String ALL = "all";

    protected HKStack(QuantityImpl alpha,
                      float p,
                      float gwidth,
                      float percentMatch,
                      QuantityImpl minH,
                      QuantityImpl stepH,
                      int numH,
                      float minK,
                      float stepK,
                      int numK,
                      float weightPs,
                      float weightPpPs,
                      float weightPsPs) {
        if(alpha.getValue() <= 0) {
            throw new IllegalArgumentException("alpha must be positive: "
                    + alpha);
        }
        if(p < 0) {
            throw new IllegalArgumentException("p must be nonnegative: " + p);
        }
        if(minK <= 0) {
            throw new IllegalArgumentException("minK must be positive: " + minK);
        }
        this.alpha = alpha;
        this.p = p;
        this.gwidth = gwidth;
        this.percentMatch = percentMatch;
        this.minH = minH.convertTo(UnitImpl.KILOMETER);
        this.stepH = stepH;
        this.numH = numH;
        this.minK = minK;
        this.stepK = stepK;
        this.numK = numK;
        this.weightPs = weightPs;
        this.weightPpPs = weightPpPs;
        this.weightPsPs = weightPsPs;
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   DataSetSeismogram recFunc) throws FissuresException {
        this(alpha,
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
             weightPsPs);
        this.recFunc = recFunc;
        this.chan = recFunc.getDataSet()
                .getChannel(recFunc.getRequestFilter().channel_id);
        calculate();
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   LocalSeismogramImpl recFuncSeis,
                   Channel chan,
                   TimeInterval shift) throws FissuresException {
        this(alpha,
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
             weightPsPs);
        this.recFunc = new MemoryDataSetSeismogram(recFuncSeis);
        this.chan = chan;
        analyticPs = new CmplxArray2D(numH, numK);
        analyticPpPs = new CmplxArray2D(numH, numK);
        analyticPsPs = new CmplxArray2D(numH, numK);
        calculate(recFuncSeis, shift);
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   CmplxArray2D analyticPs,
                   CmplxArray2D analyticPpPs,
                   CmplxArray2D analyticPsPs) {
        this(alpha,
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
             weightPsPs);
        this.recFunc = null;
        this.analyticPs = analyticPs;
        this.analyticPpPs = analyticPpPs;
        this.analyticPsPs = analyticPsPs;
        calcPhaseStack();
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   CmplxArray2D analyticPs,
                   CmplxArray2D analyticPpPs,
                   CmplxArray2D analyticPsPs,
                   DataSetSeismogram recFunc) {
        this(alpha,
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
             analyticPs,
             analyticPpPs,
             analyticPsPs);
        this.recFunc = recFunc;
        this.chan = recFunc.getDataSet()
                .getChannel(recFunc.getRequestFilter().channel_id);
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   CmplxArray2D analyticPs,
                   CmplxArray2D analyticPpPs,
                   CmplxArray2D analyticPsPs,
                   Channel chan) {
        this(alpha,
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
             analyticPs,
             analyticPpPs,
             analyticPsPs);
        this.chan = chan;
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   float[][] stack,
                   Channel chan) {
        this(alpha,
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
             weightPsPs);
        this.recFunc = null;
        this.chan = chan;
        this.stack = stack;
        this.compactAnalyticPhase = null;
        this.analyticPs = null;
        this.analyticPpPs = null;
        this.analyticPsPs = null;
    }

    public HKStack(QuantityImpl alpha,
                   float p,
                   float gwidth,
                   float percentMatch,
                   QuantityImpl minH,
                   QuantityImpl stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float weightPs,
                   float weightPpPs,
                   float weightPsPs,
                   QuantityImpl peakH,
                   Float peakK,
                   Float peakVal,
                   Channel chan) {
        this(alpha,
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
             weightPsPs);
        this.recFunc = null;
        this.chan = chan;
        this.stack = null;
        this.compactAnalyticPhase = null;
        this.analyticPs = null;
        this.analyticPpPs = null;
        this.analyticPsPs = null;
        this.peakH = peakH;
        this.peakK = peakK;
        this.peakVal = peakVal;
    }

    /**
     * returns the x and y indices for the max value in the stack. The min x
     * value is in index 0 and the y in index 1. The max x value is in 2 and the
     * y in 3.
     */
    public int[] getMinValueIndices() {
        float[][] stackOut = getStack();
        float min = stackOut[0][0];
        int minIndexX = 0;
        int minIndexY = 0;
        for(int j = 0; j < stackOut.length; j++) {
            for(int k = 0; k < stackOut[j].length; k++) {
                if(stackOut[j][k] < min) {
                    min = stackOut[j][k];
                    minIndexX = j;
                    minIndexY = k;
                }
            }
        }
        int[] xy = new int[2];
        xy[0] = minIndexX;
        xy[1] = minIndexY;
        return xy;
    }

    /**
     * returns the x and y indices for the max value in the stack. The max x
     * value is in index 0 and the y in index 1. 
     */
    public int[] getMaxValueIndices() {
        return getMaxValueIndices(0);
    }

    /**
     * returns the x and y indices for the max value in the stack for a depth
     * grater than minH.
     */
    public int[] getMaxValueIndices(int startHIndex) {
        float[][] stackOut = getStack();
        float max = stackOut[startHIndex][0];
        int maxIndexX = 0;
        int maxIndexY = 0;
        for(int j = startHIndex; j < stackOut.length; j++) {
            for(int k = 0; k < stackOut[j].length; k++) {
                if(stackOut[j][k] > max) {
                    max = stackOut[j][k];
                    maxIndexX = j;
                    maxIndexY = k;
                }
            }
        }
        return new int[] {maxIndexX, maxIndexY};
    }

    public StackMaximum getGlobalMaximum() {
        return getLocalMaxima(0, 1)[0];
    }

    public StackMaximum[] getLocalMaxima(QuantityImpl startH, int num) {
        try {
            return getLocalMaxima(getHIndex(startH), num);
        } catch(RuntimeException t) {
            System.out.println("startH=" + startH);
            throw t;
        }
    }

    /**
     * Finds the top num local maxuma that are not within minDeltaH and
     * minDeltaK of another local maxima.
     */
    public StackMaximum[] getLocalMaxima(int startHIndex, int num) {
        int[] maxIndices = getMaxValueIndices(startHIndex);
        StackMaximum[] out = new StackMaximum[num];
        int maxIndexX = maxIndices[0];
        int maxIndexY = maxIndices[1];
        float max = getStack()[maxIndexX][maxIndexY];
        
        // do twice as we need StackMaximum to calc power to populate StackMaximum
        StackMaximum dumb = new StackMaximum(maxIndexX,
                                  getHFromIndex(maxIndexX),
                                  maxIndexY,
                                  getKFromIndex(maxIndexY),
                                  max,
                                  -1,
                                  -1);
        StationResult maxResult = getMaximumAsStationResult(dumb);
        StackComplexity complexity = new StackComplexity(this,
                                                         4096,
                                                         getGaussianWidth());
        try {
            HKStack residualStack = complexity.getResidual(maxResult, 80);
            out[0] = new StackMaximum(maxIndexX,
                                      getHFromIndex(maxIndexX),
                                      maxIndexY,
                                      getKFromIndex(maxIndexY),
                                      max,
                                      getPower(),
                                      residualStack.getPower());
            if(num > 1) {
                StackMaximum[] recursion = residualStack.getLocalMaxima(startHIndex,
                                                                        num - 1);
                System.arraycopy(recursion, 0, out, 1, recursion.length);
            }
        } catch(TauModelException e) {
            throw new RuntimeException("problem getting residual for local maxima",
                                       e);
        }
        return out;
    }

    public StationResult getMaximumAsStationResult(int startHIndex) {
        StackMaximum[] maxIndex = getLocalMaxima(startHIndex, 1);
        return getMaximumAsStationResult(maxIndex[0]);
    } 
    
    public StationResult getMaximumAsStationResult(StackMaximum max) {
        return new StationResult(getChannelId().network_id,
                                 getChannelId().station_code,
                                 max.getHValue(),
                                 max.getKValue(),
                                 getAlpha(),
                                 null);
    }

    private boolean isLocalMaxima(int j, int k, float[][] stackOut) {
        if(j != 0 && stackOut[j - 1][k] > stackOut[j][k]) {
            return false;
        }
        if(j != stackOut.length - 1 && stackOut[j + 1][k] > stackOut[j][k]) {
            return false;
        }
        if(k != 0 && stackOut[j][k - 1] > stackOut[j][k]) {
            return false;
        }
        if(k != stackOut[0].length - 1 && stackOut[j][k + 1] > stackOut[j][k]) {
            return false;
        }
        // check corners
        if(j != 0) {
            if(k != 0 && stackOut[j - 1][k - 1] > stackOut[j][k]) {
                return false;
            }
            if(k != stackOut[0].length - 1
                    && stackOut[j - 1][k + 1] > stackOut[j][k]) {
                return false;
            }
        }
        if(j != stackOut.length - 1) {
            if(k != 0 && stackOut[j + 1][k - 1] > stackOut[j][k]) {
                return false;
            }
            if(k != stackOut[0].length - 1
                    && stackOut[j + 1][k + 1] > stackOut[j][k]) {
                return false;
            }
        }
        // must be a maxima
        return true;
    }

    public int getHIndex(QuantityImpl h) {
        return Math.round(getHIndexFloat(h));
    }

    public int getKIndex(float k) {
        return Math.round(getKIndexFloat(k));
    }

    public float getHIndexFloat(QuantityImpl h) {
        if(h.greaterThan(getMinH())) {
            float f = (float)(h.subtract(getMinH()).divideBy(getStepH())).getValue();
            return f;
        } else {
            return 0;
        }
    }

    public float getKIndexFloat(double k) {
        return (float)((k - getMinK()) / getStepK());
    }

    public QuantityImpl getMaxValueH() {
        if(peakH != null) {
            return peakH;
        }
        try {
            StackMaximum indicies = getGlobalMaximum();
            QuantityImpl peakH = indicies.getHValue();
            return peakH;
        } catch(Throwable e) {
            GlobalExceptionHandler.handle(e);
            return new QuantityImpl(0, UnitImpl.METER);
        }
    }

    public QuantityImpl getMaxValueH(QuantityImpl smallestH) {
        try {
            StackMaximum[] indicies = getLocalMaxima(getHIndex(smallestH), 1);
            QuantityImpl peakH = indicies[0].getHValue();
            return peakH;
        } catch(Throwable e) {
            GlobalExceptionHandler.handle(e);
            return new QuantityImpl(0, UnitImpl.METER);
        }
    }

    public QuantityImpl getHFromIndex(int index) {
        return getMinH().add(getStepH().multiplyBy(index));
    }

    public String formatMaxValueH() {
        return FissuresFormatter.formatQuantity(getMaxValueH());
    }

    public float getMaxValueK() {
        if(peakK != null) {
            return peakK.floatValue();
        }
        StackMaximum indicies = getGlobalMaximum();
        float peakK = getMinK() + getStepK() * indicies.getKIndex();
        return peakK;
    }

    public float getMaxValueK(QuantityImpl smallestH) {
        StackMaximum[] indicies = getLocalMaxima(smallestH, 1);
        return indicies[0].getKValue();
    }

    public float getKFromIndex(int index) {
        return getMinK() + getStepK() * index;
    }

    public String formatMaxValueK() {
        return vpvsFormat.format(getMaxValueK());
    }

    public float getMaxValue() {
        if(peakVal != null) {
            return peakVal.floatValue();
        }
        StackMaximum indicies = getGlobalMaximum();
        return indicies.getMaxValue();
    }

    public float getMaxValue(QuantityImpl smallestH) {
        StackMaximum[] indicies = getLocalMaxima(smallestH, 1);
        return indicies[0].getMaxValue();
    }

    public String formatMaxValue() {
        return maxValueFormat.format(getMaxValue());
    }

    public QuantityImpl getVs() {
        return getAlpha().divideBy(getMaxValueK());
    }

    public String formatVs() {
        return FissuresFormatter.formatQuantity(getVs());
    }

    public float getPoissonsRatio() {
        return (float)PoissonsRatio.calcPoissonsRatio(getMaxValueK());
    }

    public String formatPoissonsRatio() {
        return vpvsFormat.format(getPoissonsRatio());
    }

    public BorderedDisplay getStackComponent(String phase) {
        return getStackComponent(phase, minH);
    }

    public BorderedDisplay getStackComponent(String phase,
                                             QuantityImpl smallestH) {
        int startHIndex = getHIndex(smallestH);
        HKStackImage stackImage = new HKStackImage(this, phase, startHIndex);
        if(crust2 != null) {
            StationResult result = crust2.getStationResult(chan.my_site.my_station);
            stackImage.addMarker(result, Color.blue);
        }
        if(wilson != null) {
            StationResult result = wilson.getResult(chan.my_site.my_station.get_id());
            if(result != null) {
                stackImage.addMarker(result, Color.GREEN);
            }
        }
        BorderedDisplay bd = new BorderedDisplay(stackImage);
        UnitRangeImpl depthRange = new UnitRangeImpl(getMinH().getValue()
                + startHIndex * getStepH().getValue(), getMinH().getValue()
                + getNumH() * getStepH().getValue(), UnitImpl.KILOMETER);
        UnitRangeBorder depthLeftBorder = new UnitRangeBorder(Border.LEFT,
                                                              Border.DESCENDING,
                                                              "Depth",
                                                              depthRange);
        bd.add(depthLeftBorder, bd.CENTER_LEFT);
        UnitRangeBorder depthRightBorder = new UnitRangeBorder(Border.RIGHT,
                                                               Border.DESCENDING,
                                                               "Depth",
                                                               depthRange);
        bd.add(depthRightBorder, bd.CENTER_RIGHT);
        UnitRangeBorder kTopBorder = new UnitRangeBorder(Border.TOP,
                                                         Border.ASCENDING,
                                                         "Vp/Vs",
                                                         new UnitRangeImpl(getMinK(),
                                                                           getMinK()
                                                                                   + getNumK()
                                                                                   * getStepK(),
                                                                           UnitImpl.divide(UnitImpl.KILOMETER_PER_SECOND,
                                                                                           UnitImpl.KILOMETER_PER_SECOND,
                                                                                           "km/s/km/s")));
        bd.add(kTopBorder, bd.TOP_CENTER);
        UnitRangeBorder kBottomBorder = new UnitRangeBorder(Border.BOTTOM,
                                                            Border.ASCENDING,
                                                            "Vp/Vs",
                                                            new UnitRangeImpl(getMinK(),
                                                                              getMinK()
                                                                                      + getNumK()
                                                                                      * getStepK(),
                                                                              UnitImpl.divide(UnitImpl.KILOMETER_PER_SECOND,
                                                                                              UnitImpl.KILOMETER_PER_SECOND,
                                                                                              "km/s/km/s")));
        bd.add(kBottomBorder, bd.BOTTOM_CENTER);
        Dimension dim = stackImage.getPreferredSize();
        dim = new Dimension(dim.width
                + depthLeftBorder.getPreferredSize().width
                + depthRightBorder.getPreferredSize().width, dim.height
                + kTopBorder.getPreferredSize().height
                + kBottomBorder.getPreferredSize().height);
        bd.setPreferredSize(dim);
        bd.setSize(dim);
        logger.info("end getStackComponent");
        return bd;
    }

    public BufferedImage createStackImage() {
        return createStackImage(ALL);
    }

    public BufferedImage createStackImage(String phase) {
        BorderedDisplay comp = getStackComponent(phase);
        return toImage(comp);
    }

    public BufferedImage toImage(BorderedDisplay comp) {
        JFrame frame = null;
        Graphics2D g = null;
        BufferedImage bufImage = null;
        try {
            if(comp.getRootPane() == null) {
                comp.addNotify();
                comp.validate();
            }
            Dimension size = comp.getPreferredSize();
            int fullWidth = size.width + 40;
            int fullHeight = size.height + 140;
            bufImage = new BufferedImage(fullWidth,
                                         fullHeight,
                                         BufferedImage.TYPE_INT_RGB);
            g = bufImage.createGraphics();
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Color.darkGray);
            g.fillRect(0, 0, bufImage.getWidth(), bufImage.getHeight());
            g.translate(0, 5);
            String title = ChannelIdUtil.toStringNoDates(getChannelId());
            g.setColor(Color.white);
            g.drawString(title,
                         (fullWidth - fm.stringWidth(title)) / 2,
                         fm.getHeight());
            g.translate(5, fm.getHeight() + fm.getDescent());
            comp.print(g);
            g.translate(0, size.height);
            int[] xyMin = getMinValueIndices();
            StackMaximum xyMax = getGlobalMaximum();
            float min = stack[xyMin[0]][xyMin[1]];
            float max = xyMax.getMaxValue();
            g.setColor(Color.white);
            g.drawString("% match=" + percentMatch, 0, fm.getHeight());
            g.drawString("    ", 0, 2 * fm.getHeight());
            g.translate(0, 2 * fm.getHeight());
            g.drawString("Max H=" + getMaxValueH(), 0, fm.getHeight());
            g.drawString("    K=" + getMaxValueK(), 0, 2 * fm.getHeight());
            g.translate(0, 2 * fm.getHeight());
            GMTColorPalette colorPallete = ((HKStackImage)comp.get(BorderedDisplay.CENTER)).getColorPallete();
            for(int i = 0; i < size.width; i++) {
                g.setColor(colorPallete.getColor(SimplePlotUtil.linearInterp(0,
                                                                             0,
                                                                             size.width,
                                                                             max,
                                                                             i)));
                g.fillRect(i, 0, 1, 15);
            }
            g.setColor(Color.white);
            g.drawString("Min=0", 0, 15 + fm.getHeight());
            g.setColor(Color.white);
            String maxString = "Max=" + max;
            int stringWidth = fm.stringWidth(maxString);
            g.drawString(maxString,
                         size.width - 5 - stringWidth,
                         15 + fm.getHeight());
        } finally {
            if(g != null) {
                g.dispose();
            }
            if(frame != null) {
                frame.dispose();
            }
        }
        return bufImage;
    }

    public static float getPercentMatch(DataSetSeismogram recFunc) {
        String percentMatch = "-9999";
        if(recFunc != null) {
            Element e = (Element)recFunc.getAuxillaryData("recFunc.percentMatch");
            percentMatch = SodUtil.getNestedText(e);
        }
        return Float.parseFloat(percentMatch);
    }

    /**
     * Compacts the complex values for the three phases, Ps, PpPs, PsPs, into a
     * single Cmplx array, thereby cutting memory usage by factor 3, and likely
     * CPU by the same during stacking.
     * 
     * Note that the PsPs phase is opposite polarity, so subtract. Also this is
     * complex subtraction of unit vectors, so sub is correct. It is not phase 
     * subtraction, in which case minus would be wrong as need a 180 phase shift.
     * Lucky thing complex math takes care of this!
     */
    public void compact() {
        compactAnalyticPhase = new CmplxArray2D(analyticPs.getXLength(),
                                                analyticPs.getYLength());
        for(int i = 0; i < getNumH(); i++) {
            for(int k = 0; k < getNumK(); k++) {
                Cmplx ps = Cmplx.mul(analyticPs.get(i, k).zeroOrUnitVector(),
                                     getWeightPs());
                Cmplx ppps = Cmplx.mul(analyticPpPs.get(i, k)
                        .zeroOrUnitVector(), getWeightPpPs());
                Cmplx psps = Cmplx.mul(analyticPsPs.get(i, k)
                        .zeroOrUnitVector(), getWeightPsPs());
                Cmplx val = Cmplx.sub(Cmplx.add(ps, ppps), psps);
                compactAnalyticPhase.set(i, k, val);
            }
        }
        analyticPs = null;
        analyticPpPs = null;
        analyticPsPs = null;
    }

    protected void calculate() throws FissuresException {
        Element shiftElement = (Element)recFunc.getAuxillaryData("recFunc.alignShift");
        QuantityImpl shift = XMLQuantity.getQuantity(shiftElement);
        shift = shift.convertTo(UnitImpl.SECOND);
        DataGetter dataGetter = new DataGetter();
        recFunc.retrieveData(dataGetter);
        LinkedList data = dataGetter.getData();
        LocalSeismogramImpl seis;
        if(data.size() != 1) {
            throw new IllegalArgumentException("Receiver function DSS must have exactly one seismogram");
        } else {
            seis = (LocalSeismogramImpl)data.get(0);
        }
        calculate(seis, shift);
    }

    public static float[][] createArray(int numH, int numK) {
        return new float[numH][numK];
    }

    void calculate(LocalSeismogramImpl seis, QuantityImpl shift)
            throws FissuresException {
        // set up analytic signal
        Hilbert hilbert = new Hilbert();
        Cmplx[] analytic = hilbert.analyticSignal(seis);
        float[] imagFloats = new float[analytic.length];
        for(int i = 0; i < analytic.length; i++) {
            imagFloats[i] = (float)analytic[i].imag();
        }
        LocalSeismogramImpl imag = new LocalSeismogramImpl(seis, imagFloats);
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        if(Float.isNaN(etaP)) {
            throw new RuntimeException("Warning: Eta P is NaN alpha=" + alpha
                    + "  p=" + p);
        } else if(etaP <= 0) {
            throw new RuntimeException("EtaP should never be negative: " + etaP
                    + "  a=" + a + "  p=" + p);
        }
        for(int kIndex = 0; kIndex < numK; kIndex++) {
            float beta = a / (minK + kIndex * stepK);
            float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
            if(Float.isNaN(etaS)) {
                throw new RuntimeException("Warning: Eta S is NaN " + kIndex
                        + "  beta=" + beta + "  p=" + p);
            } else if(etaS <= 0) {
                throw new RuntimeException("EtaS should never be negative: "
                        + etaS + "   etaP=" + etaP + "  beta=" + beta);
            }
            for(int hIndex = 0; hIndex < numH; hIndex++) {
                float h = (float)(minH.getValue(UnitImpl.KILOMETER) + hIndex
                        * stepH.getValue(UnitImpl.KILOMETER));
                TimeInterval timePs = new TimeInterval(h * (etaS - etaP)
                        + shift.value, UnitImpl.SECOND);
                TimeInterval timePpPs = new TimeInterval(h * (etaS + etaP)
                        + shift.value, UnitImpl.SECOND);
                TimeInterval timePsPs = new TimeInterval(h * (2 * etaS)
                        + shift.value, UnitImpl.SECOND);
                calcForStack(seis,
                             imag,
                             timePs,
                             timePpPs,
                             timePsPs,
                             hIndex,
                             kIndex);
            }
        }
        calcPhaseStack();
    }

    private void calcForStack(LocalSeismogramImpl seis,
                              LocalSeismogramImpl imag,
                              TimeInterval timePs,
                              TimeInterval timePpPs,
                              TimeInterval timePsPs,
                              int hIndex,
                              int kIndex) throws FissuresException {
        analyticPs.setReal(hIndex, kIndex, getAmp(seis, timePs));
        analyticPs.setImag(hIndex, kIndex, getAmp(imag, timePs));
        analyticPpPs.setReal(hIndex, kIndex, getAmp(seis, timePpPs));
        analyticPpPs.setImag(hIndex, kIndex, getAmp(imag, timePpPs));
        analyticPsPs.setReal(hIndex, kIndex, getAmp(seis, timePsPs));
        analyticPsPs.setImag(hIndex, kIndex, getAmp(imag, timePsPs));
    }

    public static HKStack create(CachedResult cachedResult,
                                 float weightPs,
                                 float weightPpPs,
                                 float weightPsPs) throws TauModelException,
            FissuresException {
        return create(cachedResult,
                      weightPs,
                      weightPpPs,
                      weightPsPs,
                      crust2.getStationResult(cachedResult.channels[0].my_site.my_station));
    }

    public static HKStack create(CachedResult cachedResult,
                                 float weightPs,
                                 float weightPpPs,
                                 float weightPsPs,
                                 StationResult staResult)
            throws TauModelException, FissuresException {
        String[] pPhases = {"P"};
        TauPUtil tauPTime = TauPUtil.getTauPUtil(modelName);
        Arrival[] arrivals = tauPTime.calcTravelTimes(cachedResult.channels[0].my_site.my_station,
                                                      cachedResult.prefOrigin,
                                                      pPhases);
        // convert radian per sec ray param into km per sec
        float kmRayParam = (float)(arrivals[0].getRayParam() / tauPTime.getTauModel()
                .getRadiusOfEarth());
        HKStack stack = new HKStack(staResult.getVp(),
                                    kmRayParam,
                                    cachedResult.config.gwidth,
                                    cachedResult.radialMatch,
                                    getDefaultMinH(),
                                    new QuantityImpl(.25f, UnitImpl.KILOMETER),
                                    240,
                                    1.6f,
                                    .0025f,
                                    200,
                                    weightPs,
                                    weightPpPs,
                                    weightPsPs,
                                    (LocalSeismogramImpl)cachedResult.radial,
                                    cachedResult.channels[2], // chan arraay
                                    // is a, b, z
                                    RecFunc.getDefaultShift());
        return stack;
    }

    public TimeInterval getTimePs() {
        return getTimePs(p, alpha, getMaxValueK(), getMaxValueH());
    }

    public static TimeInterval getTimePs(float p,
                                         QuantityImpl alpha,
                                         float k,
                                         QuantityImpl h) {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        float beta = a / k;
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        return new TimeInterval(h.getValue(UnitImpl.KILOMETER) * (etaS - etaP),
                                UnitImpl.SECOND);
    }

    public TimeInterval getTimePpPs() {
        return getTimePpPs(p, alpha, getMaxValueK(), getMaxValueH());
    }

    public static TimeInterval getTimePpPs(float p,
                                           QuantityImpl alpha,
                                           float k,
                                           QuantityImpl h) {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        float beta = a / k;
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        return new TimeInterval(h.getValue(UnitImpl.KILOMETER) * (etaS + etaP),
                                UnitImpl.SECOND);
    }

    public TimeInterval getTimePsPs() {
        return getTimePsPs(p, alpha, getMaxValueK(), getMaxValueH());
    }

    public static TimeInterval getTimePsPs(float p,
                                           QuantityImpl alpha,
                                           float k,
                                           QuantityImpl h) {
        float a = (float)alpha.convertTo(UnitImpl.KILOMETER_PER_SECOND)
                .getValue();
        float etaP = (float)Math.sqrt(1 / (a * a) - p * p);
        float beta = a / k;
        float etaS = (float)Math.sqrt(1 / (beta * beta) - p * p);
        return new TimeInterval(h.getValue(UnitImpl.KILOMETER) * (2 * etaS),
                                UnitImpl.SECOND);
    }

    /**
     * Gets the sample corresponding to the time. Return is a float so the
     * relative position between the nearest samples can be determined.
     */
    public static float getDataIndex(LocalSeismogramImpl seis, TimeInterval time) {
        double sampOffset = time.divideBy(seis.getSampling().getPeriod())
                .getValue(UnitImpl.DIMENSONLESS);
        return (float)sampOffset;
    }

    /** gets the amp at the given time offset from the start of the seismogram. */
    float getAmp(LocalSeismogramImpl seis, TimeInterval time)
            throws FissuresException {
        float sampOffset = getDataIndex(seis, time);
        int offset = (int)Math.floor(sampOffset);
        if(sampOffset < 0 || offset > seis.getNumPoints() - 2) {
            logger.warn("time " + time
                    + " is outside of seismogram, returning 0: "
                    + seis.getBeginTime() + " - " + seis.getEndTime()
                    + " sampOffset=" + sampOffset + " npts="
                    + seis.getNumPoints());
            return 0;
        }
        float valA = seis.get_as_floats()[offset];
        float valB = seis.get_as_floats()[offset + 1];
        // linear interp
        float retVal = (float)SimplePlotUtil.linearInterp(offset,
                                                          valA,
                                                          offset + 1,
                                                          valB,
                                                          sampOffset);
        if(Float.isNaN(retVal)) {
            logger.error("Got a NaN for HKStack.getAmp() at " + time + " chan="
                    + ChannelIdUtil.toStringNoDates(seis.channel_id));
        }
        return retVal;
    }

    public ChannelId getChannelId() {
        if(recFunc != null) {
            return getRecFunc().getRequestFilter().channel_id;
        } else {
            return chan.get_id();
        }
    }

    /**
     * Returns the channel, which may be null.
     */
    public Channel getChannel() {
        return chan;
    }

    public DataSetSeismogram getRecFunc() {
        return recFunc;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public Origin getOrigin() {
        return origin;
    }

    /**
     * Optional
     */
    Origin origin = null;

    public float[][] getStack() {
        return stack;
    }

    public float getP() {
        return p;
    }

    public String formatP() {
        return vpvsFormat.format(getP());
    }

    /**
     * calculates the power (sqrt of sum of squares) over the stack for only
     * positive entries, ie negative values are set = 0.
     */
    public float getPower() {
        return getPower(0);
    }

    /**
     * calculates the power (sqrt of sum of squares) over the stack for only
     * entries > floor, ie values < floor are set = 0.
     */
    public float getPower(float floor) {
        float power = 0;
        float[][] s = getStack();
        for(int i = 0; i < s.length; i++) {
            for(int j = 0; j < s[0].length; j++) {
                if(s[i][j] > floor) {
                    power += s[i][j] * s[i][j];
                }
            }
        }
        return (float)Math.sqrt(power);
    }

    public QuantityImpl getAlpha() {
        return alpha;
    }

    public String formatAlpha() {
        return FissuresFormatter.formatQuantity(getAlpha());
    }

    public float getGaussianWidth() {
        return gwidth;
    }

    public float getPercentMatch() {
        return percentMatch;
    }

    public String formatPercentMatch() {
        return vpvsFormat.format(getPercentMatch());
    }

    public QuantityImpl getMinH() {
        return minH;
    }

    public QuantityImpl getStepH() {
        return stepH;
    }

    public int getNumH() {
        return numH;
    }

    public float getMinK() {
        return minK;
    }

    public float getStepK() {
        return stepK;
    }

    public int getNumK() {
        return numK;
    }

    public float getWeightPpPs() {
        return weightPpPs;
    }

    public String formatWeightPpPs() {
        return vpvsFormat.format(getWeightPpPs());
    }

    public float getWeightPs() {
        return weightPs;
    }

    public String formatWeightPs() {
        return vpvsFormat.format(getWeightPs());
    }

    public float getWeightPsPs() {
        return weightPsPs;
    }

    public String formatWeightPsPs() {
        return vpvsFormat.format(getWeightPsPs());
    }

    /**
     * Writes the HKStack report to a string.
     */
    public void writeReport(BufferedWriter out) throws IOException {
        out.write("p=" + p);
        out.newLine();
        out.write("alpha=" + alpha);
        out.newLine();
        int[] xyMin = getMinValueIndices();
        StackMaximum xyMax = getGlobalMaximum();
        float max = xyMax.getMaxValue();
        out.write("Max H="
                + xyMax.getHValue());
        out.write("    K=" + xyMax.getKValue());
        out.write("  max=" + max);
        out.write("alpha=" + alpha);
        out.newLine();
        out.write("percentMatch=" + percentMatch);
        out.newLine();
        out.write("minH=" + minH);
        out.newLine();
        out.write("stepH=" + stepH);
        out.newLine();
        out.write("numH=" + numH);
        out.newLine();
        out.write("minK=" + minK);
        out.newLine();
        out.write("stepK=" + stepK);
        out.newLine();
        out.write("numK=" + numK);
        out.newLine();
        out.write("stack.length=" + stack.length);
        out.newLine();
        out.write("stack[0].length=" + stack[0].length);
        out.newLine();
    }

    float calcPhaseStack(int hIndex, int kIndex) {
        return (float)(calcRegStack(hIndex, kIndex) * calcPhaseWeight(hIndex,
                                                                      kIndex));
        // return (float)(calcRegStack(hIndex, kIndex));
    }

    double calcRegStack(int hIndex, int kIndex) {
        return weightPs * analyticPs.getReal(hIndex, kIndex) + weightPpPs
                * analyticPpPs.getReal(hIndex, kIndex) - weightPsPs
                * analyticPsPs.getReal(hIndex, kIndex);
    }

    double calcPhaseWeight(int hIndex, int kIndex) {
        Cmplx ps, ppps, psps;
        double magPs = analyticPs.mag(hIndex, kIndex);
        double magPpPs = analyticPpPs.mag(hIndex, kIndex);
        double magPsPs = analyticPsPs.mag(hIndex, kIndex);
        if(magPs == 0 || magPpPs == 0 || magPsPs == 0) {
            return 0;
        }
        ps = Cmplx.div(analyticPs.get(hIndex, kIndex), magPs);
        ppps = Cmplx.div(analyticPpPs.get(hIndex, kIndex), magPpPs);
        psps = Cmplx.div(analyticPsPs.get(hIndex, kIndex), magPsPs);
        Cmplx out = Cmplx.sub(Cmplx.add(ps, ppps), psps);
        if(Double.isNaN(out.mag())) {
            System.out.println("calcPhaseWeight: NaN  " + " mag" + magPs + "\n"
                    + ps + "\n" + ppps + "\n" + psps);
        }
        return Math.pow(out.mag(), 2);
    }

    void calcPhaseStack() {
        stack = createArray(numH, numK);
        for(int i = 0; i < stack.length; i++) {
            for(int j = 0; j < stack[i].length; j++) {
                stack[i][j] = calcPhaseStack(i, j);
            }
        }
    }

    float[][] stack;

    public CmplxArray2D getAnalyticPpPs() {
        return analyticPpPs;
    }

    public CmplxArray2D getAnalyticPs() {
        return analyticPs;
    }

    public CmplxArray2D getAnalyticPsPs() {
        return analyticPsPs;
    }

    public CmplxArray2D getCompactAnalyticPhase() {
        return compactAnalyticPhase;
    }

    CmplxArray2D compactAnalyticPhase = null;

    float[][] realStack;

    CmplxArray2D analyticPs;

    CmplxArray2D analyticPpPs;

    CmplxArray2D analyticPsPs;

    float p;

    QuantityImpl alpha;

    float gwidth;

    float percentMatch;

    QuantityImpl minH;

    QuantityImpl stepH;

    int numH;

    float minK;

    float stepK;

    int numK;

    float weightPs = 1;

    float weightPpPs = 1;

    float weightPsPs = 1;

    QuantityImpl peakH;

    Float peakK;

    Float peakVal;

    private static final QuantityImpl DEFAULT_MIN_H = new QuantityImpl(10,
                                                                       UnitImpl.KILOMETER);

    private static final QuantityImpl DEFAULT_SMALLEST_H = new QuantityImpl(25,
                                                                            UnitImpl.KILOMETER);

    static String modelName = "iasp91";

    transient static Crust2 crust2 = null;

    transient static WilsonRistra wilson = null;
    static {
        try {
            crust2 = new Crust2();
        } catch(IOException e) {
            GlobalExceptionHandler.handle("Couldn't load Crust2.0", e);
        }
        try {
            wilson = new WilsonRistra();
        } catch(IOException e) {
            GlobalExceptionHandler.handle("Couldn't load Wilson RISTRA", e);
        }
    }

    public static Crust2 getCrust2() {
        return crust2;
    }

    public static WilsonRistra getWilsonRistra() {
        return wilson;
    }

    public static QuantityImpl getDefaultMinH() {
        return DEFAULT_MIN_H;
    }

    public static QuantityImpl getDefaultSmallestH() {
        return DEFAULT_SMALLEST_H;
    }

    public static QuantityImpl getBestSmallestH(Station station) {
        return getBestSmallestH(station, getDefaultSmallestH());
    }

    public static QuantityImpl getBestSmallestH(Station station,
                                                QuantityImpl smallestH) {
        Crust2Profile crust2 = HKStack.getCrust2()
                .getClosest(station.my_location.longitude,
                            station.my_location.latitude);
        QuantityImpl crust2H = crust2.getCrustThickness();
        QuantityImpl modSmallestH = smallestH;
        if(crust2H.subtract(smallestH).getValue() < 5) {
            modSmallestH = crust2H.subtract(new QuantityImpl(5,
                                                             UnitImpl.KILOMETER));
            if(modSmallestH.lessThan(HKStack.getDefaultMinH())) {
                modSmallestH = HKStack.getDefaultMinH();
            }
        }
        System.out.println("getBestSmallestH in=" + smallestH + "  c2="
                + crust2H + " out=" + modSmallestH);
        return modSmallestH;
    }

    // don't serialize the DSS
    transient DataSetSeismogram recFunc;

    Channel chan;

    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");

    private static DecimalFormat maxValueFormat = new DecimalFormat("0.0000");

    private static DecimalFormat depthFormat = new DecimalFormat("0.##");

    class DataGetter implements SeisDataChangeListener {

        LinkedList data = new LinkedList();

        LinkedList errors = new LinkedList();

        boolean finished = false;

        public boolean isFinished() {
            return finished;
        }

        public synchronized LinkedList getData() {
            while(finished == false) {
                try {
                    wait();
                } catch(InterruptedException e) {}
            }
            return data;
        }

        public synchronized void finished(SeisDataChangeEvent sdce) {
            LocalSeismogramImpl[] seis = sdce.getSeismograms();
            for(int i = 0; i < seis.length; i++) {
                data.add(seis[i]);
            }
            finished = true;
            notifyAll();
        }

        public synchronized void error(SeisDataErrorEvent sdce) {
            errors.add(sdce);
        }

        public synchronized void pushData(SeisDataChangeEvent sdce) {
            LocalSeismogramImpl[] seis = sdce.getSeismograms();
            for(int i = 0; i < seis.length; i++) {
                data.add(seis[i]);
            }
        }
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKStack.class);
}
