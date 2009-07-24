package edu.sc.seis.receiverFunction.server;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.app.VelocityEngine;

import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.AbstractHibernateDB;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.hibernate.RFInsertion;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.hibernate.RejectedMaxima;
import edu.sc.seis.receiverFunction.web.AzimuthPlot;
import edu.sc.seis.receiverFunction.web.Overview;
import edu.sc.seis.receiverFunction.web.Start;
import edu.sc.seis.receiverFunction.web.Station;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.hibernate.SodDB;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class SumStackWorker implements Runnable {

    public SumStackWorker(float minPercentMatch,
                          boolean usePhaseWeight,
                          boolean doBootstrap,
                          int bootstrapIterations,
                          Properties props) throws Exception {
        this.minPercentMatch = minPercentMatch;
        this.usePhaseWeight = usePhaseWeight;
        this.doBootstrap = doBootstrap;
        this.bootstrapIterations = bootstrapIterations;
        velocity = new VelocityEngine();
        velocity.init(props);
        stationServlet = new Station();

        RevletContext.putDefault("visibleURL", props.getProperty(edu.sc.seis.rev.Start.REV_VISIBLEURL));
        Boolean debug = Boolean.valueOf(props.getProperty(edu.sc.seis.rev.Start.REV_DEBUG, "false"));
        RevletContext.putDefault(edu.sc.seis.rev.Start.CONTEXT_DEBUG, debug);
        RevletContext.putDefault(edu.sc.seis.rev.Start.CONTEXT_TITLE, props.getProperty(edu.sc.seis.rev.Start.REV_TITLE,
                                                                  "REV"));
        RevletContext.putDefault("revBase", props.getProperty(edu.sc.seis.rev.Start.REV_BASE, "/"));
        RevletContext.putDefault(edu.sc.seis.rev.Start.CONTEXT_STATICFILES,
                                 props.getProperty(edu.sc.seis.rev.Start.REV_BASE, "/"));
    }

    public void run() {
        boolean didProcess = false;
        while (keepGoing) {
            RFInsertion insertion = RecFuncDB.getSingleton().popInsertion(RF_AGE_TIME);
            if (insertion != null) {
                processNext(insertion);
                didProcess = true;
            } else if (didProcess) {
                try {
                    // caught up, redo summary pages
                    generateSummary();
                } catch(Exception e) {
                    GlobalExceptionHandler.handle("Unable to generate summary html", e);
                } finally {
                    didProcess = false;
                }
            } else {
                try {
                    logger.info(ClockUtil.now() + " No more insertions to process, sleeping for " + SLEEP_MINUTES
                            + " minutes.");
                    Thread.sleep(SLEEP_MINUTES * 60 * 1000); // 5 minutes
                } catch(InterruptedException e) {}
            }
        }
        System.out.println("Work finished");
    }
    
    void generateSummary() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(new URL("http://ears.seis.sc.edu/overview.html"));
        req.setParameter("filetype", "html");
        req.setParameter("gaussian", "" + DEFAULT_GAUSSIAN);
        Overview overview = new Overview();
        RevletContext context = overview.getContext(req, null);
        context.put("gaussian", "" + DEFAULT_GAUSSIAN);
        File outFile = new File(RecFuncDB.getSummaryDir(DEFAULT_GAUSSIAN), SUMMARY_HTML + ".new");
        BufferedWriter overviewOut = new BufferedWriter(new FileWriter(outFile));
        velocity.mergeTemplate("overview_html.vm", context, overviewOut);
        overviewOut.close();
        outFile.renameTo(new File(RecFuncDB.getSummaryDir(DEFAULT_GAUSSIAN), SUMMARY_HTML));
        logger.info("Done with summary html");
        req.setParameter("filetype", "html");
        outFile = new File(RecFuncDB.getSummaryDir(DEFAULT_GAUSSIAN), SUMMARY_CSV + ".new");
        overviewOut = new BufferedWriter(new FileWriter(outFile));
        velocity.mergeTemplate("overview_txt.vm", context, overviewOut);
        overviewOut.close();
        outFile.renameTo(new File(RecFuncDB.getSummaryDir(DEFAULT_GAUSSIAN), SUMMARY_CSV));
        logger.info("Done with summary csv");
    }

    void processNext(RFInsertion insertion) {
        List<StationImpl> staList = NetworkDB.getSingleton()
                .getStationForNet(insertion.getNet(), insertion.getStaCode());
        if (staList.size() == 0) {
            // can't find station?
            logger.error("Can't find station: "+insertion.getNet()+"."+ insertion.getStaCode());
            return;
        }
        StationImpl oneStationByCode = staList.get(0);
        logger.debug("Start on "+StationIdUtil.toStringNoDates(oneStationByCode));
        QuantityImpl smallestH = HKStack.getBestSmallestH(oneStationByCode,
                                                          HKStack.getDefaultSmallestH());
        List<ReceiverFunctionResult> individualHK = RecFuncDB.getSingleton()
                .getSuccessful(insertion.getNet(),
                               insertion.getStaCode(),
                               insertion.getGaussianWidth());
        List<StationImpl> stations = NetworkDB.getSingleton()
                .getStationForNet(insertion.getNet(), insertion.getStaCode());
        Set<RejectedMaxima> rejects = new HashSet<RejectedMaxima>();
        rejects.addAll(RecFuncDB.getSingleton()
                .getRejectedMaxima((NetworkAttrImpl)stations.get(0)
                                           .getNetworkAttr(),
                                   insertion.getStaCode()));
        logger.info("in sum for " + StationIdUtil.toStringNoDates(oneStationByCode) + " numeq=" + individualHK.size());
        System.out.println("in sum for " + StationIdUtil.toStringNoDates(oneStationByCode) + " numeq=" + individualHK.size());

        File stationDir = RecFuncDB.getStationDir(insertion.getNet(),
                                                  insertion.getStaCode(),
                                                  insertion.getGaussianWidth());
        SumHKStack oldSumStack = RecFuncDB.getSingleton().getSumStack(insertion.getNet(),  
                                                                      insertion.getStaCode(),
                                                                      insertion.getGaussianWidth());
        // if there is only 1 eq that matches, then we can't really do a stack
        if(individualHK.size() <= 1) {
            if(oldSumStack != null) {
                logger.info("Old sumstack in db but not enough for a stack, deleting...");
                RecFuncDB.getSession().delete(oldSumStack);
                if (stationDir.exists()) { 
                    String[] files = stationDir.list(); 
                    for (int i = 0; i < files.length; i++) {
                        new File(stationDir, files[i]).delete();
                    }
                }
                stationDir.delete();
                RecFuncDB.commit();
                return;
            }
        } else {
            SumHKStack sumStack = SumHKStack.calculateForPhase(individualHK,
                                                               smallestH,
                                                               minPercentMatch,
                                                               usePhaseWeight,
                                                               rejects,
                                                               doBootstrap,
                                                               bootstrapIterations,
                                                               "all");
            TimeOMatic.print("sum for " + insertion.getNet().get_code() + "."
                    + insertion.getStaCode());
            
            try {
                calcComplexity(sumStack);
                if(oldSumStack != null) {
                    System.out.println("Old sumstack in db, replacing...");
                    sumStack.setDbid(oldSumStack.getDbid());
                    RecFuncDB.getSession().evict(oldSumStack);
                    oldSumStack = null;
                }

                BufferedImage image = sumStack.createStackImage();
                File stackImageFile = new File(stationDir, SUM_HK_STACK_IMAGE);
                ImageIO.write(image, "png", stackImageFile);
                image = null;

                ArrayList<VelocityEvent> eventList = new ArrayList<VelocityEvent>();
                for(ReceiverFunctionResult result : individualHK) {
                    eventList.add(result.createVelocityEvent());
                }
                File azplot = AzimuthPlot.plot(new VelocityStation(oneStationByCode), eventList, stationDir, AZ_PLOT_IMAGE);

                RevletContext context = new RevletContext("station.vm",
                                                          Start.getDefaultContext());
                context.put("gaussian", ""+insertion.getGaussianWidth());
                List<ReceiverFunctionResult> losers = RecFuncDB.getSingleton().getUnsuccessful(insertion.getNet(),
                                                                                               insertion.getStaCode(),
                                                                                               insertion.getGaussianWidth());
                ArrayList<VelocityEvent> loserEventList = new ArrayList<VelocityEvent>();
                for(ReceiverFunctionResult result : losers) {
                    loserEventList.add(result.createVelocityEvent());
                }
                stationServlet.populateContext(context, new VelocityNetwork(insertion.getNet()),
                                               insertion.getStaCode(), sumStack, individualHK, losers);
                BufferedWriter stationOut = new BufferedWriter(new FileWriter(new File(stationDir, STATION_HTML)));
                velocity.mergeTemplate("station.vm", context, stationOut);
                stationOut.close();
                
                RecFuncDB.getSingleton().put(sumStack);
                RecFuncDB.commit();
            } catch(RuntimeException e) {
                GlobalExceptionHandler.handle(e);
                RecFuncDB.rollback();
                RecFuncDB.getSession().saveOrUpdate(insertion);
                RecFuncDB.commit();
                throw e;
            } catch(Exception e) {
                GlobalExceptionHandler.handle(e);
                RecFuncDB.rollback();
                RecFuncDB.getSession().saveOrUpdate(insertion);
                RecFuncDB.commit();
                throw new RuntimeException(e);
            }
        }
    }

    public static float calcComplexity(SumHKStack sumStack)
            throws TauModelException {
        Channel chan = sumStack.getIndividuals()
                .get(0)
                .getChannelGroup()
                .getChannel1();
        HKStack residual = sumStack.getResidual();
        float complex = sumStack.getResidualPower();
        float complex25 = sumStack.getResidualPower(.25f);
        float complex50 = sumStack.getResidualPower(.50f);
        float bestH = (float)sumStack.getSum()
                .getMaxValueH(sumStack.getSmallestH())
                .getValue(UnitImpl.KILOMETER);
        float bestHStdDev = (float)sumStack.getHStdDev()
                .getValue(UnitImpl.KILOMETER);
        float bestK = sumStack.getSum().getMaxValueK(sumStack.getSmallestH());
        float bestKStdDev = (float)sumStack.getKStdDev();
        float bestVal = sumStack.getSum().getMaxValue(sumStack.getSmallestH());
        float hkCorrelation = (float)sumStack.getMixedVariance();
        float nextH = (float)residual.getMaxValueH(sumStack.getSmallestH())
                .getValue(UnitImpl.KILOMETER);
        float nextK = residual.getMaxValueK(sumStack.getSmallestH());
        float nextVal = residual.getMaxValue(sumStack.getSmallestH());
        StationResult crust2Result = HKStack.getCrust2()
                .getStationResult(chan.getSite().getStation());
        float crust2diff = bestH
                - (float)crust2Result.getH().getValue(UnitImpl.KILOMETER);
        sumStack.setComplexityResult(new StackComplexityResult(complex,
                                                               complex25,
                                                               complex50,
                                                               bestH,
                                                               bestHStdDev,
                                                               bestK,
                                                               bestKStdDev,
                                                               bestVal,
                                                               hkCorrelation,
                                                               nextH,
                                                               nextK,
                                                               nextVal,
                                                               crust2diff));
        System.out.println(NetworkIdUtil.toStringNoDates(chan.get_id().network_id)
                + "." + chan.get_id().station_code + " Complexity: " + complex);
        return complex;
    }

    VelocityEngine velocity;
    
    Station stationServlet;
    
    float minPercentMatch;

    boolean usePhaseWeight;

    boolean doBootstrap;

    int bootstrapIterations;

    static boolean keepGoing = true;

    public static final TimeInterval RF_AGE_TIME = new TimeInterval(1,
                                                                    UnitImpl.HOUR);

    public static int SLEEP_MINUTES = 5;

    public static final String STATION_HTML = "station.html";
    
    public static final String SUMMARY_HTML = "summary.html";
    
    public static final String SUMMARY_CSV = "summary.csv";
    
    public static final String SUM_HK_STACK_IMAGE = "SumHKStackImage.png";
    
    public static final String AZ_PLOT_IMAGE = "eventAzPlot";
    
    public static final float DEFAULT_GAUSSIAN = 2.5f;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SumStackWorker.class);

    public static void main(String[] args) throws Exception {
        boolean testing = false;
        float minPercentMatch = 80f;
        boolean bootstrap = true;
        boolean usePhaseWeight = true;
        int keepWorkingSeconds = 15;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-test")) {
                testing = true;
                if (i+1<args.length && ! args[i+1].startsWith("-")) {
                    keepWorkingSeconds = Integer.parseInt(args[i+1]);
                }
                break;
            }
        }
        Properties props = StackSummary.loadProps(args);
        RecFuncDB.setDataLoc("../Data");
        ConnMgr.setURL("jdbc:postgresql:ears");
        ConnMgr.installDbProperties(props, new String[0]);
        logger.debug("before set up hibernate");
        synchronized(HibernateUtil.class) {
            HibernateUtil.setUpFromConnMgr(props, HibernateUtil.DEFAULT_EHCACHE_CONFIG);
            SodDB.configHibernate(HibernateUtil.getConfiguration());
            RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
        }
        AbstractHibernateDB.deploySchema();
        if (testing) {
        SumStackWorker worker = new SumStackWorker(minPercentMatch,
                                                   usePhaseWeight,
                                                   bootstrap,
                                                   //SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS,
                                                   10, 
                                                   props);
        
        // this is for testing, so thread will have time to start one sum but will not run forever
        NetworkAttrImpl spNet = NetworkDB.getSingleton().getNetworkByCode("SP").get(0);
        String staCode = "LGELG";
        float gaussian = 2.5f;
        RFInsertion insertion = RecFuncDB.getSingleton().getInsertion(spNet,
                                                                      staCode,
                                                                      gaussian);
        if(insertion == null) {
            insertion = new RFInsertion(spNet, staCode, gaussian);
        }
        insertion.setInsertTime(ClockUtil.wayPast().getTimestamp());
        RecFuncDB.getSession().saveOrUpdate(insertion);
        RecFuncDB.commit();
        //worker.processNext(insertion);
        Thread t = new Thread(worker);
        t.start();
        Thread.sleep(keepWorkingSeconds*1000);
        keepGoing = false;
        } else {
            SumStackWorker worker = new SumStackWorker(minPercentMatch,
                                                       usePhaseWeight,
                                                       bootstrap,
                                                       SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS,
                                                       props);
            worker.run();
        }
        logger.info("SumStackWorker main Done!");
    }
}
