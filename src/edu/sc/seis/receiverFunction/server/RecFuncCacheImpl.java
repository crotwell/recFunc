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
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Sep 9, 2004
 */
public class RecFuncCacheImpl extends RecFuncCachePOA {
    
    public RecFuncCacheImpl() throws SQLException, ConfigurationException {
        Connection conn = ConnMgr.createConnection();
        jdbcOrigin = new JDBCOrigin(conn);
        jdbcEventAttr = new JDBCEventAttr(conn);
        jdbcChannel  = new JDBCChannel(conn);
        jdbcRecFunc = new JDBCRecFunc(conn, jdbcOrigin, jdbcEventAttr, jdbcChannel, "Ears/Data");
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
            int recFuncDbId = jdbcRecFunc.put(prefOrigin,
                       eventAttr,
                       config,
                       channels,
                       original,
                       radial,
                       radialError,
                       tansverse,
                       transverseError);
            System.out.println("insert "+recFuncDbId);
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

    private JDBCOrigin jdbcOrigin ;
    
    private JDBCEventAttr jdbcEventAttr;
    
    private JDBCChannel jdbcChannel;
    
    private JDBCRecFunc jdbcRecFunc;
    
}
