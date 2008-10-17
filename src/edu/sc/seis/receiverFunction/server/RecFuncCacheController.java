package edu.sc.seis.receiverFunction.server;

import java.util.Properties;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.Servant;
import edu.sc.seis.cormorant.AbstractController;
import edu.sc.seis.fissuresUtil.hibernate.AbstractHibernateDB;
import edu.sc.seis.receiverFunction.SumHKStack;

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
        impl = new RecFuncCacheImpl(confProps.getProperty(getPropertryPrefix()
                + "databaseURL"), confProps.getProperty(getPropertryPrefix()
                + "dataloc"), confProps);

        float minPercentMatch = 80f;
        boolean bootstrap = true;
        boolean usePhaseWeight = true;
        SumStackWorker worker = new SumStackWorker(minPercentMatch,
                                                   usePhaseWeight,
                                                   bootstrap,
                                                   SumHKStack.DEFAULT_BOOTSTRAP_ITERATONS);
        Thread t = new Thread(worker);
        t.setDaemon(true);
        t.start();
    }

    public void destroy() throws Exception {
        super.destroy();
        // maybe should have something like?
        // AbstractHibernateDB.close();
    }

    public Servant getServant() {
        return impl;
    }

    public String getInterfaceName() {
        return NSRecFuncCache.interfaceName;
    }

    private RecFuncCacheImpl impl;
}