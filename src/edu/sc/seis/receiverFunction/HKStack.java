/**
 * HKStack.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.fissuresUtil.xml.XMLQuantity;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import org.w3c.dom.Element;



public class HKStack  {

    protected HKStack(float alpha,
                      float p,
                      float minH,
                      float stepH,
                      int numH,
                      float minK,
                      float stepK,
                      int numK) {
        this.alpha = alpha;
        this.p = p;
        this.minH = minH;
        this.stepH = stepH;
        this.numH = numH;
        this.minK = minK;
        this.stepK = stepK;
        this.numK = numK;
    }

    public HKStack(float alpha,
                   float p,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   DataSetSeismogram recFunc) {
        this(alpha,p ,minH ,stepH ,numH ,minK ,stepK ,numK );
        this.recFunc = recFunc;
        calculate();
    }

    public HKStack(float alpha,
                   float p,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float[][] stack) {
        this(alpha,p ,minH ,stepH ,numH ,minK ,stepK ,numK );
        this.recFunc = null;
        this.stack = stack;
    }

    public HKStack(float alpha,
                   float p,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float[][] stack,
                   DataSetSeismogram recFunc) {
        this(alpha,p ,minH ,stepH ,numH ,minK ,stepK ,numK, stack );
        this.recFunc = recFunc;
    }

    public HKStack(float alpha,
                   float p,
                   float minH,
                   float stepH,
                   int numH,
                   float minK,
                   float stepK,
                   int numK,
                   float[][] stack,
                   ChannelId chanId) {
        this(alpha,p ,minH ,stepH ,numH ,minK ,stepK ,numK, stack );
        this.chanId = chanId;
    }

    public BufferedImage createStackImage() {
        float[][] stackOut = getStack();
        int dataH = 2*stackOut.length;
        int dataW = 2*stackOut[0].length;
        int fullWidth = dataW+40;
        int fullHeight = dataH+140;
        BufferedImage bufImage = new BufferedImage(fullWidth,
                                                   fullHeight,
                                                   BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bufImage.createGraphics();
        g.setColor(Color.darkGray);
        g.fillRect(0, 0, bufImage.getWidth(), bufImage.getHeight());
        g.translate(0, 5);
        FontMetrics fm = g.getFontMetrics();

        String title = ChannelIdUtil.toStringNoDates(getChannelId());
        g.setColor(Color.white);
        g.drawString(title, (fullWidth-fm.stringWidth(title))/2, fm.getHeight());

        g.translate(5, fm.getHeight()+fm.getDescent());

        float min = stackOut[0][0];
        int maxIndexX = 0;
        int maxIndexY = 0;
        int minIndexX = 0;
        int minIndexY = 0;
        float max = min;

        for (int j = 0; j < stackOut.length; j++) {
            for (int k = 0; k < stackOut[j].length; k++) {
                if (stackOut[j][k] < min) {
                    min = stackOut[j][k];
                    minIndexX = k;
                    minIndexY = j;
                }
                if (stackOut[j][k] > max) {
                    max = stackOut[j][k];
                    maxIndexX = k;
                    maxIndexY = j;
                }
            }
        }

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
        g.translate(0, dataH);

        g.setColor(Color.white);
        g.drawString("Min H="+(getMinH()+minIndexY*getStepH()), 0, fm.getHeight());
        g.drawString("    K="+(getMinK()+minIndexX*getStepK()), 0, 2*fm.getHeight());
        g.translate(0, 2*fm.getHeight());
        g.drawString("Max H="+(getMinH()+maxIndexY*getStepH()), 0, fm.getHeight());
        g.drawString("    K="+(getMinK()+maxIndexX*getStepK()), 0, 2*fm.getHeight());
        g.translate(0, 2*fm.getHeight());

        int minColor = makeImageable(min, max, min);
        g.setColor(new Color(minColor, minColor, minColor));
        g.fillRect(0, 0, 15, 15);
        g.setColor(Color.white);
        g.drawString("Min="+min, 0, 15+fm.getHeight());

        int maxColor = makeImageable(min, max, max);
        g.setColor(new Color(maxColor, maxColor, maxColor));
        g.fillRect(dataW-20, 0, 15, 15);
        g.setColor(Color.white);
        String maxString = "Max="+max;
        int stringWidth = fm.stringWidth(maxString);
        g.drawString(maxString, dataW-5-stringWidth, 15+fm.getHeight());

        g.dispose();
        return bufImage;
    }


    int makeImageable(float min, float max, float val) {
        float absMax = Math.max(Math.abs(min), Math.abs(max));
        return (int)SimplePlotUtil.linearInterp(-1*absMax, 0, absMax, 255, val);
    }

    protected void calculate() {
        stack = new float[numH][numK];
        float etaP = (float) Math.sqrt(1/(alpha*alpha)-p*p);
        if (Float.isNaN(etaP)) {
            System.out.println("Warning: Eta P is NaN alpha="+alpha+"  p="+p);
        }
        Element shiftElement =
            (Element)recFunc.getAuxillaryData("recFunc.alignShift");
        QuantityImpl shift = (QuantityImpl)XMLQuantity.getQuantity(shiftElement);
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
        for (int i = 0; i < numK; i++) {
            float beta = alpha/(minK + i*stepK);
            float etaS = (float) Math.sqrt(1/(beta*beta)-p*p);
            if (Float.isNaN(etaS)) {
                System.out.println("Warning: Eta S is NaN "+i+"  beta="+beta+"  p="+p);
            }
            for (int j = 0; j < numH; j++) {
                float h = minH + j*stepH;
                double timePs = h * (etaS - etaP) + shift.value;
                double timePpPs = h * (etaS + etaP) + shift.value;
                double timePsPs = h * (2 * etaS) + shift.value;
                stack[j][i] += getAmp(seis, timePs)
                    + getAmp(seis, timePpPs)
                    - getAmp(seis, timePsPs);
            }
        }
    }


    /** gets the amp at the given time offset from the start of the seismogram. */
    float getAmp(LocalSeismogramImpl seis, double time) {
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
            return chanId;
        }
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

    /** Writes the HKStack to the DataOutputStream. The DataSetSeismogram
     *  is NOT written as it is assumed that this will be saved separately.
     */
    public void write(DataOutputStream out) throws IOException {
        out.writeFloat(p);
        out.writeFloat(alpha);
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
        float minH = in.readFloat();
        float stepH = in.readFloat();
        int numH = in.readInt();
        float minK = in.readFloat();
        float stepK = in.readFloat();
        int numK = in.readInt();
        int iDim = in.readInt();
        int jDim = in.readInt();
        float[][] stack = new float[iDim][jDim];
        HKStack out = new HKStack(alpha, p, minH, stepH, numH, minK, stepK, numK, stack);
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
    float minH;
    float stepH;
    int numH;
    float minK;
    float stepK;
    int numK;
    DataSetSeismogram recFunc;
    ChannelId chanId;

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


