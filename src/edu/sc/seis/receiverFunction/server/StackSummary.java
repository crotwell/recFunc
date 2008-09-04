package edu.sc.seis.receiverFunction.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.StackComplexity;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.hibernate.RFInsertion;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.receiverFunction.web.Start;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.FissuresFormatter;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Oct 7, 2004
 */
public class StackSummary {

    public StackSummary(Properties props) throws IOException,
            SQLException, ConfigurationException, TauModelException, Exception {
        RecFuncCacheImpl.setDataLoc(props.getProperty("cormorant.servers.ears.dataloc",
                                                      RecFuncCacheImpl.getDataLoc()));
        RecFuncCacheImpl impl = new RecFuncCacheImpl(props.getProperty("cormorant.servers.ears.databaseURL"), 
                                                     props.getProperty("cormorant.servers.ears.dataloc"), 
                                                     props);
    }

    public void createSummary(String netCode,
                              float gaussianWidth,
                              float minPercentMatch,
                              QuantityImpl smallestH,
                              boolean doBootstrap,
                              boolean usePhaseWeight) throws FissuresException,
            NotFound, IOException, SQLException, TauModelException {
        List<NetworkAttrImpl> allNets;
        if(netCode.equals("-all")) {
            allNets = NetworkDB.getSingleton().getAllNetworks();
        } else {
            allNets = new ArrayList<NetworkAttrImpl>();
            allNets.add(Start.getNetwork(netCode).getWrapped());
        }
        for(NetworkAttrImpl net : allNets) {
            List<StationImpl> stations = NetworkDB.getSingleton()
                    .getStationForNet(net);
            HashMap<String, StationImpl> staCodes = new HashMap<String, StationImpl>();
            for(StationImpl sta : stations) {
                staCodes.put(sta.get_code(), sta);
            }
            for(String stationCode : staCodes.keySet()) {
                QuantityImpl modSmallestH = HKStack.getBestSmallestH(staCodes.get(stationCode), smallestH);
                createSummary(net,
                              stationCode,
                              gaussianWidth,
                              minPercentMatch,
                              modSmallestH,
                              doBootstrap,
                              usePhaseWeight);
            }
        }
        TimeOMatic.print("Time for network: " + netCode);
    }
    
    public SumHKStack createSummary(NetworkAttrImpl net,
                                    String staCode,
                                    float gaussianWidth,
                                    float minPercentMatch,
                                    QuantityImpl smallestH,
                                    boolean doBootstrap,
                                    boolean usePhaseWeight)
            throws FissuresException, NotFound, IOException, SQLException,
            TauModelException {
        System.out.println("createSummary for "
                + NetworkIdUtil.toStringNoDates(net)+"."+staCode);
        SumHKStack sumStack = sum(net,
                                  staCode,
                                  gaussianWidth,
                                  minPercentMatch,
                                  smallestH,
                                  doBootstrap,
                                  usePhaseWeight);
        if(sumStack == null) {
            System.out.println("stack is null for "
                    + NetworkIdUtil.toStringNoDates(net)+"."+staCode);
        } else {
            SumHKStack sum = RecFuncDB.getSingleton().getSumStack(net, staCode, gaussianWidth);
            if (sum != null) {
                RecFuncDB.getSession().delete(sum);   
            }
            calcComplexity(sumStack);
            RecFuncDB.getSingleton().put(sumStack);
        }
        return sumStack;
    }

