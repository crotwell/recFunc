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
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
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
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.crust2.Crust2;
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
        crust2 = new Crust2();
        dataDir = new File(RecFuncCacheImpl.getDataLoc());
        dataDir.mkdirs();
        eventFormatter = new EventFormatter(true);
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
           return new HKStack(new QuantityImpl(rs.getFloat("alpha"), UnitImpl.KILOMETER_PER_SECOND),
                              rs.getFloat("p"),
                              rs.getFloat("percentMatch"),
                              new QuantityImpl(rs.getFloat("minH"), UnitImpl.KILOMETER),
                              new QuantityImpl(rs.getFloat("stepH"), UnitImpl.KILOMETER),
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
        put.setFloat(index++, (float)stack.getAlpha().getValue(UnitImpl.KILOMETER_PER_SECOND));
        put.setFloat(index++, stack.getP());
        put.setFloat(index++, stack.getPercentMatch());
        put.setFloat(index++, (float)stack.getMinH().getValue());
        put.setFloat(index++, (float)stack.getStepH().getValue());
        put.setInt(index++, stack.getNumH());
        put.setFloat(index++, stack.getMinK());
        put.setFloat(index++, stack.getStepK());
        put.setInt(index++, stack.getNumK());
        QuantityImpl peakH;
        float peakK, peakVal = 0;
        int[] indicies = stack.getMaxValueIndices();
        peakH = stack.getMaxValueH();
        peakK = stack.getMaxValueK();
        peakVal = stack.getMaxValue();
        put.setFloat(index++, (float)peakH.convertTo(UnitImpl.KILOMETER).getValue());
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

    public void calc(String netCode,
                          String staCode,
                          float percentMatch,
                          boolean save) throws FileNotFoundException,
            FissuresException, NotFound, IOException, SQLException,
            TauModelException {
        calc(netCode, staCode, percentMatch, save, 1, 1, 1);
    }

    public void calc(String netCode,
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
        calcStmt(rs, save, weightPs, weightPpPs, weightPsPs);
    }

    public void calcAll(String netCode,
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
        calcStmt(rs, save, weightPs, weightPpPs, weightPsPs);
    }

    public void calcStmt(ResultSet rs,
                              boolean save,
                              float weightPs,
                              float weightPpPs,
                              float weightPsPs) throws FileNotFoundException,
            FissuresException, SQLException, TauModelException, NotFound,
            IOException {
        while(rs.next()) {
            int recFuncDbId = rs.getInt(1);
            System.out.println("Calc for " + recFuncDbId);
            HKStack stack = calc(recFuncDbId, weightPs, weightPpPs, weightPsPs);
            if(save) {
                int hkstack_id = put(recFuncDbId, stack);
            }
        }
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
        HKStack stack = HKStack.create(cachedResult, weightPs, weightPpPs, weightPsPs);
        System.out.println("Stack calc for "
                + ChannelIdUtil.toStringNoDates(cachedResult.channels[0]));
        return stack;
    }

    public SumHKStack sum(String netCode,
                          String staCode,
                          float percentMatch,
                          QuantityImpl smallestH) throws FissuresException, NotFound,
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
        // if there is only 1 eq that matches, then we can't really do a stack
        if(individualHK.size() > 1) {
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
        HKStack out = new HKStack(new QuantityImpl(rs.getFloat("alpha"), UnitImpl.KILOMETER_PER_SECOND),
                                  rs.getFloat("p"),
                                  rs.getFloat("percentMatch"),
                                  new QuantityImpl(rs.getFloat("minH"), UnitImpl.KILOMETER),
                                  new QuantityImpl(rs.getFloat("stepH"), UnitImpl.KILOMETER),
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

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCHKStack.class);

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: JDBCHKStack -all or -net net [-sta station]");
            return;
        }
        float minPercentMatch = 80;
        try {
            System.out.println("calc for percent match > "+minPercentMatch+" with weights of 1/3");
                       float weightPs = 1/3f;
                       float weightPpPs = 1/3f;
                       float weightPsPs = 1 - weightPs - weightPpPs;
            calcAndSave(args, minPercentMatch, true, false, weightPs, weightPpPs, weightPsPs);
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }

    public static void calcAndSave(String[] args,
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
            return;
        }
        if(staCode.length() > 0) {
            System.out.println("calc for " + netCode + "." + staCode);
            if(forceAllCalc) {
                jdbcHKStack.calcAll(netCode,
                                           staCode,
                                           minPercentMatch,
                                           save,
                                           weightPs,
                                           weightPpPs,
                                           weightPsPs);
            } else {
                jdbcHKStack.calc(netCode,
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
                            jdbcHKStack.calc(netId[i].network_code,
                                                        station[j].get_code(),
                                                        minPercentMatch,
                                                        save,
                                                        weightPs,
                                                        weightPpPs,
                                                        weightPsPs);
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
}