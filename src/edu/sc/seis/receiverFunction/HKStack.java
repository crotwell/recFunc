/**
 * HKStack.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.fissuresUtil.xml.XMLQuantity;
import java.util.LinkedList;
import org.w3c.dom.Element;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;



public class HKStack  {

    public HKStack(float alpha,
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

    public float[][] calculate(DataSetSeismogram recFunc) {
        float[][] ans = new float[numH][numK];
        float etaP = (float) Math.sqrt(1/(alpha*alpha)-p*p);
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
            for (int j = 0; j < numH; j++) {
                float h = minH + j*stepH;
                double timePs = h * (etaS - etaP) + shift.value;
                double timePpPs = h * (etaS + etaP) + shift.value;
                double timePsPs = h * (2 * etaS) + shift.value;
                ans[i][j] += getAmp(seis, timePs)
                    + getAmp(seis, timePpPs)
                    - getAmp(seis, timePsPs);
            }
        }
        return ans;
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
        return (float)SimplePlotUtil.linearInterp(offset, valA, offset+1, valB, sampOffset);
    }

    float p;
    float alpha;
    float minH;
    float stepH;
    int numH;
    float minK;
    float stepK;
    int numK;

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

