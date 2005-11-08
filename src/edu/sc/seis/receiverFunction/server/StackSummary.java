package edu.sc.seis.receiverFunction.server;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.Network;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.StackComplexity;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.FissuresFormatter;

/**
 * @author crotwell Created on Oct 7, 2004
 */
public class StackSummary {

    public StackSummary(Connection conn) throws IOException, SQLException, ConfigurationException, TauModelException,
            Exception {
        JDBCEventAccess jdbcEventAccess = new JDBCEventAccess(conn);
        JDBCChannel jdbcChannel = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn,
                                                  jdbcEventAccess,
                                                  jdbcChannel,
                                                  jdbcSodConfig,
                                                  RecFuncCacheImpl.getDataLoc());
        jdbcHKStack = new JDBCHKStack(conn, jdbcEventAccess, jdbcChannel, jdbcSodConfig, jdbcRecFunc);
        jdbcSummary = new JDBCSummaryHKStack(jdbcHKStack);
        jdbcStackComplexity = new JDBCStackComplexity(jdbcSummary);
    }

    public void createSummary(String net,
                              float gaussianWidth,
                              float minPercentMatch,
                              QuantityImpl smallestH,
                              boolean doBootstrap,
                              boolean usePhaseWeight) throws FissuresException, NotFound, IOException, SQLException, TauModelException {
        JDBCStation jdbcStation = jdbcHKStack.getJDBCChannel().getSiteTable().getStationTable();
        JDBCNetwork jdbcNetwork = jdbcStation.getNetTable();
        NetworkId[] netId = jdbcNetwork.getAllNetworkIds();
        for(int i = 0; i < netId.length; i++) {
            if(net.equals("-all") || netId[i].network_code.equals(net)) {
                Station[] station = jdbcStation.getAllStations(netId[i]);
                for(int j = 0; j < station.length; j++) {
                    QuantityImpl modSmallestH = HKStack.getBestSmallestH(station[j], smallestH);
                    createSummary(station[j].get_id(),
                                  gaussianWidth,
                                  minPercentMatch,
                                  modSmallestH,
                                  doBootstrap,
                                  usePhaseWeight);
                }
            }
        }
    }

    public void createSummary(StationId station,
                              float gaussianWidth,
                              float minPercentMatch,
                              QuantityImpl smallestH,
                              boolean doBootstrap,
                              boolean usePhaseWeight) throws FissuresException, NotFound, IOException, SQLException, TauModelException {
        System.out.println("createSummary for " + StationIdUtil.toStringNoDates(station));
        SumHKStack sumStack = sum(station.network_id.network_code,
                                  station.station_code,
                                  gaussianWidth,
                                  minPercentMatch,
                                  smallestH,
                                  doBootstrap,
                                  usePhaseWeight);
        if(sumStack == null) {
            System.out.println("stack is null for " + StationIdUtil.toStringNoDates(station));
        } else {
            int dbid;
            try {
                dbid = jdbcSummary.getDbIdForStation(station.network_id,
                                                         station.station_code,
                                                         gaussianWidth,
                                                         minPercentMatch);
                jdbcSummary.update(dbid, sumStack);
            } catch(NotFound e) {
                dbid = jdbcSummary.put(sumStack);
            }
            calcComplexity(sumStack);
        }
    }
    
    public void calcComplexity() throws SQLException, NotFound, IOException, FissuresException, TauModelException {
        HKSummaryIterator it = jdbcSummary.getAllIterator();
        System.out.println("in calc Complexity");
        while(it.hasNext()) {
            SumHKStack sumStack = (SumHKStack)it.next();
            calcComplexity(sumStack);
        }
        it.close();
    }
    
    public float calcComplexity(SumHKStack sumStack) throws FissuresException, TauModelException, SQLException {
        StackComplexity complexity = new StackComplexity(sumStack, 4096, sumStack.getSum().getGaussianWidth());
        StationResult model = new StationResult(sumStack.getChannel().get_id().network_id,
                                                sumStack.getChannel().get_id().station_code,
                                                sumStack.getSum().getMaxValueH(sumStack.getSmallestH()),
                                                sumStack.getSum().getMaxValueK(sumStack.getSmallestH()),
                                                sumStack.getSum().getAlpha(),
                                                null);
        float complex = sumStack.getResidualPower();
        jdbcStackComplexity.put(sumStack.getDbid(), complex);
        System.out.println(NetworkIdUtil.toStringNoDates(sumStack.getChannel().get_id().network_id)+"."+sumStack.getChannel().get_id().station_code+" Complexity: "+complex);
        return complex;
    }

    public SumHKStack sum(String netCode,
                          String staCode,
                          float gaussianWidth,
                          float percentMatch,
                          QuantityImpl smallestH,
                          boolean doBootstrap,
                          boolean usePhaseWeight) throws FissuresException, NotFound, IOException, SQLException {
        logger.info("in sum for " + netCode + "." + staCode);
        TimeOMatic.start();
        ArrayList individualHK = jdbcHKStack.getForStation(netCode, staCode, gaussianWidth, percentMatch, true);
        // if there is only 1 eq that matches, then we can't really do a stack
        if(individualHK.size() > 1) {
            HKStack temp = (HKStack)individualHK.get(0);
            SumHKStack sumStack = new SumHKStack((HKStack[])individualHK.toArray(new HKStack[0]),
                                                 temp.getChannel(),
                                                 percentMatch,
                                                 smallestH,
                                                 doBootstrap,
                                                 usePhaseWeight);
            TimeOMatic.print("sum for " + netCode + "." + staCode);
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
                                  boolean usePhaseWeight) throws FissuresException, NotFound, IOException, SQLException {
        logger.info("in sum for " + netCode + "." + staCode);
        TimeOMatic.start();
        HKStackIterator it = jdbcHKStack.getIteratorForStation(netCode, staCode, gaussianWidth, minPercentMatch, false);
        SumHKStack sumStack = sumForPhase(it, minPercentMatch, smallestH, phase, usePhaseWeight);
        it.close();
        TimeOMatic.print("sum for " + netCode + "." + staCode);
        return sumStack;
    }

    public SumHKStack sumForPhase(Iterator hkstackIterator,
                                  float minPercentMatch,
                                  QuantityImpl smallestH,
                                  String phase,
                                  boolean usePhaseWeight) throws FissuresException, NotFound, IOException, SQLException {
        SumHKStack sumStack = SumHKStack.calculateForPhase(hkstackIterator,
                                                           smallestH,
                                                           minPercentMatch,
                                                           usePhaseWeight,
                                                           phase);
        return sumStack;
    }

    public static void saveImage(SumHKStack sumStack,
                                 StationId station,
                                 File parentDir,
                                 float minPercentMatch,
                                 float smallestH) throws IOException {
        if(sumStack == null) {
            logger.info("No hk plots for " + StationIdUtil.toStringNoDates(station) + " with match > "
                    + minPercentMatch);
            return;
        }
        BufferedImage image = sumStack.createStackImage();
        parentDir.mkdirs();
        File outSumImageFile = new File(parentDir, "SumHKStack_" + minPercentMatch + "_"
                + FissuresFormatter.filize(ChannelIdUtil.toStringNoDates(sumStack.getChannel().get_id()) + ".png"));
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
        String defaultsFilename = "edu/sc/seis/receiverFunction/server/" + propFilename;
        for(int i = 0; i < args.length - 1; i++) {
            if(args[i].equals("-props") || args[i].equals("-p")) {
                // override with values in local directory,
                // but still load defaults with original name
                propFilename = args[i + 1];
            }
        }
        try {
            props.load((StackSummary.class).getClassLoader().getResourceAsStream(defaultsFilename));
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

    JDBCHKStack jdbcHKStack;

    JDBCSummaryHKStack jdbcSummary;

    private JDBCStackComplexity jdbcStackComplexity;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StackSummary.class);

    public static Connection initDB(Properties props) throws IOException, SQLException {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        String dbURL = props.getProperty("cormorant.servers.ears.databaseURL");
        ConnMgr.setURL(dbURL);
        Connection conn = ConnMgr.createConnection();
        return conn;
    }

    public static void parseArgsAndRun(String[] args, StackSummary summary) throws FissuresException, NotFound,
            IOException, SQLException, TauModelException {
        float gaussianWidth = 2.5f;
        float minPercentMatch = 80f;
        boolean bootstrap = true;
        boolean usePhaseWeight = true;
        boolean complexityOnly = false;
        String netArg = "";
        String staArg = "";
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-net")) {
                netArg = args[i + 1];
            } else if(args[i].equals("-sta")) {
                staArg = args[i + 1];
            } else if(args[i].equals("-all")) {
                netArg = args[i];
            } else if(args[i].equals("-g")) {
                gaussianWidth = Float.parseFloat(args[i + 1]);
            } else if(args[i].equals("--complexity")) {
                complexityOnly = true;
            } else if(args[i].equals("--nobootstrap")) {
                bootstrap = false;
            }
        }
        if (complexityOnly) {
            summary.calcComplexity();
            return;
        }
        if(staArg.equals("")) {
            summary.createSummary(netArg,
                                  gaussianWidth,
                                  minPercentMatch,
                                  HKStack.getDefaultSmallestH(),
                                  bootstrap,
                                  usePhaseWeight);
        } else {
            logger.info("calc for station");
            JDBCStation jdbcStation = summary.jdbcHKStack.getJDBCChannel().getStationTable();
            NetworkId[] nets = jdbcStation.getNetTable().getByCode(netArg);
            int sta_dbid = -1;
            for(int i = 0; i < nets.length; i++) {
                try {
                    int[] tmp = jdbcStation.getDBIds(nets[i], staArg);
                    if(tmp.length > 0) {
                        sta_dbid = tmp[0];
                        Station station = jdbcStation.get(sta_dbid);
                        summary.createSummary(station.get_id(),
                                              gaussianWidth,
                                              minPercentMatch,
                                              HKStack.getBestSmallestH(station, HKStack.getDefaultSmallestH()),
                                              bootstrap,
                                              usePhaseWeight);
                    }
                } catch(NotFound e) {
                    System.out.println("NotFound for :" + NetworkIdUtil.toStringNoDates(nets[i]));
                    // go to next network
                }
            }
        }
    }

    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: StackSummary -net netCode [ -sta staCode ]");
            return;
        }
        try {
            Properties props = loadProps(args);
            Connection conn = initDB(props);
            StackSummary summary = new StackSummary(conn);
            parseArgsAndRun(args, summary);
        } catch(Exception e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    public JDBCHKStack getJDBCHKStack() {
        return jdbcHKStack;
    }

    public JDBCSummaryHKStack getJdbcSummary() {
        return jdbcSummary;
    }

    public Connection getConnection() {
        return jdbcHKStack.getConnection();
    }
}