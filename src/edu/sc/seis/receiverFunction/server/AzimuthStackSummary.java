package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.AzimuthSumHKStack;
import edu.sc.seis.receiverFunction.BazIterator;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.receiverFunction.web.Start;
import edu.sc.seis.sod.ConfigurationException;

public class AzimuthStackSummary extends StackSummary {

    public AzimuthStackSummary(Connection conn, Properties props)
            throws IOException, SQLException, ConfigurationException,
            TauModelException, Exception {
        super(props);
    }

    public SumHKStack createSummary(StationId station,
                                    float gaussianWidth,
                                    float minPercentMatch,
                                    QuantityImpl smallestH,
                                    boolean doBootstrap,
                                    boolean usePhaseWeight)
            throws FissuresException, NotFound, IOException, SQLException {
        azimuthSum(station.network_id.network_code,
                   station.station_code,
                   gaussianWidth,
                   minPercentMatch,
                   smallestH,
                   doBootstrap,
                   usePhaseWeight);
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
                                          boolean usePhaseWeight)
            throws FissuresException, NotFound, IOException, SQLException {
        logger.info("in sum for " + netCode + "." + staCode);
        RecFuncDB rfdb = RecFuncDB.getSingleton();
        TimeOMatic.start();
        List<AzimuthSumHKStack> out = new ArrayList<AzimuthSumHKStack>();
        NetworkAttrImpl net = Start.getNetwork(netCode).getWrapped();
        List<ReceiverFunctionResult> individualHK = rfdb.getSuccessful(net,
                                                                       staCode,
                                                                       gaussianWidth,
                                                                       percentMatch);
        for(float center = 0; center < 360; center += step) {
            List<ReceiverFunctionResult> sectorHK = new ArrayList<ReceiverFunctionResult>();
            BazIterator bazIt = new BazIterator(individualHK.iterator(), center
                    - width / 2, center + width / 2);
            while(bazIt.hasNext()) {
                sectorHK.add(bazIt.next());
            }
            // if there is only 1 eq that matches, then we can't really do a
            // stack
            if(sectorHK.size() > 1) {
                List<StationImpl> sta = NetworkDB.getSingleton()
                        .getStationByCodes(netCode, staCode);
                Set<RejectedMaxima> rejects = new HashSet<RejectedMaxima>();
                rejects.addAll(rfdb.getRejectedMaxima(net, staCode));
                AzimuthSumHKStack azStack = AzimuthSumHKStack.calculateForPhase(sectorHK,
                                                                                smallestH,
                                                                                percentMatch,
                                                                                usePhaseWeight,
                                                                                rejects,
                                                                                doBootstrap,
                                                                                SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS,
                                                                                "ALL",
                                                                                center,
                                                                                width);
                AzimuthSumHKStack dbAzStack = rfdb.getAzSumStack(net,
                                                                 staCode,
                                                                 gaussianWidth,
                                                                 center,
                                                                 width);
                if(dbAzStack == null) {
                    rfdb.putAzimuthSummary(azStack);
                } else {
                    azStack.setDbid(dbAzStack.getDbid());
                    RecFuncDB.getSession().evict(dbAzStack);
                    RecFuncDB.getSession().saveOrUpdate(azStack);
                }
                TimeOMatic.print("sum for " + netCode + "." + staCode);
                out.add(azStack);
            }
        }
        return (AzimuthSumHKStack[])out.toArray(new AzimuthSumHKStack[0]);
    }

    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: AzimuthStackSummary -net netCode [ -sta staCode ]");
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

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AzimuthStackSummary.class);
}