    public static float calcComplexity(SumHKStack sumStack) throws FissuresException,
            TauModelException, SQLException {
        Channel chan = sumStack.getIndividuals().get(0).getChannelGroup().getChannel1();
        HKStack residual = sumStack.getResidual();
        float complex = sumStack.getResidualPower();
        float complex25 = sumStack.getResidualPower(.25f);
        float complex50 = sumStack.getResidualPower(.50f);
        float bestH = (float)sumStack.getSum()
                .getMaxValueH(sumStack.getSmallestH())
                .getValue(UnitImpl.KILOMETER);
        float bestHStdDev = (float)sumStack.getHStdDev()
                .getValue(UnitImpl.KILOMETER);
        float bestK = sumStack.getSum().getMaxValueK(sumStack.getSmallestH());
        float bestKStdDev = (float)sumStack.getKStdDev();
        float bestVal = sumStack.getSum().getMaxValue(sumStack.getSmallestH());
        float hkCorrelation = (float)sumStack.getMixedVariance();
        float nextH = (float)residual.getMaxValueH(sumStack.getSmallestH())
                .getValue(UnitImpl.KILOMETER);
        float nextK = residual.getMaxValueK(sumStack.getSmallestH());
        float nextVal = residual.getMaxValue(sumStack.getSmallestH());
        StationResult crust2Result = HKStack.getCrust2()
                .getStationResult(chan.getSite().getStation());
        float crust2diff = bestH
                - (float)crust2Result.getH().getValue(UnitImpl.KILOMETER);
        sumStack.setComplexityResult( new StackComplexityResult(complex,
                                complex25,
                                complex50,
                                bestH,
                                bestHStdDev,
                                bestK,
                                bestKStdDev,
                                bestVal,
                                hkCorrelation,
                                nextH,
                                nextK,
                                nextVal,
                                crust2diff));
        System.out.println(NetworkIdUtil.toStringNoDates(chan
                .get_id().network_id)
                + "."
                + chan.get_id().station_code
                + " Complexity: " + complex);
        return complex;
    }

    public SumHKStack sum(NetworkAttrImpl net,
                          String staCode,
                          float gaussianWidth,
                          float percentMatch,
                          QuantityImpl smallestH,
                          boolean doBootstrap,
                          boolean usePhaseWeight) throws FissuresException,
            NotFound, IOException, SQLException {
        List<ReceiverFunctionResult> individualHK = RecFuncDB.getSingleton().getSuccessful(net,
                                                           staCode,
                                                           gaussianWidth);
        List<StationImpl> stations = NetworkDB.getSingleton()
                .getStationForNet(net, staCode);
        Set<RejectedMaxima> rejects = new HashSet<RejectedMaxima>();
        rejects.addAll(RecFuncDB.getSingleton().getRejectedMaxima((NetworkAttrImpl)stations.get(0).getNetworkAttr(), staCode));
        logger.info("in sum for " + net.get_code() + "." + staCode + " numeq="
                + individualHK.size());
        System.out.println("in sum for " + net.get_code() + "." + staCode + " numeq="
                + individualHK.size());
        // if there is only 1 eq that matches, then we can't really do a stack
        if(individualHK.size() > 1) {
            SumHKStack sumStack = SumHKStack.calculateForPhase(individualHK,
                                                       smallestH,
                                                       percentMatch,
                                                       usePhaseWeight,
                                                       rejects,
                                                       doBootstrap,
                                                       SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS, "all");
            TimeOMatic.print("sum for " + net.get_code() + "." + staCode);
            return sumStack;
        } else {
            return null;
        }
    }

    public SumHKStack sumForPhase(String netCode,
                                  String staCode,
                                  float gaussianWidth,
                                  float minPercentMatch,
                                  QuantityImpl smallestH,
                                  String phase,
                                  boolean usePhaseWeight)
            throws FissuresException, NotFound, IOException, SQLException {
        logger.info("in sum for " + netCode + "." + staCode);
        List<NetworkAttrImpl> net = NetworkDB.getSingleton()
                .getNetworkByCode(netCode);
        for(NetworkAttrImpl networkAttr : net) {
            List<ReceiverFunctionResult> rfResults = RecFuncDB.getSingleton()
                    .getSuccessful(networkAttr,
                                   staCode,
                                   gaussianWidth);
            if(rfResults.size() > 0) {
                SumHKStack sumStack = sumForPhase(rfResults,
                                                  minPercentMatch,
                                                  smallestH,
                                                  phase,
                                                  usePhaseWeight);
                TimeOMatic.print("sum for " + netCode + "." + staCode);
                return sumStack;
            }
        }
        throw new NotFound();
    }

