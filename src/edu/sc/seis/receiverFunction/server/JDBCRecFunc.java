package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.omg.CORBA.UNKNOWN;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.DBUtil;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAttr;
import edu.sc.seis.fissuresUtil.database.event.JDBCOrigin;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.mseed.SeedFormatException;
import edu.sc.seis.fissuresUtil.xml.SeismogramFileTypes;
import edu.sc.seis.fissuresUtil.xml.URLDataSetSeismogram;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.EventFormatter;


/**
 * @author crotwell
 * Created on Sep 11, 2004
 */
public class JDBCRecFunc extends JDBCTable {
    
    public JDBCRecFunc(Connection conn,
                       JDBCOrigin jdbcOrigin,
                       JDBCEventAttr jdbcEventAttr,
                       JDBCChannel jdbcChannel,
                       String dataDirectory) throws SQLException, ConfigurationException {
        super("receiverFunction", conn);
        ConnMgr.addPropsLocation("edu/sc/seis/receiverFunction/server/");
        this.jdbcOrigin = jdbcOrigin;
        this.jdbcEventAttr = jdbcEventAttr;
        this.jdbcChannel = jdbcChannel;
        seq = new JDBCSequence(conn, "receiverFunctionSeq");
        Statement stmt = conn.createStatement();
        if(!DBUtil.tableExists("receiverFunction", conn)){
            stmt.executeUpdate(ConnMgr.getSQL("receiverFunction.create"));
        }
        dataDir = new File(dataDirectory);
        dataDir.mkdirs();
        eventFormatter = new EventFormatter(true);
        putStmt = conn.prepareStatement(" INSERT INTO receiverFunction "+
                                        "(recFunc_id, "+
                                   "origin_id,  "+
                                   "eventAttr_id, "+
                                   "chanA_id, "+
                                   "seisA, "+
                                   "chanB_id, "+
                                   "seisB, "+
                                   "chanZ_id, "+
                                   "seisZ, "+
                                   "recfuncITR,  "+
                                   "itr_error, "+
                                   "recFuncITT, "+
                                   "itt_error, "+
                                   "gwidth, "+
                                   "maxbumps,  "+
                                   "maxerror) "+
                                        "VALUES(?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?,?)");
    }
    
    public void put(Origin prefOrigin,
                    EventAttr eventAttr,
                    IterDeconConfig config,
                    Channel[] channels,
                    LocalSeismogram[] original,
                    LocalSeismogram radial,
                    float radialError,
                    LocalSeismogram transverse,
                    float transverseError) throws SQLException {
        int originDbId = jdbcOrigin.put(prefOrigin);
        int eventAttrDbId = jdbcEventAttr.put(eventAttr);
        int[] channelDbId = new int[channels.length];
        for(int i = 0; i < channels.length; i++) {
            channelDbId[i] = jdbcChannel.put(channels[i]);
        }
        CacheEvent cacheEvent = new CacheEvent(eventAttr, prefOrigin);
        File eventDir = new File(dataDir, eventFormatter.getResult(cacheEvent));
        File netDir = new File(eventDir, channels[0].get_id().network_id.network_code);
        File stationDir = new File(netDir, channels[0].get_id().station_code);
        stationDir.mkdirs();
        
        File[] seisFile = new File[original.length];
        try {
            for (int i=0; i<original.length; i++) {
                seisFile[i] = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)original[i],
                                                          stationDir,
                                                          channels[i],
                                                          cacheEvent,
                                                          fileType);
            }
            File radialFile = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)radial,
                                                          stationDir,
                                                          channels[0],
                                                          cacheEvent,
                                                          fileType);
            File transverseFile = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)transverse,
                                                              stationDir,
                                                              channels[0],
                                                              cacheEvent,
                                                              fileType);
            int index = 1;
            int id = seq.next();
            putStmt.setInt(index++, id);
            putStmt.setInt(index++, originDbId);
            
            putStmt.setInt(index++, eventAttrDbId);
            putStmt.setInt(index++, channelDbId[0] );
            putStmt.setString(index++, seisFile[0].getName() );
            putStmt.setInt(index++, channelDbId[1]);
            putStmt.setString(index++, seisFile[1].getName());
            putStmt.setInt(index++, channelDbId[2]);
            putStmt.setString(index++, seisFile[2].getName());
            putStmt.setString(index++, radialFile.getName());
            putStmt.setFloat(index++, radialError);
            putStmt.setString(index++, transverseFile.getName());
            putStmt.setFloat(index++, transverseError);
            putStmt.setFloat(index++, config.gwidth);
            putStmt.setInt(index++, config.maxBumps);
            putStmt.setFloat(index++, config.tol);
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
            throw new UNKNOWN(e.toString());
        }
        
    }
    
    File dataDir;
    
    EventFormatter eventFormatter;
    
    JDBCOrigin jdbcOrigin;
    
    JDBCEventAttr jdbcEventAttr;
    
    JDBCChannel jdbcChannel;
    
    SeismogramFileTypes fileType = SeismogramFileTypes.SAC;
    
    PreparedStatement putStmt;

    private JDBCSequence seq;
}
