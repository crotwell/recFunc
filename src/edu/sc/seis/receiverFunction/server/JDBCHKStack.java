package edu.sc.seis.receiverFunction.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.log4j.BasicConfigurator;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.DBUtil;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAttr;
import edu.sc.seis.fissuresUtil.database.event.JDBCOrigin;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.sac.SacTimeSeries;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.RecFunc;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.EventFormatter;

/**
 * @author crotwell Created on Sep 20, 2004
 */
public class JDBCHKStack extends JDBCTable {

    public JDBCHKStack(Connection conn, JDBCOrigin jdbcOrigin,
            JDBCEventAttr jdbcEventAttr, JDBCChannel jdbcChannel,
            JDBCSodConfig jdbcSodConfig, JDBCRecFunc jdbcRecFunc)
            throws SQLException, ConfigurationException, TauModelException, Exception {
        super("hkstack", conn);
        this.jdbcOrigin = jdbcOrigin;
        this.jdbcEventAttr = jdbcEventAttr;
        this.jdbcChannel = jdbcChannel;
        this.jdbcRecFunc = jdbcRecFunc;
        TableSetup.setup(getTableName(), conn, this, "edu/sc/seis/receiverFunction/server/default.props");
        dataDir = new File(RecFuncCacheImpl.getDataLoc());
        dataDir.mkdirs();
        eventFormatter = new EventFormatter(true);
        tauPTime = TauPUtil.getTauPUtil(modelName);
    }

    public int put(int recfunc_id, HKStack stack) throws SQLException,
            IOException {
        int hkstack_id = seq.next();
        int index = 1;
        put.setInt(index++, hkstack_id);
        put.setInt(index++, recfunc_id);
        put.setFloat(index++, stack.getAlpha());
        put.setFloat(index++, stack.getP());
        put.setFloat(index++, stack.getPercentMatch());
        put.setFloat(index++, stack.getMinH());
        put.setFloat(index++, stack.getStepH());
        put.setInt(index++, stack.getNumH());
        put.setFloat(index++, stack.getMinK());
        put.setFloat(index++, stack.getStepK());
        put.setInt(index++, stack.getNumK());
        float peakH, peakK, peakVal = 0;
        int[] indicies = stack.getMaxValueIndices();
        peakH = stack.getMinH() + stack.getStepH() * indicies[0];
        peakK = stack.getMinK() + stack.getStepK() * indicies[1];
        peakVal = stack.getStack()[indicies[0]][indicies[1]];
        put.setFloat(index++, peakH);
        put.setFloat(index++, peakK);
        put.setFloat(index++, peakVal);
        float[][] data = stack.getStack();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        for(int i = 0; i < data.length; i++) {
            for(int j = 0; j < data[i].length; j++) {
                dos.writeFloat(data[i][j]);
            }
        }
        byte[] valBytes = out.toByteArray();
        put.setBytes(index++, valBytes);
        put.setTimestamp(index++, ClockUtil.now().getTimestamp());
        try {
            put.executeUpdate();
        } catch(SQLException e) {
            logger.error("SQL stmt: " + put.toString());
            throw e;
        }
        return hkstack_id;
    }

    public ArrayList calc(String netCode,
                          String staCode,
                          float percentMatch,
                          boolean save) throws FileNotFoundException,
            FissuresException, NotFound, IOException, SQLException,
            TauModelException {
        return calc(netCode, staCode, percentMatch, save, 1, 1, 1);
    }

    public ArrayList calc(String netCode,
                          String staCode,
                          float percentMatch,
                          boolean save,
                          float weightPs,
                          float weightPpPs,
                          float weightPsPs) throws FileNotFoundException,
            FissuresException, NotFound, IOException, SQLException,
            TauModelException {
        // get all uncalculated rows
        uncalculated.setString(1, netCode);
        uncalculated.setString(2, staCode);
        uncalculated.setFloat(3, percentMatch);
        ResultSet rs = uncalculated.executeQuery();
        return calcStmt(rs, save, weightPs, weightPpPs, weightPsPs);
    }

    public ArrayList calcAll(String netCode,
                             String staCode,
                             float percentMatch,
                             boolean save,
                             float weightPs,
                             float weightPpPs,
                             float weightPsPs) throws FileNotFoundException,
            FissuresException, NotFound, IOException, SQLException,
            TauModelException {
        // get all uncalculated rows
        calcByPercent.setString(1, netCode);
        calcByPercent.setString(2, staCode);
        calcByPercent.setFloat(3, percentMatch);
        ResultSet rs = calcByPercent.executeQuery();
        return calcStmt(rs, save, weightPs, weightPpPs, weightPsPs);
    }

