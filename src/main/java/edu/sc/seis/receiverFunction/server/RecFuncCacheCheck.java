package edu.sc.seis.receiverFunction.server;

import org.jacorb.orb.ParsedIOR;
import org.jacorb.orb.iiop.IIOPAddress;
import org.jacorb.orb.iiop.IIOPProfile;
import org.omg.CORBA.SystemException;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.w3c.dom.Element;

import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.display.configuration.DOMHelper;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.RecFuncProcessor;
import edu.sc.seis.sod.CommonAccess;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.Start;
import edu.sc.seis.sod.status.StringTree;
import edu.sc.seis.sod.status.StringTreeLeaf;
import edu.sc.seis.sod.subsetter.eventChannel.vector.EventVectorSubsetter;


/**
 * @author crotwell
 * Created on Sep 16, 2004
 */
public class RecFuncCacheCheck implements EventVectorSubsetter {

    public RecFuncCacheCheck(Element config) throws ConfigurationException {
        deconConfig = RecFuncProcessor.parseIterDeconConfig(config);
        String dns = DOMHelper.extractText(config, "dns", "edu/sc/seis");
        String serverName = DOMHelper.extractText(config, "name", "Ears");
        try {
            cache = new NSRecFuncCache(dns, serverName, CommonAccess.getNameService());
            cache.getCorbaObject()._is_a("test");
            logger.debug("Connection to rf cacheServer ok");
        } catch (SystemException e) {
            String ior = CommonAccess.getORB().object_to_string(cache.getCorbaObject());
            ParsedIOR parsed = new ParsedIOR((org.jacorb.orb.ORB)Initializer.getORB(),
                                             ior);
            IIOPProfile profile = (IIOPProfile)parsed.getProfiles().get(0);
            IIOPAddress addr = (IIOPAddress)profile.getAddress();
            logger.debug("Server is: "+ dns + " " + serverName+" "+(addr==null?"":" ("+addr.getIP()+":"+addr.getPort()+")"));
            throw new ConfigurationException("Problem getting cache server", e);
        }
    }
    
    /**
     * returns true if the receiver functions are already cached.
     */
    public StringTree accept(CacheEvent event,
                          ChannelGroup channel,
                          CookieJar cookieJar) throws Exception {
        ChannelId[] chanId = new ChannelId[channel.getChannels().length];
        for(int i = 0; i < chanId.length; i++) {
            chanId[i] = channel.getChannels()[i].get_id();
        }
        return new StringTreeLeaf(this, cache.isCached(event.get_preferred_origin(), chanId, deconConfig));
    }
    
    NSRecFuncCache cache;

    IterDeconConfig deconConfig;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncCacheCheck.class);
}
