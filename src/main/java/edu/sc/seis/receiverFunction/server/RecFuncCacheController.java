package edu.sc.seis.receiverFunction.server;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.Servant;

import edu.sc.seis.cormorant.AbstractController;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;

/**
 * @author crotwell Created on Jan 18, 2005
 */
public class RecFuncCacheController extends AbstractController {

    /**
     * 
     */
    public RecFuncCacheController(Properties confProps,
                                  String serverPropName,
                                  ORB orb) throws Exception {
        super(confProps, serverPropName, orb);
        String databaseURL = confProps.getProperty(getPropertyPrefix()+ "databaseURL");
        ConnMgr.setURL(databaseURL);
        impl = new RecFuncCacheImpl(confProps.getProperty(getPropertyPrefix()
                + "dataloc"), confProps);
        logger.debug("Impl created, using " + databaseURL);
        // check to make sure hibernate is ok, don't care about the result just if the query succeeds
        RecFuncDB.getSingleton().getAllPriorResultsRef().size();
        float minPercentMatch = 80f;
        boolean bootstrap = true;
        boolean usePhaseWeight = true;
        worker = new SumStackWorker(minPercentMatch,
                                                   usePhaseWeight,
                                                   bootstrap,
                                                   SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS,
                                                   confProps);
        Thread t = new Thread(worker, "SumStackCalc Worker");
        t.setDaemon(true);
        t.start();
    }

    public void destroy() throws Exception {
        super.destroy();
        worker.keepGoing = false;
        // maybe should have something like?
        // AbstractHibernateDB.close();
    }

    public Servant getServant() {
        return impl;
    }

    public String getInterfaceName() {
        return NSRecFuncCache.interfaceName;
    }
    
    private SumStackWorker worker;
    
    private RecFuncCacheImpl impl;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RecFuncCacheController.class);
}