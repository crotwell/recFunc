package edu.sc.seis.receiverFunction.server;

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
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
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
    
    public int put(int recfunc_id, HKStack stack) throws SQLException {
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
        
        putStmt.setObject(index++, stack.getStack());
        putStmt.setTimestamp(index++, ClockUtil.now().getTimestamp());
        putStmt.executeUpdate();
        return hkstack_id;
    }
    
    public ArrayList calc(String netCode, String staCode, float percentMatch) throws FileNotFoundException, FissuresException, NotFound, IOException, SQLException, TauModelException {
        
        // get all uncalculated rows
        uncalculatedStmt.setString(1, netCode);
        uncalculatedStmt.setString(2, staCode);
        ResultSet rs = uncalculatedStmt.executeQuery();
        ArrayList individualHK = new ArrayList();
       
        while (rs.next()) {
            int recFuncDbId = rs.getInt(1);
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
            individualHK.add(stack);
            System.out.println("Stack calc for "+ChannelIdUtil.toStringNoDates(cachedResult.channels[0]));
        }
        return individualHK;
    }
    
    public SumHKStack sum(String netCode, String staCode, float percentMatch) throws FissuresException, NotFound, IOException, SQLException, SQLException {
        ArrayList individualHK = new ArrayList();
        int index = 1;
        getForStationStmt.setString(index++, netCode);
        getForStationStmt.setString(index++, staCode);
        getForStationStmt.setFloat(index++, percentMatch);
        ResultSet rs = getForStationStmt.executeQuery();
        while(rs.next()) {
            individualHK.add(extract(rs));
        }
        HKStack temp = (HKStack)individualHK.get(0);
        SumHKStack sumStack = new SumHKStack((HKStack[])individualHK.toArray(new HKStack[0]),
                                             temp.getChannel(),
                                             percentMatch);
        return sumStack;
    }
    
    HKStack extract(ResultSet rs) throws FileNotFoundException, FissuresException, NotFound, IOException, SQLException {
        CachedResult rfCache = jdbcRecFunc.extract(rs);
        HKStack out = new HKStack(rs.getFloat("alpha"),
                                  rs.getFloat("p"),
                                  rs.getFloat("percentMatch"),
                                  rs.getFloat("minH"),
                                  rs.getFloat("stepH"),
                                  rs.getInt("numH"),
                                  rs.getFloat("minK"),
                                  rs.getFloat("stepK"),
                                  rs.getInt("numK"),
                                  (float[][])rs.getObject("data"),
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
    
    public static void main(String[] args) {
        try {
            ConnMgr.setDB(ConnMgr.POSTGRES);
            Properties props = RecFuncCacheStart.loadProps(args);
            Connection conn = ConnMgr.createConnection();
            JDBCHKStack jdbcHKStack = new JDBCHKStack(conn);
            System.out.println("calc for IC.HIA");
            jdbcHKStack.calc("IC", "HIA", 90.0f);
            
        }catch(Exception e) {
            GlobalExceptionHandler.handle(e);   
        }
    }
}
