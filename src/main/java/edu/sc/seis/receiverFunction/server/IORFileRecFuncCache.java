package edu.sc.seis.receiverFunction.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.omg.CORBA.ORB;

import edu.iris.Fissures.IfEvent.EventAttr;
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


/**
 * @author crotwell
 * Created on Sep 17, 2004
 */
public class IORFileRecFuncCache implements RecFuncCacheOperations {
    
    public IORFileRecFuncCache(String iorFilename, ORB orb) {
        this.iorFilename = iorFilename;
        this.orb = orb;
    }
    
    public String getIORFilename() {
        return iorFilename;
    }
    
    
    public synchronized void reset() {
        if(recFuncCache != null){
            ((org.omg.CORBA.Object)recFuncCache)._release();
        }
        recFuncCache = null;
    }
    
    public synchronized RecFuncCache getCorbaObject() {
        try {
            if ( recFuncCache == null) {
                File f = new File(getIORFilename());
                BufferedReader buf = new BufferedReader(new FileReader(f));
                String ior = buf.readLine();

                recFuncCache = RecFuncCacheHelper.narrow(orb.string_to_object(ior));
            } // end of if ()
            return (RecFuncCache)recFuncCache;
        } catch(IOException e) {
            repackageException(e);
            // never gets here
            return null;
        }
    }

    
    protected void repackageException(Exception e) {
        org.omg.CORBA.TRANSIENT t =
            new org.omg.CORBA.TRANSIENT("Unable to load from "+iorFilename+" "+interfaceName+" ",
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
            try {
                Thread.sleep(100);
            } catch(InterruptedException ee) {
                // oh well
            }
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
            try {
                Thread.sleep(100);
            } catch(InterruptedException ee) {
                // oh well
            }
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
            try {
                Thread.sleep(100);
            } catch(InterruptedException ee) {
                // oh well
            }
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
            try {
                Thread.sleep(100);
            } catch(InterruptedException ee) {
                // oh well
            }
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
            try {
                Thread.sleep(100);
            } catch(InterruptedException ee) {
                // oh well
            }
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
            try {
                Thread.sleep(100);
            } catch(InterruptedException ee) {
                // oh well
            }
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
    
    protected String iorFilename;

    protected ORB orb;
    
    public static final String interfaceName = "IfReceiverFunction";
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IORFileRecFuncCache.class);

    
}
