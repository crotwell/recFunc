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
import java.util.LinkedList;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.w3c.dom.Element;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.model.UnitRangeImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.display.BorderedDisplay;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.fissuresUtil.display.borders.Border;
import edu.sc.seis.fissuresUtil.display.borders.UnitRangeBorder;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.fissuresUtil.xml.XMLQuantity;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.sod.SodUtil;



public class HKStack implements Serializable {

    protected HKStack(float alpha,
                      float p,
                      float percentMatch,
                      float minH,
                      float stepH,
                      int numH,
                      float minK,
                      float stepK,
                      int numK) {
        this.alpha = alpha;
        this.p = p;
        this.percentMatch = percentMatch;
        this.minH = minH;
        this.stepH = stepH;
        this.numH = numH;
        this.minK = minK;
        this.stepK = stepK;
        this.numK = numK;
    }

    public HKStack(float alpha,
                   float p,
                   float percentMatch,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   DataSetSeismogram recFunc)  throws FissuresException {
        this(alpha,p, percentMatch, minH ,stepH ,numH ,minK ,stepK ,numK );
        this.recFunc = recFunc;
        this.chan = recFunc.getDataSet().getChannel(recFunc.getRequestFilter().channel_id);
        calculate();
    }

    public HKStack(float alpha,
                   float p,
                   float percentMatch,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   LocalSeismogramImpl recFuncSeis,
                   Channel chan,
                   TimeInterval shift)  throws FissuresException {
        this(alpha,p, percentMatch, minH ,stepH ,numH ,minK ,stepK ,numK );
        this.recFunc = new MemoryDataSetSeismogram(recFuncSeis);
        this.chan = chan;
        calculate(recFuncSeis, shift);
    }
    
    public HKStack(float alpha,
                   float p,
                   float percentMatch,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float[][] stack) {
        this(alpha,p, percentMatch ,minH ,stepH ,numH ,minK ,stepK ,numK );
        this.recFunc = null;
        this.stack = stack;
    }

    public HKStack(float alpha,
                   float p,
                   float percentMatch,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float[][] stack,
                   DataSetSeismogram recFunc) {
        this(alpha,p, percentMatch ,minH ,stepH ,numH ,minK ,stepK ,numK, stack );
        this.recFunc = recFunc;
        this.chan = recFunc.getDataSet().getChannel(recFunc.getRequestFilter().channel_id);
    }

    public HKStack(float alpha,
                   float p,
                   float percentMatch,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float[][] stack,
                   Channel chan) {
        this(alpha,p, percentMatch ,minH ,stepH ,numH ,minK ,stepK ,numK, stack );
        this.chan = chan;
    }

