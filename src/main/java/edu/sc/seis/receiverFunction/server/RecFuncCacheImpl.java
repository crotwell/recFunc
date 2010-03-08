package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.UNKNOWN;

import edu.iris.Fissures.Orientation;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.event.EventAttrImpl;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.RecFuncCachePOA;
import edu.sc.seis.IfReceiverFunction.RecFuncNotFound;
import edu.sc.seis.IfReceiverFunction.SodConfigNotFound;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.AbstractHibernateDB;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.fissuresUtil.hibernate.EventDB;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.xml.SeismogramFileTypes;
import edu.sc.seis.fissuresUtil.xml.URLDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.QualityControl;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.SodConfig;
import edu.sc.seis.sod.hibernate.SodDB;

/**
 * @author crotwell Created on Sep 9, 2004
 */
public class RecFuncCacheImpl extends RecFuncCachePOA {

    public RecFuncCacheImpl(String dataloc) throws IOException,
            SQLException, ConfigurationException, TauModelException, Exception {
        RecFuncDB.setDataLoc(dataloc);
        qualityControl = new QualityControl();
    }

    public RecFuncCacheImpl(String dataloc,
                            Properties confProps) throws IOException,
            SQLException, ConfigurationException, TauModelException, Exception {
        this(dataloc);
        synchronized(HibernateUtil.class) {
            HibernateUtil.setUpFromConnMgr(confProps, HibernateUtil.DEFAULT_EHCACHE_CONFIG);
            SodDB.configHibernate(HibernateUtil.getConfiguration());
            RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
        }
        AbstractHibernateDB.deploySchema();
    }

    public IterDeconConfig[] getCachedConfigs(Origin prefOrigin,
                                              ChannelId[] channel) {
        try {
            ArrayList configs = new ArrayList();
            List<CacheEvent> similar = EventDB.getSingleton()
                    .getSimilarEvents(new CacheEvent(new EventAttrImpl("dummy"),
                                                     prefOrigin),
                                      timeTol,
                                      positionTol);
            ChannelGroup chanGroup = getChannelGroup(channel, new MicroSecondDate(prefOrigin.getOriginTime()));
            for(CacheEvent cacheEvent : similar) {
                IterDeconConfig[] tmpConfigs = RecFuncDB.getSingleton()
                        .getResults(cacheEvent, chanGroup);
                for(int j = 0; j < tmpConfigs.length; j++) {
                    configs.add(tmpConfigs[j]);
                }
            }
            // read only, so rollback
            RecFuncDB.rollback();
            return (IterDeconConfig[])configs.toArray(new IterDeconConfig[0]);
        } catch(Throwable e) {
            RecFuncDB.rollback();
            GlobalExceptionHandler.handle(e);
            throw new org.omg.CORBA.UNKNOWN(e.toString(),
                                            14,
                                            CompletionStatus.COMPLETED_MAYBE);
        }
    }

    protected ChannelGroup getChannelGroup(ChannelId[] channel, MicroSecondDate begin) throws NotFound {
        NetworkDB ndb = NetworkDB.getSingleton();
        ChannelImpl chanA = ndb.getChannel(channel[0].network_id.network_code,
                                           channel[0].station_code,
                                           channel[0].site_code,
                                           channel[0].channel_code,
                                           begin);
        ChannelImpl chanB = ndb.getChannel(channel[1].network_id.network_code,
                                           channel[1].station_code,
                                           channel[1].site_code,
                                           channel[1].channel_code,
                                           begin);
        ChannelImpl chanC = ndb.getChannel(channel[2].network_id.network_code,
                                           channel[2].station_code,
                                           channel[2].site_code,
                                           channel[2].channel_code,
                                           begin);
        ChannelGroup cg = ndb.getChannelGroup(chanA, chanB, chanC);
        if (cg == null) {
            throw new NotFound();
        }
        return cg;
    }
    
    protected ReceiverFunctionResult getResult(Origin prefOrigin,
                                               ChannelId[] channel,
                                               IterDeconConfig config) {
        try {
            List<CacheEvent> similar = EventDB.getSingleton()
                    .getSimilarEvents(new CacheEvent(new EventAttrImpl("dummy"),
                                                     prefOrigin),
                                      timeTol,
                                      positionTol);
            ChannelGroup chanGroup = getChannelGroup(channel,
                                        new MicroSecondDate(prefOrigin.getOriginTime()));
            for(CacheEvent cacheEvent : similar) {
                ReceiverFunctionResult result = RecFuncDB.getSingleton()
                        .getRecFuncResult(cacheEvent, chanGroup, config.gwidth);
                if(result != null) {
                    return result;
                }
            }
        } catch(NotFound e) {}
        return null;
    }

