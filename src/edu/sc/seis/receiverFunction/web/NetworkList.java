package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;


/**
 * @author crotwell
 * Created on Feb 10, 2005
 */
public class NetworkList extends Revlet {

    public NetworkList() throws SQLException {
    }
    
    public synchronized RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        RevletContext context = new RevletContext("networkList.vm", Start.getDefaultContext());
        Revlet.loadStandardQueryParams(req, context);
        ArrayList<VelocityNetwork> netList = new ArrayList<VelocityNetwork>();
        List<NetworkAttrImpl> nets = NetworkDB.getSingleton().getAllNetworks();
        for(NetworkAttrImpl networkAttrImpl : nets) {
            netList.add(new VelocityNetwork(networkAttrImpl));
        }
        context.put("networkList", netList);
        return context;
    }
}
