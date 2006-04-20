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
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.NotFound;

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
        } catch (NotFound e) {
            logger.warn("no hkstack found for recfunc="+recfunc_id);
        }
        jdbcRecFuncQC.delete(recfunc_id);
        jdbcRecFunc.delete(recfunc_id);
    }

    public void deleteOutsideChannelTimes(String netCode, String staCode) throws SQLException, NotFound, IOException {
        String outsideChanStmtStr = 
            "select originview.time_stamp, originview.origin_id, recfunc_id , inserttime, chanz_id, chan_begin_time_stamp, chan_end_time_stamp "+
            "from receiverfunction "+
            "JOIN netchan ON (chanz_id = chan_id) "+
            "JOIN originview ON (receiverfunction.origin_id = originview.origin_id) "+
            "where net_code = ? AND sta_code = ? AND "+
            "(originview.time_stamp < chan_begin_time_stamp OR originview.time_stamp > chan_end_time_stamp)";
        PreparedStatement outsideChan = jdbcRecFunc.getConnection().prepareStatement(outsideChanStmtStr);
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
    
    public void deleteRecFuncForDuplicateOrigins() throws SQLException, NotFound, IOException, FissuresException, TauModelException {
        String dupOrigins = "SELECT "
            +"a.recfunc_id, b.recfunc_id, b.itr_match "
            +"FROM receiverfunction AS a "
            +"JOIN receiverfunction AS b "
            +"ON (a.origin_id = b.origin_id "
            +"AND a.chanz_id = b.chanz_id "
            +"AND a.recfunc_id < b.recfunc_id)"
            +" ORDER BY a.recfunc_id";
        float weightPs = 1 / 3f;
        float weightPpPs = 1 / 3f;
        float weightPsPs = 1 - weightPs - weightPpPs;
        Statement stmt = jdbcRecFunc.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(dupOrigins);
        int n=0;
        int prevAId = 0;
        ArrayList otherDeletes = new ArrayList();
        while(rs.next()) {
            n++;
            int a_id = rs.getInt(1);
            int b_id = rs.getInt(2);
            if (a_id == prevAId || otherDeletes.contains(new Integer(a_id))) {
                // must have been more than 2, this one is already deleted
                continue;
            }
            System.out.println(n+" Delete rfid="+a_id+"  keep rfid="+b_id);
            delete(a_id);
            if (rs.getFloat(3)>=80) {
                // deleting a removes AnalyticPlotData for b as well
                // but only need to recalc hk for > 80% match
                try {
                    jdbcHKStack.deleteForRecFuncId(b_id);
                } catch (NotFound ee) {
                    // no hkstack for this rfid, may still need to calc
                }
                try {
                    // only recalc if b_id has not already been deleted
                    if ( ! otherDeletes.contains(new Integer(b_id))) {
                        jdbcHKStack.calc(b_id, weightPs, weightPpPs, weightPsPs, true);
                    }
                } catch (FileNotFoundException e) {
                    // couldn't load the receiver function, delete
                    // the rf and recalculate later
                    delete(b_id);
                    otherDeletes.add(new Integer(b_id));
                }
            }
            prevAId = a_id;
        }
        
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
        boolean outsideChannel = false;
        boolean dupRFOrigin = false;
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
            } else if (args[i].equals("--outsideChannel")) {
                outsideChannel = true;
            } else if (args[i].equals("--duplicateRFOrigin")) {
                dupRFOrigin = true;
            }
        }
        Deleter deleter = new Deleter(conn);
        if(rfid > 0) {
            deleter.delete(rfid);
        } else if (outsideChannel && netArg.length()>0 && staArg.length()>0) {
            deleter.deleteOutsideChannelTimes(netArg, staArg);
        } else if (dupRFOrigin) {
            deleter.deleteRecFuncForDuplicateOrigins();
        }
        conn.close();
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Deleter.class);
}
