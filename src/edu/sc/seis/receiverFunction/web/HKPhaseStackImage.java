package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKPhaseStack;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Apr 27, 2005
 */
public class HKPhaseStackImage extends HKStackImageServlet {

    /**
     *
     */
    public HKPhaseStackImage() throws SQLException, ConfigurationException,
            Exception {
        super();
        // TODO Auto-generated constructor stub
    }
    

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        try {
            logger.debug("doGet called");
            if(req.getParameter("rf") == null) { throw new Exception("rf param not set"); }
            int rf_id = RevUtil.getInt("rf", req);
            CachedResult result = hkStack.getJDBCRecFunc().get(rf_id);
            
            HKPhaseStack stack = (HKPhaseStack)HKPhaseStack.create(result, .3f, .3f, .3f);
            OutputStream out = res.getOutputStream();
            if (stack == null) {
                return;
            }
            BufferedImage image = stack.createStackImage();
            
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
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKPhaseStackImage.class);
}
