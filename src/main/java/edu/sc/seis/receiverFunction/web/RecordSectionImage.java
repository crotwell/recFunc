package edu.sc.seis.receiverFunction.web;

import java.awt.Dimension;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.AuditInfo;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.dataset.DataSetEventOrganizer;
import edu.sc.seis.fissuresUtil.display.BackAzimuthDisplay;
import edu.sc.seis.fissuresUtil.display.MicroSecondTimeRange;
import edu.sc.seis.fissuresUtil.display.RecordSectionDisplay;
import edu.sc.seis.fissuresUtil.display.registrar.PhaseAlignedTimeConfig;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.servlets.image.ImageServlet;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

public class RecordSectionImage extends ImageServlet {

    public RecordSectionImage() throws SQLException, ConfigurationException,
            Exception {
        DATA_LOC = Start.getDataLoc();
    }

    protected synchronized void writeImage(HttpServletRequest req,
                                      HttpServletResponse res)
            throws ServletException, IOException, NotFound {
            VelocityNetwork net = Start.getNetwork(req);
            int netDbId = net.getDbId();
            String staCode = RevUtil.get("stacode", req);
            float gaussianWidth = RevUtil.getFloat("gaussian",
                                                   req,
                                                   Start.getDefaultGaussian());
            float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                     req,
                                                     Start.getDefaultMinPercentMatch());
            TimeInterval prePhase = new TimeInterval(RevUtil.getFloat("prePhase",
                                                                      req,
                                                                      10),
                                                     UnitImpl.SECOND);
            TimeInterval window = new TimeInterval(RevUtil.getFloat("window",
                                                                    req,
                                                                    120),
                                                   UnitImpl.SECOND);
            List<ReceiverFunctionResult> results = RecFuncDB.getSingleton().getSuccessful(net.getWrapped(), 
                                                staCode,
                                                gaussianWidth);
            DataSetEventOrganizer organizer = new DataSetEventOrganizer();
            DataSetSeismogram[] itrDSS = new DataSetSeismogram[results.size()];
            int i = 0;
            for(ReceiverFunctionResult cr : results) {
                itrDSS[i] = new MemoryDataSetSeismogram((LocalSeismogramImpl)cr.getRadial());
                CacheEvent cEvent = cr.getEvent();
                VelocityEvent event = new VelocityEvent(cEvent);
                itrDSS[i].setName(event.getTime());
                organizer.addSeismogram(itrDSS[i], event, emptyAudit);
                Channel chan = cr.getChannelGroup().getChannel3();
                chan = new ChannelImpl(cr.getRadial().channel_id,
                                       chan.getName(),
                                       chan.getOrientation(),
                                       chan.getSamplingInfo(),
                                       chan.getEffectiveTime(),
                                       chan.getSite());
                organizer.addChannel(chan, event, emptyAudit);
                Channel outchan = itrDSS[i].getChannel();
                i++;
            }
            int xdim = RevUtil.getInt("xdim", req, xdimDefault);
            int ydim = RevUtil.getInt("ydim", req, ydimDefault);
            Dimension dim = new Dimension(xdim, ydim);
            res.setContentType("image/png");
            OutputStream out = res.getOutputStream();
            RecordSectionDisplay disp;
            String recordSectionType = RevUtil.get("type", req, "dist");
            if(recordSectionType.equals("dist")) {
                disp = new RecordSectionDisplay();
            } else if(recordSectionType.equals("baz")) {
                disp = new BackAzimuthDisplay();
            } else {
                throw new RuntimeException("Unknown type: " + recordSectionType);
            }
            disp.setMinSeisPixelHeight(RevUtil.getInt("minSeisHeight", req, 80));
            PhaseAlignedTimeConfig timeConfig = new PhaseAlignedTimeConfig("ttp");
            disp.setTimeConfig(timeConfig);
            disp.add(itrDSS);
            MicroSecondTimeRange mstr = disp.getTimeConfig().getTime();
            disp.getTimeConfig()
                    .shaleTime(prePhase.divideBy(mstr.getInterval())
                                       .getValue(SEC_PER_SEC),
                               1);
            disp.getTimeConfig().shaleTime(0,
                                           window.divideBy(mstr.getInterval())
                                                   .getValue(SEC_PER_SEC));
            disp.outputToPNG(out, dim);
            out.close();
    }

    String DATA_LOC;

    AuditInfo[] emptyAudit = new AuditInfo[0];

    public static int xdimDefault = 800;

    public static int ydimDefault = 800;

    private static final UnitImpl SEC_PER_SEC = UnitImpl.divide(UnitImpl.SECOND,
                                                                UnitImpl.SECOND);

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecordSectionImage.class);
}
