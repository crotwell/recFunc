package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;

import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.bag.Hilbert;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Apr 27, 2005
 */
public class AnalyticPhaseSeismogramImage extends SeismogramImage {

    /**
     *
     */
    public AnalyticPhaseSeismogramImage() throws SQLException,
            ConfigurationException, Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    public DataSetSeismogram[] getDSS(ReceiverFunctionResult stack) {
        DataSetSeismogram[] tmp = super.getDSS(stack);
        try {
            DataSetSeismogram[] out = new DataSetSeismogram[tmp.length+6];
            System.arraycopy(tmp, 0, out, 1, tmp.length);
            LocalSeismogramImpl radial = ((MemoryDataSetSeismogram)tmp[0]).getCache()[0];
            
            LocalSeismogramImpl hilbertSeis = hilbert.apply(radial);
            MemoryDataSetSeismogram radialDSS = new MemoryDataSetSeismogram(hilbertSeis, "hilbert RF");
            tmp[0].getDataSet().addDataSetSeismogram(radialDSS, emptyAudit);
            out[tmp.length+1] = radialDSS;
            
            Cmplx[] analytic = hilbert.analyticSignal(radial);
            float[] amp = doubleArrayToFloat(hilbert.envelope(analytic));
            float[] phase = doubleArrayToFloat(hilbert.phase(analytic));
            float[] freq = doubleArrayToFloat(hilbert.instantFreq(analytic));
            
            LocalSeismogramImpl hilbertAmp = new LocalSeismogramImpl(hilbertSeis, amp);
            MemoryDataSetSeismogram radialAmp = new MemoryDataSetSeismogram(hilbertAmp, "analytic amp");
            tmp[0].getDataSet().addDataSetSeismogram(radialAmp, emptyAudit);
            out[tmp.length+2] = radialAmp;

            LocalSeismogramImpl hilbertPhs = new LocalSeismogramImpl(hilbertSeis, phase);
            MemoryDataSetSeismogram radialPhs = new MemoryDataSetSeismogram(hilbertPhs, "analytic phase");
            tmp[0].getDataSet().addDataSetSeismogram(radialPhs, emptyAudit);
            out[tmp.length+3] = radialPhs;

            LocalSeismogramImpl hilbertFreq = new LocalSeismogramImpl(hilbertSeis, freq);
            MemoryDataSetSeismogram radialFreq = new MemoryDataSetSeismogram(hilbertFreq, "analytic freq");
            tmp[0].getDataSet().addDataSetSeismogram(radialFreq, emptyAudit);
            out[0] = radialFreq;

            float[] imag = new float[analytic.length];
            for(int i = 0; i < imag.length; i++) {
                imag[i] = (float)analytic[i].imag();
            }
            LocalSeismogramImpl imagSeis = new LocalSeismogramImpl(hilbertSeis, imag);
            MemoryDataSetSeismogram imagMSeis = new MemoryDataSetSeismogram(imagSeis, "imagXYZ");
            tmp[0].getDataSet().addDataSetSeismogram(imagMSeis, emptyAudit);
            out[tmp.length+4] = imagMSeis;
            
            float[] real = new float[analytic.length];
            for(int i = 0; i < real.length; i++) {
                real[i] = (float)analytic[i].real();
            }
            LocalSeismogramImpl realSeis = new LocalSeismogramImpl(hilbertSeis, real);
            MemoryDataSetSeismogram realMSeis = new MemoryDataSetSeismogram(realSeis, "realASD");
            tmp[0].getDataSet().addDataSetSeismogram(realMSeis, emptyAudit);
            out[tmp.length+5] = realMSeis;
            
            return out;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return tmp;
        }
    }
    
    float[] doubleArrayToFloat(double[] in) {
        float[] out = new float[in.length];
        for(int i = 0; i < out.length; i++) {
            out[i] = (float)in[i];
        }
        return out;
    }
    
    Hilbert hilbert = new Hilbert();
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AnalyticPhaseSeismogramImage.class);
}
