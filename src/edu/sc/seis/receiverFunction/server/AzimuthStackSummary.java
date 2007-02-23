package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.AzimuthSumHKStack;
import edu.sc.seis.receiverFunction.BazIterator;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.ConfigurationException;

public class AzimuthStackSummary extends StackSummary {

    public AzimuthStackSummary(Connection conn, Properties props) throws IOException, SQLException, ConfigurationException,
            TauModelException, Exception {
        super(conn, props);
        jdbcAz = new JDBCAzimuthSummaryHKStack(jdbcSummary);
    }

    public SumHKStack createSummary(StationId station,
                              float gaussianWidth,
                              float minPercentMatch,
                              QuantityImpl smallestH,
                              boolean doBootstrap,
                              boolean usePhaseWeight) throws FissuresException, NotFound, IOException, SQLException {
        AzimuthSumHKStack[] azimuthSum = azimuthSum(station.network_id.network_code,
                                                    station.station_code,
                                                    gaussianWidth,
                                                    minPercentMatch,
                                                    smallestH,
                                                    doBootstrap,
                                                    usePhaseWeight);
        for(int i = 0; i < azimuthSum.length; i++) {
            try {
                int dbid = jdbcAz.getDbIdForStation(station.network_id, station.station_code, azimuthSum[i].getAzimuth(), azimuthSum[i].getAzWidth());
                jdbcAz.update(dbid, azimuthSum[i]);
            } catch(NotFound e) {
                jdbcAz.put(azimuthSum[i]);
            }
        }
        // bad code, but isn't needed
        // fix later
        // famous last words...
        return null;
    }

    public AzimuthSumHKStack[] azimuthSum(String netCode,
                                          String staCode,
                                          float gaussianWidth,
                                          float percentMatch,
                                          QuantityImpl smallestH,
                                          boolean doBootstrap,
                                          boolean usePhaseWeight) throws FissuresException, NotFound, IOException,
            SQLException {
        logger.info("in sum for " + netCode + "." + staCode);
        TimeOMatic.start();
        List out = new ArrayList();
        ArrayList individualHK = jdbcHKStack.getForStation(netCode, staCode, gaussianWidth, percentMatch, true);
        for(float center = 0; center < 360; center += step) {
            ArrayList sectorHK = new ArrayList();
            BazIterator bazIt = new BazIterator(individualHK.iterator(), center - width / 2, center + width / 2);
            while(bazIt.hasNext()) {
                sectorHK.add(bazIt.next());
            }
            // if there is only 1 eq that matches, then we can't really do a
            // stack
            if(sectorHK.size() > 1) {
                HKStack temp = (HKStack)sectorHK.get(0);
                int netDbId = jdbcHKStack.getJDBCChannel().getStationTable().getBestNetworkDbId(netCode, staCode);
                HKBox[] rejects = jdbcRejectMax.getForStation(netDbId,
                                                              staCode);
                SumHKStack sumStack = new SumHKStack((HKStack[])sectorHK.toArray(new HKStack[0]),
                                                     temp.getChannel(),
                                                     percentMatch,
                                                     smallestH,
                                                     doBootstrap,
                                                     usePhaseWeight,
                                                     rejects);
                TimeOMatic.print("sum for " + netCode + "." + staCode);
                AzimuthSumHKStack azStack = new AzimuthSumHKStack(sumStack, center, width);
                out.add(azStack);
            }
        }
        return (AzimuthSumHKStack[])out.toArray(new AzimuthSumHKStack[0]);
    }

    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: StackSummary -net netCode [ -sta staCode ]");
            return;
        }
        try {
            Properties props = loadProps(args);
            Connection conn = initDB(props);
            AzimuthStackSummary summary = new AzimuthStackSummary(conn, props);
            parseArgsAndRun(args, summary);
        } catch(Exception e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    float step = 22.5f;

    float width = 45;

    JDBCAzimuthSummaryHKStack jdbcAz;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AzimuthStackSummary.class);
}
