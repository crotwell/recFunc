package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.IncompatibleSeismograms;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.RecFuncException;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.server.SyntheticFactory;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.locator.StationLocator;
import edu.sc.seis.sod.ConfigurationException;

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
    }

    public synchronized void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        try {
            logger.debug("doGet called");
            if(req.getParameter("rf") == null) { throw new Exception("rf param not set"); }
            ReceiverFunctionResult result = getCachedResult(req);
            HKStack stack = result.getHKstack();
            
            String phase = RevUtil.get("phase", req, "all");
            OutputStream out = res.getOutputStream();
            if (stack == null) {
                return;
            }
            String title = ChannelIdUtil.toStringNoDates(result.getChannelGroup().getChannel1());
            BufferedImage image;
            if (phase.equals("all")) {
                image = stack.createStackImage(title);
            } else {
                image = stack.createStackImage(phase, title);
            }
            res.setContentType("image/png");
            ImageIO.write(image, "png", out);
            out.close();
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            writer.write("<html><body><p>No HK stack foundfor id "+req.getParameter("rf")+"</p></body></html>");
            writer.flush();
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        }
    }
    
    public HKStack getStack(HttpServletRequest req) throws Exception {
        return getCachedResult(req).getHKstack();
    }

    ReceiverFunctionResult getCachedResult(HttpServletRequest req)
            throws NoPreferredOrigin, IncompatibleSeismograms,
            TauModelException, RecFuncException, FileNotFoundException, FissuresException, NotFound, IOException, SQLException {
        if (req.getParameter("rf").equals("synth")) {
            return SyntheticFactory.getReceiverFunctionResult();
        } else {
            int rf_id = RevUtil.getInt("rf", req);
                return RecFuncDB.getSingleton().getReceiverFunctionResult(rf_id);
        }
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKStackImageServlet.class);
    
}