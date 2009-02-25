package edu.sc.seis.receiverFunction.hibernate;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.cfg.Configuration;
import org.omg.CORBA.UNKNOWN;

import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.IfParameterMgr.ParameterRef;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.freq.CmplxArray2D;
import edu.sc.seis.fissuresUtil.hibernate.AbstractHibernateDB;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.receiverFunction.AzimuthSumHKStack;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.QCUser;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.UserReceiverFunctionQC;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.sod.hibernate.SodDB;
import edu.sc.seis.sod.status.EventFormatter;

public class RecFuncDB extends AbstractHibernateDB {

    public int put(ReceiverFunctionResult result) {
        if(result.getQc().isKeep()) {
            RFInsertion insertion = getInsertion((NetworkAttrImpl)result.getChannelGroup()
                                                         .getNetworkAttr(),
                                                 result.getChannelGroup()
                                                         .getStation()
                                                         .get_code(),
                                                 result.getGwidth());
            if(insertion == null) {
                insertion = new RFInsertion((NetworkAttrImpl)result.getChannelGroup()
                                                    .getNetworkAttr(),
                                            result.getChannelGroup()
                                                    .getStation()
                                                    .get_code(),
                                            result.getGwidth(),
                                            ClockUtil.now().getTimestamp());
            }
            getSession().saveOrUpdate(insertion);
        }
        if(result.getHKstack() != null) {
            File dir = getDir(result.getEvent(),
                              result.getChannelGroup()
                              .getChannel1(),
                      result.getGwidth());
            File stackFile = new File(dir,
                                      STACK_FILENAME);
            writeHKStackData(result.getHKstack(), stackFile);
            result.getHKstack().setStackFile(stackFile.getPath());
            
            File analyticFile = new File(dir, ANALYTIC_FILENAME+"Ps.xy");
            writeAnalyticData(result.getHKstack().getAnalyticPs(), analyticFile);
            
            analyticFile = new File(dir, ANALYTIC_FILENAME+"PpPs.xy");
            writeAnalyticData(result.getHKstack().getAnalyticPpPs(), analyticFile);
            
            analyticFile = new File(dir, ANALYTIC_FILENAME+"PsPs.xy");
            writeAnalyticData(result.getHKstack().getAnalyticPsPs(), analyticFile);
        }
        return ((Integer)getSession().save(result)).intValue();
    }

    protected void writeHKStackData(HKStack stack, File outFile) {
        try {
            float[][] stackData = stack.getStack();
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
            for(int i = 0; i < stackData.length; i++) {
                for(int j = 0; j < stackData[0].length; j++) {
                    out.writeFloat(stackData[i][j]);
                }
            }
            out.close();
        } catch(Exception e) {
            throw new RuntimeException("Unable to save stack to file: "
                    + outFile, e);
        }
    }
    
