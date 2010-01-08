package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.omg.CORBA.ORB;

import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfNetwork.NetworkDCOperations;
import edu.iris.Fissures.IfNetwork.NetworkNotFound;
import edu.iris.Fissures.model.AllVTFactory;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.sc.seis.fissuresUtil.cache.VestingNetworkDC;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;

/**
 * @author crotwell Created on Oct 5, 2004
 */
public class NetworkDBPopulate {

    public static void main(String[] args) throws NetworkNotFound,
            SQLException, IOException {
        Properties props = StackSummary.loadProps(args);
        org.omg.CORBA_2_3.ORB orb = (org.omg.CORBA_2_3.ORB)ORB.init(args, props);
        // register valuetype factories
        edu.iris.Fissures.model.AllVTFactory vt = new AllVTFactory();
        vt.register(orb);
        FissuresNamingService fisName = new FissuresNamingService(orb);
        fisName.setNameServiceCorbaLoc(props.getProperty(FissuresNamingService.CORBALOC_PROP));
        NetworkDCOperations netDC = new VestingNetworkDC("edu/iris/dmc",
                                                         "IRIS_NetworkDC",
                                                         fisName);
        NetworkAccess[] nets = netDC.a_finder().retrieve_by_code(args[0]);
        int dbid = NetworkDB.getSingleton().put((NetworkAttrImpl)nets[0].get_attributes());
        System.out.println("Done " + args[0] + " " + dbid);
    }
}