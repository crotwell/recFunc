package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import org.omg.CORBA.ORB;
import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfNetwork.NetworkDCOperations;
import edu.iris.Fissures.IfNetwork.NetworkNotFound;
import edu.iris.Fissures.model.AllVTFactory;
import edu.sc.seis.fissuresUtil.cache.BulletproofVestFactory;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;
import edu.sc.seis.fissuresUtil.simple.Initializer;

/**
 * @author crotwell Created on Oct 5, 2004
 */
public class NetworkDBPopulate {

    public static void main(String[] args) throws NetworkNotFound, SQLException, IOException {

        Properties props = RecFuncCacheStart.loadProps(args);
        org.omg.CORBA_2_3.ORB orb =
            (org.omg.CORBA_2_3.ORB)ORB.init(args, props);
        
        // register valuetype factories
        edu.iris.Fissures.model.AllVTFactory vt = new AllVTFactory();
        vt.register(orb);
        ConnMgr.setDB(ConnMgr.POSTGRES);
        FissuresNamingService fisName = new FissuresNamingService(orb);
        fisName.setNameServiceCorbaLoc(props.getProperty(FissuresNamingService.CORBALOC_PROP));
        NetworkDCOperations netDC = BulletproofVestFactory.vestNetworkDC("edu/iris/dmc",
                                                                         "IRIS_NetworkDC",
                                                                         fisName);
        NetworkAccess[] nets = netDC.a_finder().retrieve_by_code(args[0]);
        
        JDBCNetwork jdbcNet = new JDBCNetwork();
        int dbid = jdbcNet.put(nets[0].get_attributes());
        System.out.println("Done "+args[0]+" "+dbid);
    }
}