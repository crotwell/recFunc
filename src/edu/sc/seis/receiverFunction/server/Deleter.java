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
    
    public void cleanDuplicateOrigin() {
        String dupRF = "update receiverfunction set origin_id = ob.origin_id "+
        "FROM originview AS oa  "+
        "JOIN originview AS ob ON ( oa.depth_unit_id = ob.depth_unit_id "+
        "    AND abs(oa.depth_value-ob.depth_value) < 0.0000000001  "+
        "    AND oa.loc_lat = ob.loc_lat "+
        "    AND oa.loc_lon = ob.loc_lon "+
        "    AND oa.origin_time_id = ob.origin_time_id ) "+
        "WHERE oa.origin_id < ob.origin_id AND reveiverfunction.origin_id = oa.origin_id";
    }

    public void deleteOutsideChannelTimes() throws SQLException, NotFound,
            IOException {
        String outsideChanStmtStr = "select originview.time_stamp, originview.origin_id, recfunc_id , inserttime, chanz_id, chan_begin_time_stamp, chan_end_time_stamp "
                + "from receiverfunction "
                + "JOIN netchan ON (chanz_id = chan_id) "
                + "JOIN originview ON (receiverfunction.origin_id = originview.origin_id) "
                + "where "
                + "(originview.time_stamp < chan_begin_time_stamp OR originview.time_stamp > chan_end_time_stamp)";
        PreparedStatement outsideChan = jdbcRecFunc.getConnection()
                .prepareStatement(outsideChanStmtStr);
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
                + "a.recfunc_id, b.recfunc_id, a.itr_match "
                + "FROM receiverfunction AS a " 
                + "JOIN receiverfunction AS b "
                + "ON (a.origin_id = b.origin_id "
                + "AND a.chanz_id = b.chanz_id "
                + "AND a.recfunc_id < b.recfunc_id)" 
                + " ORDER BY a.recfunc_id, b.recfunc_id";
        Statement stmt = jdbcRecFunc.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(dupOrigins);
        zap(rs);
    }

    public void deleteDuplicateByCode() throws Exception {
        PreparedStatement stmt = jdbcHKStack.getConnection()
                .prepareStatement("select ra.recfunc_id, rb.recfunc_id, ra.itr_match"
                                  +" FROM receiverfunction AS ra "
                        + " JOIN netchan AS na ON (ra.chanz_id = na.chan_id) "
                        + " JOIN origin ON (ra.origin_id = origin.origin_id) "
                        + " JOIN netchan AS nb ON (na.net_id = nb.net_id AND "
                        + " na.sta_code = nb.sta_code AND na.site_code = nb.site_code AND na.chan_code = nb.chan_code) "
                        + " JOIN receiverfunction as rb ON (rb.chanz_id = nb.chan_id AND rb.origin_id = origin.origin_id) "
                        + " WHERE ra.recfunc_id < rb.recfunc_id"
                        + " ORDER BY ra.recfunc_id, rb.recfunc_id");
        ResultSet rs = stmt.executeQuery();
        zap(rs);
    }
    
    private void zap(ResultSet rs) throws SQLException,
    NotFound, IOException, FissuresException, TauModelException {
        float weightPs = 1 / 3f;
        float weightPpPs = 1 / 3f;
        float weightPsPs = 1 - weightPs - weightPpPs;
        int n = 0;
        int prevAId = 0;
        ArrayList otherDeletes = new ArrayList();
        while(rs.next()) {
            n++;
            int a_id = rs.getInt(1);
            int b_id = rs.getInt(2);
            float percentMatch = rs.getFloat(3);
            if(a_id == prevAId || otherDeletes.contains(new Integer(a_id))) {
                // must have been more than 2, this one is already deleted
                continue;
            }
            System.out.println(n + " Delete rfid=" + a_id + "  keep rfid="
                    + b_id);
            delete(a_id);
            if(percentMatch >= 80) {
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
        System.out.println("Deleted " + num
                + " summaries that no longer have >2 rf in the stack");
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
                deleter.delete(rfid);
                i++;
            } else if(args[i].equals("-originid")) {
                originId = Integer.parseInt(args[i + 1]);
                deleter.deleteOrigin(originId);
                i++;
            } else if(args[i].equals("--outsideChannel")) {
                deleter.deleteOutsideChannelTimes();
            } else if(args[i].equals("--duplicateRFOrigin")) {
                deleter.deleteRecFuncForDuplicateOrigins();
            } else if(args[i].equals("--duplicateByCode")) {
                deleter.deleteDuplicateByCode();
            } else if(args[i].equals("--emptySummary")) {
                deleter.deleteEmptySummary();
            }
        }
        conn.close();
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Deleter.class);
}
