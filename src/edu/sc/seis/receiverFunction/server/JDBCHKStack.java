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
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.sac.SacTimeSeries;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.RecFunc;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.EventFormatter;


/**
 * @author crotwell
 * Created on Sep 20, 2004
 */
public class JDBCHKStack  extends JDBCTable {

    public JDBCHKStack(Connection conn) throws SQLException, ConfigurationException, TauModelException {
        super("hkstack", conn);
        JDBCOrigin jdbcOrigin = new JDBCOrigin(conn); 
        JDBCEventAttr jdbcEventAttr = new JDBCEventAttr(conn);
        JDBCChannel jdbcChannel = new JDBCChannel(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn,
                                                  jdbcOrigin,
                                                  jdbcEventAttr,
                                                  jdbcChannel,
                                                  dataDirectory);
        this.jdbcOrigin = jdbcOrigin;
        this.jdbcEventAttr = jdbcEventAttr;
        this.jdbcChannel = jdbcChannel;
        this.jdbcRecFunc = jdbcRecFunc;

        seq = new JDBCSequence(conn, "receiverFunctionSeq");
        Statement stmt = conn.createStatement();
        if(!DBUtil.tableExists(getTableName(), conn)){
            stmt.executeUpdate(ConnMgr.getSQL("hkStack.create"));
        }
        dataDir = new File(dataDirectory);
        dataDir.mkdirs();
        eventFormatter = new EventFormatter(true);
        tauPTime = TauPUtil.getTauPUtil(modelName);
        
        uncalculatedStmt = conn.prepareStatement(ConnMgr.getSQL("hkStack.uncalculated"));
        putStmt = conn.prepareStatement(ConnMgr.getSQL("hkStack.put"));
        getForStationStmt = conn.prepareStatement(ConnMgr.getSQL("hkstack.getForStation"));
    }
    
    public int put(int recfunc_id, HKStack stack) throws SQLException, IOException {
        int hkstack_id = seq.next();
        int index = 1;
        putStmt.setInt(index++, hkstack_id);
        putStmt.setInt(index++, recfunc_id);
        putStmt.setFloat(index++, stack.getAlpha());
        putStmt.setFloat(index++, stack.getP());
        putStmt.setFloat(index++, stack.getPercentMatch());
        putStmt.setFloat(index++, stack.getMinH());
        putStmt.setFloat(index++, stack.getStepH());
        putStmt.setInt(index++, stack.getNumH());
        putStmt.setFloat(index++, stack.getMinK());
        putStmt.setFloat(index++, stack.getStepK());
        putStmt.setInt(index++, stack.getNumK());
        
        float peakH, peakK, peakVal = 0;
        int[] indicies = stack.getMaxValueIndices();
        peakH = stack.getMinH()+stack.getStepH()*indicies[0];
        peakK = stack.getMinK()+stack.getStepK()*indicies[1];
        peakVal = stack.getStack()[indicies[0]][indicies[1]];

        putStmt.setFloat(index++, peakH);
        putStmt.setFloat(index++, peakK);
        putStmt.setFloat(index++, peakVal);
        
        float[][] data = stack.getStack();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        for(int i = 0; i < data.length; i++) {
            for(int j = 0; j < data[i].length; j++) {
                dos.writeFloat(data[i][j]);
            }
        }
        byte[] valBytes = out.toByteArray();
        putStmt.setBytes(index++, valBytes);
        
        putStmt.setTimestamp(index++, ClockUtil.now().getTimestamp());
        try {
            putStmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL stmt: "+putStmt.toString());
            throw e;
        }
        return hkstack_id;
    }
    
    public ArrayList calc(String netCode, String staCode, float percentMatch) throws FileNotFoundException, FissuresException, NotFound, IOException, SQLException, TauModelException {
        
        // get all uncalculated rows
        uncalculatedStmt.setString(1, netCode);
        uncalculatedStmt.setString(2, staCode);
        uncalculatedStmt.setFloat(3, percentMatch);
        ResultSet rs = uncalculatedStmt.executeQuery();
        ArrayList individualHK = new ArrayList();
       
        while (rs.next()) {
            int recFuncDbId = rs.getInt(1);
            System.out.println("Calc for "+recFuncDbId);
            individualHK.add(calc(recFuncDbId));
        }
        return individualHK;
    }
    
