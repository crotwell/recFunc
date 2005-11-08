package edu.sc.seis.receiverFunction.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;

public class JDBCStackComplexity extends JDBCTable {

    public JDBCStackComplexity(JDBCSummaryHKStack jdbcSummaryHKStack) throws SQLException {
        super("stackComplexity", jdbcSummaryHKStack.getConnection());
        this.jdbcSummaryHKStack = jdbcSummaryHKStack;
        TableSetup.setup(getTableName(), conn, this, "edu/sc/seis/receiverFunction/server/default.props");
    }

    public void put(int hksummary_id, float complexity) throws SQLException {
        try {
            get(hksummary_id);
            // already an entry so update
            update.setFloat(1, complexity);
            update.setInt(2, hksummary_id);
            update.executeUpdate();
        } catch(NotFound e) {
            put.setInt(1, hksummary_id);
            put.setFloat(2, complexity);
            put.executeUpdate();
        }
    }

    public float get(int hksummary_id) throws NotFound, SQLException {
        get.setInt(1, hksummary_id);
        ResultSet rs = get.executeQuery();
        if(rs.next()) {
            return rs.getFloat(1);
        }
        throw new NotFound();
    }

    PreparedStatement put, get, update;

    private JDBCSummaryHKStack jdbcSummaryHKStack;
}
