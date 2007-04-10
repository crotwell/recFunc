package edu.sc.seis.receiverFunction.web;

import java.awt.Dimension;
import java.io.IOException;
import java.io.EOFException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.AuditInfo;
import edu.iris.Fissures.Sampling;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
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
import edu.sc.seis.receiverFunction.RecFunc;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.receiverFunction.server.SyntheticFactory;
import edu.sc.seis.receiverFunction.synth.SimpleSynthReceiverFunction;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
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
        jdbcRecFunc = new JDBCRecFunc(ConnMgr.createConnection(), RecFuncCacheImpl.getDataLoc());
    }

    public synchronized void doGet(HttpServletRequest req,
                                   HttpServletResponse res) throws IOException {
        try {
            logger.debug("doGet called");
            if(req.getParameter("rf") == null) {
                throw new Exception("rf param not set");
            }
            relTime = new PhaseAlignedTimeConfig("ttp");
            CachedResult result;
            if(req.getParameter("rf").equals("synth")) {
                result = SyntheticFactory.getCachedResult();
            } else {
                int rf_id = new Integer(req.getParameter("rf")).intValue();
                synchronized(jdbcRecFunc.getConnection()) {
                    result = jdbcRecFunc.get(rf_id).getCachedResult();
                }
            }
            int xdim = RevUtil.getInt("xdim", req, xdimDefault);
            int ydim = RevUtil.getInt("ydim", req, ydimDefault);
            TimeInterval prePhase = new TimeInterval(RevUtil.getFloat("prePhase",
                                                                      req,
                                                                      10),
                                                     UnitImpl.SECOND);
            TimeInterval window = new TimeInterval(RevUtil.getFloat("window",
                                                                    req,
                                                                    120),
                                                   UnitImpl.SECOND);
            Dimension dim = new Dimension(xdim, ydim);
            OutputStream out = res.getOutputStream();
            if(result == null) {
                return;
            }
            MultiSeismogramWindowDisplay disp = new MultiSeismogramWindowDisplay(new SeismogramSorter());
            disp.setTimeConfig(relTime);
            DataSetSeismogram[] dss = getDSS(result);
            disp.add(dss);
            disp.get(dss[1]).add(new DataSetSeismogram[] {dss[0]});
            MemoryDataSetSeismogram synthDSS = null;
            if(RevUtil.get("H", req, null) != null
                    && !RevUtil.get("H", req, null).equals("$maxValueH")
                    && RevUtil.get("vpvs", req, null) != null
                    && !RevUtil.get("vpvs", req, null).equals("$maxValueK")) {
                float h = RevUtil.getFloat("H", req);
                float vpvs = RevUtil.getFloat("vpvs", req);
                StationResult staResult = new StationResult(result.channels[0].get_id().network_id,
                                                            result.channels[0].get_code(),
                                                            new QuantityImpl(h,
                                                                             UnitImpl.KILOMETER),
                                                            vpvs,
                                                            crust2.getStationResult(result.channels[0].my_site.my_station)
                                                                    .getVp(),
                                                            null);
                Sampling samp = new SamplingImpl(10,
                                                 new TimeInterval(1,
                                                                  UnitImpl.SECOND));
                SimpleSynthReceiverFunction synth = new SimpleSynthReceiverFunction(staResult,
                                                                                    samp,
                                                                                    2048);
                DistAz distAz = new DistAz(result.channels[0],
                                           result.prefOrigin);
                Arrival[] arrivals = TauPUtil.getTauPUtil()
                        .calcTravelTimes(distAz.getDelta(),
                                         0,
                                         new String[] {"P"});
                float flatRP = (float)arrivals[0].getRayParam() / 6371;
                MicroSecondDate pTime = new MicroSecondDate(result.prefOrigin.origin_time);
                pTime = pTime.add(new TimeInterval(arrivals[0].getTime(),
                                                   UnitImpl.SECOND));
                // pTime = pTime.subtract(RecFunc.DEFAULT_SHIFT);
                pTime = dss[0].getBeginMicroSecondDate();
                logger.debug("synth begin=" + pTime + "  radial begin="
                        + dss[0].getBeginMicroSecondDate());
                LocalSeismogramImpl synthRadial = synth.calculate(flatRP,
                                                                  pTime.getFissuresTime(),
                                                                  RecFunc.DEFAULT_SHIFT,
                                                                  dss[0].getChannelId(),
                                                                  result.config.gwidth);

                
                synthDSS = new MemoryDataSetSeismogram(synthRadial);
                createChannel(synthRadial.channel_id, result.channels[0], dss[0].getDataSet());
                dss[0].getDataSet().addDataSetSeismogram(synthDSS,
                                                         new AuditInfo[0]);
                disp.get(dss[0]).add(new DataSetSeismogram[] {synthDSS});
                logger.debug(" max amp data: "
                        + new Statistics(((MemoryDataSetSeismogram)dss[0]).getCache()[0].get_as_floats()).max());
                logger.debug("max amp synth: "
                        + new Statistics(synthDSS.getCache()[0].get_as_floats()).max());
            }
            logger.debug("before shale data: "
                    + disp.getTimeConfig().getTime(dss[0]));
            logger.debug("            synth: "
                    + disp.getTimeConfig().getTime(synthDSS));
            MicroSecondTimeRange mstr = disp.getTimeConfig().getTime();
            disp.getTimeConfig()
                    .shaleTime(prePhase.divideBy(mstr.getInterval())
                                       .getValue(SEC_PER_SEC),
                               1);
            disp.getTimeConfig().shaleTime(0,
                                           window.divideBy(mstr.getInterval())
                                                   .getValue(SEC_PER_SEC));
            logger.debug("after shale data: "
                    + disp.getTimeConfig().getTime(dss[0]));
            logger.debug("            synth: "
                    + disp.getTimeConfig().getTime(synthDSS));
            res.setContentType("image/png");
            disp.outputToPNG(out, dim);
            out.close();
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            writer.write("<html><body><p>No waveforms found for id "
                    + req.getParameter("rf") + "</p></body></html>");
            writer.flush();
        } catch(EOFException e) {
            //oh well...
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        }
    }

    public DataSetSeismogram[] getDSS(CachedResult stack) {
        CacheEvent event = new CacheEvent(stack.event_attr, stack.prefOrigin);
        // radial RF
        LocalSeismogramImpl radial = (LocalSeismogramImpl)stack.radial;
        MemoryDataSetSeismogram radialDSS = new MemoryDataSetSeismogram(radial,
                                                                        "radial RF");
        DataSet dataset = new MemoryDataSet("temp", "Temp Dataset for "
                + radialDSS.getName(), "temp", new AuditInfo[0]);
        dataset.addDataSetSeismogram(radialDSS, emptyAudit);
        createChannel(radial.channel_id, stack.channels[0], dataset);
        // tangential RF
        LocalSeismogramImpl tangential = (LocalSeismogramImpl)stack.tansverse;
        MemoryDataSetSeismogram tangentialDSS = new MemoryDataSetSeismogram(tangential,
                                                                            "tangential RF");
        dataset.addDataSetSeismogram(tangentialDSS, emptyAudit);
        createChannel(tangential.channel_id, stack.channels[0], dataset);
        // Z
        LocalSeismogramImpl zSeis = (LocalSeismogramImpl)stack.original[0];
        MemoryDataSetSeismogram zDSS = new MemoryDataSetSeismogram(zSeis,
                                                                   zSeis.getName());
        dataset.addDataSetSeismogram(zDSS, emptyAudit);
        String channelParamName = StdDataSetParamNames.CHANNEL
                + ChannelIdUtil.toString(stack.channels[0].get_id());
        dataset.addParameter(channelParamName, stack.channels[0], emptyAudit);
        // a horizontal
        LocalSeismogramImpl aSeis = (LocalSeismogramImpl)stack.original[1];
        MemoryDataSetSeismogram aDSS = new MemoryDataSetSeismogram(aSeis,
                                                                   aSeis.getName());
        dataset.addDataSetSeismogram(aDSS, emptyAudit);
        channelParamName = StdDataSetParamNames.CHANNEL
                + ChannelIdUtil.toString(stack.channels[1].get_id());
        dataset.addParameter(channelParamName, stack.channels[1], emptyAudit);
        // b horizontal
        LocalSeismogramImpl bSeis = (LocalSeismogramImpl)stack.original[2];
        MemoryDataSetSeismogram bDSS = new MemoryDataSetSeismogram(bSeis,
                                                                   bSeis.getName());
        channelParamName = StdDataSetParamNames.CHANNEL
                + ChannelIdUtil.toString(stack.channels[2].get_id());
        dataset.addParameter(channelParamName, stack.channels[2], emptyAudit);
        dataset.addDataSetSeismogram(bDSS, emptyAudit);
        dataset.addParameter(DataSet.EVENT, event, emptyAudit);
        return new DataSetSeismogram[] {radialDSS,
                                        tangentialDSS,
                                        zDSS,
                                        aDSS,
                                        bDSS};
    }

    public Channel createChannel(ChannelId newId, Channel old, DataSet ds) {
        Channel out =  new ChannelImpl(newId,
                               "fake chan " + ChannelIdUtil.toString(newId),
                               old.an_orientation,
                               old.sampling_info,
                               old.effective_time,
                               old.my_site);
        ds.addParameter(StdDataSetParamNames.CHANNEL
                         + ChannelIdUtil.toString(newId),
                         out,
                         new AuditInfo[0]);
        return out;
    }

    public void destroy() {
        try {
            Connection conn = jdbcRecFunc.getConnection();
            if(conn != null) {
                conn.close();
            }
        } catch(SQLException e) {
            // oh well
        }
        super.destroy();
    }

    private static final UnitImpl SEC_PER_SEC = UnitImpl.divide(UnitImpl.SECOND,
                                                                UnitImpl.SECOND);

    AuditInfo[] emptyAudit = new AuditInfo[0];

    public static int xdimDefault = 800;

    public static int ydimDefault = 800;

    JDBCRecFunc jdbcRecFunc;

    Crust2 crust2 = new Crust2();

    PhaseAlignedTimeConfig relTime;

    private SeismogramDisplayConfiguration sdc = new SeismogramDisplayConfiguration();

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SeismogramImage.class);
}
