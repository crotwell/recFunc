package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.util.List;

import org.omg.CORBA.UNKNOWN;
import org.w3c.dom.Element;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.event.OriginImpl;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheOperations;
import edu.sc.seis.IfReceiverFunction.SodConfigNotFound;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.IncompatibleSeismograms;
import edu.sc.seis.fissuresUtil.bag.IterDeconResult;
import edu.sc.seis.fissuresUtil.bag.ZeroPowerException;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.display.configuration.DOMHelper;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeismogramFileTypes;
import edu.sc.seis.receiverFunction.RecFunc;
import edu.sc.seis.sod.CommonAccess;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.SodConfig;
import edu.sc.seis.sod.Threadable;
import edu.sc.seis.sod.hibernate.SodDB;
import edu.sc.seis.sod.process.waveform.AbstractSeismogramWriter;
import edu.sc.seis.sod.process.waveform.vector.IterDeconReceiverFunction;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorProcess;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorResult;
import edu.sc.seis.sod.status.StringTreeLeaf;

/**
 * @author crotwell Created on Sep 10, 2004
 */
public class RecFuncCacheProcessor extends IterDeconReceiverFunction implements WaveformVectorProcess, Threadable {

    public RecFuncCacheProcessor(Element config) throws ConfigurationException,
            TauModelException {
        super(config);
        iorFilename = DOMHelper.extractText(config, "iorfile", "../server/Ears.ior");
        File f = new File(iorFilename);
        if (! f.exists()) {
            throw new ConfigurationException("Unable to find ior file: "+iorFilename);
        }
        dns = DOMHelper.extractText(config, "dns", "edu/sc/seis");
        serverName = DOMHelper.extractText(config, "name", "Ears");
        writer = new AbstractSeismogramWriter() {
            
            @Override
            public void write(String loc, LocalSeismogramImpl seis, ChannelImpl chan, CacheEvent ev) throws Exception {
                // no op
            }
            
            @Override
            public SeismogramFileTypes getFileType() {
                return null;
            }
        };
    }


