package edu.sc.seis.receiverFunction.server;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAttr;
import edu.sc.seis.fissuresUtil.database.event.JDBCOrigin;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.crust2.Crust2;
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
        JDBCChannel jdbcChannel  = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn, jdbcEventAccess, jdbcChannel, jdbcSodConfig, RecFuncCacheImpl.getDataLoc());
        jdbcHKStack = new JDBCHKStack(conn, jdbcEventAccess, jdbcChannel, jdbcSodConfig, jdbcRecFunc);
        jdbcSummary = new JDBCSummaryHKStack(jdbcHKStack);
    }

    public void createSummary(String net,
                              File parentDir,
                              float minPercentMatch,
                              float smallestH) throws FissuresException,
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
                    System.out.println("calc for "+StationIdUtil.toStringNoDates(station[j].get_id()));

                    Crust2Profile crust2 = HKStack.getCrust2().getClosest(station[j].my_location.longitude,
                                                                          station[j].my_location.latitude);
                    double crust2H = crust2.getCrustThickness();
                    SumHKStack sumStack;
                    if (crust2H > smallestH+5) {
                        sumStack = createSummary(station[j].get_id(),
                                                 parentDir,
                                                 minPercentMatch,
                                                 smallestH);
                    } else {
                        float modSmallestH = (float)(crust2H-5);
                        if (modSmallestH < JDBCHKStack.getDefaultMinH()) {
                            modSmallestH = JDBCHKStack.getDefaultMinH();
                        }
                        sumStack = createSummary(station[j].get_id(),
                                                 parentDir,
                                                 minPercentMatch,
                                                 modSmallestH);
                    }
                    if(sumStack == null) {
                        continue;
                    }
                    String outStr = StationIdUtil.toStringNoDates(station[j].get_id()) + " "
                            + station[j].my_location.latitude + " "
                            + station[j].my_location.longitude;
                    float peakH, peakK, peakVal = 0;
                    int[] indicies = sumStack.getSum().getMaxValueIndices();
                    peakH = sumStack.getSum().getMaxValueH();
                    peakK = sumStack.getSum().getMaxValueK();
                    peakVal = sumStack.getSum().getMaxValue();
                    outStr += " " + peakH + " " + peakK + " " + peakVal+" "+sumStack.getIndividuals().length;
                    outStr += " "+(float)(2*Math.sqrt(sumStack.getHVariance()))+" "+(float)(2*Math.sqrt(sumStack.getKVariance()));
                    
                    double depth = crust2.getCrustThickness();
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
                                    float smallestH)
            throws FissuresException, NotFound, IOException, SQLException {
        SumHKStack sumStack = jdbcHKStack.sum(station.network_id.network_code,
                                              station.station_code,
                                              minPercentMatch,
                                              smallestH);
        try {
            int dbid = jdbcSummary.getDbIdForStation(station.network_id, station.station_code);
            jdbcSummary.update(dbid, sumStack);
        } catch (NotFound e) {
            jdbcSummary.put(sumStack);
        }
        
        saveImage(sumStack,
                   station,
                   parentDir,
                   minPercentMatch,
                   smallestH);
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
        String propFilename=
            "rfcache.prop";
        String defaultsFilename=
            "edu/sc/seis/receiverFunction/server/"+propFilename;
        
        for (int i=0; i<args.length-1; i++) {
            if (args[i].equals("-props")) {
                // override with values in local directory,
                // but still load defaults with original name
                propFilename = args[i+1];
            }
        }
        
        try {
            props.load((StackSummary.class).getClassLoader().getResourceAsStream( defaultsFilename ));
        } catch (IOException e) {
            System.err.println("Could not load defaults. "+e);
        }
        try {
            FileInputStream in = new FileInputStream(propFilename);
            props.load(in);
            in.close();
        } catch (FileNotFoundException f) {
            System.err.println(" file missing "+f+" using defaults");
        } catch (IOException f) {
            System.err.println(f.toString()+" using defaults");
        }
        
        // configure logging from properties...
        PropertyConfigurator.configure(props);
        logger.info("Logging configured");
        return props;
    }
    
    JDBCHKStack jdbcHKStack;
    
    JDBCSummaryHKStack jdbcSummary;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StackSummary.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: StackSummary netCode");
            return;
        }
        try {
            Properties props = loadProps(args);
            ConnMgr.setDB(ConnMgr.POSTGRES);
            Connection conn = ConnMgr.createConnection();
            StackSummary summary = new StackSummary(conn);
            float minPercentMatch = 80f;
            int smallestH = 25;
            summary.createSummary(args[0], new File("stackImages"+smallestH+"_"+minPercentMatch), minPercentMatch, smallestH);
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
