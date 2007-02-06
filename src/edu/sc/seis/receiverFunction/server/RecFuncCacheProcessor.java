package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.UNKNOWN;
import org.omg.CORBA.UserException;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.w3c.dom.Element;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheHelper;
import edu.sc.seis.IfReceiverFunction.RecFuncCacheOperations;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.IncompatibleSeismograms;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.cache.ProxyEventAccessOperations;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.display.configuration.DOMHelper;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.IterDecon;
import edu.sc.seis.receiverFunction.IterDeconResult;
import edu.sc.seis.receiverFunction.RecFunc;
import edu.sc.seis.receiverFunction.RecFuncProcessor;
import edu.sc.seis.sod.ChannelGroup;
import edu.sc.seis.sod.CommonAccess;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.SodUtil;
import edu.sc.seis.sod.Start;
import edu.sc.seis.sod.database.JDBCConfig;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorProcess;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorResult;
import edu.sc.seis.sod.status.StringTreeLeaf;

/**
 * @author crotwell Created on Sep 10, 2004
 */
public class RecFuncCacheProcessor implements WaveformVectorProcess {

    public RecFuncCacheProcessor(Element config) throws ConfigurationException,
            TauModelException {
        IterDeconConfig deconConfig = parseIterDeconConfig(config);
        gwidth = deconConfig.gwidth;
        maxBumps = deconConfig.maxBumps;
        tol = deconConfig.tol;
        Element phaseNameElement = SodUtil.getElement(config, "phaseName");
        if(phaseNameElement != null) {
            String phaseName = SodUtil.getNestedText(phaseNameElement);
            if(phaseName.equals("P")) {
                pWave = true;
            } else {
                pWave = false;
            }
        }
        String dns = DOMHelper.extractText(config, "dns", "edu/sc/seis");
        String serverName = DOMHelper.extractText(config, "name", "Ears");
        String modelName = "prem";
        taup = TauPUtil.getTauPUtil(modelName);
        recFunc = new RecFunc(taup,
                              new IterDecon(maxBumps, true, tol, gwidth),
                              pWave);
        cache = new NSRecFuncCache(dns, serverName, CommonAccess.getNameService());
    }

    public static IterDeconConfig parseIterDeconConfig(Element config) {
        float gwidth = DEFAULT_GWIDTH;
        int maxBumps = DEFAULT_MAXBUMPS;
        float tol = DEFAULT_TOL;
        Element gElement = SodUtil.getElement(config, "gaussianWidth");
        if(gElement != null) {
            String gwidthStr = SodUtil.getNestedText(gElement);
            gwidth = Float.parseFloat(gwidthStr);
        }
        Element bumpsElement = SodUtil.getElement(config, "maxBumps");
        if(bumpsElement != null) {
            String bumpsStr = SodUtil.getNestedText(bumpsElement);
            maxBumps = Integer.parseInt(bumpsStr);
        }
        Element toleranceElement = SodUtil.getElement(config, "tolerance");
        if(toleranceElement != null) {
            String toleranceStr = SodUtil.getNestedText(toleranceElement);
            tol = Float.parseFloat(toleranceStr);
        }
        return new IterDeconConfig(gwidth, maxBumps, tol);
    }