    public CachedResult get(Origin prefOrigin,
                            ChannelId[] channel,
                            IterDeconConfig config) throws RecFuncNotFound {
        try {
            ReceiverFunctionResult result = getResult(prefOrigin,
                                                      channel,
                                                      config);
            if(result != null) {
                CachedResult out = new CachedResult(result.getEvent()
                                                            .getPreferred(),
                                                    result.getEvent()
                                                            .get_attributes(),
                                                    new IterDeconConfig(result.getGwidth(),
                                                                        result.getMaxBumps(),
                                                                        result.getTol()),
                                                    result.getChannelGroup()
                                                            .getChannels(),
                                                    new LocalSeismogramImpl[] {result.getOriginal1(),
                                                                               result.getOriginal2(),
                                                                               result.getOriginal3()},
                                                    result.getRadial(),
                                                    result.getRadialMatch(),
                                                    result.getRadialBump(),
                                                    result.getTransverse(),
                                                    result.getTransverseMatch(),
                                                    result.getTransverseBump(),
                                                    result.getSodConfig()
                                                            .getDbid(),
                                                    new MicroSecondDate(result.getInsertTime()).getFissuresTime());
                RecFuncDB.commit();
                return out;
            }
            RecFuncDB.rollback();
            throw new RecFuncNotFound();
        } catch(Throwable e) {
            RecFuncDB.rollback();
            GlobalExceptionHandler.handle(e);
            throw new org.omg.CORBA.UNKNOWN(e.toString(),
                                            13,
                                            CompletionStatus.COMPLETED_MAYBE);
        }
    }

