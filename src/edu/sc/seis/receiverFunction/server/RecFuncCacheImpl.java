package edu.sc.seis.receiverFunction.server;

import edu.iris.Fissures.IfEvent.EventAccess;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.sc.seis.IfReceiverFunction.RecFuncCachePOA;


/**
 * @author crotwell
 * Created on Sep 9, 2004
 */
public class RecFuncCacheImpl extends RecFuncCachePOA {
    
    /**
     *
     */
    public LocalSeismogram[] get(EventAccess event,
                                 ChannelId[] channel,
                                 float gwidth,
                                 int maxBumps,
                                 float tol) {
        // TODO Auto-generated method stub
        return new LocalSeismogram[0];
    }
    /**
     *
     */
    public void insert(EventAccess event,
                       float gwidth,
                       int maxBumps,
                       float tol,
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
                            float gwidth,
                            int maxBumps,
                            float tol) {
        // TODO Auto-generated method stub
        return false;
    }
}