    public ArrayList calcStmt(ResultSet rs,
                              boolean save,
                              float weightPs,
                              float weightPpPs,
                              float weightPsPs) throws FileNotFoundException,
            FissuresException, SQLException, TauModelException, NotFound,
            IOException {
        ArrayList individualHK = new ArrayList();
        while(rs.next()) {
            int recFuncDbId = rs.getInt(1);
            System.out.println("Calc for " + recFuncDbId);
            HKStack stack = calc(recFuncDbId, weightPs, weightPpPs, weightPsPs);
            if(save) {
                int hkstack_id = put(recFuncDbId, stack);
            }
            individualHK.add(stack);
        }
        return individualHK;
    }

    HKStack calc(int recFuncDbId,
                 float weightPs,
                 float weightPpPs,
                 float weightPsPs) throws TauModelException,
            FileNotFoundException, FissuresException, NotFound, IOException,
            SQLException {
        CachedResult cachedResult = jdbcRecFunc.get(recFuncDbId);
        String[] pPhases = {"P"};
        Arrival[] arrivals = tauPTime.calcTravelTimes(cachedResult.channels[0].my_site.my_station,
                                                      cachedResult.prefOrigin,
                                                      pPhases);
        // convert radian per sec ray param into km per sec
        float kmRayParam = (float)(arrivals[0].getRayParam() / tauPTime.getTauModel()
                .getRadiusOfEarth());
        HKStack stack = new HKStack(6.3f,
                                    kmRayParam,
                                    cachedResult.radialMatch,
                                    getDefaultMinH(),
                                    .25f,
                                    240,
                                    1.6f,
                                    .0025f,
                                    200,
                                    weightPs,
                                    weightPpPs,
                                    weightPsPs,
                                    (LocalSeismogramImpl)cachedResult.radial,
                                    cachedResult.channels[0],
                                    RecFunc.getDefaultShift());
        System.out.println("Stack calc for "
                + ChannelIdUtil.toStringNoDates(cachedResult.channels[0]));
        return stack;
    }

    public SumHKStack sum(String netCode,
                          String staCode,
                          float percentMatch,
                          float smallestH) throws FissuresException, NotFound,
            IOException, SQLException {
        ArrayList individualHK = new ArrayList();
        int index = 1;
        getForStation.setString(index++, netCode);
        getForStation.setString(index++, staCode);
        getForStation.setFloat(index++, percentMatch);
        ResultSet rs = getForStation.executeQuery();
        while(rs.next()) {
            individualHK.add(extract(rs));
        }
        rs.close();
        rs = null;
        if(individualHK.size() != 0) {
            HKStack temp = (HKStack)individualHK.get(0);
            SumHKStack sumStack = new SumHKStack((HKStack[])individualHK.toArray(new HKStack[0]),
                                                 temp.getChannel(),
                                                 percentMatch,
                                                 smallestH);
            return sumStack;
        } else {
            return null;
        }
    }