    public SumHKStack sumForPhase(List<ReceiverFunctionResult> rfResults,
                                  float minPercentMatch,
                                  QuantityImpl smallestH,
                                  String phase,
                                  boolean usePhaseWeight)
            throws FissuresException, NotFound, IOException, SQLException {
        SumHKStack sumStack = SumHKStack.calculateForPhase(rfResults,
                                                           smallestH,
                                                           minPercentMatch,
                                                           usePhaseWeight,
                                                           new HashSet(),
                                                           false,
                                                           0,
                                                           phase);
        return sumStack;
    }

    public static void saveImage(SumHKStack sumStack,
                                 StationId station,
                                 File parentDir,
                                 float minPercentMatch,
                                 float smallestH) throws IOException {
        if(sumStack == null) {
            logger.info("No hk plots for "
                    + StationIdUtil.toStringNoDates(station) + " with match > "
                    + minPercentMatch);
            return;
        }
        BufferedImage image = sumStack.createStackImage();
        parentDir.mkdirs();
        File outSumImageFile = new File(parentDir,
                                        "SumHKStack_"
                                                + minPercentMatch
                                                + "_"
                                                + FissuresFormatter.filize(StationIdUtil.toStringNoDates(station)
                                                        + ".png"));
        if(outSumImageFile.exists()) {
            outSumImageFile.delete();
        }
        javax.imageio.ImageIO.write(image, "png", outSumImageFile);
    }

    public static Properties loadProps(String[] args) {
        Properties props = System.getProperties();
        ConnMgr.addPropsLocation("edu/sc/seis/receiverFunction/server/");
        // get some defaults
        String propFilename = "rfcache.prop";
        String defaultsFilename = "edu/sc/seis/receiverFunction/server/"
                + propFilename;
        for(int i = 0; i < args.length - 1; i++) {
            if(args[i].equals("-props") || args[i].equals("-p")) {
                // override with values in local directory,
                // but still load defaults with original name
                propFilename = args[i + 1];
            }
        }
        try {
            props.load((StackSummary.class).getClassLoader()
                    .getResourceAsStream(defaultsFilename));
        } catch(IOException e) {
            System.err.println("Could not load defaults. " + e);
        }
        try {
            FileInputStream in = new FileInputStream(propFilename);
            props.load(in);
            in.close();
        } catch(FileNotFoundException f) {
            System.err.println(" file missing " + f + " using defaults");
        } catch(IOException f) {
            System.err.println(f.toString() + " using defaults");
        }
        // configure logging from properties...
        PropertyConfigurator.configure(props);
        logger.info("Logging configured");
        return props;
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StackSummary.class);

    public static Connection initDB(Properties props) throws IOException,
            SQLException {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        String dbURL = props.getProperty("cormorant.servers.ears.databaseURL");
        ConnMgr.setURL(dbURL);
        Connection conn = ConnMgr.createConnection();
        return conn;
    }

