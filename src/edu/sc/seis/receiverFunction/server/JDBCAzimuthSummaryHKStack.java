package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.receiverFunction.AzimuthSumHKStack;

public class JDBCAzimuthSummaryHKStack extends JDBCTable {

    public JDBCAzimuthSummaryHKStack(JDBCSummaryHKStack jdbcSummaryHKStack) throws Exception {
        super("azimuthhksummary", jdbcSummaryHKStack.getConnection());
        this.jdbcSummaryHKStack = jdbcSummaryHKStack;
        TableSetup.setup(getTableName(), conn, this, "edu/sc/seis/receiverFunction/server/default.props");
        azimuthhksummarySeq = new JDBCSequence(conn, getTableName() + "Seq");
    }

    public AzimuthSumHKStack get(int id) throws NotFound, SQLException, IOException {
        get.setInt(1, id);
        ResultSet rs = get.executeQuery();
        if(rs.next()) {
            return extract(rs, true);
        } else {
            throw new NotFound();
        }
    }

    public int put(AzimuthSumHKStack summary) throws SQLException, NotFound,
            IOException {
        try {
            int index = 1;
            int hksummary_id = azimuthhksummarySeq.next();
            put.setInt(index++, hksummary_id);
            index = populateStmt(put, index, summary);
            put.executeUpdate();
            logger.debug("put done");
            return hksummary_id;
        } catch(SQLException e) {
            logger.warn("stmt = " + put, e);
            throw e;
        }
    }

    public int update(int hksummary_id, AzimuthSumHKStack summary)
            throws SQLException, NotFound, IOException {
        try {
            int index = 1;
            index = populateStmt(update, index, summary);
            update.setInt(index++, hksummary_id);
            update.executeUpdate();
            return hksummary_id;
        } catch(SQLException e) {
            logger.warn("stmt = " + update, e);
            throw e;
        }
    }
    
    int populateStmt(PreparedStatement stmt, int index, AzimuthSumHKStack summary) throws SQLException, NotFound,
            IOException {
        jdbcSummaryHKStack.populateStmt(stmt, index, summary.getStack());
        stmt.setFloat(index++, summary.getAzimuth());
        stmt.setFloat(index++, summary.getAzWidth());
        return index;
    }

    public AzimuthSumHKStack extract(ResultSet rs, boolean withData) throws NotFound, SQLException, IOException {
        AzimuthSumHKStack stack = new AzimuthSumHKStack(jdbcSummaryHKStack.extract(rs, withData),
                                                        rs.getFloat("azimuth"),
                                                        rs.getFloat("azWidth"));
        return stack;
    }

    JDBCSummaryHKStack jdbcSummaryHKStack;

    JDBCSequence azimuthhksummarySeq;

    PreparedStatement uncalculated, get, put, update, getForStation,
            getAllWithoutData;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCAzimuthSummaryHKStack.class);
}