    /**
     * @throws NoPreferredOrigin probably should never happen
     * @throws TauModelException probably should never happen
     * 
     */
    public WaveformVectorResult accept(CacheEvent event,
                                        ChannelGroup channelGroup,
                                        RequestFilter[][] original,
                                        RequestFilter[][] available,
                                        LocalSeismogramImpl[][] seismograms,
                                        CookieJar cookieJar) throws NoPreferredOrigin, TauModelException {
        try {
            if(sodconfig_id == -1) {
                try {
                    SodDB sodDb = SodDB.getSingleton();
                    SodConfig config = sodDb.getCurrentConfig();
                    int testId = 1;
                    try {
                        while(true) {
                            if(getCache().getSodConfig(testId).equals(config.getConfig())) {
                                sodconfig_id = testId;
                                break;
                            }
                            testId++;
                        }
                    } catch(SodConfigNotFound ee) {
                        // must have gone past the end without find our config
                        sodconfig_id = getCache().insertSodConfig(config.getConfig());
                    }
                } catch(Throwable e) {
                    // oh well
                    GlobalExceptionHandler.handle("Unable to get configuration from database, using id=-2",
                                                  e);
                    sodconfig_id = -2;
                }
            }
            Channel chan = channelGroup.getChannels()[0];
            Origin origin = event.get_preferred_origin();
            IterDeconConfig config = new IterDeconConfig(gwidth, maxBumps, tol);
            ChannelId[] chanIds = new ChannelId[channelGroup.getChannels().length];
            for(int i = 0; i < chanIds.length; i++) {
                chanIds[i] = channelGroup.getChannels()[i].get_id();
            }
            if(!overwrite && getCache().isCached(origin, chanIds, config)) {
                return new WaveformVectorResult(seismograms,
                                                new StringTreeLeaf(this,
                                                                   true,
                                                                   "Already calculated"));
            }
            LocalSeismogramImpl[] singleSeismograms = new LocalSeismogramImpl[3];
            for(int i = 0; i < singleSeismograms.length; i++) {
                singleSeismograms[i] = seismograms[i][0];
            }
            IterDeconResult[] ans = super.process(event,
                                                    channelGroup,
                                                    singleSeismograms);
            String[] phaseName = pWave ? new String[] {"ttp"}
                    : new String[] {"tts"};
            List<Arrival> pPhases = taup.calcTravelTimes(chan.getSite()
                    .getStation(), origin, phaseName);
            MicroSecondDate firstP = new MicroSecondDate(origin.getOriginTime());
            firstP = firstP.add(new TimeInterval(pPhases.get(0).getTime(),
                                                 UnitImpl.SECOND));
            TimeInterval shift = getShift();
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
                cookieJar.put("recFunc_percentMatch_" + chanCode, ""
                        + ans[i].getPercentMatch());
            }
            //make origin have depth in km
            OriginImpl kmOrigin = new OriginImpl(origin.get_id(),
                                                 origin.getCatalog(),
                                                 origin.getContributor(),
                                                 origin.getOriginTime(),
                                                 new Location(origin.getLocation().latitude,
                                                              origin.getLocation().longitude,
                                                              ((QuantityImpl)origin.getLocation().elevation).convertTo(UnitImpl.KILOMETER),
                                                              ((QuantityImpl)origin.getLocation().depth).convertTo(UnitImpl.KILOMETER),
                                                              origin.getLocation().type),
                                                 origin.getMagnitudes(),
                                                 origin.getParmIds());
            while(true) {
                try {
                    getCache().insert(kmOrigin,
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
                } catch(UNKNOWN e) {
                    System.err.println(ClockUtil.now()
                            + " Corba UNKNOWN while sending result to database, will not retry."
                            + e);
                    GlobalExceptionHandler.handle("Corba UNKNOWN while sending result to database, will not retry.",
                                                  e);
                    WaveformVectorResult result = new WaveformVectorResult(seismograms,
                                                                           new StringTreeLeaf(this,
                                                                                              false,
                                                                                              "UNKNOWN on cache.insert",
                                                                                              e));
                    return result;
                } catch(Throwable e) {
                    System.err.println(ClockUtil.now()
                            + " Problem while sending result to database, will retry in 1 minute..."
                            + e);
                    GlobalExceptionHandler.handle("Problem while sending result to database, will retry in 1 minute...",
                                                  e);
                    try {
                        Thread.sleep(60000);
                    } catch(InterruptedException interrupt) {
                        // ignore
                    }
                }
            }
            return new WaveformVectorResult(seismograms,
                                            new StringTreeLeaf(this,
                                                               true));
        } catch(IncompatibleSeismograms e) {
            return new WaveformVectorResult(seismograms,
                                            new StringTreeLeaf(this,
                                                               false,
                                                               "Seismograms not compatble",
                                                               e));
        } catch(FissuresException e) {
            return new WaveformVectorResult(seismograms,
                                            new StringTreeLeaf(this,
                                                               false,
                                                               "Data problem",
                                                               e));
        } catch(ZeroPowerException e) {
            return new WaveformVectorResult(seismograms,
                                            new StringTreeLeaf(this,
                                                               false,
                                                               "Zero power in numerator or demoninator",
                                                               e));
        }
    }


    protected RecFuncCacheOperations getCache() {
        if (cache == null) {
            if (iorFilename != null && new File(iorFilename).exists()) {
                cache = new IORFileRecFuncCache(iorFilename, CommonAccess.getORB());
            } else {
                logger.warn("unable to load Ears from IOR file: "+iorFilename);
                cache = new NSRecFuncCache(dns,
                                           serverName,
                                           CommonAccess.getNameService());
            }
        }
        return cache;
    }

    String iorFilename;
    
    String dns = "edu/sc/seis";

    String serverName = "Ears";

    protected int sodconfig_id = -1;

    boolean overwrite = false;

    protected RecFuncCacheOperations cache;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncCacheProcessor.class);


    public boolean isThreadSafe() {
        return true;
    }
}