package edu.sc.seis.receiverFunction;

import java.io.FileWriter;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.PhaseCut;
import edu.sc.seis.fissuresUtil.bag.PhaseNonExistent;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.server.CachedResultPlusDbId;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCRecFuncQC;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.receiverFunction.server.RecFuncQCResult;
import edu.sc.seis.receiverFunction.server.StackSummary;

public class QualityControl {

    public float transverseToRadial(CachedResult result)
            throws FissuresException, TauModelException, PhaseNonExistent {
        LocalSeismogramImpl radial = (LocalSeismogramImpl)result.radial;
        LocalSeismogramImpl transverse = (LocalSeismogramImpl)result.tansverse;
        PhaseCut cut = new PhaseCut(TauPUtil.getTauPUtil(),
                                    "P",
                                    new TimeInterval(-1, UnitImpl.SECOND),
                                    "P",
                                    new TimeInterval(1, UnitImpl.SECOND));
        radial = cut.cut(result.channels[0].my_site.my_station.my_location,
                         result.prefOrigin,
                         radial);
        transverse = cut.cut(result.channels[0].my_site.my_station.my_location,
                             result.prefOrigin,
                             transverse);
        Statistics radialStats = new Statistics(radial);
        Statistics transStats = new Statistics(transverse);
        return (float)(transStats.max() / radialStats.max());
    }

    public float radialPAmp(CachedResult result) throws FissuresException,
            TauModelException, PhaseNonExistent {
        LocalSeismogramImpl radial = (LocalSeismogramImpl)result.radial;
        LocalSeismogramImpl radialP;
        PhaseCut cut = new PhaseCut(TauPUtil.getTauPUtil(),
                                    "P",
                                    new TimeInterval(-1, UnitImpl.SECOND),
                                    "P",
                                    new TimeInterval(1, UnitImpl.SECOND));
        radialP = cut.cut(result.channels[0].my_site.my_station.my_location,
                          result.prefOrigin,
                          radial);
        Statistics radialStats = new Statistics(radial);
        Statistics radialPStats = new Statistics(radialP);
        return (float)((radialPStats.max() - radialStats.mean()) / radialStats.maxDeviation());
    }

