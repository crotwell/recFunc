package edu.sc.seis.receiverFunction.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.VelocityContext;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.receiverFunction.compare.JDBCStationResultRef;
import edu.sc.seis.receiverFunction.compare.StationResultRef;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;


/**
 * @author crotwell
 * Created on Aug 9, 2005
 */
public class PriorResultList extends Revlet {
    
    public PriorResultList() throws SQLException {
        Connection conn = ConnMgr.createConnection();
        jdbcStationResultRef = new JDBCStationResultRef(conn);  
    }

    /**
     *
     */
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        VelocityContext velContext = new VelocityContext(Start.getDefaultContext());
        RevletContext context = new RevletContext("priorResultList.vm", velContext);
        Start.loadStandardQueryParams(req, context);
        ArrayList results = new ArrayList();
        StationResultRef[] refs = jdbcStationResultRef.getAll();
        for(int i = 0; i < refs.length; i++) {
            results.add(refs[i]);
        }
        velContext.put("priorResults", results);
        return context;
    }
    
    JDBCStationResultRef jdbcStationResultRef;
}