    public HKStack extract(ResultSet rs) throws FissuresException, NotFound,
            IOException, SQLException {
        Channel[] channels = jdbcRecFunc.extractChannels(rs);
        byte[] dataBytes = rs.getBytes("data");
        int numH = rs.getInt("numH");
        int numK = rs.getInt("numK");
        float[][] data = HKStack.createArray(numH, numK);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(dataBytes));
        for(int i = 0; i < data.length; i++) {
            for(int j = 0; j < data[i].length; j++) {
                data[i][j] = dis.readFloat();
            }
        }
        System.out.println("WARNING: Weights are assumed to be 1");
        HKStack out = new HKStack(rs.getFloat("alpha"),
                                  rs.getFloat("p"),
                                  rs.getFloat("percentMatch"),
                                  rs.getFloat("minH"),
                                  rs.getFloat("stepH"),
                                  numH,
                                  rs.getFloat("minK"),
                                  rs.getFloat("stepK"),
                                  numK,
                                  1,
                                  1,
                                  1,
                                  data,
                                  channels[0]);
        return out;
    }

    private PreparedStatement uncalculated, calcByPercent, put,
            getForStation;

    private JDBCOrigin jdbcOrigin;

    private JDBCEventAttr jdbcEventAttr;

    private JDBCChannel jdbcChannel;

    private JDBCRecFunc jdbcRecFunc;

    private JDBCSequence seq;

    private File dataDir;

    private EventFormatter eventFormatter;

    String modelName = "iasp91";

    private TauPUtil tauPTime;

    private static final int DEFAULT_MIN_H = 10;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCHKStack.class);

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: JDBCHKStack -all or net [station]");
            return;
        }
        float minPercentMatch = 80;
        try {
            calcAndSave(args, minPercentMatch, true, false, 1, 1, 1);
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }

    public static ArrayList calcAndSave(String[] args,
                                        float minPercentMatch,
                                        boolean save,
                                        boolean forceAllCalc,
                                        float weightPs,
                                        float weightPpPs,
                                        float weightPsPs)
            throws FileNotFoundException, FissuresException, NotFound,
            IOException, TauModelException, SQLException,
            ConfigurationException, Exception {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        Properties props = StackSummary.loadProps(args);
        Connection conn = ConnMgr.createConnection();
        JDBCOrigin jdbcOrigin = new JDBCOrigin(conn);
        JDBCEventAttr jdbcEventAttr = new JDBCEventAttr(conn);
        JDBCChannel jdbcChannel = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn,
                                                  jdbcOrigin,
                                                  jdbcEventAttr,
                                                  jdbcChannel,
                                                  jdbcSodConfig,
                                                  RecFuncCacheImpl.getDataLoc());
        JDBCHKStack jdbcHKStack = new JDBCHKStack(conn,
                                                  jdbcOrigin,
                                                  jdbcEventAttr,
                                                  jdbcChannel,
                                                  jdbcSodConfig,
                                                  jdbcRecFunc);
        String netCode = args[0];
        if(args.length > 1) {
            String staCode = args[1];
            System.out.println("calc for " + netCode + "." + staCode);
            if(forceAllCalc) {
                return jdbcHKStack.calcAll(netCode,
                                           staCode,
                                           minPercentMatch,
                                           save,
                                           weightPs,
                                           weightPpPs,
                                           weightPsPs);
            } else {
                return jdbcHKStack.calc(netCode,
                                        staCode,
                                        minPercentMatch,
                                        save,
                                        weightPs,
                                        weightPpPs,
                                        weightPsPs);
            }
        } else {
            // do all or for a net
            JDBCStation jdbcStation = jdbcHKStack.getJDBCChannel()
                    .getSiteTable()
                    .getStationTable();
            JDBCNetwork jdbcNetwork = jdbcStation.getNetTable();
            NetworkId[] netId = jdbcNetwork.getAllNetworkIds();
            System.out.println("Found " + netId.length + " networks.");
            ArrayList out = new ArrayList();
            for(int i = 0; i < netId.length; i++) {
                System.out.println("Network: "
                        + NetworkIdUtil.toString(netId[i]));
                if(netCode.equals("-all")
                        || netId[i].network_code.equals(netCode)) {
                    Station[] station = jdbcStation.getAllStations(netId[i]);
                    for(int j = 0; j < station.length; j++) {
                        System.out.println("calc for " + netId[i].network_code
                                + "." + station[j].get_code());
                        try {
                            out.addAll(jdbcHKStack.calc(netId[i].network_code,
                                                        station[j].get_code(),
                                                        minPercentMatch,
                                                        save,
                                                        weightPs,
                                                        weightPpPs,
                                                        weightPsPs));
                        } catch(IllegalArgumentException e) {
                            System.out.println("Problem with receiver function, skipping station. "
                                    + e);
                            GlobalExceptionHandler.handle(e);
                        } catch(FileNotFoundException e) {
                            System.out.println("Problem with receiver function, skipping station. "
                                    + e);
                            GlobalExceptionHandler.handle(e);
                        }
                    }
                }
            }
            return out;
        }
    }

    public JDBCChannel getJDBCChannel() {
        return jdbcChannel;
    }

    public JDBCEventAttr getJDBCEventAttr() {
        return jdbcEventAttr;
    }

    public JDBCOrigin getJDBCOrigin() {
        return jdbcOrigin;
    }

    public JDBCRecFunc getJDBCRecFunc() {
        return jdbcRecFunc;
    }

    public TauPUtil getTauPTime() {
        return tauPTime;
    }

    public static int getDefaultMinH() {
        return DEFAULT_MIN_H;
    }
}