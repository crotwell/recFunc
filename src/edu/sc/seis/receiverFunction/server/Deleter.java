package edu.sc.seis.receiverFunction.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfNetwork.NetworkDC;
import edu.iris.Fissures.IfNetwork.NetworkDCOperations;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.cache.BulletproofVestFactory;
import edu.sc.seis.fissuresUtil.cache.VestingNetworkDC;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;
import edu.sc.seis.fissuresUtil.simple.Initializer;

public class Deleter {

    public Deleter(Connection conn) throws Exception {
        jdbcRecFunc = new JDBCRecFunc(conn, RecFuncCacheImpl.getDataLoc());
        jdbcHKStack = new JDBCHKStack(jdbcRecFunc);
        jdbcRecFuncQC = new JDBCRecFuncQC(conn);
    }

    public void delete(int recfunc_id) throws SQLException, NotFound,
            IOException {
        try {
            jdbcHKStack.deleteForRecFuncId(recfunc_id);
        } catch(NotFound e) {
            logger.warn("no hkstack found for recfunc=" + recfunc_id);
        }
        jdbcRecFuncQC.delete(recfunc_id);
        jdbcRecFunc.delete(recfunc_id);
    }

    public void deleteOrigin(int originId) throws SQLException, NotFound,
            IOException {
        String recFuncForOrigin = "SELECT recfunc_id FROM receiverfunction WHERE origin_id = ?";
        PreparedStatement stmt = jdbcRecFunc.getConnection()
                .prepareStatement(recFuncForOrigin);
        stmt.setInt(1, originId);
        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {
            delete(rs.getInt(1));
        }
        String deleteEvent = "DELETE FROM eventAccess WHERE origin_id = ?";
        stmt = jdbcRecFunc.getConnection().prepareStatement(deleteEvent);
        stmt.setInt(1, originId);
        int numChanged = stmt.executeUpdate();
        System.out.println("Deleted " + numChanged + " from event");
        String deleteOrigin = "DELETE FROM origin WHERE origin_id = ?";
        stmt = jdbcRecFunc.getConnection().prepareStatement(deleteOrigin);
        stmt.setInt(1, originId);
        numChanged = stmt.executeUpdate();
        System.out.println("Deleted " + numChanged + " from origin");
    }

    public void deleteOutsideChannelTimes(String netCode, String staCode)
            throws SQLException, NotFound, IOException {
        String outsideChanStmtStr = "select originview.time_stamp, originview.origin_id, recfunc_id , inserttime, chanz_id, chan_begin_time_stamp, chan_end_time_stamp "
                + "from receiverfunction "
                + "JOIN netchan ON (chanz_id = chan_id) "
                + "JOIN originview ON (receiverfunction.origin_id = originview.origin_id) "
                + "where net_code = ? AND sta_code = ? AND "
                + "(originview.time_stamp < chan_begin_time_stamp OR originview.time_stamp > chan_end_time_stamp)";
        PreparedStatement outsideChan = jdbcRecFunc.getConnection()
                .prepareStatement(outsideChanStmtStr);
        outsideChan.setString(1, netCode);
        outsideChan.setString(2, staCode);
        ResultSet rs = outsideChan.executeQuery();
        ArrayList rfIdList = new ArrayList();
        while(rs.next()) {
            rfIdList.add(new Integer(rs.getInt("recfunc_id")));
        }
        Iterator it = rfIdList.iterator();
        while(it.hasNext()) {
            delete(((Integer)it.next()).intValue());
        }
    }

    public void deleteRecFuncForDuplicateOrigins() throws SQLException,
            NotFound, IOException, FissuresException, TauModelException {
        String dupOrigins = "SELECT "
                + "a.recfunc_id, b.recfunc_id, b.itr_match "
                + "FROM receiverfunction AS a " + "JOIN receiverfunction AS b "
                + "ON (a.origin_id = b.origin_id "
                + "AND a.chanz_id = b.chanz_id "
                + "AND a.recfunc_id < b.recfunc_id)" + " ORDER BY a.recfunc_id";
        float weightPs = 1 / 3f;
        float weightPpPs = 1 / 3f;
        float weightPsPs = 1 - weightPs - weightPpPs;
        Statement stmt = jdbcRecFunc.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(dupOrigins);
        int n = 0;
        int prevAId = 0;
        ArrayList otherDeletes = new ArrayList();
        while(rs.next()) {
            n++;
            int a_id = rs.getInt(1);
            int b_id = rs.getInt(2);
            if(a_id == prevAId || otherDeletes.contains(new Integer(a_id))) {
                // must have been more than 2, this one is already deleted
                continue;
            }
            System.out.println(n + " Delete rfid=" + a_id + "  keep rfid="
                    + b_id);
            delete(a_id);
            if(rs.getFloat(3) >= 80) {
                // deleting a removes AnalyticPlotData for b as well
                // but only need to recalc hk for > 80% match
                try {
                    jdbcHKStack.deleteForRecFuncId(b_id);
                } catch(NotFound ee) {
                    // no hkstack for this rfid, may still need to calc
                }
                try {
                    // only recalc if b_id has not already been deleted
                    if(!otherDeletes.contains(new Integer(b_id))) {
                        jdbcHKStack.calc(b_id,
                                         weightPs,
                                         weightPpPs,
                                         weightPsPs,
                                         true);
                    }
                } catch(FileNotFoundException e) {
                    // couldn't load the receiver function, delete
                    // the rf and recalculate later
                    delete(b_id);
                    otherDeletes.add(new Integer(b_id));
                }
            }
            prevAId = a_id;
        }
    }

