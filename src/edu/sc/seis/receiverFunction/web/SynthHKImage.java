package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.StackComplexity;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;


/**
 * @author crotwell
 * Created on Oct 21, 2005
 */
public class SynthHKImage extends SummaryHKStackImageServlet  {

    
    public SynthHKImage() throws SQLException, ConfigurationException, Exception {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public SumHKStack getSumStack(HttpServletRequest req,
                                  VelocityNetwork net,
                                  String staCode) throws Exception {
        SumHKStack stack = super.getSumStack(req, net, staCode);
        StackComplexity complexity = new StackComplexity(stack, 4096);
        StationResult model = new StationResult(net.get_id(), staCode, stack.getSum().getMaxValueH(stack.getSmallestH()), stack.getSum().getMaxValueK(stack.getSmallestH()), stack.getSum().getAlpha(), null);
        return new SumHKStack(stack.getMinPercentMatch(), stack.getSmallestH(), complexity.getSynthetic(model), -1, -1, stack.getNumEQ());
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SynthHKImage.class);
}
