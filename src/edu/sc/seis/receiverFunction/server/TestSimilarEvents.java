package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.util.Properties;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;

public class TestSimilarEvents {

    public static void main(String[] args) throws Exception {
        Properties props = StackSummary.loadProps(args);
        Connection conn = StackSummary.initDB(props);
        int eventId = 3600;
        int chanDbId = -1;
        for(int i = 0; i < args.length - 1; i++) {
            if(args[i].equals("--eventid")) {
                eventId = Integer.parseInt(args[i + 1]);
            } else if (args[i].equals("--chanId")) {
                chanDbId = Integer.parseInt(args[i+1]);
            }
        }
        JDBCEventAccess jdbcEventAccess = new JDBCEventAccess(conn);
        CacheEvent event = jdbcEventAccess.getEvent(eventId);
        CacheEvent[] similar = jdbcEventAccess.getSimilarEvents(event,
                                                                new TimeInterval(10,
                                                                                 UnitImpl.SECOND),
                                                                new QuantityImpl(.5,
                                                                                 UnitImpl.DEGREE));
        Origin prefOrigin = event.getOrigin();
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn,
                                                  RecFuncCacheImpl.getDataLoc());
        for(int i = 0; i < similar.length; i++) {
            System.out.println(similar[i].getDbid() + "  " + similar[i]);
        }
        if(chanDbId != -1) {
            ChannelId chanId = jdbcRecFunc.getJDBCChannel().getId(chanDbId);
            for(int i = 0; i < similar.length; i++) {
                System.out.println("is cached "+similar[i].getDbid() + "  " + similar[i] + " "
                        + jdbcRecFunc.exists(prefOrigin, chanId, new IterDeconConfig(2.5f, 400, 0.001f)));
            }
        } else {
            CachedResultPlusDbId[] results = jdbcRecFunc.getStationsByEvent(event,
                                                                            2.5f,
                                                                            80,
                                                                            false);
            RecFuncCacheImpl cacheImpl = new RecFuncCacheImpl(props.getProperty("cormorant.servers.ears.databaseURL"),
                                                              props.getProperty("cormorant.servers.ears.dataloc"));
            for(int i = 0; i < results.length; i++) {
                ChannelId[] chanId = new ChannelId[results[i].getCachedResult().channels.length];
                for(int j = 0; j < chanId.length; j++) {
                    chanId[j] = results[i].getCachedResult().channels[j].get_id();
                }
                for(int j = 0; j < similar.length; j++) {
                    System.out.println(ChannelIdUtil.toStringNoDates(chanId[0])
                            + "  "
                            + similar[j].getDbid()
                            + "  "
                            + "  "
                            + results[i].getDbId()
                            + "  "
                            + cacheImpl.isCached(similar[j].get_preferred_origin(),
                                                 chanId,
                                                 results[i].getCachedResult().config));
                }
            }
        }
    }
}
