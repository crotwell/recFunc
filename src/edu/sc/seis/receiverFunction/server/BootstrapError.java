package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.ConfigurationException;

/**
 * @author crotwell Created on Jun 20, 2005
 */
public class BootstrapError extends StackSummary {

    /**
     *
     */
    public BootstrapError(Connection conn) throws IOException, SQLException,
            ConfigurationException, TauModelException, Exception {
        super(conn);
    }

    public SumHKStack createSummary(StationId station,
                                    File parentDir,
                                    float minPercentMatch,
                                    QuantityImpl smallestH)
            throws FissuresException, NotFound, IOException, SQLException {
        System.out.println("createSummary for "
                + StationIdUtil.toStringNoDates(station));
        SumHKStack sumStack = sum(station.network_id.network_code,
                                  station.station_code,
                                  minPercentMatch,
                                  smallestH);
        if(sumStack == null) {
            System.out.println("stack is null for "
                    + StationIdUtil.toStringNoDates(station));
        } else {
            try {
                int dbid = jdbcSummary.getDbIdForStation(station.network_id,
                                                         station.station_code);
                //jdbcSummary.update(dbid, sumStack);
            } catch(NotFound e) {
                //jdbcSummary.put(sumStack);
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
                          QuantityImpl smallestH) throws FissuresException,
            NotFound, IOException, SQLException {
        logger.info("in sum for " + netCode + "." + staCode);
        TimeOMatic.start();
        ArrayList individualHK = jdbcHKStack.getForStation(netCode,
                                                           staCode,
                                                           percentMatch);
        // if there is only 1 eq that matches, then we can't really do a stack
        if(individualHK.size() > 1) {
            HKStack temp = (HKStack)individualHK.get(0);
            SumHKStack sumStack = new SumHKStack((HKStack[])individualHK.toArray(new HKStack[0]),
                                                 temp.getChannel(),
                                                 percentMatch,
                                                 smallestH);
            HKError hkError = new HKError(individualHK, 100, percentMatch, smallestH);
            TimeOMatic.print("sum for " + netCode + "." + staCode);
            return sumStack;
        } else {
            return null;
        }
    }
    
    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: BootstrapError -net netCode [ -sta staCode ]");
            return;
        }
        try {
            Properties props = loadProps(args);
            Connection conn = initDB(props);
            StackSummary summary = new BootstrapError(conn);
            parseArgsAndRun(args, summary);
        } catch(Exception e) {
            logger.error(e);
        }
    }
    
    class HKError {

        HKError(ArrayList stacks,
                int iterations,
                float percentMatch,
                QuantityImpl smallestH) {
            this.iterations = iterations;
            HKStack temp = (HKStack)stacks.get(0);
            Random random = new Random();
            double[] hErrors = new double[iterations];
            double[] kErrors = new double[iterations];
            TimeOMatic.start();
            for(int i = 0; i < iterations; i++) {
                ArrayList sample = new ArrayList();
                for(int j = 0; j < stacks.size(); j++) {
                    sample.add(stacks.get(randomInt(stacks.size())));
                }
                SumHKStack sumStack = new SumHKStack((HKStack[])sample.toArray(new HKStack[0]),
                                                     temp.getChannel(),
                                                     percentMatch,
                                                     smallestH);
                hErrors[i] = sumStack.getSum()
                        .getMaxValueH()
                        .getValue(UnitImpl.KILOMETER);
                kErrors[i] = sumStack.getSum().getMaxValueK();
                System.out.println(i+" "+hErrors[i]+"  "+kErrors[i]);
            }
            
            Statistics hStat = new Statistics(hErrors);
            Statistics kStat = new Statistics(kErrors);
            TimeOMatic.print("Stat for "
                    + ChannelIdUtil.toStringNoDates(temp.getChannel())
                    + " h stddev=" + hStat.stddev() + "  k stddev="
                    + kStat.stddev());
        }

        int randomInt(int top) {
            return (int)Math.floor(Math.random() * top);
        }

        int iterations;

        float hError, kError;
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BootstrapError.class);
}