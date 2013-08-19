package edu.sc.seis.receiverFunction.web;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.AbstractHibernateDB;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.sod.ConfigurationException;

public class CustomStackAsXYZ  extends HttpServlet {

	public CustomStackAsXYZ() throws SQLException, ConfigurationException, Exception {
        DATA_LOC = Start.getDataLoc();
	}
	
	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            SumHKStack sumStack = CustomStack.calcCustomStack(req);
            BufferedOutputStream out = new BufferedOutputStream(res.getOutputStream());
            SumHKStackAsXYZ.doXYZOutput(sumStack, out, req, res);
        } catch(EOFException e) {
            // oh well...
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            writer.write("<html><body><p>No HK stack foundfor  "
                    + req.getParameter("staCode") + "</p></body></html>");
            writer.flush();
        } catch(Throwable e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        } finally {
            AbstractHibernateDB.rollback();
        }
	}

	String DATA_LOC;
	
}
