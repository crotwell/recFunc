package edu.sc.seis.receiverFunction.server;

import java.util.Properties;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.Servant;
import edu.sc.seis.cormorant.AbstractController;

/**
 * @author crotwell
 * Created on Jan 18, 2005
 */
public class RecFuncCacheController extends AbstractController {

    /**
     *
     */
    public RecFuncCacheController(Properties confProps, String serverPropName,
            ORB orb) throws Exception {
        super(confProps, serverPropName, orb);
        impl = new RecFuncCacheImpl();
    }

    
    
    public void destroy() throws Exception {
        super.destroy();
        impl.getConnection().close();
    }
    
    public Servant getServant() {
        return impl;
    }

    private RecFuncCacheImpl impl;
}