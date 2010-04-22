package edu.sc.seis.receiverFunction.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.IncompatibleSeismograms;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import edu.sc.seis.sod.SodConfig;
import edu.sc.seis.fissuresUtil.sac.SacToFissures;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKAlpha;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.IterDecon;
import edu.sc.seis.receiverFunction.IterDeconResult;
import edu.sc.seis.receiverFunction.RecFunc;
import edu.sc.seis.receiverFunction.RecFuncException;
import edu.sc.seis.receiverFunction.RecFuncProcessor;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;

/**
 * @author crotwell Created on Apr 29, 2005
 */
public class SyntheticFactory {

    public static LocalSeismogramImpl[] getSeismograms() throws IOException,
            FissuresException {
        LocalSeismogramImpl[] out = new LocalSeismogramImpl[3];
        for(int i = 0; i < out.length; i++) {
            out[i] = SacToFissures.getSeismogram(getSac(filebase + suffix[i]));
        }
        return out;
    }

    public static ChannelImpl[] getChannels() throws IOException {
        ChannelImpl[] out = new ChannelImpl[3];
        for(int i = 0; i < out.length; i++) {
            out[i] = SacToFissures.getChannel(getSac(filebase + suffix[i]));
        }
        return out;
    }

    public static CacheEvent getEvent() throws IOException {
        return SacToFissures.getEvent(getSac(filebase + suffix[0]));
    }

    public static TauPUtil getTauP() throws TauModelException {
        String modelName = "prem";
        TauPUtil taup = TauPUtil.getTauPUtil(modelName);
        return taup;
    }
    
    public static Arrival getFirstP() throws TauModelException, IOException {
        List<Arrival> pPhases = getTauP().calcTravelTimes(getChannels()[0].getSite().getStation(),
                                                      getEvent().getOrigin(),
                                                 new String[] {"ttp"});
        return pPhases.get(0);
    }
    
    public static TimeInterval getShift() {
        return shift;
    }
    
    public static ReceiverFunctionResult getReceiverFunctionResult() throws NoPreferredOrigin,
            FissuresException, IncompatibleSeismograms, TauModelException,
            RecFuncException, IOException {
        LocalSeismogramImpl[] seis = getSeismograms();
        CacheEvent event = getEvent();
        if(event == null) { throw new NoPreferredOrigin(); }
        ChannelImpl[] channels = getChannels();
        Channel chan = channels[0];
        Location staLoc = chan.getSite().getStation().getLocation();
        TauPUtil taup = getTauP();
        IterDecon decon = new IterDecon(RecFuncProcessor.DEFAULT_MAXBUMPS,
                                        true,
                                        RecFuncProcessor.DEFAULT_TOL,
                                        RecFuncProcessor.DEFAULT_GWIDTH);
        RecFunc recFunc = new RecFunc(taup, decon, true);
        float[] zdata = IterDecon.makePowerTwo(seis[0].get_as_floats());
        float[][] rotated = new float[2][];
        rotated[0] = IterDecon.makePowerTwo(seis[2].get_as_floats());
        rotated[0][0] += 0.0001;
        rotated[1] = IterDecon.makePowerTwo(seis[1].get_as_floats());
        SamplingImpl samp = SamplingImpl.createSamplingImpl(seis[0].sampling_info);
        double period = samp.getPeriod().convertTo(UnitImpl.SECOND).getValue();
        IterDeconResult ansRadial = recFunc.processComponent(rotated[1],
                                                             zdata,
                                                             (float)period,
                                                             staLoc,
                                                             event.getOrigin());
        IterDeconResult ansTangential = recFunc.processComponent(rotated[0],
                                                                 zdata,
                                                                 (float)period,
                                                                 staLoc,
                                                                 event.getOrigin());
        IterDeconResult[] ans = new IterDeconResult[2];
        ans[0] = ansRadial;
        ans[1] = ansTangential;
        Arrival pPhase = getFirstP();
        MicroSecondDate firstP = new MicroSecondDate(event.getOrigin().getOriginTime());
        firstP = firstP.add(new TimeInterval(pPhase.getTime(),
                                             UnitImpl.SECOND));
        shift = recFunc.getShift();
        MemoryDataSetSeismogram[] predictedDSS = new MemoryDataSetSeismogram[2];
        for(int i = 0; i < ans.length; i++) {
            float[] predicted = ans[i].getPredicted();
            // ITR for radial
            // ITT for tangential
            String chanCode = (i == 0) ? "ITR" : "ITT";
            predictedDSS[i] = RecFunc.saveTimeSeries(predicted,
                                                     "receiver function "
                                                             + seis[0].channel_id.station_code,
                                                     chanCode,
                                                     firstP.subtract(shift),
                                                     seis[0],
                                                     UnitImpl.DIMENSONLESS);
        }
        return new ReceiverFunctionResult(event,
                                          new ChannelGroup(channels),
                                seis[0], seis[1], seis[2],
                                predictedDSS[0].getCache()[0],
                                predictedDSS[1].getCache()[0],
                                ans[0].getPercentMatch(),
                                ans[0].getBump(),
                                ans[1].getPercentMatch(),
                                ans[1].getBump(),
                                RecFuncProcessor.DEFAULT_GWIDTH,
                                RecFuncProcessor.DEFAULT_MAXBUMPS,
                                RecFuncProcessor.DEFAULT_TOL,
                                new SodConfig("fake"));
    }

    public static HKAlpha getHKAlpha() {
        return new HKAlpha(new QuantityImpl(30, UnitImpl.KILOMETER),
                                 1.75f,
                                 new QuantityImpl(6.3,
                                                  UnitImpl.KILOMETER_PER_SECOND));
    }

    public static HKStack getHKStack() throws FissuresException,
            NoPreferredOrigin, TauModelException, IncompatibleSeismograms,
            RecFuncException, IOException {
        HKAlpha staResult = getHKAlpha();
        return HKStack.create(getReceiverFunctionResult(),
                              1 / 3f,
                              1 / 3f,
                              1 / 3f,
                              staResult.getVp());
    }

    public static SacTimeSeries getSac(String name) throws IOException {
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(SyntheticFactory.class.getClassLoader()
                    .getResourceAsStream(sacFilePrefix + name)));
            SacTimeSeries sac = new SacTimeSeries();
            sac.read(dis);
            dis.close();
            return sac;
        } catch(IOException e) {
            logger.error("load resource: " + sacFilePrefix + name);
            throw e;
        }
    }
    
    static TimeInterval shift = RecFunc.getDefaultShift();
    
    static String sacFilePrefix = "edu/sc/seis/receiverFunction/server/synthetic/";

    static String filebase = "test_sp.";

    static String[] suffix = new String[] {"z", "r", "t"};

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SyntheticFactory.class);
}