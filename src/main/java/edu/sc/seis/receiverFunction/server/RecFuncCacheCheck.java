package edu.sc.seis.receiverFunction.server;

import java.io.File;

import org.jacorb.orb.ParsedIOR;
import org.jacorb.orb.iiop.IIOPAddress;
import org.jacorb.orb.iiop.IIOPProfile;
import org.omg.CORBA.SystemException;
import org.w3c.dom.Element;

import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheOperations;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.display.configuration.DOMHelper;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.RecFuncException;
import edu.sc.seis.sod.CommonAccess;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.process.waveform.vector.IterDeconReceiverFunction;
import edu.sc.seis.sod.status.StringTree;
import edu.sc.seis.sod.status.StringTreeLeaf;
import edu.sc.seis.sod.subsetter.eventChannel.vector.EventVectorSubsetter;

/**
 * @author crotwell Created on Sep 16, 2004
 */
public class RecFuncCacheCheck implements EventVectorSubsetter {

    public RecFuncCacheCheck(Element config) throws ConfigurationException {
        IterDeconReceiverFunction idrf;
        try {
            idrf = new IterDeconReceiverFunction(config);
            deconConfig = new IterDeconConfig(idrf.getGwidth(), idrf.getMaxBumps(), idrf.getTol());
            iorFilename = DOMHelper.extractText(config, "iorfile", "../server/Ears.ior");
            dns = DOMHelper.extractText(config, "dns", "edu/sc/seis");
            serverName = DOMHelper.extractText(config, "name", "Ears");
        } catch(TauModelException e) {
            throw new ConfigurationException("Should not happen, problem starting taup", e);
        }
    }

    /**
     * returns true if the receiver functions are already cached.
     */
    public StringTree accept(CacheEvent event, ChannelGroup channel, CookieJar cookieJar) throws Exception {
        ChannelId[] chanId = new ChannelId[channel.getChannels().length];
        for (int i = 0; i < chanId.length; i++) {
            chanId[i] = channel.getChannels()[i].get_id();
        }
        return new StringTreeLeaf(this, getCache().isCached(event.get_preferred_origin(), chanId, deconConfig));
    }
    

    
    protected RecFuncCacheOperations getCache() {
        if (cache == null) {
            if (iorFilename != null && new File(iorFilename).exists()) {
                cache = new IORFileRecFuncCache(iorFilename, CommonAccess.getORB());
            } else {
                cache = new NSRecFuncCache(dns,
                                           serverName,
                                           CommonAccess.getNameService());
            }
        }
        return cache;
    }

   /* protected NSRecFuncCache getNSCache() throws RecFuncException {
        if (cache == null) {
            try {
                cache = new NSRecFuncCache(dns, serverName, CommonAccess.getNameService());
                cache.getCorbaObject()._is_a("test");
                logger.debug("Connection to rf cacheServer ok");
            } catch(SystemException e) {
                String ior = CommonAccess.getORB().object_to_string(cache.getCorbaObject());
                ParsedIOR parsed = new ParsedIOR((org.jacorb.orb.ORB)Initializer.getORB(), ior);
                IIOPProfile profile = (IIOPProfile)parsed.getProfiles().get(0);
                IIOPAddress addr = (IIOPAddress)profile.getAddress();
                logger.debug("Server is: " + dns + " " + serverName + " "
                        + (addr == null ? "" : " (" + addr.getIP() + ":" + addr.getPort() + ")"));
                throw new RecFuncException("Problem getting cache server", e);
            }
        }
        return cache;
    }*/

    protected void setCache(NSRecFuncCache cache) {
        this.cache = cache;
    }

    String iorFilename;
    
    String dns;

    String serverName;

    RecFuncCacheOperations cache;

    IterDeconConfig deconConfig;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncCacheCheck.class);
}
