package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.rev.StationLocator;
import edu.sc.seis.rev.velocity.VelocityStation;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelCookieJar;
import edu.sc.seis.sod.database.waveform.JDBCEventChannelStatus;

/**
 * @author crotwell Created on Feb 16, 2005
 */
public class HKStackImageServlet  extends HttpServlet {

    /**
     * @throws SQLException
     *
     */
    public HKStackImageServlet() throws SQLException {
        Connection conn = ConnMgr.createConnection();
        jdbcCookies = new JDBCEventChannelCookieJar(conn);
        jdbcEvent = new JDBCEventAccess(conn);
        jdbcECStatus = new JDBCEventChannelStatus(conn);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        try {
            logger.debug("doGet called");
            res.setContentType("image/png");
            if(req.getParameter("eq") == null) { throw new Exception("eq param not set"); }
            CacheEvent eq = jdbcEvent.getEvent(new Integer(req.getParameter("eq")).intValue());
            VelocityStation sta = staLoc.locate(req);
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private StationLocator staLoc;
    
    private JDBCEventAccess jdbcEvent;

    private JDBCEventChannelCookieJar jdbcCookies;

    private JDBCEventChannelStatus jdbcECStatus;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HKStackImageServlet.class);
    
}