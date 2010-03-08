package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.TRANSIENT;

import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.DBUtil;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.QualityControl;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.sod.hibernate.SodDB;

public class RFCopy {

    public RFCopy(String serverDNS, String serverName) throws SQLException {
        nsServer = new NSRecFuncCache(serverDNS, serverName, Initializer.getNS());
        File dbDir = new File("TransferDB");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        new org.hsqldb.jdbcDriver();
        Connection conn = DriverManager.getConnection("jdbc:hsqldb:file:TransferDB/TransferDB", "sa", "");
        if (!DBUtil.tableExists("exportedrf", conn)) {
            conn.createStatement().executeUpdate("create table exportedrf ( recfunc_id int )");
            conn.createStatement().executeUpdate("create index exportedrf_idx on exportedrf ( recfunc_id )");
        }
        if (!DBUtil.tableExists("exportfail", conn)) {
            conn.createStatement().executeUpdate("create table exportfail ( recfunc_id int )");
            conn.createStatement().executeUpdate("create index exportfail_idx on exportfail ( recfunc_id )");
        }
        if (!DBUtil.tableExists("knownSodConfig", conn)) {
            conn.createStatement().executeUpdate("create table knownSodConfig ( sodconfig varchar, server_id int )");
        }
        inExported = conn.prepareStatement("select count(*) from exportedrf where recfunc_id = ?");
        inExportedFail = conn.prepareStatement("select count(*) from exportfail where recfunc_id = ?");
        insertFail = conn.prepareStatement("insert into exportfail values ( ? )");
        insertSuccess = conn.prepareStatement("insert into exportedrf values ( ? )");
        insertSodConfig = conn.prepareStatement("insert into knownSodConfig values ( ?, ? )");
        getSodConfigId = conn.prepareStatement("select sodconfig, server_id from knownSodConfig");
        ResultSet rs = getSodConfigId.executeQuery();
        while (rs.next()) {
            knownSodConfigs.put(rs.getString(1), rs.getInt(2));
        }
    }

    public void copy(String netCode) {
        List<NetworkAttrImpl> netList = NetworkDB.getSingleton().getNetworkByCode(netCode);
        for (NetworkAttrImpl net : netList) {
            List<StationImpl> staList = NetworkDB.getSingleton().getStationForNet(net);
            for (StationImpl sta : staList) {
                int numCopied = copyStation(sta);
                logger.info("   Copied " + numCopied + " at " + StationIdUtil.toStringNoDates(sta));
            }
            logger.info("Finish with " + NetworkIdUtil.toStringNoDates(net));
        }
        NetworkDB.rollback();
    }

    public int copyStation(StationImpl station) {
        try {
            int j = 0;
            float GWIDTH = 2.5f;
            List<ReceiverFunctionResult> resultsSuccess = RecFuncDB.getSingleton()
                    .getSuccessful((NetworkAttrImpl)station.getNetworkAttr(), station.get_code(), GWIDTH);
            int l = resultsSuccess.size();
            j += copy(resultsSuccess);
            List<ReceiverFunctionResult> resultsFail = RecFuncDB.getSingleton()
                    .getUnsuccessful((NetworkAttrImpl)station.getNetworkAttr(), station.get_code(), GWIDTH);
            l += resultsFail.size();
            j += copy(resultsFail);
            System.out.println("Copy " + j + " of " + l + " for " + StationIdUtil.toStringNoDates(station));
            return j;
        } catch(Throwable t) {
            logger.fatal("Can't even get started...", t);
            System.exit(2);
        }
        // never get here
        throw new RuntimeException("SHould not happen");
    }

    public int copy(List<ReceiverFunctionResult> results) throws SQLException {
        int j = 0;
        int l = results.size();
        for (ReceiverFunctionResult rf : results) {
            if (copy(rf)) {
                System.out.println("Result " + j + " of " + l + " for "
                        + StationIdUtil.toStringNoDates(rf.getChannelGroup().getStation()));
            }
            RecFuncDB.getSession().evict(rf);
            j++;
        }
        return j;
    }

