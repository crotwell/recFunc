package edu.sc.seis.receiverFunction.hibernate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;

import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfParameterMgr.ParameterRef;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.hibernate.AbstractHibernateDB;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.receiverFunction.server.CachedResultPlusDbId;
import edu.sc.seis.receiverFunction.web.Start;

public class RecFuncDB extends AbstractHibernateDB {

    public int put(ReceiverFunctionResult result) {
        if(result.getRadialMatch() > Start.getDefaultMinPercentMatch()) {
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
        return ((Integer)getSession().save(result)).intValue();
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
                                                           float gaussian,
                                                           float percentMatch) {
        Query q = getSession().createQuery("from "
                + ReceiverFunctionResult.class.getName()
                + " where event = :event "
                + " and gwidth = :gauss and (( radialMatch > :match and qc == null ) or qc.keep = true)");
        q.setEntity("event", cacheEvent);
        q.setFloat("gauss", gaussian);
        q.setFloat("match", percentMatch);
        return q.list();
    }

    public ReceiverFunctionResult getRecFuncResult(CacheEvent cacheEvent,
                                                   ChannelGroup chanGroup,
                                                   IterDeconConfig config) {
        Query q = getSession().createQuery("from "
                + ReceiverFunctionResult.class.getName()
                + " where event = :event and channelGroup = :cg and gwidth = :gwidth and maxBumps = :maxBumps and tol = :tol");
        q.setEntity("event", cacheEvent);
        q.setEntity("cg", chanGroup);
        q.setFloat("gwidth", config.gwidth);
        q.setInteger("maxBumps", config.maxBumps);
        q.setFloat("tol", config.tol);
        return (ReceiverFunctionResult)q.uniqueResult();
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
                                                      float gaussian,
                                                      float percentMatch) {
        Query q = getSession().createQuery("from "
                + ReceiverFunctionResult.class.getName()
                + " where channelGroup.channel1.station_code = :sta and channelGroup.channel1.site.station.networkAttr = :networkAttr "
                + " and gwidth = :gauss and (( radialMatch >= :match and qc == null ) or qc.keep = true)");
        q.setString("sta", staCode);
        q.setEntity("networkAttr", networkAttr);
        q.setFloat("gauss", gaussian);
        q.setFloat("match", percentMatch);
        return q.list();
    }

    public int countSuccessful(NetworkAttrImpl networkAttr,
                                                      String staCode,
                                                      float gaussian,
                                                      float percentMatch) {
        Query q = getSession().createQuery("select count(*) from "
                + ReceiverFunctionResult.class.getName()
                + " where channelGroup.channel1.station_code = :sta and channelGroup.channel1.site.station.networkAttr = :networkAttr "
                + " and gwidth = :gauss and (( radialMatch >= :match and qc == null ) or qc.keep = true)");
        q.setString("sta", staCode);
        q.setEntity("networkAttr", networkAttr);
        q.setFloat("gauss", gaussian);
        q.setFloat("match", percentMatch);
        return (Integer)q.uniqueResult();
    }

    public List<ReceiverFunctionResult> getUnsuccessful(NetworkAttrImpl networkAttr,
                                                      String staCode,
                                                      float gaussian,
                                                      float percentMatch) {
        Query q = getSession().createQuery("from "
                + ReceiverFunctionResult.class.getName()
                + " where channelGroup.channel1.station_code = :sta and channelGroup.channel1.site.station.networkAttr = :networkAttr "
                + " and gwidth = :gauss and ( radialMatch < :match and (qc == null  or qc.keep = false))");
        q.setString("sta", staCode);
        q.setEntity("networkAttr", networkAttr);
        q.setFloat("gauss", gaussian);
        q.setFloat("match", percentMatch);
        return q.list();
    }

    public int countUnsuccessful(NetworkAttrImpl networkAttr,
                                                      String staCode,
                                                      float gaussian,
                                                      float percentMatch) {
        Query q = getSession().createQuery("select count(*) from "
                + ReceiverFunctionResult.class.getName()
                + " where channelGroup.channel1.station_code = :sta and channelGroup.channel1.site.station.networkAttr = :networkAttr "
                + " and gwidth = :gauss and ( radialMatch < :match and (qc == null  or qc.keep = false))");
        q.setString("sta", staCode);
        q.setEntity("networkAttr", networkAttr);
        q.setFloat("gauss", gaussian);
        q.setFloat("match", percentMatch);
        return (Integer)q.uniqueResult();
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
                + " where net = :net and stationCode = :staCode");
        q.setEntity("net", net);
        q.setString("staCode", staCode);
        return q.list();
    }

    public StationResultRef getPriorResultsRef(String name) {
        Query q = getSession().createQuery("select ref from "
                + StationResult.class.getName() + " where name = :name");
        q.setString("name", name);
        List l = q.list();
        if(l.size() != 0) {
            return (StationResultRef)l.get(0);
        }
        return null;
    }
    
    public List<StationResultRef> getAllPriorResultsRef() {
        Query q = getSession().createQuery("select distinct ref from "
                + StationResult.class.getName() );
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
        return (RFInsertion)q.uniqueResult();
    }

    public List<RFInsertion> getOlderInsertions(TimeInterval age,
                                                float gaussianWidth) {
        Query q = getSession().createQuery("from "
                + RFInsertion.class.getName()
                + " where now - insertTime > :age and gaussianWidth = :gaussianWidth");
        q.setDouble("age", age.getValue(UnitImpl.SECOND));
        q.setFloat("gaussianWidth", gaussianWidth);
        return q.list();
    }

    public int put(SumHKStack sum) {
        return ((Integer)getSession().save(sum)).intValue();
    }

    public SumHKStack getSumStack(NetworkAttrImpl net,
                                  String staCode,
                                  float gaussianWidth) {
        Query q = getSession().createQuery("from "
                + SumHKStack.class.getName()
                + " where net = :net and staCode = :staCode and gaussianWidth = :gaussianWidth");
        q.setEntity("net", net);
        q.setString("staCode", staCode);
        q.setFloat("gaussianWidth", gaussianWidth);
        return (SumHKStack)q.uniqueResult();
    }

    public List<SumHKStack> getAllSumStack(float gaussianWidth) {
        Query q = getSession().createQuery("from " + SumHKStack.class.getName()
                + " where gaussianWidth = :gaussianWidth");
        q.setFloat("gaussianWidth", gaussianWidth);
        return q.list();
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
}