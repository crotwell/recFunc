package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.DBUtil;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Oct 21, 2004
 */
public class JDBCSodConfig  extends JDBCTable {
    
    public JDBCSodConfig(Connection conn) throws SQLException, ConfigurationException {
        super("sodConfig", conn);
        Statement stmt = conn.createStatement();
        seq = new JDBCSequence(conn, getTableName()+"Seq");
        if(!DBUtil.tableExists(getTableName(), conn)){
            stmt.executeUpdate(ConnMgr.getSQL(getTableName()+".create"));
        }
        putStmt = conn.prepareStatement(ConnMgr.getSQL(getTableName()+".put"));
        getStmt = conn.prepareStatement(ConnMgr.getSQL(getTableName()+".get"));
    }
    
    public int put(String sodConfig) throws SQLException {
        int id = seq.next();
        putStmt.setInt(1, id);
        putStmt.setString(2, sodConfig);
        putStmt.executeUpdate();
        return id;
    }
    
    public String get(int dbid) throws SQLException, NotFound {
        getStmt.setInt(1, dbid);
        ResultSet rs = getStmt.executeQuery();
        if (rs.next()) {
            return rs.getString(1);
        }
        throw new NotFound("dbid="+dbid+" not found in sodConfig");
    }
    
    JDBCSequence seq;
    
    PreparedStatement putStmt, getStmt;
}