    public void calcForStation(int netDbId, String staCode) {}

    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: StackSummary -net netCode [ -sta staCode ]");
            return;
        }
        try {
            TimeOMatic.setWriter(new FileWriter("netTimes.txt"));
            TimeOMatic.start();
            Properties props = StackSummary.loadProps(args);
            Connection conn = StackSummary.initDB(props);
            JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn,
                                                      RecFuncCacheImpl.getDataLoc());
            QualityControl control = new QualityControl();
            String netArg = "";
            String staArg = "";
            boolean dbUpdate = false;
            int rfbad = -1;
            String rfBadReason = "manual";
            for(int i = 0; i < args.length; i++) {
                if(args[i].equals("-net")) {
                    netArg = args[i + 1];
                    i++;
                } else if(args[i].equals("-sta")) {
                    staArg = args[i + 1];
                    i++;
                } else if(args[i].equals("-db")) {
                    dbUpdate = true;
                } else if(args[i].equals("-rfbad")) {
                    rfbad = Integer.parseInt(args[i+1]);
                    rfBadReason = args[i+2];
                    i+=2;
                }
            }
            JDBCStation jdbcStation = jdbcRecFunc.getJDBCChannel().getStationTable();
            JDBCRecFuncQC jdbcRecFuncQC = new JDBCRecFuncQC(conn);
            if (rfbad != -1) {
                // set single one bad
                CachedResultPlusDbId resultWithDbId = jdbcRecFunc.get(rfbad);
                CachedResult result = resultWithDbId.getCachedResult();
                float tToR = control.transverseToRadial(result);
                float pAmp = control.radialPAmp(result);
                jdbcRecFuncQC.put(new RecFuncQCResult(resultWithDbId.getDbId(),
                                                      false,
                                                      true,
                                                      tToR,
                                                      pAmp,
                                                      rfBadReason,
                                                      ClockUtil.now().getTimestamp()));
            }
            logger.info("calc for station: " + netArg + "." + staArg);
            NetworkId[] nets = jdbcStation.getNetTable().getByCode(netArg);
            System.out.println("calc for "+nets.length+" nets");
            for(int i = 0; i < nets.length; i++) {
                try {
                    String[] staCodes;
                    if(staArg.length() == 0) {
                        System.out.println("Calc for all stations in "+NetworkIdUtil.toStringFormatDates(nets[i]));
                        StationId[] staIds = jdbcStation.getAllStationIds(nets[i]);
                        System.out.println("Got "+staIds.length+" stations");
                        Set allCodes = new HashSet();
                        for(int j = 0; j < staIds.length; j++) {
                            allCodes.add(staIds[j].station_code);
                        }
                        staCodes = (String[])allCodes.toArray(new String[0]);
                    } else {
                        staCodes = new String[] {staArg};
                    }
                    for(int j = 0; j < staCodes.length; j++) {
                        System.out.println("Processing "+staCodes[j]);
                        int[] dbids = jdbcStation.getDBIds(nets[i], staCodes[j]);
                        Station station = jdbcStation.get(dbids[0]);
                        int numGood = 0;
                        int netDbId = jdbcStation.getNetTable()
                                .getDbId(station.my_network.get_id());
                        CachedResultPlusDbId[] resultsWithDbId = jdbcRecFunc.getSuccessful(netDbId,
                                                                          staCodes[j],
                                                                          2.5f,
                                                                          80f);
                        for(int r = 0; r < resultsWithDbId.length; r++) {
                            CachedResult result = resultsWithDbId[r].getCachedResult();
                            float tToR = control.transverseToRadial(result);
                            float pAmp = control.radialPAmp(result);
                            if(tToR > MAX_T_TO_R_RATIO || pAmp < MIN_P_TO_MAX_AMP_RATIO) {
                                if (dbUpdate) {
                                    jdbcRecFuncQC.put(new RecFuncQCResult(resultsWithDbId[r].getDbId(),
                                                                          false,
                                                                          false,
                                                                          tToR,
                                                                          pAmp,
                                                                          "",
                                                                          ClockUtil.now().getTimestamp()));
                                }
                                System.out.println(decFormat.format(result.radialMatch)
                                        + " "
                                        + StationIdUtil.toStringFormatDates(station)
                                        + " origin="
                                        + result.prefOrigin.origin_time.date_time
                                        + "  T to R="
                                        + decFormat.format(tToR)
                                        + "  P amp=" + decFormat.format(pAmp));
                            } else {
                                numGood++;
                            }
                        }
                        System.out.println(StationIdUtil.toStringNoDates(station)
                                + " Num good = "
                                + numGood
                                + " out of "
                                + resultsWithDbId.length
                                + "  => "
                                + decFormat.format(numGood * 100.0
                                        / resultsWithDbId.length)+"%");
                    }

                    if (dbUpdate) {
                        System.out.println("Update recfuncQC in database");
                    } else {
                        System.out.println("Database NOT updated");
                    }
                } catch(NotFound e) {
                    System.out.println("NotFound for :"
                            + NetworkIdUtil.toStringNoDates(nets[i]));
                    // go to next network
                }
            }
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }

    public static float getMAX_T_TO_R_RATIO() {
        return MAX_T_TO_R_RATIO;
    }

    public static float getMIN_P_TO_MAX_AMP_RATIO() {
        return MIN_P_TO_MAX_AMP_RATIO;
    }
    
    private static float MAX_T_TO_R_RATIO = .5f;
    
    private static float MIN_P_TO_MAX_AMP_RATIO = .8f;

    private static DecimalFormat decFormat = new DecimalFormat("0.000");

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(QualityControl.class);

    
}
