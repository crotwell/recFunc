package edu.sc.seis.receiverFunction.compare;

import java.sql.Connection;
import java.sql.SQLException;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;


/**
 * @author crotwell
 * Created on Mar 25, 2005
 */
public class JDBCStationResult extends JDBCTable {

    /**
     * @throws SQLException
     *
     */
    public JDBCStationResult(Connection conn) throws SQLException {
        super("stationResult", conn);
        TableSetup.setup(this, "edu.sc.seis.receiverFunction.compare.default.prop");
        
    }
}