    /**
     * 
     */
    public void insert(Origin prefOrigin,
                       EventAttr eventAttr,
                       IterDeconConfig config,
                       Channel[] channels,
                       LocalSeismogram[] original,
                       LocalSeismogram radial,
                       float radialMatch,
                       int radialBump,
                       LocalSeismogram transverse,
                       float transverseMatch,
                       int transverseBump,
                       int sodConfig_id) {
        try {
            CacheEvent event = null;
            try {
                List<CacheEvent> similar = EventDB.getSingleton()
                        .getSimilarEvents(new CacheEvent(eventAttr, prefOrigin),
                                          timeTol,
                                          positionTol);
                if(similar.size() > 0) {
                    // already an origin in the database, keep using
                    // existing origin
                    event = similar.get(0);
                }
            } catch(NotFound e) {
                // event not yet in db, so proceed as new
            }
            if(event == null) {
                event = new CacheEvent(eventAttr, prefOrigin);
                EventDB.getSingleton().put(event);
            }
            ChannelImpl[] channelImpls = ChannelImpl.implize(channels);
            for (int i = 0; i < channelImpls.length; i++) {
                channelImpls[i] = CleanNetwork.cleanInsertChannel(channelImpls[i]);
            }
            NetworkDB ndb = NetworkDB.getSingleton();
            ChannelGroup cg = NetworkDB.getSingleton().getChannelGroup(channelImpls[0], channelImpls[1], channelImpls[2]);
            if (cg == null) {
                cg = new ChannelGroup(channelImpls);
                NetworkDB.getSingleton().put(cg);
            }
            SodConfig sodConfig = SodDB.getSingleton().getConfig(sodConfig_id);
            File stationDir = RecFuncDB.getDir(event, channels[0], config.gwidth);
            File[] seisFile = new File[original.length];
            for(int i = 0; i < original.length; i++) {
                seisFile[i] = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)original[i],
                                                          stationDir,
                                                          channels[i],
                                                          event,
                                                          SeismogramFileTypes.SAC);
            }
            int zChanNum = 0;
            for(int i = 0; i < channels.length; i++) {
                if(channels[i].get_code().endsWith("Z")) {
                    zChanNum = i;
                    break;
                }
            }
            Channel zeroChannel = channels[zChanNum];
            ChannelId radialChannelId = new ChannelId(zeroChannel.get_id().network_id,
                                                      zeroChannel.get_id().station_code,
                                                      zeroChannel.get_id().site_code,
                                                      "ITR",
                                                      zeroChannel.get_id().begin_time);
            Channel radialChannel = new ChannelImpl(radialChannelId,
                                                    "receiver function fake channel for "
                                                            + ChannelIdUtil.toStringNoDates(zeroChannel.get_id()),
                                                    new Orientation(0, 0),
                                                    zeroChannel.getSamplingInfo(),
                                                    zeroChannel.getEffectiveTime(),
                                                    zeroChannel.getSite());
            File radialFile = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)radial,
                                                          stationDir,
                                                          radialChannel,
                                                          event,
                                                          SeismogramFileTypes.SAC);
            ChannelId transverseChannelId = new ChannelId(zeroChannel.get_id().network_id,
                                                          zeroChannel.get_id().station_code,
                                                          zeroChannel.get_id().site_code,
                                                          "ITT",
                                                          zeroChannel.get_id().begin_time);
            Channel transverseChannel = new ChannelImpl(transverseChannelId,
                                                        "receiver function fake channel for "
                                                                + ChannelIdUtil.toStringNoDates(zeroChannel.get_id()),
                                                        new Orientation(0, 0),
                                                        zeroChannel.getSamplingInfo(),
                                                        zeroChannel.getEffectiveTime(),
                                                        zeroChannel.getSite());
            File transverseFile = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)transverse,
                                                              stationDir,
                                                              transverseChannel,
                                                              event,
                                                              SeismogramFileTypes.SAC);
            ReceiverFunctionResult result = new ReceiverFunctionResult(event,
                                                                       cg,
                                                                       seisFile[0].getName(),
                                                                       seisFile[1].getName(),
                                                                       seisFile[2].getName(),
                                                                       radialFile.getName(),
                                                                       transverseFile.getName(),
                                                                       radialMatch,
                                                                       radialBump,
                                                                       transverseMatch,
                                                                       transverseBump,
                                                                       config.gwidth,
                                                                       config.maxBumps,
                                                                       config.tol,
                                                                       sodConfig);
            try {
                qualityControl.check(result);
                if(result.getQc().isKeep()) {
                    float weightPs = 1 / 3f;
                    float weightPpPs = 1 / 3f;
                    float weightPsPs = 1 - weightPs - weightPpPs;
                    result.setHKstack(HKStack.create(result,
                                                     weightPs,
                                                     weightPpPs,
                                                     weightPsPs));
                }
                RecFuncDB.getSingleton().put(result);
                RecFuncDB.commit();
                logger.info("Insert RF: "+event+"  "+StationIdUtil.toStringNoDates(cg.getStation()));
            } catch(Throwable e) {
                RecFuncDB.rollback();
                GlobalExceptionHandler.handle("Problem with "
                        + ChannelIdUtil.toString(channels[0].get_id())
                        + " for origin time="
                        + prefOrigin.getOriginTime().date_time, e);
                throw new UNKNOWN(e.toString(),
                                  12,
                                  CompletionStatus.COMPLETED_MAYBE);
            }
        } catch(UNKNOWN e) {
            // pass it on from previous catch
            throw e;
        } catch(Throwable t) {
            RecFuncDB.rollback();
            GlobalExceptionHandler.handle(t);
            throw new UNKNOWN(t.getMessage());
        } 
    }

    /**
     * 
     */
    public boolean isCached(Origin prefOrigin,
                            ChannelId[] channel,
                            IterDeconConfig config) {
        try {
            List<CacheEvent> similar = EventDB.getSingleton()
            .getSimilarEvents(new CacheEvent(new EventAttrImpl("dummy"),
                                             prefOrigin),
                              timeTol,
                              positionTol);
            for(CacheEvent cacheEvent : similar) {
                if (RecFuncDB.getSingleton().isResultInDB(cacheEvent, 
                                                          channel[0].network_id.network_code,
                                                          channel[0].station_code, 
                                                          config.gwidth)) {
                    RecFuncDB.rollback();
                    return true;
                }
            }
            RecFuncDB.rollback();
            return false;
        } catch(Throwable e) {
            RecFuncDB.rollback();
            GlobalExceptionHandler.handle(e);
            throw new UNKNOWN(e.getMessage());
        }
    }

    public int insertSodConfig(String config) {
        try {
            SodConfig sc = new SodConfig(config);
            SodDB.getSingleton().putConfig(sc);
            SodDB.commit();
            return sc.getDbid();
        } catch(Throwable t) {
            SodDB.rollback();
            GlobalExceptionHandler.handle(t);
            throw new UNKNOWN(t.getMessage(),
                              10,
                              CompletionStatus.COMPLETED_MAYBE);
        }
    }

    public String getSodConfig(int sodConfig_id) throws SodConfigNotFound {
        try {
            SodConfig sc = SodDB.getSingleton().getConfig(sodConfig_id);
            if (sc == null) {
                SodDB.rollback();
                throw new SodConfigNotFound();
            }
            String s = sc.getConfig();
            SodDB.commit();
            return s;
        } catch(SodConfigNotFound e) {
            throw e;
        } catch(Throwable t) {
            GlobalExceptionHandler.handle(t);
            throw new UNKNOWN(t.getMessage(),
                              11,
                              CompletionStatus.COMPLETED_MAYBE);
        } finally {
            SodDB.rollback();
        }
    }
    
    private TimeInterval timeTol = new TimeInterval(10, UnitImpl.SECOND);

    private QuantityImpl positionTol = new QuantityImpl(.5, UnitImpl.DEGREE);

    private QualityControl qualityControl;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncCacheImpl.class);
}