    public boolean copy(ReceiverFunctionResult rf) throws SQLException {
        try {
            // check already exported
            inExported.setInt(1, rf.getDbid());
            ResultSet rs = inExported.executeQuery();
            rs.next();
            if (rs.getInt(1) != 0) {
                // skip items already in failures
                return false;
            }
            // check failed
            inExportedFail.setInt(1, rf.getDbid());
            rs = inExportedFail.executeQuery();
            rs.next();
            if (rs.getInt(1) != 0) {
                // skip items already in failures
                return false;
            }
            ChannelId[] chanId = new ChannelId[3];
            chanId[0] = rf.getChannelGroup().getChannel1().getId();
            chanId[1] = rf.getChannelGroup().getChannel2().getId();
            chanId[2] = rf.getChannelGroup().getChannel3().getId();
            IterDeconConfig iterDeconConfig = new IterDeconConfig(rf.getGwidth(), rf.getMaxBumps(), rf.getTol());
            if (nsServer.isCached(rf.getEvent().getPreferred(), chanId, iterDeconConfig)) {
                // already in other db
                insertSuccess.setInt(1, rf.getDbid());
                insertSuccess.executeUpdate();
                return false;
            }
            if (sanityCheck(rf)) {
                if (!knownSodConfigs.containsKey(rf.getSodConfig().getConfig())) {
                    System.out.println("Before insert sod config cached: " + rf.getSodConfig().getDbid());
                    int id = nsServer.insertSodConfig(rf.getSodConfig().getConfig());
                    knownSodConfigs.put(rf.getSodConfig().getConfig(), id);
                    insertSodConfig.setString(1, rf.getSodConfig().getConfig());
                    insertSodConfig.setInt(2, id);
                    insertSodConfig.executeUpdate();
                    System.out.println("After insert sod config cached: " + rf.getSodConfig().getDbid() + " " + id
                            + " " + knownSodConfigs.get(rf.getSodConfig().getConfig()));
                }
                LocalSeismogram[] original = new LocalSeismogram[] {rf.getOriginal1(),
                                                                    rf.getOriginal2(),
                                                                    rf.getOriginal3()};
                COMM_FAILURE lastCommFailure = null;
                for (int i = 0; i < 2; i++) {
                    try {
                        nsServer.insert(rf.getEvent().getPreferred(),
                                        rf.getEvent().get_attributes(),
                                        iterDeconConfig,
                                        rf.getChannelGroup().getChannels(),
                                        original,
                                        rf.getRadial(),
                                        rf.getRadialMatch(),
                                        rf.getRadialBump(),
                                        rf.getTransverse(),
                                        rf.getTransverseMatch(),
                                        rf.getTransverseBump(),
                                        knownSodConfigs.get(rf.getSodConfig().getConfig()));
                        insertSuccess.setInt(1, rf.getDbid());
                        insertSuccess.executeUpdate();
                        return true;
                    } catch(COMM_FAILURE ee) {
                        logger.warn("COMM_FAILURE, trying once more...", ee);
                        lastCommFailure = ee;
                        nsServer.reset();
                    }
                }
                throw lastCommFailure;
            } else {
                insertFail.setInt(1, rf.getDbid());
                insertFail.executeUpdate();
                logger.warn("Sanity check fail for :"
                        + ChannelIdUtil.toString(rf.getChannelGroup().getChannel1().get_id()) + "  "
                        + rf.getEvent().getPreferred());
                return false;
            }
        } catch(TRANSIENT e) {
            throw e;
        } catch(org.omg.CORBA.UNKNOWN t) {
            logger.warn("failure for rfdbid=" + rf.getDbid() + " at "
                    + StationIdUtil.toString(rf.getChannelGroup().getStation()), t);
            insertFail.setInt(1, rf.getDbid());
            insertFail.executeUpdate();
            return false;
        } catch(org.omg.CORBA.COMM_FAILURE t) {
            logger.warn("failure for rfdbid=" + rf.getDbid() + " at "
                    + StationIdUtil.toString(rf.getChannelGroup().getStation()), t);
            insertFail.setInt(1, rf.getDbid());
            insertFail.executeUpdate();
            return false;
        } catch(NoPreferredOrigin e) {
            // can't happen
            throw new RuntimeException(e);
        }
    }

