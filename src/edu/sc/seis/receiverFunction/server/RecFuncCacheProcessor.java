package edu.sc.seis.receiverFunction.server;

import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.UserException;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.w3c.dom.Element;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheHelper;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheOperations;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.IncompatibleSeismograms;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.cache.ProxyEventAccessOperations;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.IterDecon;
import edu.sc.seis.receiverFunction.IterDeconResult;
import edu.sc.seis.receiverFunction.RecFunc;
import edu.sc.seis.receiverFunction.RecFuncProcessor;
import edu.sc.seis.sod.ChannelGroup;
import edu.sc.seis.sod.CommonAccess;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.Start;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorProcess;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorResult;
import edu.sc.seis.sod.status.StringTreeLeaf;

/**
 * @author crotwell Created on Sep 10, 2004
 */
public class RecFuncCacheProcessor extends RecFuncProcessor implements
        WaveformVectorProcess {

    public RecFuncCacheProcessor(Element config) throws ConfigurationException,
            TauModelException {
        super(config);
        String modelName = "prem";
        taup = new TauPUtil(modelName);
        recFunc = new RecFunc(taup, new IterDecon(maxBumps, true, tol, gwidth));
        FissuresNamingService fisName = CommonAccess.getCommonAccess()
                .getFissuresNamingService();
        cache = new NSRecFuncCache(dns, serverName, fisName);
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
        try {
            if (sodconfig_id == -1) {
                System.out.println("Bad socConfig_id");
            }
            Channel chan = channelGroup.getChannels()[0];
            Location staLoc = chan.my_site.my_station.my_location;
            Origin origin = event.get_preferred_origin();
            Location evtLoc = origin.my_location;
            IterDeconConfig config = new IterDeconConfig(gwidth, maxBumps, tol);
            ChannelId[] chanIds = new ChannelId[channelGroup.getChannels().length];
            for(int i = 0; i < chanIds.length; i++) {
                chanIds[i] = channelGroup.getChannels()[i].get_id();
            }
            if(!overwrite && cache.isCached(origin, chanIds, config)) { return new WaveformVectorResult(seismograms,
                                                                                                        new StringTreeLeaf(this,
                                                                                                                           true,
                                                                                                                           "Already calculated")); }
            LocalSeismogramImpl[] singleSeismograms = new LocalSeismogramImpl[3];
            for(int i = 0; i < singleSeismograms.length; i++) {
                singleSeismograms[i] = seismograms[i][0];
            }
            IterDeconResult[] ans = recFunc.process(event,
                                                    channelGroup,
                                                    singleSeismograms);
            Arrival[] pPhases = taup.calcTravelTimes(chan.my_site.my_station,
                                                     origin,
                                                     new String[] {"ttp"});
            MicroSecondDate firstP = new MicroSecondDate(origin.origin_time);
            firstP = firstP.add(new TimeInterval(pPhases[0].getTime(),
                                                 UnitImpl.SECOND));
            TimeInterval shift = recFunc.getShift();
            MemoryDataSetSeismogram[] predictedDSS = new MemoryDataSetSeismogram[2];
            for(int i = 0; i < ans.length; i++) {
                float[] predicted = ans[i].getPredicted();
                // ITR for radial
                // ITT for tangential
                String chanCode = (i == 0) ? "ITR" : "ITT";
                predictedDSS[i] = RecFunc.saveTimeSeries(predicted,
                                                         "receiver function "
                                                                 + singleSeismograms[0].channel_id.station_code,
                                                         chanCode,
                                                         firstP.subtract(shift),
                                                         singleSeismograms[0],
                                                         UnitImpl.DIMENSONLESS);
                cookieJar.put("recFunc_percentMatch_"+chanCode, ""+HKStack.getPercentMatch(predictedDSS[i]));
            }
            while(true) {
                try {
                    cache.insert(origin,
                                 event.get_attributes(),
                                 config,
                                 channelGroup.getChannels(),
                                 singleSeismograms,
                                 predictedDSS[0].getCache()[0],
                                 ans[0].getPercentMatch(),
                                 ans[0].getBump(),
                                 predictedDSS[1].getCache()[0],
                                 ans[1].getPercentMatch(),
                                 ans[1].getBump(),
                                 sodconfig_id);
                    break;
                } catch(Throwable e) {
                    GlobalExceptionHandler.handle("Problem while sending result to database, will retry in 1 minute...",
                                                  e);
                    try {
                        Thread.sleep(60000);
                    } catch(InterruptedException interrupt) {
                        // ignore
                    }
                }
            }
            WaveformVectorResult result = new WaveformVectorResult(seismograms,
                                                                   new StringTreeLeaf(this,
                                                                                      true));
            return result;
        } catch(IncompatibleSeismograms e) {
            WaveformVectorResult result = new WaveformVectorResult(seismograms,
                                                                   new StringTreeLeaf(this,
                                                                                      false,
                                                                                      "Seismograms not compatble",
                                                                                      e));
            return result;
        }
    }

    String dns = "edu/sc/seis";

    String interfaceName = "IfReceiverFunction";

    String serverName = "Ears";

    int sodconfig_id = -1;
    
    boolean overwrite = false;

    NSRecFuncCache cache;

    TauPUtil taup;

    RecFunc recFunc;
}