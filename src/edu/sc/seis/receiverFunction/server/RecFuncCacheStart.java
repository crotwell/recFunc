/*
 * Created on Sep 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.sc.seis.receiverFunction.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.omg.CORBA.ORB;
import edu.iris.Fissures.model.AllVTFactory;
import edu.sc.seis.IfReceiverFunction.RecFuncCache;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;


/**
 * @author crotwell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RecFuncCacheStart {
    
    public static void main(String[] args) {
        Properties props = loadProps(args);
        org.omg.CORBA_2_3.ORB orb =
            (org.omg.CORBA_2_3.ORB)ORB.init(args, props);
        
        // register valuetype factories
        edu.iris.Fissures.model.AllVTFactory vt = new AllVTFactory();
        vt.register(orb);
        
        RecFuncCacheImpl impl = new RecFuncCacheImpl();
        RecFuncCache server = impl._this(orb);
        
         serviceName = System.getProperty("recFunc.cacheServer.serverName", serviceName);
         serviceDNS = System.getProperty("recFunc.cacheServer.serverDNS", serviceDNS);
        
        FissuresNamingService fissuresNamingService = new FissuresNamingService(orb);
        
        String addNS = System.getProperty("recFunc.cacheServer.additionalNameService");
        if (addNS != null) {
            fissuresNamingService.addOtherNameServiceCorbaLoc(addNS);
        }
        
        try {
            fissuresNamingService.rebind(serviceDNS, serviceName, server, interfacename);
            
            logger.info("Bound to Name Service");
            //
            // Run implementation
            //
            org.omg.PortableServer.POA rootPOA =
                org.omg.PortableServer.POAHelper.narrow(
                                                        orb.resolve_initial_references("RootPOA"));
            org.omg.PortableServer.POAManager manager =
                rootPOA.the_POAManager();
            manager.activate();
            orb.run();
        }catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }
    
    private static Properties loadProps(String[] args) {
        
        Properties props = System.getProperties();
        
        // get some defaults
        String propFilename=
            "rfcache.prop";
        String defaultsFilename=
            "edu/sc/seis/receiverFunction/server/"+propFilename;
        
        for (int i=0; i<args.length-1; i++) {
            if (args[i].equals("-props")) {
                // override with values in local directory,
                // but still load defaults with original name
                propFilename = args[i+1];
            }
        }
        
        try {
            props.load((RecFuncCacheStart.class).getClassLoader().getResourceAsStream( defaultsFilename ));
        } catch (IOException e) {
            System.err.println("Could not load defaults. "+e);
        }
        try {
            FileInputStream in = new FileInputStream(propFilename);
            props.load(in);
            in.close();
        } catch (FileNotFoundException f) {
            System.err.println(" file missing "+f+" using defaults");
        } catch (IOException f) {
            System.err.println(f.toString()+" using defaults");
        }
        
        // configure logging from properties...
        PropertyConfigurator.configure(props);
        logger.info("Logging configured");
        return props;
    }
    
    static String serviceName = "Ears";
    static String serviceNameAlt = null;
    static String serviceDNS = "edu/sc/seis";
    static String interfacename = "IfReceiverFunction";
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncCacheStart.class);
    
}
