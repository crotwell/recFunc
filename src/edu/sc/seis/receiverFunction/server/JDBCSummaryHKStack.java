package edu.sc.seis.receiverFunction.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;


/**
 * @author crotwell
 * Created on Mar 1, 2005
 */
public class JDBCSummaryHKStack extends JDBCTable {

    public JDBCSummaryHKStack(JDBCHKStack jdbcHKStack) throws Exception {
        super("hksummary", jdbcHKStack.getConnection());
        this.jdbcHKStack = jdbcHKStack;
        hksummarySeq = new JDBCSequence(conn, getTableName()+"Seq");
        TableSetup.setup(getTableName(),
                         conn,
                         this,
                         "edu/sc/seis/receiverFunction/server/default.props");
        TableSetup.setup("htstackToSummary", conn, this, "edu/sc/seis/receiverFunction/server/default.props");
    }
    
    public int calc(int netDbId, String stationCode, float percentMatch) throws SQLException {
        int index = 1;
        uncalculated.setInt(index++, netDbId);
        uncalculated.setString(index++, stationCode);
        uncalculated.setInt(index++, netDbId);
        uncalculated.setString(index++, stationCode);
        ResultSet rs = uncalculated.executeQuery();
        while (rs.next()) {
            System.out.println("uncalc receiver function id="+rs.getInt(1));
        }
        return -1;
    }
    
    public SumHKStack get(int id) throws NotFound, SQLException, IOException {
        get.setInt(1, id);
        ResultSet rs = get.executeQuery();
        if (rs.next()) {
            return extract(rs);
        } else {
            throw new NotFound();
        }
    }
    
    public int put(SumHKStack summary) throws SQLException, NotFound, IOException {
        int index = 1;
        int hksummary_id = hksummarySeq.next();
        put.setInt(index++, hksummary_id);
        put.setInt(index++, jdbcHKStack.getJDBCChannel().getDBId(summary.getChannel().get_id()));
        put.setInt(index++, jdbcHKStack.getJDBCChannel().getDBId(summary.getChannel().get_id()));
        put.setInt(index++, jdbcHKStack.getJDBCChannel().getDBId(summary.getChannel().get_id()));
        put.setFloat(index++, summary.getSum().getAlpha());
        put.setFloat(index++, summary.getMinPercentMatch());
        put.setFloat(index++, summary.getSmallestH());
        put.setInt(index++, summary.getSum().getNumH());
        put.setFloat(index++, summary.getSum().getStepH());
        
        put.setFloat(index++, summary.getSum().getMinK());
        put.setInt(index++, summary.getSum().getNumK());
        put.setFloat(index++, summary.getSum().getStepK());

        put.setFloat(index++, summary.getSum().getMaxValueH());
        put.setFloat(index++, summary.getSum().getMaxValueK());
        put.setFloat(index++, summary.getSum().getMaxValue());

        put.setFloat(index++, summary.getSum().getWeightPs());
        put.setFloat(index++, summary.getSum().getWeightPpPs());
        put.setFloat(index++, summary.getSum().getWeightPsPs());
        
        float[][] data = summary.getSum().getStack();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        for(int i = 0; i < data.length; i++) {
            for(int j = 0; j < data[i].length; j++) {
                dos.writeFloat(data[i][j]);
            }
        }
        byte[] valBytes = out.toByteArray();
        put.setBytes(index++, valBytes);
        
        put.setFloat(index++, (float)summary.getHVariance());
        put.setFloat(index++, (float)summary.getKVariance());
        
        put.executeUpdate();
        return hksummary_id;
    }
    
    public SumHKStack extract(ResultSet rs) throws NotFound, SQLException, IOException {
        Channel chan = jdbcHKStack.getJDBCChannel().get(rs.getInt("chanZ_id"));
        int numH = rs.getInt("numH");
        int numK = rs.getInt("numK");
        float[][] data = jdbcHKStack.extractData(rs, numH, numK);
        HKStack stack = new HKStack(rs.getFloat("alpha"),
                                  rs.getFloat("p"),
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
                                        rs.getFloat("smallestH"),
                                        stack,
                                        rs.getFloat("hVariance"),
                                        rs.getFloat("kVariance"));
        return sum;
    }
    
    JDBCHKStack jdbcHKStack;
    
    JDBCSequence hksummarySeq;
    
    PreparedStatement uncalculated, get, put;
}
