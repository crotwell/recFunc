package edu.sc.seis.receiverFunction.server;

import org.w3c.dom.Element;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheOperations;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.cache.ProxyEventAccessOperations;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.IterDecon;
import edu.sc.seis.receiverFunction.IterDeconResult;
import edu.sc.seis.receiverFunction.RecFunc;
import edu.sc.seis.receiverFunction.RecFuncProcessor;
import edu.sc.seis.sod.ChannelGroup;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorProcess;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorResult;
import edu.sc.seis.sod.status.StringTreeLeaf;


/**
 * @author crotwell
 * Created on Sep 10, 2004
 */
public class RecFuncCacheProcessor extends RecFuncProcessor implements WaveformVectorProcess  {
    
    public RecFuncCacheProcessor(Element config)  throws ConfigurationException, TauModelException {
        super(config);
        String modelName = "prem";
        taup = new TauPUtil(modelName);
        recFunc = new RecFunc(taup,
                              new IterDecon(100, true, tol, gwidth));
    }
    
    
    /**
     *
     */
    public WaveformVectorResult process(EventAccessOperations event,
                                        ChannelGroup channelGroup,
                                        RequestFilter[][] original,
                                        RequestFilter[][] available,
                                        LocalSeismogramImpl[][] seismograms,
                                        CookieJar cookieJar) throws Exception {
        
        LocalSeismogramImpl[] singleSeismograms = new LocalSeismogramImpl[3];
        for(int i = 0; i < singleSeismograms.length; i++) {
            singleSeismograms[i] = seismograms[i][0];
        }
        
        IterDeconResult[] ans = recFunc.process(event,
                                                channelGroup,
                                                singleSeismograms);
        
        Channel chan = channelGroup.getChannels()[0];
        Location staLoc = chan.my_site.my_station.my_location;
        Origin origin = event.get_preferred_origin();
        Location evtLoc = origin.my_location;
        
        Arrival[] pPhases = taup.calcTravelTimes(chan.my_site.my_station, origin, new String[] { "ttp"});
        MicroSecondDate firstP = new MicroSecondDate(origin.origin_time);
        firstP = firstP.add(new TimeInterval(pPhases[0].getTime(), UnitImpl.SECOND));
        
        TimeInterval shift = recFunc.getShift();
        MemoryDataSetSeismogram[] predictedDSS = new MemoryDataSetSeismogram[2];
        for (int i = 0; i < ans.length; i++) {
            float[] predicted = ans[i].getPredicted();
            String chanCode = (i==0)?"ITR":"ITT"; // ITR for radial
            // ITT for tangential
            predictedDSS[i] = RecFunc.saveTimeSeries(predicted,
                                                     "receiver function "+singleSeismograms[0].channel_id.station_code,
                                                     chanCode,
                                                     firstP.subtract(shift),
                                                     singleSeismograms[0],
                                                     UnitImpl.DIMENSONLESS);
        }

        IterDeconConfig config = new IterDeconConfig(gwidth, maxBumps, tol);
        cache.insert(((ProxyEventAccessOperations)event).getCorbaObject(),
                     config,
                     channelGroup.getChannels(),
                     singleSeismograms,
                     predictedDSS[0].getCache()[0], ans[0].getPercentMatch(),
                     predictedDSS[1].getCache()[0], ans[1].getPercentMatch());
        
        WaveformVectorResult result = new WaveformVectorResult(seismograms, new StringTreeLeaf(this, true));
        return result;
    }
    
    RecFuncCacheOperations cache = new RecFuncCacheImpl();
    
    float gwidth = 2.5f;
    
    float tol = .001f;
    
    int maxBumps = 100;
    
    TauPUtil taup;
    
    RecFunc recFunc;
}
