package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.velocity.VelocityNetwork;


/**
 * @author crotwell
 * Created on Feb 10, 2005
 */
public class Networks extends Revlet {

    public Networks() throws SQLException {
        jdbcNetwork = new JDBCNetwork();
    }
    /**
     *
     */
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        RevletContext context = new RevletContext("networkList.vm");
        ArrayList netList = new ArrayList();
        NetworkAttr[] nets = jdbcNetwork.getAllNetworkAttrs();
        for(int i = 0; i < nets.length; i++) {
            netList.add(new VelocityNetwork(nets[i]));
        }
        context.put("networkList", netList);
        return context;
    }
    
    JDBCNetwork jdbcNetwork;
}