    public void cleanOverlappingChannels(String[] args) throws SQLException {
        Initializer.init(args);
        FissuresNamingService fisName = Initializer.getNS();
        NetworkDCOperations netDC = new VestingNetworkDC("edu.iris.dmc",
                                                         "IRIS_NetworkDC",
                                                         fisName);
        NetworkAccess[] nets = netDC.a_finder().retrieve_all();
        String dupChannels = "select a.chan_id, b.chan_id, a.net_code, a.net_begin_time_stamp, a.sta_code, a.site_code, a.chan_code, "
                + "a.chan_begin_time_stamp AS a_chan_begin_time_stamp, "
                + "b.chan_begin_time_stamp AS b_chan_begin_time_stamp, "
                + "a.chan_end_time_stamp AS a_chan_end_time_stamp, "
                + "b.chan_end_time_stamp  AS b_chan_end_time_stamp "
                + "FROM netchan AS a JOIN netchan AS b ON (a.net_code = b.net_code AND a.sta_code = b.sta_code AND a.site_code = b.site_code AND a.chan_code = b.chan_code AND a.chan_end_time_stamp > b.chan_begin_time_stamp AND a.chan_end_time_stamp < b.chan_end_time_stamp)";
        Statement stmt = jdbcRecFunc.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(dupChannels);
        while(rs.next()) {
            ChannelId aChan = new ChannelId(new NetworkId(rs.getString("net_code"),
                                                          new MicroSecondDate(rs.getTimestamp("net_begin_time_stamp")).getFissuresTime()),
                                            rs.getString("sta_code"),
                                            rs.getString("site_code"),
                                            rs.getString("chan_code"),
                                            new MicroSecondDate(rs.getTimestamp("a_chan_begin_time_stamp")).getFissuresTime());
            ChannelId bChan = new ChannelId(new NetworkId(rs.getString("net_code"),
                                                          new MicroSecondDate(rs.getTimestamp("net_begin_time_stamp")).getFissuresTime()),
                                            rs.getString("sta_code"),
                                            rs.getString("site_code"),
                                            rs.getString("chan_code"),
                                            new MicroSecondDate(rs.getTimestamp("b_chan_begin_time_stamp")).getFissuresTime());
            NetworkAccess curNet;
            for(int i = 0; i < nets.length; i++) {
                if(NetworkIdUtil.areEqual(aChan.network_id,
                                          nets[i].get_attributes().get_id())) {}
            }
        }
    }

    public void deleteEmptySummary() throws Exception {
        PreparedStatement stmt = jdbcHKStack.getConnection()
                .prepareStatement("select hksummary_id FROM"
                        + " hksummary LEFT JOIN ("
                        + " SELECT net_id, sta_code, count(*) as countEQ FROM netchan"
                        + "    JOIN  receiverFunction ON (chanz_id = chan_id ) "
                        + "    LEFT JOIN recfuncQC ON (receiverFunction.recfunc_id = recfuncQC.recfunc_id ) "
                        + "    WHERE"
                        + "    gwidth = 2.5"
                        + "    AND itr_match >= 80"
                        + "    AND (keep = true OR keep IS NULL)"
                        + "    GROUP BY net_id, sta_code) as a"
                        + " ON (hksummary.net_id = a.net_id AND hksummary.sta_code = a.sta_code)"
                        + " WHERE countEQ IS NULL OR countEQ = 1");
        JDBCSummaryHKStack jdbcSum = new JDBCSummaryHKStack(jdbcHKStack);
        JDBCStackComplexity jdbcComplexity = new JDBCStackComplexity(jdbcSum);
        ResultSet rs = stmt.executeQuery();
        int num = 0;
        while(rs.next()) {
            int dbid = rs.getInt(1);
            jdbcComplexity.delete(dbid);
            num += jdbcSum.delete(dbid);
        }
        System.out.println("Deleted "+num+" summaries that no longer have >2 rf in the stack");
    }

    JDBCRecFunc jdbcRecFunc;

    JDBCRecFuncQC jdbcRecFuncQC;

    JDBCHKStack jdbcHKStack;

    public static void main(String[] args) throws Exception {
        Properties props = StackSummary.loadProps(args);
        Connection conn = StackSummary.initDB(props);
        String netArg = "";
        String staArg = "";
        int rfid = -1;
        int originId = -1;
        boolean outsideChannel = false;
        boolean dupRFOrigin = false;
        Deleter deleter = new Deleter(conn);
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-net")) {
                netArg = args[i + 1];
                i++;
            } else if(args[i].equals("-sta")) {
                staArg = args[i + 1];
                i++;
            } else if(args[i].equals("-rfid")) {
                rfid = Integer.parseInt(args[i + 1]);
                i++;
            } else if(args[i].equals("-originid")) {
                originId = Integer.parseInt(args[i + 1]);
                i++;
            } else if(args[i].equals("--outsideChannel")) {
                outsideChannel = true;
            } else if(args[i].equals("--duplicateRFOrigin")) {
                dupRFOrigin = true;
            } else if(args[i].equals("--emptySummary")) {
                deleter.deleteEmptySummary();
            }
        }
        if(rfid > 0) {
            deleter.delete(rfid);
        } else if(originId > 0) {
            deleter.deleteOrigin(originId);
        } else if(outsideChannel && netArg.length() > 0 && staArg.length() > 0) {
            deleter.deleteOutsideChannelTimes(netArg, staArg);
        } else if(dupRFOrigin) {
            deleter.deleteRecFuncForDuplicateOrigins();
        }
        conn.close();
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Deleter.class);
}
