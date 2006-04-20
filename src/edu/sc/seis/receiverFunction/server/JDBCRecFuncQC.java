package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;

public class JDBCRecFuncQC extends JDBCTable {

    public JDBCRecFuncQC(Connection conn) throws SQLException {
        super("recfuncQC", conn);
        TableSetup.setup(getTableName(),
                         conn,
                         this,
                         "edu/sc/seis/receiverFunction/server/default.props");
    }

    public RecFuncQCResult get(int recFunc_id) throws SQLException, NotFound {
        get.setInt(1, recFunc_id);
        ResultSet rs = get.executeQuery();
        if(rs.next()) {
            return extract(rs);
        }
        throw new NotFound();
    }

    public void put(RecFuncQCResult result) throws SQLException {
        int index = 1;
        try {
            get(result.getRecFunc_id());
            // exists so update
            update.setBoolean(index++, result.isKeep());
            update.setBoolean(index++, result.isManualOverride());
            update.setFloat(index++, result.getTransRadialRatio());
            update.setFloat(index++, result.getPMaxAmpRatio());
            update.setString(index++, result.getReason());
            update.setTimestamp(index++, result.getInsertTime());
            update.setInt(index++, result.getRecFunc_id());
            update.executeUpdate();
        } catch(NotFound e) {
            try {
            // doesn't exist, so put
            put.setInt(index++, result.getRecFunc_id());
            put.setBoolean(index++, result.isKeep());
            put.setBoolean(index++, result.isManualOverride());
            put.setFloat(index++, result.getTransRadialRatio());
            put.setFloat(index++, result.getPMaxAmpRatio());
            put.setString(index++, result.getReason());
            put.setTimestamp(index++, result.getInsertTime());
            put.executeUpdate();
            } catch (SQLException ee) {
                System.out.println("JDBCRecFuncQC.put="+put);
                throw ee;
            }
        }
    }
    
    public int delete(int dbid) throws SQLException {
        delete.setInt(1, dbid);
        return delete.executeUpdate();
    }

    public RecFuncQCResult extract(ResultSet rs) throws SQLException {
        return new RecFuncQCResult(rs.getInt("recFunc_id"),
                                   rs.getBoolean("keep"),
                                   rs.getBoolean("manualOverride"),
                                   rs.getFloat("transRadialRatio"),
                                   rs.getFloat("pMaxAmpRatio"),
                                   rs.getString("reason"),
                                   rs.getTimestamp("insertTime"));
    }

    PreparedStatement put, update, get, delete;
}
