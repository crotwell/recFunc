package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.StackComplexity;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;


/**
 * @author crotwell
 * Created on Oct 21, 2005
 */
public class ComplexityResidualImage extends SummaryHKStackImageServlet {


    public ComplexityResidualImage() throws SQLException, ConfigurationException, Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    public SumHKStack getSumStack(HttpServletRequest req,
                                  VelocityNetwork net,
                                  String staCode) throws Exception {
        SumHKStack stack = super.getSumStack(req, net, staCode);
        StackComplexity complexity = new StackComplexity(stack, 2048);
        StationResult model = new StationResult(net.get_id(), staCode, stack.getSum().getMaxValueH(stack.getSmallestH()), stack.getSum().getMaxValueK(stack.getSmallestH()), stack.getSum().getAlpha(), null);
        return new SumHKStack(stack.getMinPercentMatch(), stack.getSmallestH(), complexity.getResidual(model), -1, -1, stack.getNumEQ());
    }
}
