package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
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
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Feb 23, 2005
 */
public class SummaryHKStackImageServlet extends HttpServlet {

    public synchronized void doGet(HttpServletRequest req,
                                   HttpServletResponse res) throws IOException {
        try {
            logger.debug("doGet called");
            VelocityNetwork net = Start.getNetwork(req);
            String staCode = RevUtil.get("stacode", req);
            SumHKStack sumStack = getSumStack(req, net, staCode);
            logger.info("before check for null");
            OutputStream out = res.getOutputStream();
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
            output(sumStack, out, req, res);
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            writer.write("<html><body><p>No HK stack foundfor  "
                    + req.getParameter("staCode") + "</p></body></html>");
            writer.flush();
        } catch(Throwable e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        }
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
        edu.iris.Fissures.IfNetwork.Station station = NetworkDB.getSingleton()
                .getStationForNet(net.getWrapped(), staCode)
                .get(0);
        QuantityImpl smallestH = new QuantityImpl(RevUtil.getFloat("smallestH",
                                                                   req,
                                                                   (float)HKStack.getBestSmallestH(station)
                                                                           .getValue(UnitImpl.KILOMETER)),
                                                  UnitImpl.KILOMETER);
        if(phase.equals("all") && minBaz == 0 && maxBaz == 360) {
            if(usePhaseWeight) {
                // phase weight stacks are stored, so don't need to
                // calculate
                sumStack = RecFuncDB.getSingleton()
                        .getSumStack(net.getWrapped(), staCode, gaussianWidth);
                System.out.println("Got summary plot from database "
                        + net.getCode() + "." + staCode);
            } else {
                sumStack = stackSummary.sum(net.getWrapped(),
                                            staCode,
                                            gaussianWidth,
                                            minPercentMatch,
                                            smallestH,
                                            doBootstrap,
                                            usePhaseWeight);
            }
        } else {
            if(minBaz == 0 && maxBaz == 360) {
                sumStack = stackSummary.sumForPhase(station.get_id().network_id.network_code,
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
                                       gaussianWidth,
                                       minPercentMatch);
                BazIterator bazIt = new BazIterator(results.iterator(),
                                                    minBaz,
                                                    maxBaz);
                while(bazIt.hasNext()) {
                    resutsInBaz.add(bazIt.next());
                }
                sumStack = stackSummary.sumForPhase(resutsInBaz,
                                                    minPercentMatch,
                                                    smallestH,
                                                    phase,
                                                    usePhaseWeight);
            }
        }
        return sumStack;
    }

    void output(SumHKStack sumStack,
                OutputStream out,
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
