package edu.sc.seis.receiverFunction.web;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.AuditInfo;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.display.MicroSecondTimeRange;
import edu.sc.seis.fissuresUtil.display.MultiSeismogramWindowDisplay;
import edu.sc.seis.fissuresUtil.display.SeismogramSorter;
import edu.sc.seis.fissuresUtil.display.configuration.SeismogramDisplayConfiguration;
import edu.sc.seis.fissuresUtil.display.registrar.PhaseAlignedTimeConfig;
import edu.sc.seis.fissuresUtil.xml.DataSet;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSet;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.StdDataSetParamNames;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;

/**
 * @author crotwell Created on Feb 24, 2005
 */
public class SeismogramImage extends HttpServlet {

    /**
     * @throws Exception
     * @throws ConfigurationException
     * @throws SQLException
     */
    public SeismogramImage() throws SQLException, ConfigurationException,
            Exception {
        Connection conn = ConnMgr.createConnection();
        JDBCEventAccess jdbcEvent = new JDBCEventAccess(conn);
        JDBCChannel jdbcChannel = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn,
                                      jdbcEvent,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      RecFuncCacheImpl.getDataLoc());
        relTime.setPhaseName("ttp");
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        try {
            logger.debug("doGet called");
            if(req.getParameter("rf") == null) { throw new Exception("rf param not set"); }
            int rf_id = new Integer(req.getParameter("rf")).intValue();
            int xdim = RevUtil.getInt("xdim", req, xdimDefault);
            int ydim = RevUtil.getInt("ydim", req, ydimDefault);
            Dimension dim = new Dimension(xdim, ydim);
            CachedResult stack = jdbcRecFunc.get(rf_id);
            CacheEvent event = new CacheEvent(stack.event_attr,
                                              stack.prefOrigin);
            OutputStream out = res.getOutputStream();
            if(stack == null) { return; }
            MultiSeismogramWindowDisplay disp = new MultiSeismogramWindowDisplay(new SeismogramSorter());
            disp.setTimeConfig(relTime);
            LocalSeismogramImpl radial = (LocalSeismogramImpl)stack.radial;
            MemoryDataSetSeismogram radialDSS = new MemoryDataSetSeismogram(radial,
                                                                            "radial RF");
            DataSet dataset = new MemoryDataSet("temp", "Temp Dataset for "
                    + radialDSS.getName(), "temp", new AuditInfo[0]);
            dataset.addDataSetSeismogram(radialDSS, emptyAudit);
            String channelParamName = StdDataSetParamNames.CHANNEL
                    + ChannelIdUtil.toString(radial.channel_id);
            dataset.addParameter(channelParamName,
                                 stack.channels[0],
                                 emptyAudit);
            LocalSeismogramImpl zSeis = (LocalSeismogramImpl)stack.original[0];
            MemoryDataSetSeismogram zDSS = new MemoryDataSetSeismogram(zSeis,
                                                                       zSeis.getName());
            dataset.addDataSetSeismogram(zDSS, emptyAudit);
            channelParamName = StdDataSetParamNames.CHANNEL
                    + ChannelIdUtil.toString(stack.channels[0].get_id());
            dataset.addParameter(channelParamName,
                                 stack.channels[0],
                                 emptyAudit);
            LocalSeismogramImpl aSeis = (LocalSeismogramImpl)stack.original[1];
            MemoryDataSetSeismogram aDSS = new MemoryDataSetSeismogram(aSeis,
                                                                       aSeis.getName());
            dataset.addDataSetSeismogram(aDSS, emptyAudit);
            channelParamName = StdDataSetParamNames.CHANNEL
                    + ChannelIdUtil.toString(stack.channels[1].get_id());
            dataset.addParameter(channelParamName,
                                 stack.channels[1],
                                 emptyAudit);
            LocalSeismogramImpl bSeis = (LocalSeismogramImpl)stack.original[2];
            MemoryDataSetSeismogram bDSS = new MemoryDataSetSeismogram(bSeis,
                                                                       bSeis.getName());
            channelParamName = StdDataSetParamNames.CHANNEL
                    + ChannelIdUtil.toString(stack.channels[2].get_id());
            System.out.println("Chan: "
                    + channelParamName
                    + "  seis:"
                    + ChannelIdUtil.toString(bDSS.getRequestFilter().channel_id));
            dataset.addParameter(channelParamName,
                                 stack.channels[2],
                                 emptyAudit);
            dataset.addDataSetSeismogram(bDSS, emptyAudit);
            Origin o = stack.prefOrigin;
            dataset.addParameter(DataSet.EVENT, event, emptyAudit);
            disp.add(new DataSetSeismogram[] {radialDSS, zDSS, aDSS, bDSS});
            MicroSecondTimeRange mstr = disp.getTimeConfig().getTime();
            TimeInterval window = new TimeInterval(120, UnitImpl.SECOND);
            disp.getTimeConfig().shaleTime(0,
                                           window.divideBy(mstr.getInterval())
                                                   .convertTo(SEC_PER_SEC)
                                                   .get_value());
            System.out.println(window.divideBy(mstr.getInterval()));
            res.setContentType("image/png");
            disp.outputToPNG(out, dim);
            out.close();
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            System.out.println("No waveforms found for id "
                    + req.getParameter("rf"));
            writer.write("<html><body><p>No waveforms found for id "
                    + req.getParameter("rf") + "</p></body></html>");
            writer.flush();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static final UnitImpl SEC_PER_SEC = UnitImpl.divide(UnitImpl.SECOND,
                                                   UnitImpl.SECOND);

    AuditInfo[] emptyAudit = new AuditInfo[0];

    public static int xdimDefault = 800;

    public static int ydimDefault = 800;

    JDBCRecFunc jdbcRecFunc;

    PhaseAlignedTimeConfig relTime = new PhaseAlignedTimeConfig();

    private SeismogramDisplayConfiguration sdc = new SeismogramDisplayConfiguration();

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SeismogramImage.class);
}