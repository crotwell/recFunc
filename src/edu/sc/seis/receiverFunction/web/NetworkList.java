package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.VelocityContext;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.status.FissuresFormatter;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;


/**
 * @author crotwell
 * Created on Feb 10, 2005
 */
public class NetworkList extends Revlet {

    public NetworkList() throws SQLException {
        jdbcNetwork = new JDBCNetwork();
    }
    
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        RevletContext context = new RevletContext("networkList.vm", Start.getDefaultContext());
        Start.loadStandardQueryParams(req, context);
        ArrayList netList = new ArrayList();
        int[] netdbids = jdbcNetwork.getAllNetworkDBIds();
        NetworkAttr[] nets = new NetworkAttr[netdbids.length];
        for(int i = 0; i < nets.length; i++) {
            nets[i] = jdbcNetwork.get(netdbids[i]);
            netList.add(new VelocityNetwork(nets[i], netdbids[i]));
        }
        context.put("networkList", netList);
        return context;
    }
    
    JDBCNetwork jdbcNetwork;
}
