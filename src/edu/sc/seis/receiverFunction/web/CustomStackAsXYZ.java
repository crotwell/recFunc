package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCRejectedMaxima;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

public class CustomStackAsXYZ  extends HttpServlet {

	public CustomStackAsXYZ() throws SQLException, ConfigurationException, Exception {
        DATA_LOC = Start.getDataLoc();
        Connection conn = ConnMgr.createConnection();
        JDBCEventAccess jdbcEventAccess = new JDBCEventAccess(conn);
        JDBCChannel jdbcChannel = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      DATA_LOC);
        jdbcHKStack = new JDBCHKStack(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      jdbcRecFunc);
        jdbcRejectMax = new JDBCRejectedMaxima(conn);
	}
	
	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            SumHKStack sumStack = CustomStack.calcCustomStack(req, jdbcHKStack, jdbcRejectMax);
            OutputStream out = res.getOutputStream();
            SumHKStackAsXYZ.doXYZOutput(sumStack, out, req, res);
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            writer.write("<html><body><p>No HK stack foundfor  "
                    + req.getParameter("staCode") + "</p></body></html>");
            writer.flush();
        } catch(Throwable e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        }
	}


	JDBCHKStack jdbcHKStack;
	JDBCRejectedMaxima jdbcRejectMax;
	String DATA_LOC;
	
}
