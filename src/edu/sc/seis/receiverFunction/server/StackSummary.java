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
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import edu.iris.Fissures.FissuresException;
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
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.FissuresFormatter;

/**
 * @author crotwell Created on Oct 7, 2004
 */
public class StackSummary {

    public StackSummary(Connection conn) throws IOException, SQLException,
            ConfigurationException, TauModelException, Exception {
        JDBCEventAccess jdbcEventAccess = new JDBCEventAccess(conn);
        JDBCChannel jdbcChannel = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn,
                                                  jdbcEventAccess,
                                                  jdbcChannel,
                                                  jdbcSodConfig,
                                                  RecFuncCacheImpl.getDataLoc());
        jdbcHKStack = new JDBCHKStack(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      jdbcRecFunc);
        jdbcSummary = new JDBCSummaryHKStack(jdbcHKStack);
    }

    public void createSummary(String net,
                              File parentDir,
                              float minPercentMatch,
                              QuantityImpl smallestH,
                              boolean doBootstrap,
                              boolean usePhaseWeight) throws FissuresException,
            NotFound, IOException, SQLException {
        JDBCStation jdbcStation = jdbcHKStack.getJDBCChannel()
                .getSiteTable()
                .getStationTable();
        JDBCNetwork jdbcNetwork = jdbcStation.getNetTable();
        NetworkId[] netId = jdbcNetwork.getAllNetworkIds();
        File textSummary = new File(parentDir, "depth_vpvs.txt");
        parentDir.mkdirs();
        BufferedWriter textSumm = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(textSummary)));
        for(int i = 0; i < netId.length; i++) {
            if(net.equals("-all") || netId[i].network_code.equals(net)) {
                Station[] station = jdbcStation.getAllStations(netId[i]);
                for(int j = 0; j < station.length; j++) {
                    QuantityImpl modSmallestH = HKStack.getBestSmallestH(station[j], smallestH);
                    SumHKStack sumStack = createSummary(station[j].get_id(),
                                                        parentDir,
                                                        minPercentMatch,
                                                        modSmallestH,
                                                        doBootstrap,
                                                        usePhaseWeight);
                    
                    if(sumStack == null) {
                        continue;
                    }
                    String outStr = StationIdUtil.toStringNoDates(station[j].get_id())
                            + " "
                            + station[j].my_location.latitude
                            + " "
                            + station[j].my_location.longitude;
                    QuantityImpl peakH;
                    float peakK, peakVal = 0;
                    int[] indicies = sumStack.getSum().getMaxValueIndices();
                    peakH = sumStack.getSum().getMaxValueH();
                    peakK = sumStack.getSum().getMaxValueK();
                    peakVal = sumStack.getSum().getMaxValue();
                    outStr += " " + peakH + " " + peakK + " " + peakVal + " "
                            + sumStack.getIndividuals().length;
                    outStr += " "
                            + (float)(2 * Math.sqrt(sumStack.getHVariance()))
                            + " "
                            + (float)(2 * Math.sqrt(sumStack.getKVariance()));

                    Crust2Profile crust2 = HKStack.getCrust2()
                            .getClosest(station[j].my_location.longitude,
                                        station[j].my_location.latitude);
                    QuantityImpl depth = crust2.getCrustThickness();
                    double vpvs = crust2.getPWaveAvgVelocity()
                            / crust2.getSWaveAvgVelocity();
                    outStr += " " + depth + " " + vpvs;
                    textSumm.write(outStr);
                    textSumm.newLine();
                    textSumm.flush();
                }
            }
        }
        textSumm.close();
    }

    public SumHKStack createSummary(StationId station,
                                    File parentDir,
                                    float minPercentMatch,
                                    QuantityImpl smallestH,
                                    boolean doBootstrap,
                                    boolean usePhaseWeight) throws FissuresException,
            NotFound, IOException, SQLException {
        System.out.println("createSummary for "
                           + StationIdUtil.toStringNoDates(station));
        SumHKStack sumStack = sum(station.network_id.network_code,
                                              station.station_code,
                                              minPercentMatch,
                                              smallestH,
                                              doBootstrap,
                                              usePhaseWeight);
        if(sumStack == null) {
            System.out.println("stack is null for "
                    + StationIdUtil.toStringNoDates(station));
        } else {
            try {
                int dbid = jdbcSummary.getDbIdForStation(station.network_id,
                                                         station.station_code);
                jdbcSummary.update(dbid, sumStack);
            } catch(NotFound e) {
                jdbcSummary.put(sumStack);
            }
        }
        
        //        saveImage(sumStack,
        //                   station,
        //                   parentDir,
        //                   minPercentMatch,
        //                   smallestH);
        return sumStack;
    }

    public SumHKStack sum(String netCode,
                          String staCode,
                          float percentMatch,
                          QuantityImpl smallestH,
                          boolean doBootstrap,
                          boolean usePhaseWeight) throws FissuresException, NotFound,
            IOException, SQLException {
        logger.info("in sum for "+netCode+"."+staCode);
        TimeOMatic.start();
        ArrayList individualHK = jdbcHKStack.getForStation(netCode, staCode, percentMatch, true);
        // if there is only 1 eq that matches, then we can't really do a stack
        if(individualHK.size() > 1) {
            HKStack temp = (HKStack)individualHK.get(0);
            SumHKStack sumStack = new SumHKStack((HKStack[])individualHK.toArray(new HKStack[0]),
                                                 temp.getChannel(),
                                                 percentMatch,
                                                 smallestH,
                                                 doBootstrap,
                                                 usePhaseWeight);
            TimeOMatic.print("sum for "+netCode+"."+staCode);
            return sumStack;
        } else {
            return null;
        }
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
                                                + FissuresFormatter.filize(ChannelIdUtil.toStringNoDates(sumStack.getChannel()
                                                        .get_id())
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

    JDBCHKStack jdbcHKStack;

    JDBCSummaryHKStack jdbcSummary;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StackSummary.class);

    public static Connection initDB(Properties props) throws IOException, SQLException {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        String dbURL = props.getProperty("cormorant.servers.ears.databaseURL");
        ConnMgr.setURL(dbURL);
        Connection conn = ConnMgr.createConnection();
        return conn;
    }
    
    public static void parseArgsAndRun(String[] args, StackSummary summary) throws FissuresException, NotFound, IOException, SQLException {
        float minPercentMatch = 80f;
        boolean bootstrap = true;
        boolean usePhaseWeight = true;
        String netArg = "";
        String staArg = "";
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-net")) {
                netArg = args[i + 1];
            } else if(args[i].equals("-sta")) {
                staArg = args[i + 1];
            } else if(args[i].equals("-all")) {
                netArg = args[i];
            } else if (args[i].equals("--nobootstrap")) {
                bootstrap = false;
            }
        }
        if (staArg.equals("")) {
        summary.createSummary(netArg, new File("stackImages" + HKStack.getDefaultSmallestH().getValue(UnitImpl.KILOMETER)
                + "_" + minPercentMatch), minPercentMatch, HKStack.getDefaultSmallestH(), bootstrap, usePhaseWeight);
        } else {
            logger.info("calc for station");
            JDBCStation jdbcStation = summary.jdbcHKStack.getJDBCChannel().getStationTable();
            NetworkId[] nets = jdbcStation.getNetTable().getByCode(netArg);
            int sta_dbid = -1;
            for(int i = 0; i < nets.length; i++) {
                try {
                    int[] tmp = jdbcStation.getDBIds(nets[i],staArg);
                    if (tmp.length > 0) {
                        sta_dbid = tmp[0];
                        Station station = jdbcStation.get(sta_dbid);
                        summary.createSummary(station.get_id(),
                                              new File("stackImages" + HKStack.getDefaultSmallestH().getValue(UnitImpl.KILOMETER)+ "_" + minPercentMatch),
                                              minPercentMatch,
                                              HKStack.getBestSmallestH(station, HKStack.getDefaultSmallestH()), bootstrap, usePhaseWeight);
                    }
                } catch (NotFound e) {
                    System.out.println("NotFound for :"+NetworkIdUtil.toStringNoDates(nets[i]));
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
}