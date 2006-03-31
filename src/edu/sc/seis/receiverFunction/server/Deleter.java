package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import edu.sc.seis.fissuresUtil.database.NotFound;

public class Deleter {

    public Deleter(Connection conn) throws Exception {
        jdbcRecFunc = new JDBCRecFunc(conn, RecFuncCacheImpl.getDataLoc());
        jdbcHKStack = new JDBCHKStack(jdbcRecFunc);
    }

    public void delete(int recfunc_id) throws SQLException, NotFound,
            IOException {
        try {
            jdbcHKStack.deleteForRecFuncId(recfunc_id);
        } catch (NotFound e) {
            logger.warn("no hkstack found for recfunc="+recfunc_id);
        }
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

    JDBCRecFunc jdbcRecFunc;

    JDBCHKStack jdbcHKStack;

    public static void main(String[] args) throws Exception {
        Properties props = StackSummary.loadProps(args);
        Connection conn = StackSummary.initDB(props);
        String netArg = "";
        String staArg = "";
        int rfid = -1;
        boolean outsideChannel = false;
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
            }
        }
        Deleter deleter = new Deleter(conn);
        if(rfid > 0) {
            deleter.delete(rfid);
        } else if (outsideChannel && netArg.length()>0 && staArg.length()>0) {
            deleter.deleteOutsideChannelTimes(netArg, staArg);
        }
        conn.close();
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Deleter.class);
}
