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
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
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

    public JDBCHKStack(Connection conn, JDBCEventAccess jdbcEventAccess,
            JDBCChannel jdbcChannel, JDBCSodConfig jdbcSodConfig,
            JDBCRecFunc jdbcRecFunc) throws SQLException,
            ConfigurationException, TauModelException, Exception {
        super("hkstack", conn);
        this.jdbcEventAccess = jdbcEventAccess;
        this.jdbcChannel = jdbcChannel;
        this.jdbcRecFunc = jdbcRecFunc;
        hkstackSeq = new JDBCSequence(conn, getTableName()+"Seq");
        TableSetup.setup(getTableName(),
                         conn,
                         this,
                         "edu/sc/seis/receiverFunction/server/default.props");
        
        dataDir = new File(RecFuncCacheImpl.getDataLoc());
        dataDir.mkdirs();
        eventFormatter = new EventFormatter(true);
        tauPTime = TauPUtil.getTauPUtil(modelName);
    }
    
    public HKStack get(int recfunc_id) throws IOException, SQLException, NotFound {
       get.setInt(1, recfunc_id);
       ResultSet rs = get.executeQuery();
       if (rs.next()) {
           byte[] stackBytes = rs.getBytes("data");
           int numH = rs.getInt("numH");
           int numK = rs.getInt("numK");
           float[][] data = new float[numH][numK];
           ByteArrayInputStream out = new ByteArrayInputStream(stackBytes);
           DataInputStream dos = new DataInputStream(out);
           for(int i = 0; i < data.length; i++) {
               for(int j = 0; j < data[i].length; j++) {
                   data[i][j] = dos.readFloat();
               }
           }
           Channel chan = JDBCChannel.extract(rs, jdbcChannel.getSiteTable(), jdbcChannel.getTimeTable(), jdbcChannel.getQuantityTable());
           return new HKStack(rs.getFloat("alpha"),
                              rs.getFloat("p"),
                              rs.getFloat("percentMatch"),
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
       } else {
           System.out.println("get hkstack: "+get);
           rs.close();
           throw new NotFound();
       }
    }
    
    public int put(int recfunc_id, HKStack stack) throws SQLException,
            IOException {
        int hkstack_id = hkstackSeq.next();
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
        put.setFloat(index++, stack.getWeightPs());
        put.setFloat(index++, stack.getWeightPpPs());
        put.setFloat(index++, stack.getWeightPsPs());
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
    
    void calcAndStore(int recFuncDbId,
                      float weightPs,
                      float weightPpPs,
                      float weightPsPs) throws TauModelException,
                 FileNotFoundException, FissuresException, NotFound, IOException,
                 SQLException {
        HKStack stack = calc(recFuncDbId, weightPs, weightPpPs, weightPsPs);
        put(recFuncDbId, stack);
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
        int numH = rs.getInt("numH");
        int numK = rs.getInt("numK");
        float[][] data = extractData(rs, numH, numK);
        HKStack out = new HKStack(rs.getFloat("alpha"),
                                  rs.getFloat("p"),
                                  rs.getFloat("percentMatch"),
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
                                  channels[0]);
        return out;
    }
    
    public float[][] extractData(ResultSet rs, int numH, int numK) throws SQLException, IOException {
        byte[] dataBytes = rs.getBytes("data");
        float[][] data = HKStack.createArray(numH, numK);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(dataBytes));
        for(int i = 0; i < data.length; i++) {
            for(int j = 0; j < data[i].length; j++) {
                data[i][j] = dis.readFloat();
            }
        }
        return data;
    }

    private PreparedStatement uncalculated, calcByPercent, put, get, getForStation;

    private JDBCEventAccess jdbcEventAccess;

    private JDBCChannel jdbcChannel;

    private JDBCRecFunc jdbcRecFunc;

    private JDBCSequence hkstackSeq;

    private File dataDir;

    private EventFormatter eventFormatter;

    String modelName = "iasp91";

    private TauPUtil tauPTime;

    private static final int DEFAULT_MIN_H = 10;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCHKStack.class);

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: JDBCHKStack -all or -net net [-sta station]");
            return;
        }
        float minPercentMatch = 80;
        try {
            System.out.println("calc with weights of 1/3");
                       float weightPs = 1/3f;
                       float weightPpPs = 1/3f;
                       float weightPsPs = 1 - weightPs - weightPpPs;
            calcAndSave(args, minPercentMatch, true, false, weightPs, weightPpPs, weightPsPs);
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
        ConnMgr.setURL(props.getProperty("URL"));
        Connection conn = ConnMgr.createConnection();
        JDBCEventAccess jdbcEventAccess = new JDBCEventAccess(conn);
        JDBCChannel jdbcChannel = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn,
                                                  jdbcEventAccess,
                                                  jdbcChannel,
                                                  jdbcSodConfig,
                                                  RecFuncCacheImpl.getDataLoc());
        JDBCHKStack jdbcHKStack = new JDBCHKStack(conn,
                                                  jdbcEventAccess,
                                                  jdbcChannel,
                                                  jdbcSodConfig,
                                                  jdbcRecFunc);
        
        String netCode = "";
        String staCode = "";
        for(int i = 0; i < args.length; i++) {
            if (args[i].equals("-all")) {
                netCode = args[i];
            } else if (args[i].equals("-net")) {
                netCode = args[i+1];
            } else if (args[i].equals("-sta")) {
                staCode = args[i+1];
            }
        }
        if (staCode.length() > 0 && netCode.length() == 0 ) {
            System.err.println("If using -sta, you must also use -net netCode");
            return new ArrayList();
        }
        if(staCode.length() > 0) {
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

    public JDBCEventAccess getJDBCEventAccess() {
        return jdbcEventAccess;
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