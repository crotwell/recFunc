package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.omg.CORBA.UNKNOWN;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.RecFuncCachePOA;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
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
    
    public RecFuncCacheImpl() throws SQLException, ConfigurationException, IOException {
        ConnMgr.setDB(ConnMgr.POSTGRES);
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
    public CachedResult get(Origin prefOrigin,
						        ChannelId[] channel,
								IterDeconConfig config) {
        // TODO Auto-generated method stub
        return new CachedResult();
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
        try {
            int tmp = jdbcRecFunc.getDbId(prefOrigin, channel, config);
            return true;
        } catch(NotFound e) {
            return false;
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        }
        return false;
    }

    private JDBCOrigin jdbcOrigin ;
    
    private JDBCEventAttr jdbcEventAttr;
    
    private JDBCChannel jdbcChannel;
    
    private JDBCRecFunc jdbcRecFunc;
    
}