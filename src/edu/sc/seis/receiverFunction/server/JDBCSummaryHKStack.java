package edu.sc.seis.receiverFunction.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;

/**
 * @author crotwell Created on Mar 1, 2005
 */
public class JDBCSummaryHKStack extends JDBCTable {

    public JDBCSummaryHKStack(JDBCHKStack jdbcHKStack) throws Exception {
        super("hksummary", jdbcHKStack.getConnection());
        this.jdbcHKStack = jdbcHKStack;
        hksummarySeq = new JDBCSequence(conn, getTableName() + "Seq");
        TableSetup.setup(getTableName(),
                         conn,
                         this,
                         "edu/sc/seis/receiverFunction/server/default.props");
        TableSetup.setup("hkstackToSummary",
                         conn,
                         new Object(),
                         "edu/sc/seis/receiverFunction/server/default.props");
    }

    public int calc(int netDbId, String stationCode, float percentMatch)
            throws SQLException {
        int index = 1;
        uncalculated.setInt(index++, netDbId);
        uncalculated.setString(index++, stationCode);
        uncalculated.setInt(index++, netDbId);
        uncalculated.setString(index++, stationCode);
        ResultSet rs = uncalculated.executeQuery();
        while(rs.next()) {
            System.out.println("uncalc receiver function id=" + rs.getInt(1));
        }
        return -1;
    }

    public SumHKStack get(int id) throws NotFound, SQLException, IOException {
        get.setInt(1, id);
        ResultSet rs = get.executeQuery();
        if(rs.next()) {
            return extract(rs);
        } else {
            throw new NotFound();
        }
    }

    public int put(SumHKStack summary) throws SQLException, NotFound,
            IOException {
        try {
            int index = 1;
            int hksummary_id = hksummarySeq.next();
            put.setInt(index++, hksummary_id);
            index = populateStmt(put, index, summary);
            put.executeUpdate();
            return hksummary_id;
        } catch(SQLException e) {
            logger.warn("stmt = " + put, e);
            throw e;
        }
    }

    public int update(int hksummary_id, SumHKStack summary)
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

    int populateStmt(PreparedStatement stmt, int index, SumHKStack summary)
            throws SQLException, NotFound, IOException {
        stmt.setInt(index++, jdbcHKStack.getJDBCChannel()
                .getDBId(summary.getChannel().get_id()));
        stmt.setInt(index++, jdbcHKStack.getJDBCChannel()
                .getDBId(summary.getChannel().get_id()));
        stmt.setInt(index++, jdbcHKStack.getJDBCChannel()
                .getDBId(summary.getChannel().get_id()));
        stmt.setFloat(index++, summary.getSum().getAlpha());
        stmt.setFloat(index++, summary.getMinPercentMatch());
        stmt.setFloat(index++, summary.getSmallestH());
        stmt.setFloat(index++, summary.getSum().getStepH());
        stmt.setInt(index++, summary.getSum().getNumH());
        stmt.setFloat(index++, summary.getSum().getMinK());
        stmt.setFloat(index++, summary.getSum().getStepK());
        stmt.setInt(index++, summary.getSum().getNumK());
        stmt.setFloat(index++, summary.getSum().getMaxValueH());
        stmt.setFloat(index++, summary.getSum().getMaxValueK());
        stmt.setFloat(index++, summary.getSum().getMaxValue());
        stmt.setFloat(index++, summary.getSum().getWeightPs());
        stmt.setFloat(index++, summary.getSum().getWeightPpPs());
        stmt.setFloat(index++, summary.getSum().getWeightPsPs());
        float[][] data = summary.getSum().getStack();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        for(int i = 0; i < data.length; i++) {
            for(int j = 0; j < data[i].length; j++) {
                dos.writeFloat(data[i][j]);
            }
        }
        byte[] valBytes = out.toByteArray();
        stmt.setBytes(index++, valBytes);
        stmt.setTimestamp(index++, ClockUtil.now().getTimestamp());
        stmt.setFloat(index++, (float)summary.getHVariance());
        stmt.setFloat(index++, (float)summary.getKVariance());
        return index;
    }

    public SumHKStack extract(ResultSet rs) throws NotFound, SQLException,
            IOException {
        Channel chan = jdbcHKStack.getJDBCChannel().get(rs.getInt("chanZ_id"));
        int numH = rs.getInt("numH");
        int numK = rs.getInt("numK");
        float[][] data = jdbcHKStack.extractData(rs, numH, numK);
        HKStack stack = new HKStack(rs.getFloat("alpha"),
                                    0,
                                    rs.getFloat("minPercentMatch"),
                                    rs.getFloat("minH"),
                                    rs.getFloat("stepH"),
                                    numH,
                                    rs.getFloat("minK"),
                                    rs.getFloat("stepK"),
                                    numK,
                                    rs.getFloat("weightPs"),
                                    rs.getFloat("weightPpPs"),
                                    rs.getFloat("weightPsPs"),
                                    data,
                                    chan);
        SumHKStack sum = new SumHKStack(rs.getFloat("minPercentMatch"),
                                        rs.getFloat("minH"),
                                        stack,
                                        rs.getFloat("hVariance"),
                                        rs.getFloat("kVariance"));
        return sum;
    }

    public int getDbIdForStation(NetworkId net, String station_code)
            throws SQLException, NotFound {
        int index = 1;
        getForStation.setInt(index++, jdbcHKStack.getJDBCChannel()
                .getNetworkTable()
                .getDbId(net));
        getForStation.setString(index++, station_code);
        ResultSet rs = getForStation.executeQuery();
        if(rs.next()) { return rs.getInt("hksummary_id"); }
        throw new NotFound("No Summary stack for "
                + NetworkIdUtil.toString(net) + " " + station_code);
    }

    JDBCHKStack jdbcHKStack;

    JDBCSequence hksummarySeq;

    PreparedStatement uncalculated, get, put, update, getForStation;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCSummaryHKStack.class);
}