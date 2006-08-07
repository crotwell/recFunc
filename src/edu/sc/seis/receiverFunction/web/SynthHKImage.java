package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.FissuresException;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.StackComplexity;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.server.HKBox;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Oct 21, 2005
 */
public class SynthHKImage extends SummaryHKStackImageServlet {

    public SynthHKImage() throws SQLException, ConfigurationException, Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    public SumHKStack getSumStack(HttpServletRequest req, VelocityNetwork net, String staCode) throws Exception {
        SumHKStack stack = super.getSumStack(req, net, staCode);
        return getSynthStack(stack, req, net, staCode);
    }
    
    public static SumHKStack getSynthStack(SumHKStack stack, HttpServletRequest req, VelocityNetwork net, String staCode) throws FissuresException, TauModelException {
        float gaussianWidth = RevUtil.getFloat("gaussian", req, Start.getDefaultGaussian());
        gaussianWidth =  RevUtil.getFloat("synthGaussian", req, gaussianWidth);
        float minPercentMatch = RevUtil.getFloat("minPercentMatch", req, Start.getDefaultMinPercentMatch());
        StackComplexity complexity = new StackComplexity(stack.getSum(), 4096, gaussianWidth);
        StationResult model = new StationResult(net.get_id(),
                                                staCode, 
                                                stack.getSum().getMaxValueH(stack.getSmallestH()),
                                                stack.getSum().getMaxValueK(stack.getSmallestH()),
                                                stack.getSum().getAlpha(), null);
        String distList = RevUtil.get("dist", req, "60");
        ArrayList individuals = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(distList, ",");
        while(tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            float dist = Float.parseFloat(token);
            HKStack hk = complexity.getSyntheticForDist(model, dist);
            individuals.add(hk);
        }
        HKStack[] stacks = (HKStack[])individuals.toArray(new HKStack[0]);
        SumHKStack distStack =  new SumHKStack(stacks, stacks[0].getChannel(), -1, stack.getSmallestH(), false, true, new HKBox[0]);
        return distStack;   
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SynthHKImage.class);
}
