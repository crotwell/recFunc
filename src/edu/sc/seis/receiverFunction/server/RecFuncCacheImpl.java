package edu.sc.seis.receiverFunction.server;

import edu.iris.Fissures.IfEvent.EventAccess;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.sc.seis.IfReceiverFunction.RecFuncCachePOA;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;


/**
 * @author crotwell
 * Created on Sep 9, 2004
 */
public class RecFuncCacheImpl extends RecFuncCachePOA {
    
	public IterDeconConfig[] getCachedConfigs(EventAccess event,
				                    ChannelId[] channel) {
		
	return null;
	}
	
    /**
     *
     */
    public LocalSeismogram[] get(EventAccess event,
						        ChannelId[] channel,
								IterDeconConfig config) {
        // TODO Auto-generated method stub
        return new LocalSeismogram[0];
    }
    /**
     *
     */
    public void insert(EventAccess event,
            IterDeconConfig config,
            Channel[] channels,
	           LocalSeismogram[] original,
	           LocalSeismogram radial,
	           float radialError,
	           LocalSeismogram tansverse,
	           float transverseError) {
    // TODO Auto-generated method stub
        System.out.println("insert ");
    }
    
    /**
     *
     */
    public boolean isCached(EventAccess event,
            ChannelId[] channel,
            IterDeconConfig config) {
        // TODO Auto-generated method stub
        return false;
    }
}