    protected void writeAnalyticData(CmplxArray2D analyticData, File outFile) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
            for(int i = 0; i < analyticData.getXLength(); i++) {
                for(int j = 0; j < analyticData.getYLength(); j++) {
                    out.writeFloat(analyticData.getReal(i, j));
                    out.writeFloat(analyticData.getImag(i, j));
                }
            }
            out.close();
        } catch(Exception e) {
            throw new RuntimeException("Unable to save stack to file: "
                    + outFile, e);
        }
    }

    public static float[][] loadHKStackFile(HKStack stack) {
        try {
            if (stack.getStackFile() == null || stack.getStackFile().length() == 0) {
                throw new RuntimeException("stack does not have a stackFile set");
            }
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(getDataDir(), stack.getStackFile()))));
            float[][] loadStack = HKStack.createArray(stack.getNumH(),
                                                      stack.getNumK());
            for(int i = 0; i < loadStack.length; i++) {
                for(int j = 0; j < loadStack[0].length; j++) {
                    loadStack[i][j] = in.readFloat();
                }
            }
            in.close();
            return loadStack;
        } catch(Exception e) {
            throw new RuntimeException("Unable to load stack from file: "
                    + stack.getStackFile(), e);
        }
    }
    
    public static CmplxArray2D loadAnalyticFile(HKStack stack, String phase) {
        try {
            if (stack.getStackFile() == null || stack.getStackFile().length() == 0) {
                throw new RuntimeException("stack does not have a stackFile set");
            }
            File dir = new File(stack.getStackFile()).getParentFile();
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(dir, RecFuncDB.ANALYTIC_FILENAME+phase+".xy"))));
            CmplxArray2D out = new CmplxArray2D(stack.getNumH(), stack.getNumK());
            for(int i = 0; i < stack.getNumH(); i++) {
                for(int j = 0; j < stack.getNumK(); j++) {
                    out.setReal(i, j, in.readFloat());
                    out.setImag(i, j, in.readFloat());
                }
            }
            in.close();
            return out;
        } catch(Exception e) {
            throw new RuntimeException("Unable to load analytic for "+phase+" from file: "
                    + stack.getStackFile(), e);
        }
    }

    public ReceiverFunctionResult getReceiverFunctionResult(int dbid) {
        return (ReceiverFunctionResult)getSession().get(ReceiverFunctionResult.class,
                                                        new Integer(dbid));
    }

    private static RecFuncDB singleton;

    public static RecFuncDB getSingleton() {
        if(singleton == null) {
            singleton = new RecFuncDB();
        }
        return singleton;
    }

    public List<ReceiverFunctionResult> getStationsByEvent(CacheEvent cacheEvent,
                                                           float gaussian) {
        Query q = getSession().createQuery("from "
                + ReceiverFunctionResult.class.getName()
                + " r where r.event = :eventxxx "
                + " and r.gwidth = :gauss and r.qc.keep = true)");
        q.setEntity("eventxxx", cacheEvent);
        q.setFloat("gauss", gaussian);
        return q.list();
    }

    public ReceiverFunctionResult getRecFuncResult(CacheEvent cacheEvent,
                                                   ChannelGroup chanGroup,
                                                   float gwidth) {
        Query q = getSession().createQuery("from "
                + ReceiverFunctionResult.class.getName()
                + " where event = :event and channelGroup = :cg and gwidth = :gwidth");
        q.setEntity("event", cacheEvent);
        q.setEntity("cg", chanGroup);
        q.setFloat("gwidth", gwidth);
        q.setMaxResults(1);
        List<ReceiverFunctionResult> l = q.list();
        if(l.size() != 0) {
            return l.get(0);
        }
        return null;
    }

    public IterDeconConfig[] getResults(CacheEvent cacheEvent,
                                        ChannelGroup chanGroup) {
        Query q = getSession().createQuery("select gwidth, maxBumps, tol from "
                + ReceiverFunctionResult.class.getName()
                + " where event = :event and channelGroup = :cg");
        q.setEntity("event", cacheEvent);
        q.setEntity("cg", chanGroup);
        List<Object[]> r = q.list();
        List<IterDeconConfig> out = new ArrayList<IterDeconConfig>();
        for(Object[] object : r) {
            out.add(new IterDeconConfig((Float)object[0],
                                        (Integer)object[1],
                                        (Float)object[2]));
        }
        return out.toArray(new IterDeconConfig[0]);
    }

    public List<ReceiverFunctionResult> getSuccessful(NetworkAttrImpl networkAttr,
                                                      String staCode,
                                                      float gaussian) {
        Query q = getSession().createQuery("from "
                + ReceiverFunctionResult.class.getName()
                + " where channelGroup.channel1.site.station.id.station_code = :sta "
                + " and channelGroup.channel1.site.station.networkAttr = :networkAttr "
                + " and gwidth = :gauss and qc.keep = true");
        q.setString("sta", staCode);
        q.setEntity("networkAttr", networkAttr);
        q.setFloat("gauss", gaussian);
        return q.list();
    }

    public int countSuccessful(NetworkAttrImpl networkAttr,
                               String staCode,
                               float gaussian) {
        Query q = getSession().createQuery("select count(*) from "
                + ReceiverFunctionResult.class.getName()
                + " where channelGroup.channel1.site.station.id.station_code = :sta "
                + " and channelGroup.channel1.site.station.networkAttr = :networkAttr "
                + " and gwidth = :gauss and qc.keep = true");
        q.setString("sta", staCode);
        q.setEntity("networkAttr", networkAttr);
        q.setFloat("gauss", gaussian);
        return ((Long)q.uniqueResult()).intValue();
    }

    public List<ReceiverFunctionResult> getUnsuccessful(NetworkAttrImpl networkAttr,
                                                        String staCode,
                                                        float gaussian) {
        Query q = getSession().createQuery("from "
                + ReceiverFunctionResult.class.getName()
                + " where channelGroup.channel1.site.station.id.station_code = :sta "
                + " and channelGroup.channel1.site.station.networkAttr = :networkAttr "
                + " and gwidth = :gauss and qc.keep = false");
        q.setString("sta", staCode);
        q.setEntity("networkAttr", networkAttr);
        q.setFloat("gauss", gaussian);
        return q.list();
    }

    public int countUnsuccessful(NetworkAttrImpl networkAttr,
                                 String staCode,
                                 float gaussian) {
        Query q = getSession().createQuery("select count(*) from "
                + ReceiverFunctionResult.class.getName()
                + " where channelGroup.channel1.site.station.id.station_code = :sta "
                + " and channelGroup.channel1.site.station.networkAttr = :networkAttr "
                + " and gwidth = :gauss and qc.keep = false");
        q.setString("sta", staCode);
        q.setEntity("networkAttr", networkAttr);
        q.setFloat("gauss", gaussian);
        return ((Long)q.uniqueResult()).intValue();
    }

    public int put(RejectedMaxima reject) {
        return ((Integer)getSession().save(reject)).intValue();
    }

    public List<RejectedMaxima> getRejectedMaxima(NetworkAttrImpl net,
                                                  String staCode) {
        Query q = getSession().createQuery("from "
                + RejectedMaxima.class.getName()
                + " where net = :net and staCode = :staCode");
        q.setEntity("net", net);
        q.setString("staCode", staCode);
        return q.list();
    }

    public int put(StationResult prior) {
        return ((Integer)getSession().save(prior)).intValue();
    }

    public List<StationResult> getPriorResults(String name) {
        Query q = getSession().createQuery("from "
                + StationResult.class.getName() + " where name = :name");
        q.setString("name", name);
        return q.list();
    }

    public List<StationResult> getPriorResults(NetworkAttrImpl net,
                                               String staCode) {
        Query q = getSession().createQuery("from "
                + StationResult.class.getName()
                + " where net = :net and staCode = :staCode");
        q.setEntity("net", net);
        q.setString("staCode", staCode);
        return q.list();
    }

    public StationResultRef getPriorResultsRef(String name) {
        Query q = getSession().createQuery("select ref from "
                + StationResult.class.getName() + " where name = :name");
        q.setString("name", name);
        q.setMaxResults(1);
        List<StationResultRef> l = q.list();
        if(l.size() != 0) {
            return l.get(0);
        }
        return null;
    }

    public List<StationResultRef> getAllPriorResultsRef() {
        Query q = getSession().createQuery("select distinct ref from "
                + StationResult.class.getName());
        return q.list();
    }

    public RFInsertion getInsertion(NetworkAttrImpl net,
                                    String staCode,
                                    float gaussianWidth) {
        Query q = getSession().createQuery("from "
                + RFInsertion.class.getName()
                + " where net = :net and staCode = :staCode and gaussianWidth = :gaussianWidth");
        q.setEntity("net", net);
        q.setString("staCode", staCode);
        q.setFloat("gaussianWidth", gaussianWidth);
        q.setMaxResults(1);
        List<RFInsertion> l = q.list();
        if(l.size() != 0) {
            return l.get(0);
        }
        return null;
    }

    public RFInsertion getOlderInsertion(TimeInterval age) {
        Query q = getSession().createQuery("from "
                + RFInsertion.class.getName()
                + " where insertTime < :oldTime  order by insertTime");
        q.setTimestamp("oldTime", ClockUtil.now().subtract(age).getTimestamp());
        q.setMaxResults(1);
        Iterator<RFInsertion> it = q.iterate();
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    public void put(SumHKStack sum) {
        File stackFile = new File(getStationDir(sum.getNet(),
                                                sum.getStaCode(),
                                                sum.getGaussianWidth()),
                                  SUM_STACK_FILENAME);
        writeHKStackData(sum.getSum(), stackFile);
        sum.getSum().setStackFile(stackFile.getPath().substring(getDataLoc().length()+1)); // +1 to get the /
        getSession().saveOrUpdate(sum);
    }

    public int putAzimuthSummary(AzimuthSumHKStack sum) {
        File stackFile = new File(getStationDir(sum.getNet(),
                                                sum.getStaCode(),
                                                sum.getGaussianWidth()),
                                  "AZ_"+sum.getAzimuthCenter()+"_"+sum.getAzimuthWidth()+"_"+SUM_STACK_FILENAME);
        writeHKStackData(sum.getSum(), stackFile);
        sum.getSum().setStackFile(stackFile.getPath());
        return ((Integer)getSession().save(sum)).intValue();
    }

    public AzimuthSumHKStack getAzSumStack(NetworkAttrImpl net,
                                           String staCode,
                                           float gaussianWidth,
                                           float azCenter,
                                           float azWidth) {
        Query q = getSession().createQuery("from "
                + AzimuthSumHKStack.class.getName()
                + " where net = :net and staCode = :staCode and gaussianWidth = :gaussianWidth");
        q.setEntity("net", net);
        q.setString("staCode", staCode);
        q.setFloat("gaussianWidth", gaussianWidth);
        q.setFloat("azimuthWidth", azWidth);
        q.setFloat("azimuthCenter", azCenter);
        q.setMaxResults(1);
        List<AzimuthSumHKStack> result = q.list();
        if(result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    public SumHKStack getSumStack(NetworkAttrImpl net,
                                  String staCode,
                                  float gaussianWidth) {
        Query q = getSession().createQuery("from "
                + SumHKStack.class.getName()
                + " where net = :n and stacode = :stacode and gaussianWidth = :gw");
        q.setEntity("n", net);
        q.setString("stacode", staCode);
        q.setFloat("gw", gaussianWidth);
        q.setMaxResults(1);
        Iterator<SumHKStack> result = q.iterate();
        if(result.hasNext() ) {
            logger.debug("found sum stack");
            return result.next();
        }
        logger.debug("Didn't find sum stack");
        return null;
    }

    public int put(UserReceiverFunctionQC qc) {
        return ((Integer)getSession().save(qc)).intValue();
    }

    public QCUser getQCUser(String hash) {
        Query q = getSession().createQuery("from "+QCUser.class.getName()+" where passwordHash = :hash");
        q.setString("hash", hash);
        q.setMaxResults(1);
        List ans = q.list();
        if (ans.size() == 0) {
            return null;
        }
        return (QCUser)ans.get(0);
    }

    public List<SumHKStack> getAllSumStack(float gaussianWidth) {
        Query q = getSession().createQuery("from " + SumHKStack.class.getName()
                + " where gaussianWidth = :gaussianWidth");
        q.setFloat("gaussianWidth", gaussianWidth);
        return q.list();
    }
    
    public static File getDataDir() {
        if(dataDir == null) {
            dataDir = new File(getDataLoc());
            dataDir.mkdirs();
        }
        return dataDir;
    }

    public static File getDir(CacheEvent cacheEvent,
                              Channel chan,
                              float gaussianWidth) {
        
        File gaussDir = new File(getDataDir(), "gauss_" + gaussianWidth);
        File eventDir = new File(gaussDir, eventFormatter.getResult(cacheEvent));
        File netDir = new File(eventDir, chan.get_id().network_id.network_code);
        File stationDir = new File(netDir, chan.get_id().station_code);
        boolean dirsCreated = stationDir.exists();
        if(!dirsCreated) {
            dirsCreated = stationDir.mkdirs();
        }
        if(!dirsCreated) {
            logger.debug("initial mkdirs returned false: " + dirsCreated + "  "
                    + stationDir.exists());
            // try once more just for kicks...
            for(int i = 0; i < 10 && !dirsCreated; i++) {
                try {
                    Thread.sleep(1000 * i);
                } catch(InterruptedException e) {}
                dirsCreated = stationDir.mkdirs();
                logger.debug(i + " mkdirs returned false: " + dirsCreated
                        + "  " + stationDir.exists());
            }
            if(!dirsCreated) {
                File tmpDir = stationDir;
                while(tmpDir.getParentFile() != null
                        && !tmpDir.getParentFile().exists()) {
                    tmpDir = tmpDir.getParentFile();
                }
                logger.error("mkdirs seemed to fail on this directory: "
                        + tmpDir);
                throw new UNKNOWN("Unable to create directory");
            }
        }
        return stationDir;
    }

    public static File getStationDir(NetworkAttr net,
                                     String staCode,
                                     float gaussianWidth) {
        File summaryDir = new File(getDataDir(), "Summary");
        File gaussDir = new File(summaryDir, "gauss_" + gaussianWidth);
        File netDir = new File(gaussDir, NetworkIdUtil.toStringNoDates(net));
        File stationDir = new File(netDir, staCode);
        boolean dirsCreated = stationDir.exists();
        if(!dirsCreated) {
            dirsCreated = stationDir.mkdirs();
            if(!dirsCreated) {
                throw new RuntimeException("Unable to mkdirs on " + stationDir);
            }
        }
        return stationDir;
    }

    public static void setDataLoc(String loc) {
        DATA_LOC = loc;
    }

    public static String getDataLoc() {
        return DATA_LOC;
    }

    public static void addToParms(Origin o, float itr_match, int recFunc_id) {
        ParameterRef[] parms = o.getParmIds();
        ParameterRef[] newParms = new ParameterRef[parms.length + 2];
        System.arraycopy(parms, 0, newParms, 0, parms.length);
        newParms[newParms.length - 2] = new ParameterRef("itr_match", ""
                + itr_match);
        newParms[newParms.length - 1] = new ParameterRef("recFunc_id", ""
                + recFunc_id);
        o.setParmIds(newParms);
    }

    public static final String STACK_FILENAME = "HKStack.xy";
    public static final String ANALYTIC_FILENAME = "Analytic_";

    public static final String SUM_STACK_FILENAME = "SumHKStack.xy";
    
    public static File dataDir = null;

    protected static String DATA_LOC = "../Data";

    public static EventFormatter eventFormatter = EventFormatter.makeFilizer();

    static String configFile = "edu/sc/seis/receiverFunction/hibernate/RecFunc.hbm.xml";

    public static void configHibernate(Configuration config) {
        logger.debug("adding to HibernateUtil   " + configFile);
        config.addResource(configFile, RecFuncDB.class.getClassLoader());
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncDB.class);
}
