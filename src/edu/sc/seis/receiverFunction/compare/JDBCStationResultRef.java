package edu.sc.seis.receiverFunction.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;


/**
 * @author crotwell
 * Created on Mar 27, 2005
 */
public class JDBCStationResultRef extends JDBCTable {

    /**
     * @throws SQLException
     *
     */
    public JDBCStationResultRef(Connection conn) throws SQLException {
        super("stationResultRef", conn);
        seq = new JDBCSequence(conn, "stationResultRefSeq");
        TableSetup.setup(this, "edu.sc.seis.receiverFunction.compare.default.prop");
    }
    
    public int put(String name, String reference, String method) throws SQLException {
        int seqNum = seq.next();
        int index = 1;
        put.setInt(index++, seqNum);
        put.setString(index++, name);
        put.setString(index++, reference);
        put.setString(index++, method);
        put.execute();
        return seqNum;
    }
    
    JDBCSequence seq;
    
    PreparedStatement put;
}
