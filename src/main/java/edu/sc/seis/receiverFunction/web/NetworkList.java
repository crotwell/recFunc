package edu.sc.seis.receiverFunction.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    public NetworkList() {
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
        Collections.sort(netList, new Comparator<VelocityNetwork>() {
            public int compare(VelocityNetwork n1, VelocityNetwork n2) {
                if (n1.get_code().equals(n2.get_code())) {
                    return n1.getId().begin_time.date_time.compareTo(n2.getId().begin_time.date_time);
                }
                return n1.getCode().compareTo(n2.getCode());
            }});
        context.put("networkList", netList);
        return context;
    }
}
