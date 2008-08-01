package edu.sc.seis.receiverFunction;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.PhaseCut;
import edu.sc.seis.fissuresUtil.bag.PhaseNonExistent;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.RecFuncQCResult;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.receiverFunction.server.StackSummary;
import edu.sc.seis.receiverFunction.web.Start;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

public class QualityControl {

    public QualityControl() {}

    public float transverseToRadial(ReceiverFunctionResult result)
            throws FissuresException, TauModelException, PhaseNonExistent,
            NoPreferredOrigin {
        LocalSeismogramImpl radial = (LocalSeismogramImpl)result.getRadial();
        LocalSeismogramImpl transverse = (LocalSeismogramImpl)result.getTransverse();
        PhaseCut cut = new PhaseCut(TauPUtil.getTauPUtil(),
                                    "P",
                                    new TimeInterval(-1, UnitImpl.SECOND),
                                    "P",
                                    new TimeInterval(1, UnitImpl.SECOND));
        radial = cut.cut(result.getChannelGroup()
                .getChannel1()
                .getSite()
                .getStation()
                .getLocation(), result.getEvent().getPreferred(), radial);
        transverse = cut.cut(result.getChannelGroup()
                .getChannel1()
                .getSite()
                .getStation()
                .getLocation(), result.getEvent().getPreferred(), transverse);
        Statistics radialStats = new Statistics(radial);
        Statistics transStats = new Statistics(transverse);
        return (float)(transStats.max() / radialStats.max());
    }

    public float radialPAmp(ReceiverFunctionResult result)
            throws FissuresException, TauModelException, PhaseNonExistent,
            NoPreferredOrigin {
        LocalSeismogramImpl radial = (LocalSeismogramImpl)result.getRadial();
        LocalSeismogramImpl radialP;
        PhaseCut cut = new PhaseCut(TauPUtil.getTauPUtil(),
                                    "P",
                                    new TimeInterval(-1, UnitImpl.SECOND),
                                    "P",
                                    new TimeInterval(1, UnitImpl.SECOND));
        radialP = cut.cut(result.getChannelGroup()
                .getChannel1()
                .getSite()
                .getStation()
                .getLocation(), result.getEvent().getPreferred(), radial);
        Statistics radialStats = new Statistics(radial);
        Statistics radialPStats = new Statistics(radialP);
        return (float)((radialPStats.max() - radialStats.mean()) / radialStats.maxDeviation());
    }

    public boolean check(ReceiverFunctionResult result)
            throws FissuresException, TauModelException, PhaseNonExistent,
            SQLException, NoPreferredOrigin {
        float tToR = transverseToRadial(result);
        float pAmp = radialPAmp(result);
        if(tToR > MAX_T_TO_R_RATIO || pAmp < MIN_P_TO_MAX_AMP_RATIO) {
            result.setQc(new RecFuncQCResult(false,
                                             false,
                                             tToR,
                                             pAmp,
                                             "",
                                             ClockUtil.now().getTimestamp()));
            System.out.println(decFormat.format(result.getRadialMatch())
                    + " "
                    + StationIdUtil.toStringFormatDates(result.getChannelGroup()
                            .getChannel1()
                            .getSite()
                            .getStation())
                    + " origin="
                    + result.getEvent().getPreferred().getOriginTime().date_time
                    + "  T to R=" + decFormat.format(tToR) + "  P amp="
                    + decFormat.format(pAmp));
            return false;
        } else {
            result.setQc(new RecFuncQCResult(true,
                                             false,
                                             tToR,
                                             pAmp,
                                             "",
                                             ClockUtil.now().getTimestamp()));
            return true;
        }
    }

    static JSAP setUpArgParser() throws JSAPException {
        JSAP jsap = new JSAP();
        Parameter hkbad = new FlaggedOption("hkbad").setList(true)
                .setListSeparator(',')
                .setRequired(false)
                .setStringParser(JSAP.FLOAT_PARSER)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("hkbad")
                .setHelp("hMin,hMax,kMin,kMax");
        jsap.registerParameter(hkbad);
        FlaggedOption net = new FlaggedOption("net").setRequired(false)
                .setShortFlag('n')
                .setLongFlag("net");
        jsap.registerParameter(net);
        FlaggedOption sta = new FlaggedOption("sta").setRequired(false)
                .setShortFlag('s')
                .setLongFlag("sta");
        jsap.registerParameter(sta);
        FlaggedOption rfbad = new FlaggedOption("rfbad").setRequired(false)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("rfbad")
                .setStringParser(JSAP.INTEGER_PARSER);
        jsap.registerParameter(rfbad);
        FlaggedOption reason = new FlaggedOption("reason").setRequired(false)
                .setDefault("manual")
                .setShortFlag('r')
                .setLongFlag("reason");
        jsap.registerParameter(reason);
        Switch useDB = new Switch("db").setLongFlag("db")
                .setShortFlag(JSAP.NO_SHORTFLAG);
        jsap.registerParameter(useDB);
        FlaggedOption props = new FlaggedOption("props").setRequired(true)
                .setShortFlag('p')
                .setLongFlag("props");
        jsap.registerParameter(props);
        Switch help = new Switch("help").setLongFlag("help").setShortFlag('h');
        jsap.registerParameter(help);
        return jsap;
    }

