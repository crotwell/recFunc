package edu.sc.seis.receiverFunction.server;

import org.apache.log4j.Logger;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.EventChannelFinder;
import edu.iris.Fissures.IfEvent.EventDC;
import edu.iris.Fissures.IfEvent.EventFinder;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.RecFuncCache;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheHelper;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheOperations;
import edu.sc.seis.IfReceiverFunction.RecFuncNotFound;
import edu.sc.seis.IfReceiverFunction.SodConfigNotFound;
import edu.sc.seis.fissuresUtil.cache.NSEventDC;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;


/**
 * @author crotwell
 * Created on Sep 17, 2004
 */
public class NSRecFuncCache implements RecFuncCacheOperations {
    
    public NSRecFuncCache(String serverDNS,
                          String serverName,
                          FissuresNamingService fissuresNamingService) {
        this.serverDNS = serverDNS;
        this.serverName = serverName;
        this.namingService = fissuresNamingService;
    } // NSEventDC constructor
    
    public String getServerDNS() {
        return serverDNS;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public synchronized void reset() {
        recFuncCache = null;
    }
    
    public synchronized RecFuncCache getCorbaObject() {
        if ( recFuncCache == null) {
            try {
                try {
                    recFuncCache = RecFuncCacheHelper.narrow(namingService.resolve(serverDNS, interfaceName, serverName));
                } catch (Throwable t) {
                    namingService.reset();
                    recFuncCache = RecFuncCacheHelper.narrow(namingService.resolve(serverDNS, interfaceName, serverName));
                }
            } catch (org.omg.CosNaming.NamingContextPackage.NotFound e) {
                repackageException(e);
            } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed e) {
                repackageException(e);
            } catch (org.omg.CosNaming.NamingContextPackage.InvalidName e) {
                repackageException(e);
            } // end of try-catch
        } // end of if ()
        return (RecFuncCache)recFuncCache;
    }
    
    protected void repackageException(org.omg.CORBA.UserException e) {
        org.omg.CORBA.TRANSIENT t =
            new org.omg.CORBA.TRANSIENT("Unable to resolve "+serverName+" "+interfaceName+" "+serverDNS+" "+namingService.getNameServiceCorbaLoc()+" "+e.toString(),
                                        0,
                                        org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        t.initCause(e);
        throw t;
    }
    
    /**
     *
     */
    public boolean isCached(Origin prefOrigin,
                            ChannelId[] channel,
                            IterDeconConfig config) {
        try {
            return getCorbaObject().isCached(prefOrigin, channel, config);
        } catch (Throwable e) {
            // retry in case regetting from name service helps
            logger.warn("Exception in isCached(), regetting from nameservice to try again.", e);
            reset();
            return getCorbaObject().isCached(prefOrigin, channel, config);
        } // end of try-catch
    }
    
    /**
     *
     */
    public IterDeconConfig[] getCachedConfigs(Origin prefOrigin,
                                            ChannelId[] channel) {
        try {
            return getCorbaObject().getCachedConfigs(prefOrigin, channel);
        } catch (Throwable e) {
            // retry in case regetting from name service helps
            logger.warn("Exception in getCachedConfigs(), regetting from nameservice to try again.", e);
            reset();
            return getCorbaObject().getCachedConfigs(prefOrigin, channel);
        } // end of try-catch
    }
    
    /**
     * @throws RecFuncNotFound
     *
     */
    public CachedResult get(Origin prefOrigin,
                            ChannelId[] channel,
                            IterDeconConfig config) throws RecFuncNotFound {
        try {
            return getCorbaObject().get(prefOrigin, channel, config);
        } catch (RecFuncNotFound e) {
            throw e;
        } catch (Throwable e) {
            // retry in case regetting from name service helps
            logger.warn("Exception in get(), regetting from nameservice to try again.", e);
            reset();
            return getCorbaObject().get(prefOrigin, channel, config);
        } // end of try-catch
    }
    

    public int insertSodConfig(String config) {
        try {
            return getCorbaObject().insertSodConfig(config);
        } catch (Throwable e) {
            // retry in case regetting from name service helps
            logger.warn("Exception in insertSodConfig(), regetting from nameservice to try again.", e);
            reset();
            return getCorbaObject().insertSodConfig(config);
        } // end of try-catch
    }

    public String getSodConfig(int sodConfig_id) throws SodConfigNotFound {
        try {
            return getCorbaObject().getSodConfig(sodConfig_id);
        } catch (SodConfigNotFound e) {
            throw e;
        } catch (Throwable e) {
            // retry in case regetting from name service helps
            logger.warn("Exception in getSodConfig(), regetting from nameservice to try again.", e);
            reset();
            return getCorbaObject().getSodConfig(sodConfig_id);
        } // end of try-catch
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
                       LocalSeismogram tansverse,
                       float transverseMatch,
                       int transverseBump,
                       int sodConfig_id) {
        try {
            getCorbaObject().insert(prefOrigin,
                                    eventAttr,
                                    config,
                                    channels,
                                    original,
                                    radial,
                                    radialMatch,
                                    radialBump,
                                    tansverse,
                                    transverseMatch,
                                    transverseBump,
                                    sodConfig_id);
        } catch (Throwable e) {
            // retry in case regetting from name service helps
            logger.warn("Exception in insert(), regetting from nameservice to try again.", e);
            reset();
            getCorbaObject().insert(prefOrigin,
                                    eventAttr,
                                    config,
                                    channels,
                                    original,
                                    radial,
                                    radialMatch,
                                    radialBump,
                                    tansverse,
                                    transverseMatch,
                                    transverseBump,
                                    sodConfig_id);
        } // end of try-catch
    }
    
    protected RecFuncCacheOperations recFuncCache = null;
    
    protected String serverDNS;
    
    protected String serverName;

    public static final String interfaceName = "IfReceiverFunction";
    
    protected FissuresNamingService namingService;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(NSRecFuncCache.class);

    
}
