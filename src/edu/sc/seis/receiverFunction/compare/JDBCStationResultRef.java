package edu.sc.seis.receiverFunction.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
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
        TableSetup.setup(this, "edu/sc/seis/receiverFunction/compare/default.props");
    }
    
    public int put(String name, String reference, String method) throws SQLException {
        try {
            return getDbId(name, reference, method);
        } catch (NotFound e) {
            int seqNum = seq.next();
            int index = 1;
            put.setInt(index++, seqNum);
            put.setString(index++, name);
            put.setString(index++, reference);
            put.setString(index++, method);
            put.execute();
            return seqNum;
        }
    }

    public int put(StationResultRef ref) throws SQLException {
        return put(ref.getName(), ref.getReference(), ref.getMethod());
    }
    
    public int getDbId(String name, String reference, String method) throws SQLException, NotFound {
        int index = 1;
        getDbId.setString(index++, name);
        getDbId.setString(index++, reference);
        getDbId.setString(index++, method);
        ResultSet rs = getDbId.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            throw new NotFound();
        }
    }
    
    public StationResultRef extract(ResultSet rs) throws SQLException {
        return new StationResultRef(rs.getString("name"),
                                    rs.getString("reference"),
                                    rs.getString("method"));
    }
    
    JDBCSequence seq;
    
    PreparedStatement put, getDbId;
}
