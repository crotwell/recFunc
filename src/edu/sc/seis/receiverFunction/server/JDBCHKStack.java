package edu.sc.seis.receiverFunction.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.freq.CmplxArray2D;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.EventFormatter;

/**
 * @author crotwell Created on Sep 20, 2004
 */
public class JDBCHKStack extends JDBCTable {

	public JDBCHKStack(JDBCRecFunc jdbcRecFunc) throws SQLException,
			ConfigurationException, TauModelException, Exception {
		this(jdbcRecFunc.getConnection(), jdbcRecFunc.getJDBCEventAccess(),
				jdbcRecFunc.getJDBCChannel(), jdbcRecFunc.getJDBCSodConfig(),
				jdbcRecFunc);
	}

	public JDBCHKStack(Connection conn, JDBCEventAccess jdbcEventAccess,
			JDBCChannel jdbcChannel, JDBCSodConfig jdbcSodConfig,
			JDBCRecFunc jdbcRecFunc) throws SQLException,
			ConfigurationException, TauModelException, Exception {
		super("hkstack", conn);
		this.jdbcEventAccess = jdbcEventAccess;
		this.jdbcChannel = jdbcChannel;
		this.jdbcRecFunc = jdbcRecFunc;
		hkstackSeq = new JDBCSequence(conn, getTableName() + "Seq");
		TableSetup.setup(getTableName(), conn, this,
				"edu/sc/seis/receiverFunction/server/default.props");
		crust2 = new Crust2();
		dataDir = new File(RecFuncCacheImpl.getDataLoc());
		dataDir.mkdirs();
		eventFormatter = new EventFormatter(true);
		getForStation.setFetchSize(50);
		getForStation.setFetchDirection(ResultSet.FETCH_FORWARD);
	}

	public HKStack get(int recfunc_id) throws IOException, SQLException,
			NotFound {
		get.setInt(1, recfunc_id);
		ResultSet rs = get.executeQuery();
		if (rs.next()) {
			return extract(rs);
		} else {
			rs.close();
			throw new NotFound();
		}
	}

	public int put(CachedResultPlusDbId recfuncResult, HKStack stack)
			throws SQLException, IOException {
        return put(recfuncResult, stack, true);
    }