    private static boolean sanityCheck(ReceiverFunctionResult r) {
        try {
            QualityControl qc = new QualityControl();
            float f = qc.transverseToRadial(r); // throws exceptions in some bad cases, so good check via side effect
            f = qc.radialPAmp(r);  // similar check
            if (r.getOriginal1() == null) {
                logger.error("original1 is null");
                return false;
            }
            if (r.getOriginal2() == null) {
                logger.error("original2 is null");
                return false;
            }
            if (r.getOriginal3() == null) {
                logger.error("original3 is null");
                return false;
            }
            if (r.getRadial() == null) {
                logger.error("radial is null");
                return false;
            }
            if (r.getTransverse() == null) {
                logger.error("transverse is null");
                return false;
            }
            float[] radialData = r.getRadial().get_as_floats();
            for (int i = 0; i < radialData.length; i++) {
                if (Float.isNaN(radialData[i])) {
                    logger.error("radial has NaN");
                    return false;
                }
            }
            radialData = null;
            float[] transData = r.getTransverse().get_as_floats();
            for (int i = 0; i < transData.length; i++) {
                if (Float.isNaN(transData[i])) {
                    logger.error("transverse has NaN");
                    return false;
                }
            }
            return true;
        } catch(Throwable e) {
            try {
                logger.error(ChannelIdUtil.toStringNoDates(r.getChannelGroup().getChannel1()) + " "
                        + r.getEvent().getPreferred().getTime(), e);
            } catch(NoPreferredOrigin e1) {
                logger.error("No preferred origin", e1);
            }
            return false;
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        Properties props = Initializer.loadProperties(args);
        PropertyConfigurator.configure(props);
        RecFuncDB.setDataLoc(props.getProperty("cormorant.servers.ears.dataloc", RecFuncDB.getDataLoc()));
        ConnMgr.setURL(props.getProperty("fissuresUtil.database.url"));
        HibernateUtil.setUpFromConnMgr(props, HibernateUtil.DEFAULT_EHCACHE_CONFIG);
        synchronized(HibernateUtil.class) {
            SodDB.configHibernate(HibernateUtil.getConfiguration());
            RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
        }
        RFCopy copy = new RFCopy("edu/iris/dmc", "Ears");
        // copy.copy("XA");
        // copy.copy("XM");
        // copy.copy("AZ");
        //
        // copy.copy("CD");
        // copy.copy("G");
        // copy.copy("CZ");
        // copy.copy("NL");
        // copy.copy("TS");
        // copy.copy("CD");
        // copy.copy("GT");
        // copy.copy("PS");
        // copy.copy("UW");
        // copy.copy("CN");
        // copy.copy("II");
        // copy.copy("IU");
        // copy.copy("IC");
        // copy.copy("US");
        // copy.copy("BK");
        // copy.copy("CI");
        // copy.copy("TA");
        // copy.copy("MN");
        List<NetworkAttrImpl> netList = NetworkDB.getSingleton().getAllNetworks();
        HashSet<String> allCodes = new HashSet<String>();
        for (NetworkAttrImpl net : netList) {
            allCodes.add(net.get_code());
        }
        for (String netCode : allCodes) {
            copy.copy(netCode);
        }
    }

    Map<String, Integer> knownSodConfigs = new HashMap<String, Integer>();

    PreparedStatement inExported;

    PreparedStatement inExportedFail;

    PreparedStatement insertFail;

    PreparedStatement insertSuccess;

    PreparedStatement insertSodConfig;

    PreparedStatement getSodConfigId;

    NSRecFuncCache nsServer;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RFCopy.class);
}
