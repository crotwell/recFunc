package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.SQLException;
import org.omg.CORBA.UNKNOWN;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.sc.seis.IfReceiverFunction.RecFuncCachePOA;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAttr;
import edu.sc.seis.fissuresUtil.database.event.JDBCOrigin;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;


/**
 * @author crotwell
 * Created on Sep 9, 2004
 */
public class RecFuncCacheImpl extends RecFuncCachePOA {
    
    public RecFuncCacheImpl() throws SQLException {
        Connection conn = ConnMgr.createConnection();
        jdbcOrigin = new JDBCOrigin(conn);
        jdbcEventAttr = new JDBCEventAttr(conn);
        jdbcChannel  = new JDBCChannel(conn);
    }
    
	public IterDeconConfig[] getCachedConfigs(Origin prefOrigin,
				                    ChannelId[] channel) {
		
	return null;
	}
	
    /**
     *
     */
    public LocalSeismogram[] get(Origin prefOrigin,
						        ChannelId[] channel,
								IterDeconConfig config) {
        // TODO Auto-generated method stub
        return new LocalSeismogram[0];
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
                       float radialError,
                       LocalSeismogram tansverse,
                       float transverseError) {
        // TODO Auto-generated method stub
        try {
            int eventDbId = jdbcOrigin.put(prefOrigin);
            int eventyAttrDbId = jdbcEventAttr.put(eventAttr);
            int[] channelDbId = new int[channels.length];
            for(int i = 0; i < channels.length; i++) {
                channelDbId[i] = jdbcChannel.put(channels[i]);
            }
            System.out.println("insert "+eventDbId+"  "+channelDbId[0]);
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
            throw new UNKNOWN(e.toString());
        }
    }
    
    /**
     *
     */
    public boolean isCached(Origin prefOrigin,
            ChannelId[] channel,
            IterDeconConfig config) {
        // TODO Auto-generated method stub
        return false;
    }
    
    JDBCEventAttr jdbcEventAttr;
    
    JDBCOrigin jdbcOrigin;
    
    JDBCChannel jdbcChannel;
}