    public int put(CachedResultPlusDbId recfuncResult, HKStack stack, boolean forceDeleteAnalyticData)
            throws SQLException, IOException {
		int hkstack_id = hkstackSeq.next();
		CachedResult rfResult = recfuncResult.getCachedResult();
		File datadir = jdbcRecFunc.getDir(recfuncResult.getEvent(),
				rfResult.channels[0], rfResult.config.gwidth);
		File analyticData = new File(datadir, ANALYTIC_DATA);
		if (analyticData.exists()) {
            if (forceDeleteAnalyticData) {
                logger.warn("Analytic data already exists, deleteing: "+analyticData);
                analyticData.delete();
            } else {
			throw new IOException(analyticData + " already exists...");
            }
		}
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(analyticData)));
		CmplxArray2D[] dataByPhase = new CmplxArray2D[] {
				stack.getAnalyticPs(), stack.getAnalyticPpPs(),
				stack.getAnalyticPsPs() };
		if(dataByPhase[0].getXLength() != stack.getNumH()) {
			throw new IllegalArgumentException("must be same: nulH="+stack.getNumH()+"  dataByPhase="+dataByPhase[0].getXLength());
		}
		if(dataByPhase[0].getYLength() != stack.getNumK()) {
			throw new IllegalArgumentException("must be same: numK="+stack.getNumK()+"  dataByPhase="+dataByPhase[0].getYLength());
		}
		for (int p = 0; p < dataByPhase.length; p++) {
			for (int i = 0; i < dataByPhase[p].getXLength(); i++) {
				for (int j = 0; j < dataByPhase[p].getYLength(); j++) {
					out.writeFloat(dataByPhase[p].getReal(i, j));
					out.writeFloat(dataByPhase[p].getImag(i, j));
				}
			}
		}
		out.close();
		// double check to make sure everything is there...

		// 3 arrays, 2 floats per entry, numH x numK, 4 bytes per float
		int expectedSize = 3*2*4*stack.getNumH()*stack.getNumK();
		if (analyticData.length() != expectedSize) {
			throw new IOException("File size is incorrect file="+analyticData.length()+" expected="+expectedSize+"  "+analyticData);
		}
		
		int index = 1;
		put.setInt(index++, hkstack_id);
		put.setInt(index++, recfuncResult.getDbId());
		put.setFloat(index++, (float) stack.getAlpha().getValue(
				UnitImpl.KILOMETER_PER_SECOND));
		put.setFloat(index++, stack.getP());
		put.setFloat(index++, stack.getPercentMatch());
		put.setFloat(index++, (float) stack.getMinH().getValue());
		put.setFloat(index++, (float) stack.getStepH().getValue());
		put.setInt(index++, stack.getNumH());
		put.setFloat(index++, stack.getMinK());
		put.setFloat(index++, stack.getStepK());
		put.setInt(index++, stack.getNumK());
		QuantityImpl peakH;
		float peakK, peakVal = 0;
		peakH = stack.getMaxValueH();
		peakK = stack.getMaxValueK();
		peakVal = stack.getMaxValue();
		put.setFloat(index++, (float) peakH.getValue(UnitImpl.KILOMETER));
		put.setFloat(index++, peakK);
		put.setFloat(index++, peakVal);
		put.setFloat(index++, stack.getWeightPs());
		put.setFloat(index++, stack.getWeightPpPs());
		put.setFloat(index++, stack.getWeightPsPs());
		put.setTimestamp(index++, ClockUtil.now().getTimestamp());
		try {
			put.executeUpdate();
		} catch (SQLException e) {
			logger.error("SQL stmt: " + put.toString());
			throw e;
		}
		getConnection().commit();
		return hkstack_id;
	}

	public void deleteForRecFuncId(int recfunc_id) throws SQLException,
			NotFound, IOException {
		get.setInt(1, recfunc_id);
		ResultSet rs = get.executeQuery();
		if (rs.next()) {
			Channel[] channels = jdbcRecFunc.extractChannels(rs);
			CachedResultPlusDbId recFunc = jdbcRecFunc
					.extractWithoutSeismograms(rs);
			File datadir = jdbcRecFunc.getDir(recFunc.getEvent(), channels[0],
					recFunc.getCachedResult().config.gwidth);
			File analyticData = new File(datadir, "AnalyticPlotData");
			rs.close();
			if (analyticData.exists()) {
				analyticData.delete();
			}
			deleteStmt.setInt(1, recfunc_id);
			System.out.println("Deleting: " + deleteStmt);
			try {
				int numChanged = deleteStmt.executeUpdate();
				System.out.println("deleteForRecFuncId " + recfunc_id
						+ " deleted=" + numChanged);
			} catch (SQLException e) {
				logger.error("statement causeing error: " + deleteStmt);
				throw e;
			}
		} else {
			rs.close();
			throw new NotFound();
		}
	}

	public void calc(String netCode, String staCode, float percentMatch,
			boolean save) throws FileNotFoundException, FissuresException,
			NotFound, IOException, SQLException, TauModelException {
		calc(netCode, staCode, percentMatch, save, 1, 1, 1);
	}

	public void calc(String netCode, String staCode, float percentMatch,
			boolean save, float weightPs, float weightPpPs, float weightPsPs)
			throws FileNotFoundException, FissuresException, NotFound,
			IOException, SQLException, TauModelException {
		// get all uncalculated rows
		uncalculated.setString(1, netCode);
		uncalculated.setString(2, staCode);
		uncalculated.setFloat(3, percentMatch);
		ResultSet rs = uncalculated.executeQuery();
		calcStmt(rs, save, weightPs, weightPpPs, weightPsPs);
	}

	public void calcAll(String netCode, String staCode, float percentMatch,
			boolean save, float weightPs, float weightPpPs, float weightPsPs)
			throws FileNotFoundException, FissuresException, NotFound,
			IOException, SQLException, TauModelException {
		// get all uncalculated rows
		calcByPercent.setString(1, netCode);
		calcByPercent.setString(2, staCode);
		calcByPercent.setFloat(3, percentMatch);
		ResultSet rs = calcByPercent.executeQuery();
		calcStmt(rs, save, weightPs, weightPpPs, weightPsPs);
	}

	public void calcStmt(ResultSet rs, boolean save, float weightPs,
			float weightPpPs, float weightPsPs) throws FileNotFoundException,
			FissuresException, SQLException, TauModelException, NotFound,
			IOException {
		while (rs.next()) {
			int recFuncDbId = rs.getInt(1);
			System.out.println("Calc for " + recFuncDbId);
			HKStack stack = calc(recFuncDbId, weightPs, weightPpPs, weightPsPs,
					save);
		}
	}

	void calcAndStore(int recFuncDbId, float weightPs, float weightPpPs,
			float weightPsPs) throws TauModelException, FileNotFoundException,
			FissuresException, NotFound, IOException, SQLException {
		HKStack stack = calc(recFuncDbId, weightPs, weightPpPs, weightPsPs,
				true);
	}

    public HKStack calc(int recFuncDbId, boolean save) throws TauModelException,
            FileNotFoundException, FissuresException, NotFound, IOException,
            SQLException {
        return calc(recFuncDbId, DEFAULT_WEIGHT_Ps, DEFAULT_WEIGHT_PpPs, DEFAULT_WEIGHT_PsPs, save);
    }
    
    public HKStack calc(int recFuncDbId, float weightPs, float weightPpPs,
                        float weightPsPs, boolean save) throws TauModelException,
                        FileNotFoundException, FissuresException, NotFound, IOException,
                        SQLException {
		CachedResultPlusDbId cachedResult = jdbcRecFunc.get(recFuncDbId);
		HKStack stack = HKStack.create(cachedResult.getCachedResult(),
				weightPs, weightPpPs, weightPsPs);
		System.out
				.println("Stack calc for "
						+ ChannelIdUtil.toStringNoDates(cachedResult
								.getCachedResult().channels[2]));
		if (save) {
			int hkstack_id = put(cachedResult, stack);
		}
		return stack;
	}

	public ArrayList getForStation(String netCode, String staCode,
			float gaussianWidth, float percentMatch) throws FissuresException,
			NotFound, IOException, SQLException {
		return getForStation(netCode, staCode, gaussianWidth, percentMatch,
				false);
	}

	public ArrayList getForStation(String netCode, String staCode,
			float gaussianWidth, float percentMatch, boolean compact)
			throws FissuresException, NotFound, IOException, SQLException {
		ArrayList individualHK = new ArrayList();
		HKStackIterator it = getIteratorForStation(netCode, staCode,
				gaussianWidth, percentMatch, compact);
		while (it.hasNext()) {
			HKStack stack = (HKStack) it.next();
			if (compact) {
				stack.compact();
			}
			individualHK.add(stack);
		}
		it.close();
		return individualHK;
	}

	public HKStackIterator getIteratorForStation(String netCode,
			String staCode, float gaussinWidth, float minPercentMatch,
			boolean compact) throws SQLException {
		return getIteratorForStation(netCode, staCode, gaussinWidth,
				minPercentMatch, compact, false);
	}

	public HKStackIterator getIteratorForStation(String netCode,
			String staCode, float gaussinWidth, float minPercentMatch,
			boolean compact, boolean withRadialSeismogram) throws SQLException {
		int index = 1;
		getForStation.setString(index++, netCode);
		getForStation.setString(index++, staCode);
		getForStation.setFloat(index++, gaussinWidth);
		getForStation.setFloat(index++, minPercentMatch);
		boolean autoCommit = conn.getAutoCommit();
		getConnection().setAutoCommit(false);
		ResultSet rs = getForStation.executeQuery();
		HKStackIterator iter = new HKStackIterator(rs, this, autoCommit,
				withRadialSeismogram);
		return iter;
	}

	public HKStack extract(ResultSet rs) throws NotFound, IOException,
			SQLException {
		return extract(rs, false);
	}

	public HKStack extract(ResultSet rs, boolean compact) throws NotFound,
			IOException, SQLException {
		return extract(rs, compact, false);
	}

	public HKStack extract(ResultSet rs, boolean compact, boolean withRadialSeis)
			throws NotFound, IOException, SQLException {
		Channel[] channels = jdbcRecFunc.extractChannels(rs);
		CachedResultPlusDbId recFunc;
		if (withRadialSeis) {
			try {
				recFunc = jdbcRecFunc.extract(rs);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(
						"Problem getting receiver function from file", e);
			} catch (FissuresException e) {
				throw new RuntimeException(
						"Problem getting receiver function from file", e);
			}
		} else {
			recFunc = jdbcRecFunc.extractWithoutSeismograms(rs);
		}
		int numH = rs.getInt("numH");
		int numK = rs.getInt("numK");
		File datadir = jdbcRecFunc.getDir(recFunc.getEvent(), channels[0],
				recFunc.getCachedResult().config.gwidth);
		File analyticData = new File(datadir, ANALYTIC_DATA);
		HKStack out;
        // 3 arrays, 2 floats per entry, numH x numK, 4 bytes per float
        int expectedSize = 3*2*4*numH*numK;
		if (analyticData.canRead() && analyticData.length() == expectedSize) {
			try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(
					new FileInputStream(analyticData)));
			CmplxArray2D[] dataByPhase = new CmplxArray2D[3];
			for (int p = 0; p < dataByPhase.length; p++) {
				dataByPhase[p] = new CmplxArray2D(numH, numK);
				for (int i = 0; i < dataByPhase[p].getXLength(); i++) {
					for (int j = 0; j < dataByPhase[p].getYLength(); j++) {
						try {
						dataByPhase[p].setReal(i, j, in.readFloat());
						dataByPhase[p].setImag(i, j, in.readFloat());
						}catch(EOFException e) {
							logger.error("p="+p+" i="+i+" j="+j);
							throw e;
						}
					}
				}
			}
			out = new HKStack(new QuantityImpl(rs.getFloat("alpha"),
					UnitImpl.KILOMETER_PER_SECOND), rs.getFloat("p"), rs
					.getFloat("gwidth"), rs.getFloat("percentMatch"),
					new QuantityImpl(rs.getFloat("minH"), UnitImpl.KILOMETER),
					new QuantityImpl(rs.getFloat("stepH"), UnitImpl.KILOMETER),
					numH, rs.getFloat("minK"), rs.getFloat("stepK"), numK, rs
							.getFloat("weightPs"), rs.getFloat("weightPpPs"),
					rs.getFloat("weightPsPs"), dataByPhase[0], dataByPhase[1],
					dataByPhase[2], channels[0]);
			} catch (EOFException e) {
				// bad phase file?
				logger.warn("Problem with "+analyticData, e);
				throw e;
			}
		} else {

            if (analyticData.length() != expectedSize) {
                analyticData.delete();
            }
			int rfdbid = recFunc.getDbId();
			float weightPs = rs.getFloat("weightPs");
			float weightPpPs = rs.getFloat("weightPpPs");
			float weightPsPs = rs.getFloat("weightPsPs");
			logger.error(ANALYTIC_DATA + " does not exist for rfid=" + rfdbid+"  path="+analyticData);
			// didn't read data
			Connection deleteConn = ConnMgr.createConnection();
			JDBCHKStack deleteStack;
			try {
				deleteStack = new JDBCHKStack(new JDBCRecFunc(deleteConn, RecFuncCacheImpl.getDataLoc()));
				deleteStack.deleteForRecFuncId(rfdbid);
				try {
					out = deleteStack.calc(rfdbid, weightPs, weightPpPs, weightPsPs, true);
				} catch (Throwable e) {
					FileNotFoundException ee = new FileNotFoundException(
							analyticData.toString());
					ee.initCause(e);
					throw ee;
				}
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			} finally {
                deleteConn.commit();
				deleteConn.close();
			}
		}
		out.setOrigin(recFunc.getCachedResult().prefOrigin);
		if (withRadialSeis) {
			out.setRecFunc(new MemoryDataSetSeismogram(
					(LocalSeismogramImpl) recFunc.getCachedResult().radial));
		}
		if (compact) {
			out.compact();
		}
		return out;
	}

	private PreparedStatement uncalculated, calcByPercent, put, get,
			getForStation, deleteStmt;

	private JDBCEventAccess jdbcEventAccess;

	private JDBCChannel jdbcChannel;

	private JDBCRecFunc jdbcRecFunc;

	private JDBCSequence hkstackSeq;

	private File dataDir;

	private EventFormatter eventFormatter;

	public static final String ANALYTIC_DATA = "AnalyticPlotData";

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(JDBCHKStack.class);

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out
					.println("Usage: JDBCHKStack -all or -net net [-sta station]");
			return;
		}
		float minPercentMatch = 80;
		try {
			System.out.println("calc for percent match > " + minPercentMatch
					+ " with weights of 1/3");
			calcAndSave(args, minPercentMatch, true, false, DEFAULT_WEIGHT_Ps,
					DEFAULT_WEIGHT_PpPs, DEFAULT_WEIGHT_PsPs);
		} catch (Exception e) {
			GlobalExceptionHandler.handle(e);
		}
	}

	public static void calcAndSave(String[] args, float minPercentMatch,
			boolean save, boolean forceAllCalc, float weightPs,
			float weightPpPs, float weightPsPs) throws FileNotFoundException,
			FissuresException, NotFound, IOException, TauModelException,
			SQLException, ConfigurationException, Exception {
		ConnMgr.setDB(ConnMgr.POSTGRES);
		Properties props = StackSummary.loadProps(args);
		ConnMgr.setURL(props.getProperty("URL"));
		Connection conn = ConnMgr.createConnection();
		JDBCEventAccess jdbcEventAccess = new JDBCEventAccess(conn);
		JDBCChannel jdbcChannel = new JDBCChannel(conn);
		JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
		JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn, jdbcEventAccess,
				jdbcChannel, jdbcSodConfig, props.getProperty("cormorant.servers.ears.dataloc", RecFuncCacheImpl.getDataLoc()));
		JDBCHKStack jdbcHKStack = new JDBCHKStack(conn, jdbcEventAccess,
				jdbcChannel, jdbcSodConfig, jdbcRecFunc);
		String netCode = "";
		String staCode = "";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-all")) {
				netCode = args[i];
			} else if (args[i].equals("-net")) {
				netCode = args[i + 1];
			} else if (args[i].equals("-sta")) {
				staCode = args[i + 1];
			}
		}
		if (staCode.length() > 0 && netCode.length() == 0) {
			System.err.println("If using -sta, you must also use -net netCode");
			return;
		}
		if (staCode.length() > 0) {
			System.out.println("calc for " + netCode + "." + staCode);
			if (forceAllCalc) {
				jdbcHKStack.calcAll(netCode, staCode, minPercentMatch, save,
						weightPs, weightPpPs, weightPsPs);
			} else {
				jdbcHKStack.calc(netCode, staCode, minPercentMatch, save,
						weightPs, weightPpPs, weightPsPs);
			}
		} else {
			// do all or for a net
			JDBCStation jdbcStation = jdbcHKStack.getJDBCChannel()
					.getSiteTable().getStationTable();
			JDBCNetwork jdbcNetwork = jdbcStation.getNetTable();
			NetworkId[] netId = jdbcNetwork.getAllNetworkIds();
			System.out.println("Found " + netId.length + " networks.");
			for (int i = 0; i < netId.length; i++) {
				System.out.println("Network: "
						+ NetworkIdUtil.toString(netId[i]));
				if (netCode.equals("-all")
						|| netId[i].network_code.equals(netCode)) {
					Station[] station = jdbcStation.getAllStations(netId[i]);
					for (int j = 0; j < station.length; j++) {
						System.out.println("calc for " + netId[i].network_code
								+ "." + station[j].get_code());
						try {
							jdbcHKStack.calc(netId[i].network_code, station[j]
									.get_code(), minPercentMatch, save,
									weightPs, weightPpPs, weightPsPs);
						} catch (IllegalArgumentException e) {
							System.out
									.println("Problem with receiver function, skipping station. "
											+ e);
							GlobalExceptionHandler.handle(e);
						} catch (FileNotFoundException e) {
							System.out
									.println("Problem with receiver function, skipping station. "
											+ e);
							GlobalExceptionHandler.handle(e);
						}
					}
				}
			}
		}
		if (conn != null) {
			conn.close();
		}
	}

	public JDBCChannel getJDBCChannel() {
		return jdbcChannel;
	}

	public JDBCEventAccess getJDBCEventAccess() {
		return jdbcEventAccess;
	}

	public JDBCRecFunc getJDBCRecFunc() {
		return jdbcRecFunc;
	}

	Crust2 crust2;
    
    public static final float DEFAULT_WEIGHT_Ps = 1 / 3f;
    public static final float DEFAULT_WEIGHT_PpPs = 1 / 3f;
    public static final float DEFAULT_WEIGHT_PsPs = 1 - DEFAULT_WEIGHT_Ps - DEFAULT_WEIGHT_PpPs;
}