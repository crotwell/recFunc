package edu.sc.seis.receiverFunction.web;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.AuditInfo;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.cache.EventUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.display.DisplayUtils;
import edu.sc.seis.fissuresUtil.display.SeismogramDisplay;
import edu.sc.seis.fissuresUtil.display.configuration.SeismogramDisplayConfiguration;
import edu.sc.seis.fissuresUtil.display.registrar.PhaseAlignedTimeConfig;
import edu.sc.seis.fissuresUtil.xml.DataSet;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSet;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.process.waveform.SeismogramImageProcess;


/**
 * @author crotwell
 * Created on Feb 24, 2005
 */
public class SeismogramImage extends HttpServlet {

    /**
     * @throws Exception
     * @throws ConfigurationException
     * @throws SQLException
     *
     */
    public SeismogramImage() throws SQLException, ConfigurationException, Exception {
        Connection conn = ConnMgr.createConnection();
        JDBCEventAccess jdbcEvent = new JDBCEventAccess(conn);
        
        JDBCChannel jdbcChannel  = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn, jdbcEvent, jdbcChannel, jdbcSodConfig, RecFuncCacheImpl.getDataLoc());
        
        //imager = new SeismogramImageProcess();
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
            CacheEvent event = new CacheEvent(stack.event_attr, stack.prefOrigin);
            OutputStream out = res.getOutputStream();
            if (stack == null) {
                return;
            }

            SeismogramDisplay bsd = sdc.createDisplay();

            bsd.setTimeConfig(relTime);
            
            LocalSeismogramImpl radial = (LocalSeismogramImpl)stack.radial;
            MemoryDataSetSeismogram memDSS = new MemoryDataSetSeismogram(radial, "radial");
            DataSet dataset = new MemoryDataSet("temp", "Temp Dataset for "
                    + memDSS.getName(), "temp", new AuditInfo[0]);
            dataset.addDataSetSeismogram(memDSS, new AuditInfo[0]);
            
            Origin o = stack.prefOrigin;
            dataset.addParameter(DataSet.EVENT, event, new AuditInfo[0]);
            
            bsd.outputToPNG(out, dim);
            res.setContentType("image/png");
            out.close();
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            System.out.println("No HKStack found for id "+req.getParameter("rf"));
            writer.write("<html><body><p>No HK stack foundfor id "+req.getParameter("rf")+"</p></body></html>");
            writer.flush();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public static int xdimDefault = 600;
    
    public static int ydimDefault = 150;
    
    JDBCRecFunc jdbcRecFunc;
    
    PhaseAlignedTimeConfig relTime = new PhaseAlignedTimeConfig();
    
    private SeismogramDisplayConfiguration sdc = new SeismogramDisplayConfiguration();

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SeismogramImage.class);
    
}
