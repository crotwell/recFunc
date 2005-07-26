package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.IncompatibleSeismograms;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.RecFuncException;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.receiverFunction.server.SyntheticFactory;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.locator.StationLocator;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelCookieJar;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelStatus;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Feb 16, 2005
 */
public class HKStackImageServlet  extends HttpServlet {

    /**
     * @throws SQLException
     * @throws ConfigurationException
     * @throws Exception
     * @throws 
     * @throws ConfigurationException
     *
     */
    public HKStackImageServlet() throws SQLException, ConfigurationException, Exception {
        Connection conn = ConnMgr.createConnection();
        jdbcEvent = new JDBCEventAccess(conn);
        
        JDBCChannel jdbcChannel  = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn, jdbcEvent, jdbcChannel, jdbcSodConfig, RecFuncCacheImpl.getDataLoc());
        hkStack = new JDBCHKStack(conn, jdbcEvent, jdbcChannel, jdbcSodConfig, jdbcRecFunc);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        try {
            logger.debug("doGet called");
            if(req.getParameter("rf") == null) { throw new Exception("rf param not set"); }

            HKStack stack;
            if (req.getParameter("rf").equals("synth")) {
                 stack = SyntheticFactory.getHKStack();
            } else {
                int rf_id = RevUtil.getInt("rf", req);
                stack = hkStack.get(rf_id);
            }
            
            String phase = RevUtil.get("phase", req, "all");
            OutputStream out = res.getOutputStream();
            if (stack == null) {
                return;
            }
            BufferedImage image;
            if (phase.equals("all")) {
                image = stack.createStackImage();
            } else {
                System.out.println("phase arg is "+phase);
                image = stack.createStackImage(phase);
            }
            res.setContentType("image/png");
            ImageIO.write(image, "png", out);
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

    CachedResult getCachedResult(HttpServletRequest req)
            throws NoPreferredOrigin, IncompatibleSeismograms,
            TauModelException, RecFuncException, FileNotFoundException, FissuresException, NotFound, IOException, SQLException {
        if (req.getParameter("rf").equals("synth")) {
            return SyntheticFactory.getCachedResult();
        } else {
            int rf_id = RevUtil.getInt("rf", req);
            return hkStack.getJDBCRecFunc().get(rf_id);
        }
    }
    
    private StationLocator staLoc;
    
    private JDBCEventAccess jdbcEvent;
    
    protected JDBCHKStack hkStack;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKStackImageServlet.class);
    
}