package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.UNKNOWN;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.RecFuncCachePOA;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.SodConfigNotFound;
import edu.sc.seis.TauP.TauModelException;
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
    
    public RecFuncCacheImpl() throws IOException, SQLException, ConfigurationException, TauModelException {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        Connection conn = ConnMgr.createConnection();
        jdbcOrigin = new JDBCOrigin(conn);
        jdbcEventAttr = new JDBCEventAttr(conn);
        jdbcChannel  = new JDBCChannel(conn);
        jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn, jdbcOrigin, jdbcEventAttr, jdbcChannel, jdbcSodConfig, DATA_LOC);
        jdbcHKStack = new JDBCHKStack(conn,  jdbcOrigin, jdbcEventAttr, jdbcChannel, jdbcSodConfig, jdbcRecFunc);
    }
    
	public IterDeconConfig[] getCachedConfigs(Origin prefOrigin,
				                    ChannelId[] channel) {
	    try {
	        synchronized (jdbcRecFunc.getConnection()) {
	            return jdbcRecFunc.getCachedConfigs(prefOrigin, channel);
	        }
        } catch(NotFound e) {
            logger.info("NotFound: ", e);
            return new IterDeconConfig[0];
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
            throw new org.omg.CORBA.UNKNOWN(e.toString());
        }
	}
	
    /**
     *
     */
    public CachedResult get(Origin prefOrigin,
						        ChannelId[] channel,
								IterDeconConfig config) {
        try {
            synchronized (jdbcRecFunc.getConnection()) {
                return jdbcRecFunc.get(prefOrigin, channel, config);
            }
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
            throw new org.omg.CORBA.UNKNOWN(e.toString());
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
                       LocalSeismogram tansverse,
                       float transverseMatch,
                       int transverseBump,
                       int sodConfig_id) {
        try {
            synchronized (jdbcRecFunc.getConnection()) {
                int recFuncDbId = jdbcRecFunc.put(prefOrigin,
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
                System.out.println("insert "+recFuncDbId);
                jdbcHKStack.calc(recFuncDbId);
            }
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
            synchronized (jdbcRecFunc.getConnection()) {
                int tmp = jdbcRecFunc.getDbId(prefOrigin, channel, config);
            }
            return true;
        } catch(NotFound e) {
            return false;
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        }
        return false;
    }


    public int insertSodConfig(String config) {
        try {
            synchronized(jdbcRecFunc.getConnection()) {
                return jdbcSodConfig.put(config);
            }
        } catch(SQLException e) {
            GlobalExceptionHandler.handle(e);
            throw new UNKNOWN(e.getMessage(), 7, CompletionStatus.COMPLETED_NO);
        }
    }

    public String getSodConfig(int sodConfig_id) throws SodConfigNotFound {
        try {
            synchronized(jdbcRecFunc.getConnection()) {
                return jdbcSodConfig.get(sodConfig_id);
            }
        } catch(SQLException e) {
            GlobalExceptionHandler.handle(e);
            throw new UNKNOWN(e.getMessage(), 8, CompletionStatus.COMPLETED_NO);
        } catch(NotFound e) {
            GlobalExceptionHandler.handle(e);
            throw new UNKNOWN(e.getMessage(), 9, CompletionStatus.COMPLETED_NO);
        }
    }
    
    private JDBCOrigin jdbcOrigin ;
    
    private JDBCEventAttr jdbcEventAttr;
    
    private JDBCChannel jdbcChannel;
    
    private JDBCRecFunc jdbcRecFunc;
    
    private JDBCHKStack jdbcHKStack;
    
    private JDBCSodConfig jdbcSodConfig;

    public static String DATA_LOC = "../Ears/Data";
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncCacheImpl.class);
}
