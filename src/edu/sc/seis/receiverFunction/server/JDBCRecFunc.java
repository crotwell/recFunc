package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.Orientation;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.IfParameterMgr.ParameterRef;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.JDBCLocation;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.JDBCTime;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAttr;
import edu.sc.seis.fissuresUtil.database.event.JDBCOrigin;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.fissuresUtil.sac.SacToFissures;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.fissuresUtil.xml.SeismogramFileTypes;
import edu.sc.seis.fissuresUtil.xml.URLDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.UnsupportedFileTypeException;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.EventFormatter;

/**
 * @author crotwell Created on Sep 11, 2004
 */
public class JDBCRecFunc extends JDBCTable {

    public JDBCRecFunc(Connection conn, String dataDirectory) throws Exception {
        this(conn,
             new JDBCEventAccess(conn),
             new JDBCChannel(conn),
             new JDBCSodConfig(conn),
             dataDirectory);
    }

    public JDBCRecFunc(Connection conn,
                       JDBCEventAccess jdbcEventAccess,
                       JDBCChannel jdbcChannel,
                       JDBCSodConfig jdbcSodConfig,
                       String dataDirectory) throws SQLException,
            ConfigurationException, Exception {
        super("receiverFunction", conn);
        this.dataDirectory = dataDirectory;
        this.jdbcOrigin = jdbcEventAccess.getJDBCOrigin();
        this.jdbcEventAttr = jdbcEventAccess.getJDBCAttr();
        this.jdbcEventAccess = jdbcEventAccess;
        this.jdbcChannel = jdbcChannel;
        this.jdbcSodConfig = jdbcSodConfig;
        receiverFunctionSeq = new JDBCSequence(conn, getTableName() + "Seq");
        TableSetup.setup(getTableName(),
                         conn,
                         this,
                         "edu/sc/seis/receiverFunction/server/default.props");
        eventFormatter = new EventFormatter(true);
        putStmt = conn.prepareStatement(" INSERT INTO receiverFunction "
                + "(recFunc_id, " + "origin_id,  " + "eventAttr_id, "
                + "chanA_id, " + "seisA, " + "chanB_id, " + "seisB, "
                + "chanZ_id, " + "seisZ, " + "recfuncITR,  " + "itr_match, "
                + "itr_bump, " + "recFuncITT, " + "itt_match, " + "itt_bump, "
                + "gwidth, " + "maxbumps,  " + "tol, " + "insertTime, "
                + "sodconfig_id ) "
                + "VALUES(?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?,?)");
        isCachedStmt = conn.prepareStatement("SELECT recFunc_id from "
                + getTableName() + " WHERE origin_id = ? "
                + " AND chanZ_id = ? AND ( "
                + " ( chanA_id = ? AND  chanB_id = ? ) OR "
                + " ( chanB_id = ? AND  chanA_id = ? ) )" + " AND gwidth = ? "
                + " AND maxbumps = ? " + " AND tol = ? ");
        getConfigsStmt = conn.prepareStatement("SELECT gwidth, maxbumps, tol from "
                + getTableName()
                + " WHERE "
                + " origin_id = ? "
                + " AND chanZ_id = ? AND ( "
                + " ( chanA_id = ? AND  chanB_id = ? ) OR "
                + " ( chanB_id = ? AND  chanA_id = ? ) )");
        getStmt = conn.prepareStatement("SELECT *  from " + getTableName()
                + " WHERE " + " origin_id = ? " + " AND chanZ_id = ? AND ( "
                + " ( chanA_id = ? AND  chanB_id = ? ) OR "
                + " ( chanB_id = ? AND  chanA_id = ? ) )" + " AND gwidth = ? "
                + " AND maxbumps = ? " + " AND tol = ? ");
        getByDbIdStmt = conn.prepareStatement("SELECT * from " + getTableName()
                + " WHERE recfunc_id = ? ");
    }

