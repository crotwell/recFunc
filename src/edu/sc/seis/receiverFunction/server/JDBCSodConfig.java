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
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Oct 21, 2004
 */
public class JDBCSodConfig  extends JDBCTable {
    
    public JDBCSodConfig(Connection conn) throws SQLException, ConfigurationException, Exception {
        super("sodConfig", conn);
        TableSetup.setup(getTableName(), conn, this, "edu/sc/seis/receiverFunction/server/default.props");
    }
    
    public int put(String sodConfig) throws SQLException {
        try {
            return getDbId(sodConfig);
        } catch (NotFound e) {
            // not in db yet
            int id = seq.next();
            put.setInt(1, id);
            put.setString(2, sodConfig);
            put.executeUpdate();
            return id;
        }
    }
    
    public String get(int dbid) throws SQLException, NotFound {
        get.setInt(1, dbid);
        ResultSet rs = get.executeQuery();
        if (rs.next()) {
            return rs.getString(1);
        }
        throw new NotFound("dbid="+dbid+" not found in sodConfig");
    }
    
    public int getDbId(String sodConfig) throws SQLException, NotFound {
        getDbId.setString(1, sodConfig);
        ResultSet rs = getDbId.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        throw new NotFound("dbid not found for sodConfig");
    }
    
    JDBCSequence seq;
    
    PreparedStatement put, get, getDbId;
}
