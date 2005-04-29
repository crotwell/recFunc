package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.bag.Hilbert;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
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
    

    public DataSetSeismogram[] getDSS(CachedResult stack) {
        DataSetSeismogram[] tmp = super.getDSS(stack);
        try {
            DataSetSeismogram[] out = new DataSetSeismogram[tmp.length+4];
            System.arraycopy(tmp, 0, out, 1, tmp.length);
            LocalSeismogramImpl radial = ((MemoryDataSetSeismogram)tmp[0]).getCache()[0];
            
            LocalSeismogramImpl hilbertSeis = hilbert.apply(radial);
            MemoryDataSetSeismogram radialDSS = new MemoryDataSetSeismogram(hilbertSeis, "hilbert RF");
            tmp[0].getDataSet().addDataSetSeismogram(radialDSS, emptyAudit);
            out[tmp.length+1] = radialDSS;
            
            Cmplx[] analytic = hilbert.analyticSignal(radial);
            double[] amp = hilbert.envelope(analytic);
            double[] phase = hilbert.unwrapPhase(analytic);
            double[] freq = hilbert.instantFreq(analytic);
            
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
            
            return out;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return tmp;
        }
    }
    
    Hilbert hilbert = new Hilbert();
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AnalyticPhaseSeismogramImage.class);
}
