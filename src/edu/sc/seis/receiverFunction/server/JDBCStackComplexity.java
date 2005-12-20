package edu.sc.seis.receiverFunction.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;

public class JDBCStackComplexity extends JDBCTable {

	public JDBCStackComplexity(JDBCSummaryHKStack jdbcSummaryHKStack)
			throws SQLException {
		super("stackComplexity", jdbcSummaryHKStack.getConnection());
		this.jdbcSummaryHKStack = jdbcSummaryHKStack;
		TableSetup.setup(getTableName(), conn, this,
				"edu/sc/seis/receiverFunction/server/default.props");
	}

	public void put(int hksummary_id, float complexity, float complexity25,
			float complexity50, float bestH, float bestHStdDev, float bestK,
			float bestKStdDev, float bestVal, float covar, float nextH,
			float nextK, float nextVal, float crust2diff) throws SQLException {
		try {
			get(hksummary_id);
			// already an entry so update
			int index = 1;
			update.setFloat(index++, complexity);
			update.setFloat(index++, complexity25);
			update.setFloat(index++, complexity50);
			update.setFloat(index++, bestH);
			update.setFloat(index++, bestHStdDev);
			update.setFloat(index++, bestK);
			update.setFloat(index++, bestKStdDev);
			update.setFloat(index++, bestVal);
			update.setFloat(index++, covar);
			update.setFloat(index++, nextH);
			update.setFloat(index++, nextK);
			update.setFloat(index++, nextVal);
			update.setFloat(index++, crust2diff);
			update.setInt(index++, hksummary_id);
			update.executeUpdate();
		} catch (NotFound e) {
			int index = 1;
			put.setInt(index++, hksummary_id);
			put.setFloat(index++, complexity);
			put.setFloat(index++, complexity25);
			put.setFloat(index++, complexity50);
			put.setFloat(index++, bestH);
			put.setFloat(index++, bestHStdDev);
			put.setFloat(index++, bestK);
			put.setFloat(index++, bestKStdDev);
			put.setFloat(index++, bestVal);
			put.setFloat(index++, covar);
			put.setFloat(index++, nextH);
			put.setFloat(index++, nextK);
			put.setFloat(index++, nextVal);
			put.setFloat(index++, crust2diff);
			put.executeUpdate();
		}
	}

	/**
	 * @returns complexity for floor of 0, .25 and .50 respectively
	 */
	public StackComplexityResult get(int hksummary_id) throws NotFound, SQLException {
        get.setInt(1, hksummary_id);
        ResultSet rs = get.executeQuery();
        if(rs.next()) {
        	StackComplexityResult out = new StackComplexityResult(
        			hksummary_id,
            rs.getFloat("complexity"),
            rs.getFloat("complexity25"),
            rs.getFloat("complexity50"),
            rs.getFloat("bestH"),
            rs.getFloat("bestHStdDev"),
            rs.getFloat("bestK"),
            rs.getFloat("bestKStdDev"),
            rs.getFloat("bestVal"),
            rs.getFloat("hkCorrelation"),
            rs.getFloat("nextH"),
            rs.getFloat("nextK"),
            rs.getFloat("nextVal"),
            rs.getFloat("crust2diff"));
            return out;
        }
        throw new NotFound();
    }
	PreparedStatement put, get, update;

	private JDBCSummaryHKStack jdbcSummaryHKStack;
}