    public int put(Origin prefOrigin,
                   EventAttr eventAttr,
                   IterDeconConfig config,
                   Channel[] channels,
                   LocalSeismogram[] original,
                   LocalSeismogram radial,
                   float radialMatch,
                   int radialBump,
                   LocalSeismogram transverse,
                   float transverseMatch,
                   int transverseBump,
                   int sodConfig_id) throws SQLException, IOException,
            NoPreferredOrigin, CodecException, UnsupportedFileTypeException,
            SeedFormatException {
        logger.debug("put: " + eventAttr.name + " "
                + ChannelIdUtil.toStringNoDates(channels[0].get_id()));
        Connection conn = jdbcOrigin.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        CacheEvent cacheEvent = new CacheEvent(eventAttr, prefOrigin);
        int originDbId = -1;
        int eventAttrDbId = -1;
        try {
            int eventAccessDbId = jdbcEventAccess.put(cacheEvent, "", "", "");
            try {
                originDbId = jdbcOrigin.getDBId(prefOrigin);
                eventAttrDbId = jdbcEventAttr.getDBId(eventAttr);
            } catch(NotFound e) {
                // should never happen since we just put the cacheEvent
                throw new RuntimeException("origin or eventattr was not found after putting. "
                                                   + cacheEvent.toString(),
                                           e);
            }
            int[] channelDbId = new int[channels.length];
            for(int i = 0; i < channels.length; i++) {
                channelDbId[i] = jdbcChannel.put(channels[i]);
            }
            File stationDir = getDir(cacheEvent, channels[0], config.gwidth);
            stationDir.mkdirs();
            File[] seisFile = new File[original.length];
            for(int i = 0; i < original.length; i++) {
                seisFile[i] = URLDataSetSeismogram.saveAs((LocalSeismogramImpl)original[i],
                                                          stationDir,
                                                          channels[i],
                                                          cacheEvent,
                                                          fileType);
            }
            int zChanNum = 0;
            for(int i = 0; i < channels.length; i++) {
                if(channels[i].get_code().endsWith("Z")) {
                    zChanNum = i;
                    break;
                }
            }
            Channel zeroChannel = channels[zChanNum];
            ChannelId radialChannelId = new ChannelId(zeroChannel.get_id().network_id,
                                                      zeroChannel.get_id().station_code,
                                                      zeroChannel.get_id().site_code,
                                                      "ITR",
                                                      zeroChannel.get_id().begin_time);
            Channel radialChannel = new ChannelImpl(radialChannelId,
                                                    "receiver function fake channel for "
                                                            + ChannelIdUtil.toStringNoDates(zeroChannel.get_id()),
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
                                                        "receiver function fake channel for "
                                                                + ChannelIdUtil.toStringNoDates(zeroChannel.get_id()),
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
            int id = receiverFunctionSeq.next();
            putStmt.setInt(index++, id);
            putStmt.setInt(index++, originDbId);
            putStmt.setInt(index++, eventAttrDbId);
            // put Z chan in right colum, let horizontals be a and b
            // assume seismograms and channels are in common order
            switch(zChanNum){
                case 0:
                    putStmt.setInt(index++, channelDbId[1]);
                    putStmt.setString(index++, seisFile[1].getName());
                    putStmt.setInt(index++, channelDbId[2]);
                    putStmt.setString(index++, seisFile[2].getName());
                    break;
                case 1:
                    putStmt.setInt(index++, channelDbId[0]);
                    putStmt.setString(index++, seisFile[0].getName());
                    putStmt.setInt(index++, channelDbId[2]);
                    putStmt.setString(index++, seisFile[2].getName());
                    break;
                case 2:
                    putStmt.setInt(index++, channelDbId[0]);
                    putStmt.setString(index++, seisFile[0].getName());
                    putStmt.setInt(index++, channelDbId[1]);
                    putStmt.setString(index++, seisFile[1].getName());
                    break;
            }
            putStmt.setInt(index++, channelDbId[zChanNum]);
            putStmt.setString(index++, seisFile[zChanNum].getName());
            putStmt.setString(index++, radialFile.getName());
            putStmt.setFloat(index++, radialMatch);
            putStmt.setFloat(index++, radialBump);
            putStmt.setString(index++, transverseFile.getName());
            putStmt.setFloat(index++, transverseMatch);
            putStmt.setFloat(index++, transverseBump);
            putStmt.setFloat(index++, config.gwidth);
            putStmt.setInt(index++, config.maxBumps);
            putStmt.setFloat(index++, config.tol);
            putStmt.setTimestamp(index++, ClockUtil.now().getTimestamp());
            putStmt.setInt(index++, sodConfig_id);
            putStmt.executeUpdate();
            logger.debug("insert: " + putStmt);
            conn.commit();
            return id;
        } catch(SQLException e) {
            conn.rollback();
            throw e;
        } catch(IOException e) {
            conn.rollback();
            throw e;
        } catch(NoPreferredOrigin e) {
            conn.rollback();
            throw e;
        } catch(CodecException e) {
            conn.rollback();
            throw e;
        } catch(UnsupportedFileTypeException e) {
            conn.rollback();
            throw e;
        } catch(SeedFormatException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    public int getDbId(Origin prefOrigin,
                       ChannelId[] channels,
                       IterDeconConfig config) throws SQLException, NotFound {
        populateGetStmt(isCachedStmt, prefOrigin, channels, config);
        ResultSet rs = isCachedStmt.executeQuery();
        if(rs.next()) {
            return rs.getInt(1);
        }
        throw new NotFound("No rec func entry found");
    }

    public CachedResultPlusDbId get(int dbid) throws FileNotFoundException,
            FissuresException, NotFound, IOException, SQLException {
        getByDbIdStmt.setInt(1, dbid);
        ResultSet rs = getByDbIdStmt.executeQuery();
        if(rs.next()) {
            return extract(rs);
        }
        throw new NotFound("No rec func entry found for dbid=" + dbid);
    }
    
    public void delete(int dbid) throws SQLException, NotFound, FileNotFoundException, IOException {
        getByDbIdStmt.setInt(1, dbid);
        ResultSet rs = getByDbIdStmt.executeQuery();
        if(rs.next()) {
            CachedResultPlusDbId withDbId = extractWithoutSeismograms(rs);
            CachedResult result = withDbId.getCachedResult();
            CacheEvent cacheEvent = new CacheEvent(result.event_attr,
                                                   result.prefOrigin);
            File stationDir = getDir(cacheEvent,
                                     result.channels[0],
                                     result.config.gwidth);
            File f = new File(stationDir, rs.getString("recfuncITR"));
            f.delete();
            f = new File(stationDir, rs.getString("recfuncITT"));
            f.delete();
            rs.close();
            deleteStmt.setInt(1, dbid);
            try {
                deleteStmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("statement causeing error: "+deleteStmt);
                throw e;
            }
            return;
        }
        throw new NotFound("No rec func entry found for dbid=" + dbid);
    }

    public CachedResultPlusDbId getWithoutSeismograms(int dbid)
            throws FileNotFoundException, FissuresException, NotFound,
            IOException, SQLException {
        getByDbIdStmt.setInt(1, dbid);
        ResultSet rs = getByDbIdStmt.executeQuery();
        if(rs.next()) {
            return extractWithoutSeismograms(rs);
        }
        throw new NotFound("No rec func entry found for dbid=" + dbid);
    }

    public CachedResultPlusDbId get(Origin prefOrigin,
                            ChannelId[] channel,
                            IterDeconConfig config) throws NotFound,
            FileNotFoundException, IOException, SQLException, FissuresException {
        populateGetStmt(getStmt, prefOrigin, channel, config);
        ResultSet rs = getStmt.executeQuery();
        if(rs.next()) {
            return extract(rs);
        }
        throw new NotFound("No rec func entry found");
    }

    public CachedResultPlusDbId extract(ResultSet rs) throws NotFound,
            FileNotFoundException, IOException, SQLException, FissuresException {
        CachedResultPlusDbId withDbId = extractWithoutSeismograms(rs);
        CachedResult result = withDbId.getCachedResult();
        CacheEvent cacheEvent = new CacheEvent(result.event_attr,
                                               result.prefOrigin);
        File stationDir = getDir(cacheEvent,
                                 result.channels[0],
                                 result.config.gwidth);
        SacTimeSeries itrSAC = new SacTimeSeries();
        File f = new File(stationDir, rs.getString("recfuncITR"));
        itrSAC.read(new File(stationDir, rs.getString("recfuncITR")));
        LocalSeismogramImpl itrSeis = SacToFissures.getSeismogram(itrSAC);
        itrSeis.y_unit = UnitImpl.DIMENSONLESS;
        result.radial = itrSeis;
        SacTimeSeries ittSAC = new SacTimeSeries();
        ittSAC.read(new File(stationDir, rs.getString("recfuncITT")));
        LocalSeismogramImpl ittSeis = SacToFissures.getSeismogram(ittSAC);
        ittSeis.y_unit = UnitImpl.DIMENSONLESS;
        result.tansverse = ittSeis;
        LocalSeismogramImpl[] originals = new LocalSeismogramImpl[3];
        SacTimeSeries SACa = new SacTimeSeries();
        SACa.read(new File(stationDir, rs.getString("seisA")));
        originals[0] = SacToFissures.getSeismogram(SACa);
        originals[0].channel_id = result.channels[0].get_id();
        SacTimeSeries SACb = new SacTimeSeries();
        SACb.read(new File(stationDir, rs.getString("seisB")));
        originals[1] = SacToFissures.getSeismogram(SACb);
        originals[1].channel_id = result.channels[1].get_id();
        SacTimeSeries SACz = new SacTimeSeries();
        SACz.read(new File(stationDir, rs.getString("seisZ")));
        originals[2] = SacToFissures.getSeismogram(SACz);
        originals[2].channel_id = result.channels[2].get_id();
        result.original = originals;
        return withDbId;
    }

    public CachedResultPlusDbId extractWithoutSeismograms(ResultSet rs)
            throws NotFound, FileNotFoundException, IOException, SQLException {
        Origin origin = jdbcOrigin.get(rs.getInt("origin_id"));
        EventAttr eventAttr = jdbcEventAttr.get(rs.getInt("eventAttr_id"));
        Channel[] channels = extractChannels(rs);
        MicroSecondDate insertTime = new MicroSecondDate(rs.getTimestamp("inserttime"));
        CachedResult result = new CachedResult(origin,
                                               eventAttr,
                                               new IterDeconConfig(rs.getFloat("gwidth"),
                                                                   rs.getInt("maxBumps"),
                                                                   rs.getFloat("tol")),
                                               channels,
                                               new LocalSeismogramImpl[0],
                                               null,
                                               rs.getFloat("itr_match"),
                                               rs.getInt("itr_bump"),
                                               null,
                                               rs.getFloat("itt_match"),
                                               rs.getInt("itt_bump"),
                                               rs.getInt("sodConfig_id"),
                                               insertTime.getFissuresTime());
        return new CachedResultPlusDbId(result, rs.getInt("recfunc_id"));
    }

    public Channel[] extractChannels(ResultSet rs) throws SQLException,
            NotFound {
        return new Channel[] {jdbcChannel.get(rs.getInt("chanA_id")),
                              jdbcChannel.get(rs.getInt("chanB_id")),
                              jdbcChannel.get(rs.getInt("chanZ_id"))};
    }

    public IterDeconConfig[] getCachedConfigs(Origin prefOrigin,
                                              ChannelId[] channel)
            throws SQLException, NotFound {
        populateGetStmt(getConfigsStmt, prefOrigin, channel);
        ResultSet rs = getConfigsStmt.executeQuery();
        ArrayList out = new ArrayList();
        while(rs.next()) {
            out.add(new IterDeconConfig(rs.getFloat("gwidth"),
                                        rs.getInt("maxBumps"),
                                        rs.getFloat("tol")));
        }
        return (IterDeconConfig[])out.toArray(new IterDeconConfig[0]);
    }

    public int countSeccessfulEvents(int netDbId,
                                     String stationCode,
                                     float gaussianWidth,
                                     float minPercentMatch)
            throws SQLException, NotFound {
        int index = 1;
        countOriginByStationByPercent.setInt(index++, netDbId);
        countOriginByStationByPercent.setString(index++, stationCode);
        countOriginByStationByPercent.setFloat(index++, gaussianWidth);
        countOriginByStationByPercent.setFloat(index++, minPercentMatch);
        ResultSet rs = countOriginByStationByPercent.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    public CacheEvent[] getSuccessfulEvents(int netDbId,
                                            String stationCode,
                                            float gaussianWidth)
            throws SQLException, NotFound {
        int index = 1;
        getOriginByStation.setInt(index++, netDbId);
        getOriginByStation.setString(index++, stationCode);
        getOriginByStation.setFloat(index++, gaussianWidth);
        ResultSet rs = getOriginByStation.executeQuery();
        TimeOMatic.print("result set");
        ArrayList out = new ArrayList();
        while(rs.next()) {
            Origin o = jdbcOrigin.extract(rs);
            // TimeOMatic.print("origin");
            EventAttr attr = jdbcEventAttr.extract(rs);
            // TimeOMatic.print("attr");
            CacheEvent event = new CacheEvent(attr, o);
            ParameterRef[] parms = o.parm_ids;
            ParameterRef[] newParms = new ParameterRef[parms.length + 2];
            System.arraycopy(parms, 0, newParms, 0, parms.length);
            newParms[newParms.length - 2] = new ParameterRef("itr_match", ""
                    + rs.getFloat("itr_match"));
            newParms[newParms.length - 1] = new ParameterRef("recFunc_id", ""
                    + rs.getInt("recFunc_id"));
            o.parm_ids = newParms;
            out.add(event);
        }
        return (CacheEvent[])out.toArray(new CacheEvent[0]);
    }
    
    public CachedResultPlusDbId[] getStationsByEvent(CacheEvent event, float gaussianWidth, float minPercentMatch, boolean withSeismograms) throws SQLException, NotFound, FileNotFoundException, FissuresException, IOException {
        int index = 1;
        getStationsByEventByPercent.setInt(index++, event.getDbId());
        getStationsByEventByPercent.setFloat(index++, gaussianWidth);
        getStationsByEventByPercent.setFloat(index++, minPercentMatch);
        ResultSet rs = getStationsByEventByPercent.executeQuery();
        ArrayList out = new ArrayList();
        while(rs.next()) {
            if (withSeismograms) {
                out.add(extract(rs));
            } else {
                out.add(extractWithoutSeismograms(rs));
            }
        }
        return (CachedResultPlusDbId[])out.toArray(new CachedResultPlusDbId[0]);
    }

    public CachedResult[] getByPercent(int netDbId,
                                       String stationCode,
                                       float gaussianWidth,
                                       float percentMatch)
            throws FileNotFoundException, FissuresException, NotFound,
            IOException, SQLException {
        int index = 1;
        getOriginByStationByPercent.setInt(index++, netDbId);
        getOriginByStationByPercent.setString(index++, stationCode);
        getOriginByStationByPercent.setFloat(index++, gaussianWidth);
        getOriginByStationByPercent.setFloat(index++, percentMatch);
        ArrayList out = new ArrayList();
        ResultSet rs = getOriginByStationByPercent.executeQuery();
        while(rs.next()) {
            out.add(extract(rs));
        }
        return (CachedResult[])out.toArray(new CachedResult[0]);
    }

    protected int populateGetStmt(PreparedStatement stmt,
                                  Origin prefOrigin,
                                  ChannelId[] channels) throws SQLException,
            NotFound {
        int originDbId = jdbcOrigin.getDBId(prefOrigin);
        int[] chanDbId = new int[channels.length];
        int zChan = -1;
        for(int i = 0; i < channels.length; i++) {
            if(channels[i].channel_code.endsWith("Z")) {
                zChan = i;
            }
        }
        if(zChan == -1) {
            throw new NotFound("Channel for Z not found");
        }
        for(int i = 0; i < channels.length; i++) {
            chanDbId[i] = jdbcChannel.getDBId(channels[i]);
        }
        int index = 1;
        stmt.setInt(index++, originDbId);
        stmt.setInt(index++, chanDbId[zChan]);
        switch(zChan){
            case 0:
                stmt.setInt(index++, chanDbId[1]);
                stmt.setInt(index++, chanDbId[2]);
                stmt.setInt(index++, chanDbId[1]);
                stmt.setInt(index++, chanDbId[2]);
                break;
            case 1:
                stmt.setInt(index++, chanDbId[0]);
                stmt.setInt(index++, chanDbId[2]);
                stmt.setInt(index++, chanDbId[0]);
                stmt.setInt(index++, chanDbId[2]);
                break;
            case 2:
                stmt.setInt(index++, chanDbId[0]);
                stmt.setInt(index++, chanDbId[1]);
                stmt.setInt(index++, chanDbId[0]);
                stmt.setInt(index++, chanDbId[1]);
                break;
            default:
                throw new NotFound("Channel for Z is " + zChan);
        }
        return index;
    }

    protected int populateGetStmt(PreparedStatement stmt,
                                  Origin prefOrigin,
                                  ChannelId[] channels,
                                  IterDeconConfig config) throws SQLException,
            NotFound {
        int index = populateGetStmt(stmt, prefOrigin, channels);
        stmt.setFloat(index++, config.gwidth);
        stmt.setFloat(index++, config.maxBumps);
        stmt.setFloat(index++, config.tol);
        return index;
    }

    protected File getDir(CacheEvent cacheEvent,
                          Channel chan,
                          float gaussianWidth) {
        if(dataDir == null) {
            dataDir = new File(dataDirectory);
            dataDir.mkdirs();
        }
        File gaussDir = new File(dataDir, "gauss_" + gaussianWidth);
        File eventDir = new File(gaussDir, eventFormatter.getResult(cacheEvent));
        File netDir = new File(eventDir, chan.get_id().network_id.network_code);
        File stationDir = new File(netDir, chan.get_id().station_code);
        return stationDir;
    }

    public JDBCEventAccess getJDBCEventAccess() {
        return jdbcEventAccess;
    }

    public JDBCSodConfig getJDBCSodConfig() {
        return jdbcSodConfig;
    }

    public JDBCOrigin getJDBCOrigin() {
        return jdbcOrigin;
    }

    public JDBCChannel getJDBCChannel() {
        return jdbcChannel;
    }

    public JDBCEventAttr getJDBCEventAttr() {
        return jdbcEventAttr;
    }

    private String dataDirectory;

    private File dataDir = null;

    private EventFormatter eventFormatter;

    private JDBCEventAccess jdbcEventAccess;

    private JDBCOrigin jdbcOrigin;

    private JDBCEventAttr jdbcEventAttr;

    private JDBCChannel jdbcChannel;

    private JDBCSodConfig jdbcSodConfig;

    private SeismogramFileTypes fileType = SeismogramFileTypes.SAC;

    private PreparedStatement putStmt, isCachedStmt, getConfigsStmt, getStmt,
            getByDbIdStmt, getOriginByStation, getOriginByStationByPercent,
            countOriginByStationByPercent, getStationsByEventByPercent,
            deleteStmt;

    private JDBCSequence receiverFunctionSeq;
    
    
    private class ChannelInCache {
        ChannelInCache(int dbid, Channel chan) {
            this.dbid = dbid;
            this.chan = chan;
        }
        int dbid;
        Channel chan;
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JDBCRecFunc.class);
}
