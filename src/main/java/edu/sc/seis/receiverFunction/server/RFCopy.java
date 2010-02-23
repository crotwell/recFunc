package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.fissuresUtil.database.DBUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;

public class RFCopy {

    public RFCopy() throws SQLException {
        File dbDir = new File("TransferDB");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        new org.hsqldb.jdbcDriver();
        Connection conn = DriverManager.getConnection("jdbc:hsqldb:file:TransferDB/TransferDB", "sa", "");
        if (!DBUtil.tableExists("exportedrf", conn)) {
            conn.createStatement().executeUpdate("create table exportedrf ( recfunc_id int )");
        }
        if (!DBUtil.tableExists("exportfail", conn)) {
            conn.createStatement().executeUpdate("create table exportfail ( recfunc_id int )");
        }
        inExported = conn.prepareStatement("select count(*) from exportedrf where recfunc_id = ?");
        inExportedFail = conn.prepareStatement("select count(*) from exportfail where recfunc_id = ?");
        insertFail = conn.prepareStatement("insert into exportfail values ( ? )");
        insertSuccess = conn.prepareStatement("insert into exportedrf values ( ? )");
    }

    public void copy(String serverDNS, String serverName) {
        List<StationImpl> staList = NetworkDB.getSingleton().getStationByCodes("SP", "DWDAN");
        NSRecFuncCache nsServer = new NSRecFuncCache(serverDNS, serverName, Initializer.getNS());
        copyStation(staList.get(0), nsServer);
    }

    public void copyStation(StationImpl station, NSRecFuncCache cache) {
        float GWIDTH = 2.5f;
        Map<Integer, Integer> knownSodConfigs = new HashMap<Integer, Integer>();
        List<ReceiverFunctionResult> results = RecFuncDB.getSingleton()
                .getSuccessful((NetworkAttrImpl)station.getNetworkAttr(), station.get_code(), GWIDTH);
        try {
            int j = 0;
            int l = results.size();
            for (ReceiverFunctionResult rf : results) {
                try {
                    // check already exported
                    inExported.setInt(1, rf.getDbid());
                    ResultSet rs = inExported.executeQuery();
                    rs.next();
                    if (rs.getInt(1) != 0) {
                        // skip items already in failures
                        continue;
                    }
                    // check failed
                    inExportedFail.setInt(1, rf.getDbid());
                    rs = inExportedFail.executeQuery();
                    rs.next();
                    if (rs.getInt(1) != 0) {
                        // skip items already in failures
                        continue;
                    }
                    ChannelId[] chanId = new ChannelId[3];
                    chanId[0] = rf.getChannelGroup().getChannel1().getId();
                    chanId[1] = rf.getChannelGroup().getChannel2().getId();
                    chanId[2] = rf.getChannelGroup().getChannel3().getId();
                    IterDeconConfig iterDeconConfig = new IterDeconConfig(rf.getGwidth(), rf.getMaxBumps(), rf.getTol());
                    if (cache.isCached(rf.getEvent().getPreferred(), chanId, iterDeconConfig)) {
                        // already in other db
                        insertSuccess.setInt(1, rf.getDbid());
                        insertSuccess.executeUpdate();
                        continue;
                    }
                    if (sanityCheck(rf)) {
                        if (!knownSodConfigs.containsKey(new Integer(rf.getSodConfig().getDbid()))) {
                            int id = cache.insertSodConfig(rf.getSodConfig().getConfig());
                            knownSodConfigs.put(rf.getSodConfig().getDbid(), id);
                        }
                        LocalSeismogram[] original = new LocalSeismogram[] {rf.getOriginal1(),
                                                                            rf.getOriginal2(),
                                                                            rf.getOriginal3()};
                        cache.insert(rf.getEvent().getPreferred(),
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
                                     knownSodConfigs.get(rf.getSodConfig().getDbid()));
                        insertSuccess.setInt(1, rf.getDbid());
                        insertSuccess.executeUpdate();
                        System.out.println("Result " + j + " of " + l + " for " + station);
                    } else {
                        insertFail.setInt(1, rf.getDbid());
                        insertFail.executeUpdate();
                        logger.warn("Sanity check fail for :"
                                + ChannelIdUtil.toString(rf.getChannelGroup().getChannel1().get_id()) + "  "
                                + rf.getEvent().getPreferred());
                    }
                } catch(Throwable t) {
                    logger.warn("failure for rfdbid=" + rf.getDbid() + " ", t);
                    insertFail.setInt(1, rf.getDbid());
                    insertFail.executeUpdate();
                }
            }
        } catch(Throwable t) {
            logger.fatal("Can't even get started...", t);
            System.exit(2);
        }
    }

    private static boolean sanityCheck(ReceiverFunctionResult r) {
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
        return true;
    }

    public static void main(String[] args) throws SQLException {
        Initializer.init(args);
        RFCopy copy = new RFCopy();
        copy.copy("edu/iris/dmc", "Ears");
    }

    PreparedStatement inExported;

    PreparedStatement inExportedFail;

    PreparedStatement insertFail;

    PreparedStatement insertSuccess;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RFCopy.class);
}
