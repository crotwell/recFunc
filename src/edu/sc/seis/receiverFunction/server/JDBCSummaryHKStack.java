package edu.sc.seis.receiverFunction.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.freq.Cmplx;
import edu.sc.seis.fissuresUtil.freq.CmplxArray2D;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;

/**
 * @author crotwell Created on Mar 1, 2005
 */
public class JDBCSummaryHKStack extends JDBCTable {

    public JDBCSummaryHKStack(JDBCHKStack jdbcHKStack) throws Exception {
        // super("hksummaryTMP", jdbcHKStack.getConnection());
        super("hksummary", jdbcHKStack.getConnection());
        this.jdbcHKStack = jdbcHKStack;
        hksummarySeq = new JDBCSequence(conn, getTableName() + "Seq");
        TableSetup.setup(getTableName(),
                         conn,
                         this,
                         "edu/sc/seis/receiverFunction/server/default.props");
    }

    public int calc(int netDbId,
                    String stationCode,
                    float gaussianWidth,
                    float percentMatch) throws SQLException {
        int index = 1;
        uncalculated.setInt(index++, netDbId);
        uncalculated.setString(index++, stationCode);
        uncalculated.setFloat(index++, gaussianWidth);
        uncalculated.setInt(index++, netDbId);
        uncalculated.setString(index++, stationCode);
        uncalculated.setFloat(index++, gaussianWidth);
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
            return extract(rs, true);
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
            logger.debug("put done");
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

    public int delete(int hksummary_id) throws SQLException {
        deleteStmt.setInt(1, hksummary_id);
        return deleteStmt.executeUpdate();
    }
    
    int populateStmt(PreparedStatement stmt, int index, SumHKStack summary)
            throws SQLException, NotFound, IOException {
        stmt.setInt(index++, jdbcHKStack.getJDBCChannel()
                .getDBId(summary.getChannel().get_id()));
        stmt.setInt(index++, jdbcHKStack.getJDBCChannel()
                .getDBId(summary.getChannel().get_id()));
        stmt.setInt(index++, jdbcHKStack.getJDBCChannel()
                .getDBId(summary.getChannel().get_id()));
        stmt.setFloat(index++, (float)summary.getSum()
                .getAlpha()
                .getValue(UnitImpl.KILOMETER_PER_SECOND));
        stmt.setFloat(index++, summary.getSum().getGaussianWidth());
        stmt.setFloat(index++, summary.getMinPercentMatch());
        stmt.setFloat(index++, (float)summary.getSmallestH()
                .convertTo(UnitImpl.KILOMETER)
                .getValue());
        stmt.setFloat(index++, (float)summary.getSum()
                .getStepH()
                .convertTo(UnitImpl.KILOMETER)
                .getValue());
        stmt.setInt(index++, summary.getSum().getNumH());
        stmt.setFloat(index++, summary.getSum().getMinK());
        stmt.setFloat(index++, summary.getSum().getStepK());
        stmt.setInt(index++, summary.getSum().getNumK());
        stmt.setFloat(index++, (float)summary.getSum()
                .getMaxValueH()
                .convertTo(UnitImpl.KILOMETER)
                .getValue());
        stmt.setFloat(index++, summary.getSum().getMaxValueK());
        stmt.setFloat(index++, summary.getSum().getMaxValue());
        stmt.setFloat(index++, summary.getSum().getWeightPs());
        stmt.setFloat(index++, summary.getSum().getWeightPpPs());
        stmt.setFloat(index++, summary.getSum().getWeightPsPs());
        float[][] data = summary.getSum().getStack();
        ByteArrayOutputStream real = new ByteArrayOutputStream();
        DataOutputStream realdos = new DataOutputStream(real);
        for(int i = 0; i < data.length; i++) {
            for(int j = 0; j < data[0].length; j++) {
                realdos.writeFloat(data[i][j]);
            }
        }
        byte[] valBytes = real.toByteArray();
        stmt.setBytes(index++, valBytes);
        stmt.setTimestamp(index++, ClockUtil.now().getTimestamp());
        stmt.setFloat(index++, (float)summary.getHVariance());
        stmt.setFloat(index++, (float)summary.getKVariance());
        stmt.setInt(index++, summary.getNumEQ());
        return index;
    }

    public SumHKStack extract(ResultSet rs, boolean withData) throws NotFound,
            SQLException, IOException {
        Channel chan = jdbcHKStack.getJDBCChannel().get(rs.getInt("chanz_id"));
        int numH = rs.getInt("numH");
        int numK = rs.getInt("numK");
        HKStack stack;
        if(withData) {
            float[][] data = new float[numH][numK];
            byte[] dataBytes = rs.getBytes("stack");
            DataInputStream realdis = new DataInputStream(new ByteArrayInputStream(dataBytes));
            for(int i = 0; i < data.length; i++) {
                for(int j = 0; j < data[0].length; j++) {
                    data[i][j] = (float)realdis.readFloat();
                }
            }
            realdis.close();
            stack = new HKStack(new QuantityImpl(rs.getFloat("alpha"),
                                                 UnitImpl.KILOMETER_PER_SECOND),
                                0,
                                rs.getFloat("gwidth"),
                                rs.getFloat("minPercentMatch"),
                                new QuantityImpl(rs.getFloat("minH"),
                                                 UnitImpl.KILOMETER),
                                new QuantityImpl(rs.getFloat("stepH"),
                                                 UnitImpl.KILOMETER),
                                numH,
                                rs.getFloat("minK"),
                                rs.getFloat("stepK"),
                                numK,
                                rs.getFloat("weightPs"),
                                rs.getFloat("weightPpPs"),
                                rs.getFloat("weightPsPs"),
                                data,
                                chan);
        } else {
            stack = new HKStack(new QuantityImpl(rs.getFloat("alpha"),
                                                 UnitImpl.KILOMETER_PER_SECOND),
                                0,
                                rs.getFloat("gwidth"),
                                rs.getFloat("minPercentMatch"),
                                new QuantityImpl(rs.getFloat("minH"),
                                                 UnitImpl.KILOMETER),
                                new QuantityImpl(rs.getFloat("stepH"),
                                                 UnitImpl.KILOMETER),
                                numH,
                                rs.getFloat("minK"),
                                rs.getFloat("stepK"),
                                numK,
                                rs.getFloat("weightPs"),
                                rs.getFloat("weightPpPs"),
                                rs.getFloat("weightPsPs"),
                                new QuantityImpl(rs.getFloat("peakH"),
                                                 UnitImpl.KILOMETER),
                                new Float(rs.getFloat("peakK")),
                                new Float(rs.getFloat("peakVal")),
                                chan);
        }
        SumHKStack sum = new SumHKStack(rs.getFloat("minPercentMatch"),
                                        new QuantityImpl(rs.getFloat("minH"),
                                                         UnitImpl.KILOMETER),
                                        stack,
                                        rs.getFloat("hVariance"),
                                        rs.getFloat("kVariance"),
                                        rs.getInt("numEQ"));
        sum.setDbid(rs.getInt("hksummary_id"));
        sum.setComplexityResult(JDBCStackComplexity.extract(rs, sum.getDbid()));
        return sum;
    }

    public SumHKStack getForStation(NetworkId net,
                                    String station_code,
                                    float gaussianWidth,
                                    float percentMatch,
                                    boolean withData) throws NotFound,
            SQLException, IOException {
        return getForStation(jdbcHKStack.getJDBCChannel()
                                     .getNetworkTable()
                                     .getDbId(net),
                             station_code,
                             gaussianWidth,
                             percentMatch,
                             withData);
    }

    public SumHKStack getForStation(int netId,
                                    String station_code,
                                    float gaussianWidth,
                                    float percentMatch,
                                    boolean withData) throws NotFound,
            SQLException, IOException {
        try {
            int index = 1;
            getForStation.setInt(index++, netId);
            getForStation.setString(index++, station_code);
            getForStation.setFloat(index++, gaussianWidth);
            getForStation.setFloat(index++, percentMatch);
            ResultSet rs = getForStation.executeQuery();
            if(rs.next()) {
                return extract(rs, withData);
            }
        } catch(SQLException e) {
            SQLException s = new SQLException(e.getMessage() + " sql="
                    + getForStation.toString());
            s.initCause(e);
            throw s;
        }
        throw new NotFound("No Summary stack for " + netId + " " + station_code);
    }

    public int getDbIdForStation(NetworkId net,
                                 String station_code,
                                 float gaussianWidth,
                                 float percentMatch) throws SQLException,
            NotFound {
        int index = 1;
        getForStation.setInt(index++, jdbcHKStack.getJDBCChannel()
                .getNetworkTable()
                .getDbId(net));
        getForStation.setString(index++, station_code);
        getForStation.setFloat(index++, gaussianWidth);
        getForStation.setFloat(index++, percentMatch);
        ResultSet rs = getForStation.executeQuery();
        if(rs.next()) {
            return rs.getInt("hksummary_id");
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
        try {
            ResultSet rs = getAllWithoutData.executeQuery();
            while(rs.next()) {
                out.add(extract(rs, false));
            }
        } catch(SQLException e) {
            GlobalExceptionHandler.handle("" + getAllWithoutData, e);
        }
        return out;
    }

    public HKSummaryIterator getAllIterator() throws SQLException {
        boolean autoCommit = conn.getAutoCommit();
        getConnection().setAutoCommit(false);
        ResultSet rs = getAll.executeQuery();
        HKSummaryIterator iter = new HKSummaryIterator(rs, this, autoCommit);
        return iter;
    }

    JDBCHKStack jdbcHKStack;

    JDBCSequence hksummarySeq;

    PreparedStatement uncalculated, get, put, update, getForStation,
            getAllWithoutData, getAll, deleteStmt;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCSummaryHKStack.class);
}