    public static void main(String[] args) throws JSAPException {
        JSAP jsap = setUpArgParser();
        try {
            TimeOMatic.setWriter(new FileWriter("netTimes.txt"));
            TimeOMatic.start();
            Properties props = StackSummary.loadProps(args);
            Connection conn = StackSummary.initDB(props);
            QualityControl control = new QualityControl();
            String netArg = "";
            String staArg = "";
            boolean dbUpdate = false;
            int rfbad = -1;
            float hMin = -1, hMax = -1, kMin = -1, kMax = -1;
            JSAPResult argResult = jsap.parse(args);
            if(args.length == 0 || argResult.getBoolean("help")
                    || !argResult.success()) {
                for(Iterator errs = argResult.getErrorMessageIterator(); errs.hasNext();) {
                    System.err.println("Error: " + errs.next());
                }
                System.err.println();
                System.err.println("Usage: java "
                        + control.getClass().getName());
                System.err.println("                " + jsap.getUsage());
                System.err.println();
                System.err.println(jsap.getHelp());
                System.exit(1);
            }
            netArg = argResult.getString("net", "");
            staArg = argResult.getString("sta", "");
            dbUpdate = argResult.getBoolean("db");
            String reason = argResult.getString("reason");
            rfbad = argResult.getInt("rfbad", -1);
            if(argResult.contains("hkbad")) {
                float[] hkbadList = argResult.getFloatArray("hkbad");
                hMin = hkbadList[0];
                hMax = hkbadList[1];
                kMin = hkbadList[2];
                kMax = hkbadList[3];
            }
            if(hMin != -1) {
                if(netArg.equals("") || staArg.equals("")) {
                    throw new NotFound(netArg + " " + staArg);
                }
                VelocityNetwork net = Start.getNetwork(netArg);
                RecFuncDB.getSingleton()
                        .put(new RejectedMaxima(net.getWrapped(),
                                                staArg,
                                                hMin,
                                                hMax,
                                                kMin,
                                                kMax,
                                                reason));
                return;
            } else if(argResult.contains("rfbad") && rfbad != -1) {
                // set single one bad
                ReceiverFunctionResult result = RecFuncDB.getSingleton()
                        .getReceiverFunctionResult(rfbad);
                float tToR = control.transverseToRadial(result);
                float pAmp = control.radialPAmp(result);
                result.setQc(new RecFuncQCResult(false,
                                                 true,
                                                 tToR,
                                                 pAmp,
                                                 reason,
                                                 ClockUtil.now().getTimestamp()));
            }
            logger.info("calc for station: " + netArg + "." + staArg);
            List<NetworkAttrImpl> nets = NetworkDB.getSingleton()
                    .getNetworkByCode(netArg);
            System.out.println("calc for " + nets.size() + " nets");
            NetworkDB netdb = NetworkDB.getSingleton();
            for(NetworkAttrImpl networkAttrImpl : nets) {
                String[] staCodes;
                if(staArg.length() == 0) {
                    System.out.println("Calc for all stations in "
                            + NetworkIdUtil.toStringFormatDates(networkAttrImpl));
                    List<StationImpl> staIds = netdb.getStationForNet(networkAttrImpl);
                    System.out.println("Got " + staIds.size() + " stations");
                    Set allCodes = new HashSet();
                    for(StationImpl stationImpl : staIds) {
                        allCodes.add(stationImpl.get_code());
                    }
                    staCodes = (String[])allCodes.toArray(new String[0]);
                } else {
                    staCodes = new String[] {staArg};
                }
                for(String staCode : staCodes) {
                    System.out.println("Processing " + staCode);
                    List<StationImpl> stations = netdb.getStationByCodes(networkAttrImpl.get_code(),
                                                                         staCode);
                    int numGood = 0;
                    List<ReceiverFunctionResult> results = RecFuncDB.getSingleton()
                            .getSuccessful((NetworkAttrImpl)stations.get(0)
                                                   .getNetworkAttr(),
                                           staCode,
                                           2.5f,
                                           80f);
                    for(ReceiverFunctionResult receiverFunctionResult : results) {
                        if(control.check(receiverFunctionResult)) {
                            numGood++;
                        }
                    }
                    System.out.println(StationIdUtil.toStringNoDates(stations.get(0)
                            .getId())
                            + " Num good = "
                            + numGood
                            + " out of "
                            + results.size()
                            + "  => "
                            + decFormat.format(numGood * 100.0 / results.size())
                            + "%");
                }
                if(dbUpdate) {
                    System.out.println("Update recfuncQC in database");
                } else {
                    System.out.println("Database NOT updated");
                }
            }
        } catch(Exception e) {
            System.out.println("Problem, see log file: " + e.getMessage());
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
