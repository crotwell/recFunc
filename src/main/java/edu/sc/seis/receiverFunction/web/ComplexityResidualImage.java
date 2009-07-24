package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.fissuresUtil.display.BorderedDisplay;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.HKStackImage;
import edu.sc.seis.receiverFunction.StackComplexity;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Oct 21, 2005
 */
public class ComplexityResidualImage extends SummaryHKStackImageServlet {

    public ComplexityResidualImage() throws SQLException,
            ConfigurationException, Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    public SumHKStack getSumStack(HttpServletRequest req,
                                  VelocityNetwork net,
                                  String staCode) throws Exception {
        SumHKStack stack = super.getSumStack(req, net, staCode);
        lastStackMax = stack.getSum().getMaxValue();
        SumHKStack synth = SynthHKImage.getSynthStack(stack, req, net, staCode);
        return new SumHKStack(stack.getMinPercentMatch(),
                              stack.getSmallestH(),
                              StackComplexity.getResidual(stack.getSum(),
                                                          synth.getSum()),
                              -1,
                              -1,
                              stack.getNumEQ(),
                              stack.getIndividuals(),
                              stack.getRejectedMaxima());
    }

    void output(SumHKStack sumStack,
                BufferedOutputStream out,
                HttpServletRequest req,
                HttpServletResponse res) throws IOException {
        BorderedDisplay comp = sumStack.getSum().getStackComponent(HKStack.ALL);
        ((HKStackImage)comp.get(BorderedDisplay.CENTER)).setColorMapMax(lastStackMax);
        BufferedImage image = sumStack.getSum().toImage(comp, "Complexity Residual "+NetworkIdUtil.toStringNoDates(sumStack.getNet())+"."+sumStack.getStaCode());
        logger.debug("finish create image");
        res.setContentType("image/png");
        ImageIO.write(image, "png", out);
        out.close();
    }

    private float lastStackMax;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ComplexityResidualImage.class);
}
