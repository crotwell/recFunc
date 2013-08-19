package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;

import edu.iris.Fissures.TimeRange;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.sod.hibernate.SodDB;
import edu.sc.seis.sod.subsetter.network.NetworkEffectiveTimeOverlap;


public class PriorResultCopy {

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        ConnMgr.setURL("jdbc:postgresql:ears");
        Properties props = System.getProperties();
        synchronized(HibernateUtil.class) {
            HibernateUtil.setUpFromConnMgr(props, HibernateUtil.DEFAULT_EHCACHE_CONFIG);
            SodDB.configHibernate(HibernateUtil.getConfiguration());
            RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
        }
        Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5741/ears",
                                                      "",
                                                      "");
        ResultSet rs = conn.createStatement().executeQuery("select * from stationresultref");
        List<StationResultRef> refList = new ArrayList<StationResultRef>();
        while (rs.next()) {
            StationResultRef ref = new StationResultRef(rs.getString(2), rs.getString(3), rs.getString(4));
            ref.setDbid(rs.getInt(1));
            refList.add(ref);
            //RecFuncDB.getSingleton().put(ref);
        }
        rs.close();
        for (StationResultRef ref : refList) {
            rs = conn.createStatement().executeQuery("select * from stationresult join network on (stationresult.net_id = network.net_id) join time on (network.begin_id = time.time_id) where ref_id = "+ref.getDbid());
            while (rs.next()) {
                List<StationImpl> netList = NetworkDB.getSingleton().getStationByCodes(rs.getString(8), rs.getString(2));
                MicroSecondDate netBegin = new MicroSecondDate(rs.getTimestamp(15));
                TimeRange tr = new TimeRange(netBegin.getFissuresTime(),
                                             netBegin.getFissuresTime());
                if (new NetworkEffectiveTimeOverlap(tr).overlaps(netList.get(0).getNetworkAttr().getEffectiveTime())) {
                StationResult sr = new StationResult((NetworkAttrImpl)netList.get(0).getNetworkAttr(), 
                                                     rs.getString(2), 
                                                     new QuantityImpl(rs.getDouble(3), UnitImpl.KILOMETER), 
                                                     rs.getFloat(4), 
                                                     new QuantityImpl(rs.getDouble(5), UnitImpl.KILOMETER_PER_SECOND), 
                                                     ref);
                //RecFuncDB.getSingleton().put(sr);
                } else {
                    logger.warn("Net zero doesn't overlap, maybe mulitple stations with same netcode.stacode?"+StationIdUtil.toString(netList.get(0))+" "+rs.getString(8)+" "+ rs.getString(2));
                }
            }
        }
        //RecFuncDB.commit();
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PriorResultCopy.class);
}
