package edu.sc.seis.receiverFunction.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.FissuresFormatter;


/**
 * @author crotwell
 * Created on Oct 7, 2004
 */
public class StackSummary {

    public StackSummary(Connection conn) throws IOException, SQLException, ConfigurationException, TauModelException {
        jdbcHKStack = new JDBCHKStack(conn);
    }
    
    public void createSummary(String net, File parentDir, float minPercentMatch) throws FissuresException, NotFound, IOException, SQLException {
        JDBCStation jdbcStation = jdbcHKStack.getJDBCChannel().getSiteTable().getStationTable();
        JDBCNetwork jdbcNetwork = jdbcStation.getNetTable();
        NetworkId[] netId = jdbcNetwork.getAllNetworkIds();
        for(int i = 0; i < netId.length; i++) {
            if (netId[i].network_code.equals(net)) {
                Station[] station = jdbcStation.getAllStations(netId[i]);
                for(int j = 0; j < station.length; j++) {
                    createSummary(station[j].get_id(), parentDir, minPercentMatch);
                }
            }
        }
    }
    
    public void createSummary(StationId station, File parentDir, float minPercentMatch) throws FissuresException, NotFound, IOException, SQLException {
        SumHKStack sumStack = jdbcHKStack.sum(station.network_id.network_code, station.station_code, minPercentMatch);
        if (sumStack == null) { 
            logger.info("No hk plots for "+StationIdUtil.toStringNoDates(station)+" with match > "+minPercentMatch);
            return; 
        }
        BufferedImage image = sumStack.createStackImage();
        
        File outSumImageFile  = new File(parentDir,"SumHKStack_"+minPercentMatch+"_"+FissuresFormatter.filize(ChannelIdUtil.toStringNoDates(sumStack.getChannel().get_id())+".png"));
        if (outSumImageFile.exists()) {outSumImageFile.delete();}
        javax.imageio.ImageIO.write(image, "png", outSumImageFile);
    }
    
    JDBCHKStack jdbcHKStack;
 
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StackSummary.class);
    
    public static void main(String[] args) {
    try {
        Properties props = RecFuncCacheStart.loadProps(args);
        ConnMgr.setDB(ConnMgr.POSTGRES);
        Connection conn = ConnMgr.createConnection();
        StackSummary summary = new StackSummary(conn);
        summary.createSummary(args[0], new File("stackImages"), 90f);
    } catch(Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
    }
}
