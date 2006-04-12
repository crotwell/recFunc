package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.network.NetworkIdUtil;
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

    public AzimuthSumHKStack getForStation(NetworkId net,
                                    String station_code,
                                    boolean withData, float az, float width) throws NotFound,
            SQLException, IOException {
        return getForStation(jdbcSummaryHKStack.jdbcHKStack.getJDBCChannel()
                .getNetworkTable()
                .getDbId(net), station_code, withData, az, width);
    }

    public AzimuthSumHKStack getForStation(int netId,
                                    String station_code,
                                    boolean withData, float az, float width) throws NotFound,
            SQLException, IOException {
        int index = 1;
        getForStation.setInt(index++, netId);
        getForStation.setString(index++, station_code);
        getForStation.setFloat(index++, az);
        getForStation.setFloat(index++, width);
        ResultSet rs = getForStation.executeQuery();
        if(rs.next()) {
            return extract(rs, withData);
        }
        throw new NotFound("No Summary stack for " + netId + " " + station_code);
    }

    public int getDbIdForStation(NetworkId net, String station_code, float az, float width)
            throws SQLException, NotFound {
        int index = 1;
        getForStation.setInt(index++, jdbcSummaryHKStack.jdbcHKStack.getJDBCChannel()
                .getNetworkTable()
                .getDbId(net));
        getForStation.setString(index++, station_code);
        getForStation.setFloat(index++, az);
        getForStation.setFloat(index++, width);
        ResultSet rs = getForStation.executeQuery();
        if(rs.next()) {
            return rs.getInt("azimuthhksummary_id");
        }
        throw new NotFound("No Summary stack for "
                + NetworkIdUtil.toString(net) + " " + station_code);
    }

    /**
     * Gets all summary HKStacks, but without the actual stack.
     * 
     * @throws SQLException
     * @throws IOException
     * @throws NotFound
     */
    public ArrayList getAllWithoutData() throws SQLException, NotFound,
            IOException {
        ArrayList out = new ArrayList();
        ResultSet rs = getAllWithoutData.executeQuery();
        while(rs.next()) {
            out.add(extract(rs, false));
        }
        return out;
    }
    
    int populateStmt(PreparedStatement stmt, int index, AzimuthSumHKStack summary) throws SQLException, NotFound,
            IOException {
        index = jdbcSummaryHKStack.populateStmt(stmt, index, summary.getStack());
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
