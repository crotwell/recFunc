package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import edu.iris.Fissures.FissuresException;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.receiverFunction.StackComplexity;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Oct 21, 2005
 */
public class SynthHKImage extends SummaryHKStackImageServlet {

    public SynthHKImage() throws SQLException, ConfigurationException,
            Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    public SumHKStack getSumStack(HttpServletRequest req,
                                  VelocityNetwork net,
                                  String staCode) throws Exception {
        SumHKStack stack = super.getSumStack(req, net, staCode);
        return getSynthStack(stack, req, net, staCode);
    }

    public static SumHKStack getSynthStack(SumHKStack stack,
                                           HttpServletRequest req,
                                           VelocityNetwork net,
                                           String staCode)
            throws FissuresException, TauModelException {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        gaussianWidth = RevUtil.getFloat("synthGaussian", req, gaussianWidth);
        float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch());
        StackComplexity complexity = new StackComplexity(stack.getSum(),
                                                         4096,
                                                         gaussianWidth);
        StationResult model = new StationResult(net.getWrapped(),
                                                staCode,
                                                stack.getSum()
                                                        .getMaxValueH(stack.getSmallestH()),
                                                stack.getSum()
                                                        .getMaxValueK(stack.getSmallestH()),
                                                stack.getSum().getAlpha(),
                                                null);
        String distList = RevUtil.get("dist", req, "60");
        ArrayList<ReceiverFunctionResult> individuals = new ArrayList<ReceiverFunctionResult>();
        StringTokenizer tokenizer = new StringTokenizer(distList, ",");
        while(tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            float dist = Float.parseFloat(token);
            individuals.add(complexity.getSyntheticForDist(model, dist));
        }
        SumHKStack distStack = SumHKStack.calculateForPhase(individuals,
                                                            stack.getSmallestH(),
                                                            minPercentMatch,
                                                            true,
                                                            new HashSet<RejectedMaxima>(),
                                                            false,
                                                            0,
                                                            "all");
        return distStack;
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SynthHKImage.class);
}
