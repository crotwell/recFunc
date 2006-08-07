package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;


public class JDBCRejectedMaxima extends JDBCTable {

    public JDBCRejectedMaxima(Connection conn) throws SQLException {
        super("rejectedMaxima", conn);
        rejectedMaximumSeq = new JDBCSequence(conn, getTableName() + "Seq");
        TableSetup.setup(getTableName(),
                         conn,
                         this,
                         "edu/sc/seis/receiverFunction/server/default.props");
    }
    
    public int put(int netId, String staCode, float hMin, float hMax, float kMin, float kMax, String reason) throws SQLException {
      int index=1;
      int dbid = rejectedMaximumSeq.next();
      put.setInt(index++, dbid);
      put.setInt(index++, netId);
      put.setString(index++, staCode);
      put.setFloat(index++, hMin);
      put.setFloat(index++, hMax);
      put.setFloat(index++, kMin);
      put.setFloat(index++, kMax);
      put.setString(index++, reason);
      put.setTimestamp(index++, ClockUtil.now().getTimestamp());
      put.executeUpdate();
      return dbid;
    }
    
    public HKBox[] getForStation(int netId, String staCode) throws SQLException {
        int index=1;
        getForStation.setInt(index++, netId);
        getForStation.setString(index++, staCode);
        ResultSet rs = getForStation.executeQuery();
        ArrayList out = new ArrayList();
        while(rs.next()) {
            out.add(new HKBox(rs.getFloat("hMin"),
                              rs.getFloat("hMax"),
                              rs.getFloat("kMin"),
                              rs.getFloat("kMax"),
                              rs.getString("reason"),
                              rs.getTimestamp("insertTime")));
        }
        return (HKBox[])out.toArray(new HKBox[0]);
    }
    
    PreparedStatement put, getForStation;
    
    private JDBCSequence rejectedMaximumSeq;
}