    /**
     * 
     */
    public WaveformVectorResult process(EventAccessOperations event,
                                        ChannelGroup channelGroup,
                                        RequestFilter[][] original,
                                        RequestFilter[][] available,
                                        LocalSeismogramImpl[][] seismograms,
                                        CookieJar cookieJar) throws Exception {
        try {
            if(sodconfig_id == -1) {
                Connection conn = null;
                try {
                    conn = ConnMgr.createConnection();
                    JDBCConfig jdbcConfig = new JDBCConfig(conn);
                    String sodConfig = jdbcConfig.getCurrentConfig();
                    sodconfig_id = cache.insertSodConfig(sodConfig);
                    conn.close();
                } catch(Exception e) {
                    // oh well
                    GlobalExceptionHandler.handle(e);
                    sodconfig_id = -2;
                } finally {
                    if(conn != null) {
                        conn.close();
                    }
                }
            }
            Channel chan = channelGroup.getChannels()[0];
            Origin origin = event.get_preferred_origin();
            IterDeconConfig config = new IterDeconConfig(gwidth, maxBumps, tol);
            ChannelId[] chanIds = new ChannelId[channelGroup.getChannels().length];
            for(int i = 0; i < chanIds.length; i++) {
                chanIds[i] = channelGroup.getChannels()[i].get_id();
            }
            if(!overwrite && cache.isCached(origin, chanIds, config)) {
                return new WaveformVectorResult(seismograms,
                                                new StringTreeLeaf(this,
                                                                   true,
                                                                   "Already calculated"));
            }
            LocalSeismogramImpl[] singleSeismograms = new LocalSeismogramImpl[3];
            for(int i = 0; i < singleSeismograms.length; i++) {
                singleSeismograms[i] = seismograms[i][0];
            }
            IterDeconResult[] ans = recFunc.process(event,
                                                    channelGroup,
                                                    singleSeismograms);
            String[] phaseName = pWave ? new String[] {"ttp"}
                    : new String[] {"tts"};
            Arrival[] pPhases = taup.calcTravelTimes(chan.my_site.my_station,
                                                     origin,
                                                     phaseName);
            MicroSecondDate firstP = new MicroSecondDate(origin.origin_time);
            firstP = firstP.add(new TimeInterval(pPhases[0].getTime(),
                                                 UnitImpl.SECOND));
            TimeInterval shift = recFunc.getShift();
            MemoryDataSetSeismogram[] predictedDSS = new MemoryDataSetSeismogram[2];
            for(int i = 0; i < ans.length; i++) {
                float[] predicted = ans[i].getPredicted();
                // ITR for radial
                // ITT for tangential
                String chanCode = (i == 0) ? "ITR" : "ITT";
                predictedDSS[i] = RecFunc.saveTimeSeries(predicted,
                                                         "receiver function "
                                                                 + singleSeismograms[0].channel_id.station_code,
                                                         chanCode,
                                                         firstP.subtract(shift),
                                                         singleSeismograms[0],
                                                         UnitImpl.DIMENSONLESS);
                cookieJar.put("recFunc_percentMatch_" + chanCode, ""
                        + ans[i].getPercentMatch());
            }
            while(true) {
                try {
                    cache.insert(origin,
                                 event.get_attributes(),
                                 config,
                                 channelGroup.getChannels(),
                                 singleSeismograms,
                                 predictedDSS[0].getCache()[0],
                                 ans[0].getPercentMatch(),
                                 ans[0].getBump(),
                                 predictedDSS[1].getCache()[0],
                                 ans[1].getPercentMatch(),
                                 ans[1].getBump(),
                                 sodconfig_id);
                    break;
                } catch(UNKNOWN e) {
                    System.err.println(ClockUtil.now()
                            + " Corba UNKNOWN while sending result to database, will not retry."
                            + e);
                    GlobalExceptionHandler.handle("Corba UNKNOWN while sending result to database, will not retry.",
                                                  e);
                    WaveformVectorResult result = new WaveformVectorResult(seismograms,
                                                                           new StringTreeLeaf(this,
                                                                                              false,
                                                                                              "UNKNOWN on cache.insert",
                                                                                              e));
                    return result;
                } catch(Throwable e) {
                    System.err.println(ClockUtil.now()
                            + " Problem while sending result to database, will retry in 1 minute..."
                            + e);
                    GlobalExceptionHandler.handle("Problem while sending result to database, will retry in 1 minute...",
                                                  e);
                    try {
                        Thread.sleep(60000);
                    } catch(InterruptedException interrupt) {
                        // ignore
                    }
                }
            }
            WaveformVectorResult result = new WaveformVectorResult(seismograms,
                                                                   new StringTreeLeaf(this,
                                                                                      true));
            return result;
        } catch(IncompatibleSeismograms e) {
            WaveformVectorResult result = new WaveformVectorResult(seismograms,
                                                                   new StringTreeLeaf(this,
                                                                                      false,
                                                                                      "Seismograms not compatble",
                                                                                      e));
            return result;
        }
    }

    public static float DEFAULT_GWIDTH = 2.5f;

    public static int DEFAULT_MAXBUMPS = 400;

    public static float DEFAULT_TOL = 0.001f;

    protected float gwidth;

    protected float tol;

    protected int maxBumps;

    protected boolean pWave = true;

    String dns = "edu/sc/seis";

    String serverName = "Ears";

    int sodconfig_id = -1;

    boolean overwrite = false;

    NSRecFuncCache cache;

    TauPUtil taup;

    RecFunc recFunc;
}