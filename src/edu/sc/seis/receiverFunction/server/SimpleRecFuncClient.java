package edu.sc.seis.receiverFunction.server;

import edu.sc.seis.IfReceiverFunction.SodConfigNotFound;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;
import edu.sc.seis.fissuresUtil.simple.Initializer;


/**
 * @author crotwell
 * Created on Dec 16, 2004
 */
public class SimpleRecFuncClient {

    public static void main(String[] args) throws Exception {
        /* Initializes the corba orb, finds the naming service and other startup
         * tasks. See Initializer for the code in this method. */
        Initializer.init(args);
        

        String serverDNS;
        String serverName;

        // iris
        //serverDNS="edu/iris/dmc";
        //serverName = "IRIS_EventDC";

        // Berkeley
        //serverDNS="edu/berkeley/geo/quake";
        //serverName = "NCEDC_EventDC";
        // or
        //serverName = "NCSN_DataCenter";

        // South Carolina (SCEPP)
        serverDNS="edu/sc/seis";
        serverName="EARS";

        NSRecFuncCache cache;
        cache = new NSRecFuncCache(serverDNS, serverName, Initializer.getNS());
        
        cache.getSodConfig(1);
    }
}
