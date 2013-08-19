package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.receiverFunction.BazIterator;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.server.StackSummary;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.servlets.image.ImageServlet;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Feb 23, 2005
 */
public class SummaryHKStackImageServlet extends ImageServlet {

    public synchronized void writeImage(HttpServletRequest req,
                                   HttpServletResponse res) throws Exception {
            logger.debug("SummaryHKStackImageServlet.doGet() called");
            VelocityNetwork net = Start.getNetwork(req);
            String staCode = RevUtil.get("stacode", req);
            SumHKStack sumStack = getSumStack(req, net, staCode);
            logger.info("before check for null");
            if(sumStack == null) {
                logger.warn("summary stack is null for " + net.getCode() + "."
                        + staCode);
                return;
            }
            if(sumStack.getSum() == null
                    || sumStack.getSum().getStack().length == 0) {
                logger.warn("summary hkstack is null for " + net.getCode()
                        + "." + staCode);
            }
            BufferedOutputStream out = new BufferedOutputStream(res.getOutputStream());
            output(sumStack, out, req, res);
            logger.debug("SummaryHKStackImageServlet.doGet() end");
    }

    public SumHKStack getSumStack(HttpServletRequest req,
                                  VelocityNetwork net,
                                  String staCode) throws Exception {
        // possible that there are multiple stations with the same code
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch());
        boolean usePhaseWeight = RevUtil.getBoolean("usePhaseWeight", req, true);
        boolean doBootstrap = false;
        SumHKStack sumStack;
        String phase = RevUtil.get("phase", req, "all");
        float minBaz = RevUtil.getFloat("minBaz", req, 0);
        float maxBaz = RevUtil.getFloat("maxBaz", req, 360);
        if(phase.equals("all") && minBaz == 0 && maxBaz == 360 && usePhaseWeight) {
            // phase weight stacks are stored, so don't need to
            // calculate
            logger.debug("default get sumStack");
            sumStack = RecFuncDB.getSingleton().getSumStack(net.getWrapped(), staCode, gaussianWidth);
            logger.debug("Got summary plot from database "
                               + net.getCode() + "." + staCode);
            return sumStack;
        }
        List<StationImpl> staList = NetworkDB.getSingleton().getStationForNet(net.getWrapped(), staCode);
        if (staList.size() == 0) {
            throw new NotFound("No station '"+staCode+"' in network.");
        }
        StationImpl station = staList.get(0);
        QuantityImpl smallestH = new QuantityImpl(RevUtil.getFloat("smallestH",
                                                                   req,
                                                                   (float)HKStack.getBestSmallestH(station)
                                                                   .getValue(UnitImpl.KILOMETER)),
                                                                   UnitImpl.KILOMETER);
        if (phase.equals("all") && minBaz == 0 && maxBaz == 360 && ! usePhaseWeight) {
            return stackSummary.sum(net.getWrapped(),
                                        staCode,
                                        gaussianWidth,
                                        minPercentMatch,
                                        smallestH,
                                        doBootstrap,
                                        usePhaseWeight);
        } else if(minBaz == 0 && maxBaz == 360) {
            return stackSummary.sumForPhase(station.get_id().network_id.network_code,
                                            staCode,
                                            gaussianWidth,
                                            minPercentMatch,
                                            smallestH,
                                            phase,
                                            usePhaseWeight);
        } else {
            // subset based on Baz
            List<ReceiverFunctionResult> resutsInBaz = new ArrayList<ReceiverFunctionResult>();
            List<ReceiverFunctionResult> results = RecFuncDB.getSingleton()
            .getSuccessful(net.getWrapped(),
                           staCode,
                           gaussianWidth);
            BazIterator bazIt = new BazIterator(results.iterator(),
                                                minBaz,
                                                maxBaz);
            while(bazIt.hasNext()) {
                resutsInBaz.add(bazIt.next());
            }
            return stackSummary.sumForPhase(resutsInBaz,
                                                minPercentMatch,
                                                smallestH,
                                                phase,
                                                usePhaseWeight);
        }
    }

    void output(SumHKStack sumStack,
                BufferedOutputStream out,
                HttpServletRequest req,
                HttpServletResponse res) throws IOException {
        BufferedImage image = sumStack.createStackImage();
        logger.debug("finish create image");
        res.setContentType("image/png");
        ImageIO.write(image, "png", out);
        out.close();
    }

    private StackSummary stackSummary;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SummaryHKStackImageServlet.class);
}
