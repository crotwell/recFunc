package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.receiverFunction.BazIterator;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.JDBCSummaryHKStack;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.receiverFunction.server.StackSummary;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.locator.StationLocator;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelStatus;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Feb 23, 2005
 */
public class SummaryHKStackImageServlet extends HttpServlet {

    /**
     * @throws SQLException
     * @throws Exception
     * @throws ConfigurationException
     */
    public SummaryHKStackImageServlet() throws SQLException,
            ConfigurationException, Exception {
        Connection conn = ConnMgr.createConnection();
        stackSummary = new StackSummary(conn);
        jdbcHKStack = stackSummary.getJDBCHKStack();
        jdbcSumHKStack = stackSummary.getJdbcSummary();
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        try {
            logger.debug("doGet called");
            int netDbId = RevUtil.getInt("netdbid", req);
            VelocityNetwork net = new VelocityNetwork(jdbcHKStack.getJDBCChannel()
                                                              .getStationTable()
                                                              .getNetTable()
                                                              .get(netDbId),
                                                      netDbId);
            // possible that there are multiple stations with the same code
            String staCode = req.getParameter("stacode");
            if(req.getParameter("minPercentMatch") == null) {
                throw new Exception("minPercentMatch param not set");
            }
            float minPercentMatch = new Float(req.getParameter("minPercentMatch")).floatValue();
            boolean usePhaseWeight = RevUtil.getBoolean("usePhaseWeight",
                                                        req,
                                                        true);
            boolean doBootstrap = false;
            SumHKStack sumStack;
            String phase = RevUtil.get("phase", req, "all");
            float minBaz = RevUtil.getFloat("minBaz", req, 0);
            float maxBaz = RevUtil.getFloat("maxBaz", req, 360);
            JDBCStation jdbcStation = jdbcHKStack.getJDBCChannel()
                    .getStationTable();
            int[] dbids = jdbcStation.getDBIds(net.get_id(), staCode);
            edu.iris.Fissures.IfNetwork.Station station = jdbcStation.get(dbids[0]);
            QuantityImpl smallestH = new QuantityImpl(RevUtil.getFloat("smallestH",
                                                                       req,
                                                                       (float)HKStack.getBestSmallestH(station)
                                                                               .getValue(UnitImpl.KILOMETER)),
                                                      UnitImpl.KILOMETER);
            if(phase.equals("all") && minBaz == 0 && maxBaz == 360) {
                if(usePhaseWeight) {
                    try {
                        // phase weight stacks are stored, so don't need to
                        // calculate
                        synchronized(jdbcSumHKStack.getConnection()) {
                            int dbid = jdbcSumHKStack.getDbIdForStation(net.get_id(),
                                                                        staCode);
                            sumStack = jdbcSumHKStack.get(dbid);
                            System.out.println("Got summary plot from database "
                                    + dbid);
                        }
                    } catch(NotFound e) {
                        sumStack = null;
                    }
                } else {
                    synchronized(stackSummary.getConnection()) {
                        sumStack = stackSummary.sum(station.get_id().network_id.network_code,
                                                    staCode,
                                                    minPercentMatch,
                                                    smallestH,
                                                    doBootstrap,
                                                    usePhaseWeight);
                    }
                }
            } else {
                if(minBaz == 0 && maxBaz == 360) {
                    synchronized(stackSummary.getConnection()) {
                        sumStack = stackSummary.sumForPhase(station.get_id().network_id.network_code,
                                                            staCode,
                                                            minPercentMatch,
                                                            smallestH,
                                                            phase,
                                                            usePhaseWeight);
                    }
                } else {
                    // subset based on Baz
                    synchronized(jdbcHKStack.getConnection()) {
                        Iterator it = jdbcHKStack.getIteratorForStation(station.get_id().network_id.network_code,
                                                                        staCode,
                                                                        minPercentMatch,
                                                                        false);
                        BazIterator bazIt = new BazIterator(it, minBaz, maxBaz);
                        sumStack = stackSummary.sumForPhase(bazIt,
                                                            minPercentMatch,
                                                            smallestH,
                                                            phase,
                                                            usePhaseWeight);
                    }
                }
            }
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
            System.out.println("No HKStack found for "
                    + req.getParameter("staCode"));
            writer.write("<html><body><p>No HK stack foundfor  "
                    + req.getParameter("staCode") + "</p></body></html>");
            writer.flush();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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

    private JDBCHKStack jdbcHKStack;

    private JDBCSummaryHKStack jdbcSumHKStack;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SummaryHKStackImageServlet.class);
}
