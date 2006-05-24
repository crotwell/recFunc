package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.UNKNOWN;
import edu.iris.Fissures.TimeRange;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.event.EventAttrImpl;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.RecFuncCachePOA;
import edu.sc.seis.IfReceiverFunction.RecFuncNotFound;
import edu.sc.seis.IfReceiverFunction.SodConfigNotFound;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.receiverFunction.QualityControl;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.subsetter.channel.ChannelEffectiveTimeOverlap;

/**
 * @author crotwell Created on Sep 9, 2004
 */
public class RecFuncCacheImpl extends RecFuncCachePOA {

    public RecFuncCacheImpl(String databaseURL, String dataloc)
            throws IOException, SQLException, ConfigurationException,
            TauModelException, Exception {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(databaseURL);
        DATA_LOC = dataloc;
        Connection conn = ConnMgr.createConnection();
        jdbcEventAccess = new JDBCEventAccess(conn);
        jdbcChannel = new JDBCChannel(conn);
        jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      DATA_LOC);
        jdbcHKStack = new JDBCHKStack(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      jdbcRecFunc);
        qualityControl = new QualityControl(conn);
    }

    Connection getConnection() {
        return jdbcRecFunc.getConnection();
    }

    public IterDeconConfig[] getCachedConfigs(Origin prefOrigin,
                                              ChannelId[] channel) {
        try {
            ArrayList configs = new ArrayList();
            synchronized(jdbcRecFunc.getConnection()) {
                CacheEvent[] similar = jdbcEventAccess.getSimilarEvents(new CacheEvent(new EventAttrImpl("dummy"),
                                                                                       prefOrigin),
                                                                        timeTol,
                                                                        positionTol);
                for(int i = 0; i < similar.length; i++) {
                    try {
                        IterDeconConfig[] tmpConfigs = jdbcRecFunc.getCachedConfigs(similar[i].get_preferred_origin(),
                                                                                    channel);
                        for(int j = 0; j < tmpConfigs.length; j++) {
                            configs.add(tmpConfigs[j]);
                        }
                    } catch(NotFound e) {}
                }
                return (IterDeconConfig[])configs.toArray(new IterDeconConfig[0]);
            }
        } catch(Throwable e) {
            GlobalExceptionHandler.handle(e);
            throw new org.omg.CORBA.UNKNOWN(e.toString(),
                                            14,
                                            CompletionStatus.COMPLETED_MAYBE);
        }
    }

    /**
     * 
     */
    public CachedResult get(Origin prefOrigin,
                            ChannelId[] channel,
                            IterDeconConfig config) throws RecFuncNotFound {
        try {
            synchronized(jdbcRecFunc.getConnection()) {
                CacheEvent[] similar = jdbcEventAccess.getSimilarEvents(new CacheEvent(new EventAttrImpl("dummy"),
                                                                                       prefOrigin),
                                                                        timeTol,
                                                                        positionTol);
                for(int i = 0; i < similar.length; i++) {
                    try {
                        CachedResult out = jdbcRecFunc.get(similar[i].get_preferred_origin(),
                                                           channel,
                                                           config)
                                .getCachedResult();
                        if(out != null) {
                            return out;
                        }
                    } catch(NotFound e) {}
                }
                throw new RecFuncNotFound();
            }
        } catch(Throwable e) {
            GlobalExceptionHandler.handle(e);
            throw new org.omg.CORBA.UNKNOWN(e.toString(),
                                            13,
                                            CompletionStatus.COMPLETED_MAYBE);
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
            Connection conn = jdbcRecFunc.getConnection();
            synchronized(conn) {
                try {
                    CacheEvent[] similar = jdbcEventAccess.getSimilarEvents(new CacheEvent(new EventAttrImpl("dummy"),
                                                                                           prefOrigin),
                                                                            timeTol,
                                                                            positionTol);
                    if(similar.length > 0) {
                        // already an origin in the database, keep using
                        // existing origin
                        prefOrigin = similar[0].get_preferred_origin();
                    }
                } catch(NotFound e) {
                    // event not yet in db, so proceed as new
                } catch(NoPreferredOrigin e) {
                    // should never happen, just act as if new origin
                }
                boolean autocommit = conn.getAutoCommit();
                try {
                    if(autocommit) {
                        conn.setAutoCommit(false);
                    }
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
                    CachedResultPlusDbId result = jdbcRecFunc.get(recFuncDbId);
                    if(qualityControl.check(result)) {
                        System.out.println("insert " + recFuncDbId
                                + " with weights of 1/3 for gaussian="
                                + config.gwidth + " for "
                                + ChannelIdUtil.toStringNoDates(channels[0]));
                        float weightPs = 1 / 3f;
                        float weightPpPs = 1 / 3f;
                        float weightPsPs = 1 - weightPs - weightPpPs;
                        jdbcHKStack.calcAndStore(recFuncDbId,
                                                 weightPs,
                                                 weightPpPs,
                                                 weightPsPs);
                    }
                    conn.commit();
                } catch(Throwable e) {
                    conn.rollback();
                    GlobalExceptionHandler.handle("Problem with "
                            + ChannelIdUtil.toString(channels[0].get_id())
                            + " for origin time="
                            + prefOrigin.origin_time.date_time, e);
                    throw new UNKNOWN(e.toString(),
                                      12,
                                      CompletionStatus.COMPLETED_MAYBE);
                } finally {
                    if(autocommit) {
                        conn.setAutoCommit(autocommit);
                    }
                }
            }
        } catch(SQLException e) {
            // why would get and set AutoCommit throw?
            GlobalExceptionHandler.handle("AutoCommit problem "
                    + ChannelIdUtil.toString(channels[0].get_id())
                    + " for origin time=" + prefOrigin.origin_time.date_time, e);
            throw new UNKNOWN(e.toString(),
                              12,
                              CompletionStatus.COMPLETED_MAYBE);
        }
    }

    /**
     * 
     */
    public boolean isCached(Origin prefOrigin,
                            ChannelId[] channel,
                            IterDeconConfig config) {
        try {
            ChannelId chanz = null;
            for(int i = 0; i < channel.length; i++) {
                if (channel[i].channel_code.endsWith("Z")) {
                    chanz = channel[i];
                }
            }
            if (chanz == null) {
                throw new UNKNOWN("Can't find Z channel: ");
            }
            synchronized(jdbcRecFunc.getConnection()) {
                return jdbcRecFunc.exists(prefOrigin, chanz, config);
            }
        } catch(NotFound e) {
            return false;
        } catch(Throwable e) {
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
        } catch(Throwable t) {
            GlobalExceptionHandler.handle(t);
            throw new UNKNOWN(t.getMessage(),
                              10,
                              CompletionStatus.COMPLETED_MAYBE);
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
        } catch(Throwable t) {
            GlobalExceptionHandler.handle(t);
            throw new UNKNOWN(t.getMessage(),
                              11,
                              CompletionStatus.COMPLETED_MAYBE);
        }
    }

    public static String getDataLoc() {
        return DATA_LOC;
    }

    private TimeInterval timeTol = new TimeInterval(10, UnitImpl.SECOND);

    private QuantityImpl positionTol = new QuantityImpl(.5, UnitImpl.DEGREE);

    private JDBCEventAccess jdbcEventAccess;

    private JDBCChannel jdbcChannel;

    private JDBCRecFunc jdbcRecFunc;

    private JDBCHKStack jdbcHKStack;

    private JDBCSodConfig jdbcSodConfig;

    private QualityControl qualityControl;

    private static String DATA_LOC = "../Data";

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncCacheImpl.class);
}
