package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.omg.CORBA.UNKNOWN;
import edu.iris.Fissures.Orientation;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.DBUtil;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAttr;
import edu.sc.seis.fissuresUtil.database.event.JDBCOrigin;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.mseed.SeedFormatException;
import edu.sc.seis.fissuresUtil.xml.SeismogramFileTypes;
import edu.sc.seis.fissuresUtil.xml.URLDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.UnsupportedFileTypeException;
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
        isCachedStmt = conn.prepareStatement("SELECT recFunc_id from "+getTableName()+
                                             " WHERE origin_id = ? "+
                                             " AND chanZ_id = ? AND ( "+
                                             "( chanA_id = ? AND  chanB_id = ? ) OR "+
                                             "( chanB_id = ? AND  chanA_id = ? ) )"+
                                             " AND gwidth = ? "+
                                             " AND maxbumps = ? "+
                                             " AND maxerror = ? "
        );
        //        getConfigsStmt = conn.prepareStatement("SELECT gwidth, maxbumps, maxerror from "+getTableName()+
        //                                               " WHERE ");
    }
    
    public int put(Origin prefOrigin,
                   EventAttr eventAttr,
                   IterDeconConfig config,
                   Channel[] channels,
                   LocalSeismogram[] original,
                   LocalSeismogram radial,
                   float radialError,
                   LocalSeismogram transverse,
                   float transverseError) throws SQLException, IOException, NoPreferredOrigin, CodecException, UnsupportedFileTypeException, SeedFormatException {
        logger.debug("put: "+eventAttr.name+" "+ChannelIdUtil.toStringNoDates(channels[0].get_id()));
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
        
        for (int i=0; i<original.length; i++) {
            seisFile[i] = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)original[i],
                                                      stationDir,
                                                      channels[i],
                                                      cacheEvent,
                                                      fileType);
        }
        Channel zeroChannel = channels[0];
        ChannelId radialChannelId = new ChannelId(zeroChannel.get_id().network_id,
                                                  zeroChannel.get_id().station_code,
                                                  zeroChannel.get_id().site_code,
                                                  "ITR",
                                                  zeroChannel.get_id().begin_time);
        Channel radialChannel = new ChannelImpl(radialChannelId,
                                                "receiver function fake channel for "+ChannelIdUtil.toStringNoDates(zeroChannel.get_id()),
                                                new Orientation(0, 0),
                                                zeroChannel.sampling_info,
                                                zeroChannel.effective_time,
                                                zeroChannel.my_site);
        File radialFile = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)radial,
                                                      stationDir,
                                                      radialChannel,
                                                      cacheEvent,
                                                      fileType);
        
        ChannelId transverseChannelId = new ChannelId(zeroChannel.get_id().network_id,
                                                      zeroChannel.get_id().station_code,
                                                      zeroChannel.get_id().site_code,
                                                      "ITT",
                                                      zeroChannel.get_id().begin_time);
        Channel transverseChannel = new ChannelImpl(transverseChannelId,
                                                    "receiver function fake channel for "+ChannelIdUtil.toStringNoDates(zeroChannel.get_id()),
                                                    new Orientation(0, 0),
                                                    zeroChannel.sampling_info,
                                                    zeroChannel.effective_time,
                                                    zeroChannel.my_site);
        File transverseFile = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)transverse,
                                                          stationDir,
                                                          transverseChannel,
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
        putStmt.executeUpdate();
        logger.debug("insert: "+putStmt);
        return id;
        
    }
    
    public int getDbId(Origin prefOrigin,
                       ChannelId[] channels,
                       IterDeconConfig config) throws SQLException, NotFound {
        int originDbId = jdbcOrigin.getDBId(prefOrigin);
        int[] chanDbId = new int[channels.length];
        int zChan = -1;
        for(int i = 0; i < channels.length; i++) {
            if (channels[i].channel_code.endsWith("Z")) {
                zChan = i;
            }
        }
        if (zChan == -1) {
            throw new NotFound("Channel for Z not found");
        }
        for(int i = 0; i < channels.length; i++) {
            chanDbId[i] = jdbcChannel.getDBId(channels[i]);
        }
        int index = 1;
        isCachedStmt.setInt(index++, originDbId);
        isCachedStmt.setInt(index++, chanDbId[zChan]);
        switch (zChan) {
            case 0:
                isCachedStmt.setInt(index++, chanDbId[1]);
                isCachedStmt.setInt(index++, chanDbId[2]);
                isCachedStmt.setInt(index++, chanDbId[1]);
                isCachedStmt.setInt(index++, chanDbId[2]);
                break;
            case 1:
                isCachedStmt.setInt(index++, chanDbId[0]);
                isCachedStmt.setInt(index++, chanDbId[2]);
                isCachedStmt.setInt(index++, chanDbId[0]);
                isCachedStmt.setInt(index++, chanDbId[2]);
                break;
            case 2:
                isCachedStmt.setInt(index++, chanDbId[0]);
                isCachedStmt.setInt(index++, chanDbId[1]);
                isCachedStmt.setInt(index++, chanDbId[0]);
                isCachedStmt.setInt(index++, chanDbId[1]);
                break;
        }
        isCachedStmt.setFloat(index++, config.gwidth);
        isCachedStmt.setFloat(index++, config.maxBumps);
        isCachedStmt.setFloat(index++, config.tol);
        ResultSet rs = isCachedStmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        throw new NotFound("No rec func entry for origin="+originDbId+" chan="+chanDbId[0]+","+chanDbId[1]+","+chanDbId[2]);
    }
    
    private File dataDir;
    
    private EventFormatter eventFormatter;
    
    private JDBCOrigin jdbcOrigin;
    
    private JDBCEventAttr jdbcEventAttr;
    
    private JDBCChannel jdbcChannel;
    
    private SeismogramFileTypes fileType = SeismogramFileTypes.SAC;
    
    private PreparedStatement putStmt, isCachedStmt, getConfigsStmt, getStmt;
    
    private JDBCSequence seq;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCRecFunc.class);
}
