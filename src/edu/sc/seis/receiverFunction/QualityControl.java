package edu.sc.seis.receiverFunction;

import java.io.FileWriter;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Properties;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
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
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
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
        return (float)(radialPStats.max() / radialStats.maxDeviation());
    }

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
            for(int i = 0; i < args.length; i++) {
                if(args[i].equals("-net")) {
                    netArg = args[i + 1];
                } else if(args[i].equals("-sta")) {
                    staArg = args[i + 1];
                }
            }
            if(staArg.equals("")) {
                System.out.println("Must give a station with -sta");
                return;
            } else {
                logger.info("calc for station: " + netArg + "." + staArg);
                JDBCStation jdbcStation = jdbcRecFunc.getJDBCChannel()
                        .getStationTable();
                NetworkId[] nets = jdbcStation.getNetTable().getByCode(netArg);
                int sta_dbid = -1;
                for(int i = 0; i < nets.length; i++) {
                    try {
                        int[] tmp = jdbcStation.getDBIds(nets[i], staArg);
                        if(tmp.length > 0) {
                            sta_dbid = tmp[0];
                            Station station = jdbcStation.get(sta_dbid);
                            int netDbId = jdbcStation.getNetTable()
                                    .getDbId(station.my_network.get_id());
                            CachedResult[] results = jdbcRecFunc.getByPercent(netDbId,
                                                                              staArg,
                                                                              2.5f,
                                                                              80f);
                            for(int j = 0; j < results.length; j++) {
                                System.out.println(StationIdUtil.toStringFormatDates(station)
                                        + " origin="
                                        + results[j].prefOrigin.origin_time.date_time
                                        + "  T to R="
                                        + decFormat.format(control.transverseToRadial(results[j]))
                                        + "  P amp="
                                        + decFormat.format(control.radialPAmp(results[j])));
                            }
                        }
                    } catch(NotFound e) {
                        System.out.println("NotFound for :"
                                + NetworkIdUtil.toStringNoDates(nets[i]));
                        // go to next network
                    }
                }
            }
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }

    private static DecimalFormat decFormat = new DecimalFormat("0.000");

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(QualityControl.class);
}
