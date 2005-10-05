package edu.sc.seis.receiverFunction.web;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.AuditInfo;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.display.BackAzimuthDisplay;
import edu.sc.seis.fissuresUtil.display.RecordSectionDisplay;
import edu.sc.seis.fissuresUtil.display.registrar.PhaseAlignedTimeConfig;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.xml.DataSet;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSet;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.gee.organizer.DataSetEventOrganizer;
import edu.sc.seis.receiverFunction.compare.JDBCStationResult;
import edu.sc.seis.receiverFunction.compare.JDBCStationResultRef;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.JDBCSummaryHKStack;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

public class RecordSectionImage extends HttpServlet {

    public RecordSectionImage() throws SQLException, ConfigurationException,
            Exception {
        this("jdbc:postgresql:ears", RecFuncCacheImpl.getDataLoc());
    }

    public RecordSectionImage(String databaseURL, String dataloc)
            throws SQLException, ConfigurationException, Exception {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(databaseURL);
        DATA_LOC = dataloc;
        Connection conn = ConnMgr.createConnection();
        jdbcEventAccess = new JDBCEventAccess(conn);
        jdbcChannel = new JDBCChannel(conn);
        jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      DATA_LOC);
        jdbcHKStack = new JDBCHKStack(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      jdbcRecFunc);
        jdbcSummaryHKStack = new JDBCSummaryHKStack(jdbcHKStack);
        jdbcStationResult = new JDBCStationResult(jdbcChannel.getNetworkTable(),
                                                  new JDBCStationResultRef(conn));
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            int netDbId = RevUtil.getInt("netdbid", req);
            String staCode = req.getParameter("stacode");
            float match = RevUtil.getFloat("percentMatch", req, 80);
            CachedResult[] results = jdbcRecFunc.getByPercent(netDbId,
                                                              staCode,
                                                              match);
            DataSetEventOrganizer organizer = new DataSetEventOrganizer();
            DataSetSeismogram[] itrDSS = new DataSetSeismogram[results.length];
            for(int i = 0; i < itrDSS.length; i++) {
                itrDSS[i] = new MemoryDataSetSeismogram((LocalSeismogramImpl)results[i].radial);
                CacheEvent event = new CacheEvent(results[i].event_attr, results[i].prefOrigin);
                organizer.addSeismogram(itrDSS[i], event, emptyAudit);
                Channel chan = results[i].channels[2];
                chan = new ChannelImpl(results[i].radial.channel_id,
                                       chan.name, chan.an_orientation, chan.sampling_info, chan.effective_time, chan.my_site);
                organizer.addChannel(chan, event, emptyAudit);
                Channel outchan = itrDSS[i].getChannel();
                System.out.println("outchan: "+outchan);
            }
            
            int xdim = RevUtil.getInt("xdim", req, xdimDefault);
            int ydim = RevUtil.getInt("ydim", req, ydimDefault);
            System.out.println("record section dim: "+xdim+", "+ydim);
            Dimension dim = new Dimension(xdim, ydim);
            res.setContentType("image/png");
            OutputStream out = res.getOutputStream();
            RecordSectionDisplay disp;
            String recordSectionType = RevUtil.get("type", req, "dist");
            System.out.println("Record Section TYpe = "+recordSectionType);
            if (recordSectionType.equals("dist")) {
                disp = new RecordSectionDisplay();
            } else if (recordSectionType.equals("baz")) {
                disp = new BackAzimuthDisplay();
            } else {
                throw new RuntimeException("Unknown type: "+recordSectionType);
            }
            disp.setMinSeisPixelHeight(RevUtil.getInt("minSeisHeight",req, 80));
            disp.setTimeConfig(new PhaseAlignedTimeConfig("ttp"));
            disp.add(itrDSS);
            disp.outputToPNG(out, dim);
            out.close();
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
            throw new ServletException(e);
        }
    }

    String DATA_LOC;

    JDBCEventAccess jdbcEventAccess;

    JDBCChannel jdbcChannel;

    JDBCHKStack jdbcHKStack;

    JDBCSummaryHKStack jdbcSummaryHKStack;

    JDBCRecFunc jdbcRecFunc;

    JDBCSodConfig jdbcSodConfig;

    JDBCStationResult jdbcStationResult;

    AuditInfo[] emptyAudit = new AuditInfo[0];

    public static int xdimDefault = 800;

    public static int ydimDefault = 800;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecordSectionImage.class);
}