    public static void parseArgsAndRun(String[] args, StackSummary summary)
            throws FissuresException, NotFound, IOException, SQLException,
            TauModelException {
        float gaussianWidth = 2.5f;
        float minPercentMatch = 80f;
        boolean bootstrap = true;
        boolean bootstrapXY = false;
        boolean usePhaseWeight = true;
        boolean neededOnly = false;
        String netArg = "";
        String staArg = "";
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("--help") || args[i].equals("-h")) {
                System.out.println("Usage:");
                System.out.println(" [ -net network [-sta station]] | -all | --needRecalc");
                System.out.println("-g gaussian");
                System.out.println("--nobootstrap");
                System.out.println("--bootstrapXY");
                System.out.println();
                return;
            }
        }
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-net") || args[i].equals("--net")
                    || args[i].equals("-n")) {
                netArg = args[i + 1];
            } else if(args[i].equals("-sta") || args[i].equals("--sta")
                    || args[i].equals("-s")) {
                staArg = args[i + 1];
            } else if(args[i].equals("-all")) {
                netArg = args[i];
            } else if(args[i].equals("-g")) {
                gaussianWidth = Float.parseFloat(args[i + 1]);
            } else if(args[i].equals("--nobootstrap")) {
                bootstrap = false;
            } else if(args[i].equals("--needRecalc")) {
                neededOnly = true;
            } else if(args[i].equals("--bootstrapXY")) {
                bootstrapXY = true;
            }
        }
        if(neededOnly) {
            System.out.println("needed only");
            List<RFInsertion> netSta = RecFuncDB.getSingleton().getOlderInsertions(RF_AGE_TIME, gaussianWidth);
            for(RFInsertion insertion : netSta) {
                StationImpl oneStationByCode = NetworkDB.getSingleton().getStationForNet(insertion.getNet(),
                                                                                     insertion.getStaCode()).get(0);
                summary.createSummary(insertion.getNet(),
                                      insertion.getStaCode(),
                                      gaussianWidth,
                                      minPercentMatch,
                                      HKStack.getBestSmallestH(oneStationByCode,
                                                               HKStack.getDefaultSmallestH()),
                                      bootstrap,
                                      usePhaseWeight);
            }
        } else if(staArg.equals("")) {
            System.out.println("net arg " + netArg);
            summary.createSummary(netArg,
                                  gaussianWidth,
                                  minPercentMatch,
                                  HKStack.getDefaultSmallestH(),
                                  bootstrap,
                                  usePhaseWeight);
        } else {
            System.out.println("calc for staion " + netArg + " " + staArg);
            logger.info("calc for station");
            VelocityNetwork net = Start.getNetwork(netArg);
            int sta_dbid = -1;
            boolean foundNet = false;
            NetworkDB netdb = NetworkDB.getSingleton();
            try {
                List<StationImpl> stations = netdb.getStationByCodes(net.get_code(),
                                                                     staArg);
                if(stations.size() > 0) {
                    foundNet = true;
                    Station station = stations.get(0);;
                    SumHKStack sum = summary.createSummary((NetworkAttrImpl)station.getNetworkAttr(),
                                                           station.get_code(),
                                                           gaussianWidth,
                                                           minPercentMatch,
                                                           HKStack.getBestSmallestH(station,
                                                                                    HKStack.getDefaultSmallestH()),
                                                           bootstrap,
                                                           usePhaseWeight);
                    if(bootstrapXY) {
                        double[] h = sum.getHBootstrap();
                        double[] k = sum.getKBootstrap();
                        System.out.println("max h=" + sum.getBest().formatH());
                        System.out.println("max k="
                                + sum.getBest().formatVpVs());
                        System.out.println("k stddev=" + sum.getKStdDev());
                        System.out.println("h stddev=" + sum.getHStdDev());
                        for(int j = 0; j < h.length; j++) {
                            System.out.println(k[j] + " " + h[j]);
                        }
                    }
                }
            } catch(NotFound e) {
                System.out.println("NotFound for :" + net.getCodeWithYear());
                // go to next network
            }
            if(!foundNet) {
                System.out.println("Warning: didn't find net for " + netArg);
            }
        }
        System.out.println("Done.");
    }

    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: StackSummary -net netCode [ -sta staCode ] or --needRecalc");
            return;
        }
        Connection conn = null;
        try {
            TimeOMatic.setWriter(new FileWriter("netTimes.txt"));
            TimeOMatic.start();
            Properties props = loadProps(args);
            conn = initDB(props);
            StackSummary summary = new StackSummary(props);
            parseArgsAndRun(args, summary);
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        } finally {
            if(conn != null) {
                try {
                    conn.close();
                } catch(SQLException e) {
                    // oh well
                }
            }
        }
    }
    
    public static final TimeInterval RF_AGE_TIME = new TimeInterval(1, UnitImpl.HOUR);
}