    HKStack calc(int recFuncDbId) throws TauModelException, FileNotFoundException, FissuresException, NotFound, IOException, SQLException {
        CachedResult cachedResult = jdbcRecFunc.get(recFuncDbId);
        String[] pPhases = { "P" };
        Arrival[] arrivals =
            tauPTime.calcTravelTimes(cachedResult.channels[0].my_site.my_station, cachedResult.prefOrigin, pPhases);
        // convert radian per sec ray param into km per sec
        float kmRayParam = (float)(arrivals[0].getRayParam()/tauPTime.getTauModel().getRadiusOfEarth());
        HKStack stack = new HKStack(6.5f,
                                    kmRayParam,
                                    cachedResult.radialMatch,
                                    10, .25f, 240,
                                    1.6f, .0025f, 200,
                                    (LocalSeismogramImpl)cachedResult.radial,
                                    cachedResult.channels[0],
                                    RecFunc.getDefaultShift());
        int hkstack_id = put(recFuncDbId, stack);
        System.out.println("Stack calc for "+ChannelIdUtil.toStringNoDates(cachedResult.channels[0]));
        return stack;
    }
    
    public SumHKStack sum(String netCode, String staCode, float percentMatch, float smallestH) throws FissuresException, NotFound, IOException, SQLException, SQLException {
        ArrayList individualHK = new ArrayList();
        int index = 1;
        getForStationStmt.setString(index++, netCode);
        getForStationStmt.setString(index++, staCode);
        getForStationStmt.setFloat(index++, percentMatch);
        ResultSet rs = getForStationStmt.executeQuery();
        while(rs.next()) {
            individualHK.add(extract(rs));
        }
        if (individualHK.size() != 0) {
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
    
    HKStack extract(ResultSet rs) throws FileNotFoundException, FissuresException, NotFound, IOException, SQLException {
        CachedResult rfCache = jdbcRecFunc.extract(rs);
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
        
        
        HKStack out = new HKStack(rs.getFloat("alpha"),
                                  rs.getFloat("p"),
                                  rs.getFloat("percentMatch"),
                                  rs.getFloat("minH"),
                                  rs.getFloat("stepH"),
                                  numH,
                                  rs.getFloat("minK"),
                                  rs.getFloat("stepK"),
                                  numK,
                                  data,
                                  rfCache.channels[0]);
        return out;
    }
    
    private PreparedStatement uncalculatedStmt, putStmt, getForStationStmt;
    
    private JDBCOrigin jdbcOrigin;
    
    private JDBCEventAttr jdbcEventAttr;
    
    private JDBCChannel jdbcChannel;

    private JDBCRecFunc jdbcRecFunc;
    
    private JDBCSequence seq;
    
    private String dataDirectory = "Ears/Data";
    
    private File dataDir;
    
    private EventFormatter eventFormatter;
    
    String modelName = "iasp91";

    private TauPUtil tauPTime;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCHKStack.class);
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: JDBCHKStack -all or net [station]");
            return;
        }
        float minPercentMatch = 80;
        try {
            ConnMgr.setDB(ConnMgr.POSTGRES);
            Properties props = RecFuncCacheStart.loadProps(args);
            Connection conn = ConnMgr.createConnection();
            JDBCHKStack jdbcHKStack = new JDBCHKStack(conn);
            String netCode = args[0];
            if (args.length > 1) {
                String staCode = args[1];
                System.out.println("calc for "+netCode+"."+staCode);
                jdbcHKStack.calc(netCode, staCode, minPercentMatch);
            } else {
                // do all or for a net
                JDBCStation jdbcStation = jdbcHKStack.getJDBCChannel().getSiteTable().getStationTable();
                JDBCNetwork jdbcNetwork = jdbcStation.getNetTable();
                NetworkId[] netId = jdbcNetwork.getAllNetworkIds();
                for(int i = 0; i < netId.length; i++) {
                    if(netCode.equals("-all") || netId[i].network_code.equals(netCode)) {
                        Station[] station = jdbcStation.getAllStations(netId[i]);
                        for(int j = 0; j < station.length; j++) {
                            System.out.println("calc for "+netId[i].network_code+"."+station[j].get_code());
                            jdbcHKStack.calc(netCode, station[j].get_code(), minPercentMatch);
                        }
                    }
                }
            }
        }catch(Exception e) {
            GlobalExceptionHandler.handle(e);   
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
}