    /** returns the x and y indices for the max value in the stack. The
     *  min x value is in index 0 and the y in index 1. The max x value is in
     *  2 and the y in 3.
     */
    public int[] getMinValueIndices() {
        float[][] stackOut = getStack();
        float min = stackOut[0][0];
        int minIndexX = 0;
        int minIndexY = 0;
        for (int j = 0; j < stackOut.length; j++) {
            for (int k = 0; k < stackOut[j].length; k++) {
                if (stackOut[j][k] < min) {
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

    /** returns the x and y indices for the max value in the stack. The
     *  min x value is in index 0 and the y in index 1. The max x value is in
     *  2 and the y in 3.
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
        for (int j = startHIndex; j < stackOut.length; j++) {
            for (int k = 0; k < stackOut[j].length; k++) {
                if (stackOut[j][k] > max) {
                    max = stackOut[j][k];
                    maxIndexX = j;
                    maxIndexY = k;
                }
            }
        }
        int[] xy = new int[2];
        xy[0] = maxIndexX;
        xy[1] = maxIndexY;
        return xy;
    }
    

    public JComponent getStackComponent() {
        return getStackComponent(minH);
    }
    
    public JComponent getStackComponent(float smallestH) {
        int startHIndex = (int)Math.round((smallestH-minH)/stepH);
        HKStackImage stackImage = new HKStackImage(this, startHIndex);
        if (crust2 != null) {
            Crust2Profile profile = crust2.getClosest(chan.my_site.my_station.my_location.longitude,
                                                      chan.my_site.my_station.my_location.latitude);
            int depthIndex = (int)Math.round((profile.getCrustThickness()-minH)/stepH);
            double vpvs = profile.getPWaveAvgVelocity() / profile.getSWaveAvgVelocity();
            int vpvsIndex = (int)Math.round((vpvs-minK)/stepK);
            System.out.println("Crust2 "+StationIdUtil.toString(chan.my_site.my_station.get_id())+" depth="+
                              profile.getLayer(7).topDepth+" "+depthIndex+"  VpVs="+vpvs+" "+vpvsIndex);
            stackImage.addMarker(vpvsIndex, depthIndex);
        }
        BorderedDisplay bd = new BorderedDisplay(stackImage);
        UnitRangeImpl depthRange = new UnitRangeImpl(getMinH()+startHIndex*getStepH(),
                                                     getMinH()+getNumH()*getStepH(),
                                                     UnitImpl.KILOMETER);
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
                                                                           getMinK()+getNumK()*getStepK(),
                                                                           UnitImpl.divide(UnitImpl.KILOMETER_PER_SECOND,UnitImpl.KILOMETER_PER_SECOND, "km/s/km/s")));
        bd.add(kTopBorder, bd.TOP_CENTER);
        UnitRangeBorder kBottomBorder = new UnitRangeBorder(Border.BOTTOM,
                                                            Border.ASCENDING,
                                                            "Vp/Vs",
                                                            new UnitRangeImpl(getMinK(),
                                                                              getMinK()+getNumK()*getStepK(),
                                                                              UnitImpl.divide(UnitImpl.KILOMETER_PER_SECOND,UnitImpl.KILOMETER_PER_SECOND, "km/s/km/s")));
        bd.add(kBottomBorder, bd.BOTTOM_CENTER);
        return bd;
    }

    public BufferedImage createStackImage() {
        JComponent comp =  getStackComponent();
        JFrame frame = null;
        Graphics2D g = null;
        BufferedImage bufImage = null;
        try {
            if(comp.getRootPane() == null){
                comp.addNotify();
            }
            Dimension size = comp.getSize();

            int fullWidth = size.width+40;
            int fullHeight = size.height+140;
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
            g.drawString(title, (fullWidth-fm.stringWidth(title))/2, fm.getHeight());

            g.translate(5, fm.getHeight()+fm.getDescent());

            comp.print(g);

            g.translate(0, size.height);


            int[] xyMin = getMinValueIndices();
            int[] xyMax = getMaxValueIndices();

            float min = stack[xyMin[0]][xyMin[1]];
            float max = stack[xyMax[0]][xyMax[1]];

            g.setColor(Color.white);
            g.drawString("% match="+percentMatch, 0, fm.getHeight());
            g.drawString("    ", 0, 2*fm.getHeight());
            g.translate(0, 2*fm.getHeight());
            g.drawString("Max H="+(getMinH()+xyMax[0]*getStepH()), 0, fm.getHeight());
            g.drawString("    K="+(getMinK()+xyMax[1]*getStepK()), 0, 2*fm.getHeight());
            g.translate(0, 2*fm.getHeight());

            int minColor = HKStackImage.makeImageable(0, max, 0);
            g.setColor(new Color(minColor, minColor, minColor));
            g.fillRect(0, 0, 15, 15);
            g.setColor(Color.white);
            g.drawString("Min=0", 0, 15+fm.getHeight());

            int maxColor = HKStackImage.makeImageable(min, max, max);
            g.setColor(new Color(maxColor, maxColor, maxColor));
            g.fillRect(size.width-20, 0, 15, 15);
            g.setColor(Color.white);
            String maxString = "Max="+max;
            int stringWidth = fm.stringWidth(maxString);
            g.drawString(maxString, size.width-5-stringWidth, 15+fm.getHeight());

        } finally {
            if (g != null) {
                g.dispose();
            }
            if (frame != null) {
                frame.dispose();
            }
        }
        return bufImage;
    }

    public static float getPercentMatch(DataSetSeismogram recFunc) {
        String percentMatch = "-9999";
        if (recFunc != null) {
            Element e = (Element)recFunc.getAuxillaryData("recFunc.percentMatch");
            percentMatch = SodUtil.getNestedText(e);
        }
        return Float.parseFloat(percentMatch);
    }


    protected void calculate()  throws FissuresException {
        Element shiftElement =
            (Element)recFunc.getAuxillaryData("recFunc.alignShift");
        QuantityImpl shift = XMLQuantity.getQuantity(shiftElement);
        shift = shift.convertTo(UnitImpl.SECOND);
        DataGetter dataGetter = new DataGetter();
        recFunc.retrieveData(dataGetter);
        LinkedList data = dataGetter.getData();
        LocalSeismogramImpl seis;
        if (data.size() != 1) {
            throw new IllegalArgumentException("Receiver function DSS must have exactly one seismogram");
        } else {
            seis = (LocalSeismogramImpl)data.get(0);
        }
        calculate(seis, shift);
    }
    
    public static float[][] createArray(int numH, int numK) {
        return new float[numH][numK];
    }
    
    void calculate(LocalSeismogramImpl seis, QuantityImpl shift) throws FissuresException  {
        stack = createArray(numH, numK);
        float etaP = (float) Math.sqrt(1/(alpha*alpha)-p*p);
        if (Float.isNaN(etaP)) {
            System.out.println("Warning: Eta P is NaN alpha="+alpha+"  p="+p);
        }
        for (int kIndex = 0; kIndex < numK; kIndex++) {
            float beta = alpha/(minK + kIndex*stepK);
            float etaS = (float) Math.sqrt(1/(beta*beta)-p*p);
            if (Float.isNaN(etaS)) {
                System.out.println("Warning: Eta S is NaN "+kIndex+"  beta="+beta+"  p="+p);
            }
            for (int hIndex = 0; hIndex < numH; hIndex++) {
                float h = minH + hIndex*stepH;
                double timePs = h * (etaS - etaP) + shift.value;
                double timePpPs = h * (etaS + etaP) + shift.value;
                double timePsPs = h * (2 * etaS) + shift.value;
                
                stack[hIndex][kIndex] += weightPs * getAmp(seis, timePs)
                    + weightPpPs * getAmp(seis, timePpPs)
                    - weightPsPs * getAmp(seis, timePsPs);
            }
        }
    }


    /** gets the amp at the given time offset from the start of the seismogram. */
    float getAmp(LocalSeismogramImpl seis, double time)  throws FissuresException {
        double sampOffset = time/seis.getSampling().getPeriod().convertTo(UnitImpl.SECOND).value;
        if (sampOffset < 0 || sampOffset > seis.getNumPoints()-2) {
            throw new IllegalArgumentException("time "+time+" is outside of seismogram");
        }
        int offset = (int)Math.floor(sampOffset);

        float valA = seis.get_as_floats()[offset];
        float valB = seis.get_as_floats()[offset+1];
        // linear interp
        float retVal = (float)SimplePlotUtil.linearInterp(offset, valA, offset+1, valB, sampOffset);
        if (Float.isNaN(retVal)) {
            System.out.println("Got a NaN for getAmp at "+time);
        }
        return retVal;
    }

    public ChannelId getChannelId() {
        if (recFunc != null) {
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

    public float[][] getStack() {
        return stack;
    }

    public float getP() {
        return p;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getPercentMatch() {
        return percentMatch;
    }

    public float getMinH() {
        return minH;
    }

    public float getStepH() {
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

    /** Writes the HKStack report to a string.
     */
    public void writeReport(BufferedWriter out) throws IOException {
        out.write("p="+p);out.newLine();
        out.write("alpha="+alpha);out.newLine();int[] xyMin = getMinValueIndices();

        int[] xyMax = getMaxValueIndices();
        float max = stack[xyMax[0]][xyMax[1]];
        out.write("Max H="+(getMinH()+xyMax[0]*getStepH()));
        out.write("    K="+(getMinK()+xyMax[1]*getStepK()));
        out.write("  max="+max);
        out.write("alpha="+alpha);out.newLine();
        out.write("percentMatch="+percentMatch);out.newLine();
        out.write("minH="+minH);out.newLine();
        out.write("stepH="+stepH);out.newLine();
        out.write("numH="+numH);out.newLine();
        out.write("minK="+minK);out.newLine();
        out.write("stepK="+stepK);out.newLine();
        out.write("numK="+numK);out.newLine();
        out.write("stack.length="+stack.length);out.newLine();
        out.write("stack[0].length="+stack[0].length);out.newLine();

    }
    /** Writes the HKStack to the DataOutputStream. The DataSetSeismogram
     *  is NOT written as it is assumed that this will be saved separately.
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeFloat(p);
        out.writeFloat(alpha);
        out.writeFloat(percentMatch);
        out.writeFloat(minH);
        out.writeFloat(stepH);
        out.writeInt(numH);
        out.writeFloat(minK);
        out.writeFloat(stepK);
        out.writeInt(numK);
        out.writeInt(stack.length);
        out.writeInt(stack[0].length);
        for (int i = 0; i < stack.length; i++) {
            for (int j = 0; j < stack[0].length; j++) {
                out.writeFloat(stack[i][j]);
            }
        }
    }

    /** Reads the HKStack from the DataInputStream. The DataSetSeismogram
     *  is NOT read as it is assumed that this will be saved separatedly.
     */
    public static HKStack read(DataInputStream in, DataSetSeismogram recFunc) throws IOException {
        HKStack hks = read(in);
        hks.recFunc = recFunc;
        return hks;
    }

    /** Reads the HKStack from the DataInputStream. The DataSetSeismogram
     *  is NOT read as it is assumed that this will be saved separatedly.
     */
    public static HKStack read(DataInputStream in) throws IOException {
        float p = in.readFloat();
        float alpha = in.readFloat();
        float percentMatch = in.readFloat();
        float minH = in.readFloat();
        float stepH = in.readFloat();
        int numH = in.readInt();
        float minK = in.readFloat();
        float stepK = in.readFloat();
        int numK = in.readInt();
        int iDim = in.readInt();
        int jDim = in.readInt();
        float[][] stack = new float[iDim][jDim];
        HKStack out = new HKStack(alpha, p, percentMatch, minH, stepH, numH, minK, stepK, numK, stack);
        for (int i = 0; i < stack.length; i++) {
            for (int j = 0; j < stack[0].length; j++) {
                stack[i][j] = in.readFloat();
            }
        }
        return out;
    }

    float[][] stack;
    float p;
    float alpha;
    float percentMatch;
    float minH;
    float stepH;
    int numH;
    float minK;
    float stepK;
    int numK;

    float weightPs = 1;
    float weightPpPs = 1;
    float weightPsPs = 1;
    
    transient static Crust2 crust2 = null;
    static {
        try {
            crust2 = new Crust2();
        } catch (IOException e) {
            GlobalExceptionHandler.handle("Couldn't load Crust2.0", e);
        }
    }
    
    public static Crust2 getCrust2() {
        return crust2;
    }

    // don't serialize the DSS
    transient DataSetSeismogram recFunc;

    Channel chan;

    class DataGetter implements SeisDataChangeListener {

        LinkedList data = new LinkedList();

        LinkedList errors = new LinkedList();

        boolean finished = false;

        public boolean isFinished() {
            return finished;
        }

        public synchronized LinkedList getData() {
            while (finished == false) {
                try {
                    wait();
                } catch (InterruptedException e) { }
            }
            return data;
        }

        public synchronized void finished(SeisDataChangeEvent sdce) {
            LocalSeismogramImpl[] seis = sdce.getSeismograms();
            for (int i = 0; i < seis.length; i++) {
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
            for (int i = 0; i < seis.length; i++) {
                data.add(seis[i]);
            }
        }
    }
}


