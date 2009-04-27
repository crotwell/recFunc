package edu.sc.seis.receiverFunction.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.SocketException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Sep 2, 2005
 */
public class SumHKStackAsXYZ extends SummaryHKStackImageServlet {

    public SumHKStackAsXYZ() throws SQLException, ConfigurationException,
            Exception {
        super();
    }

    public synchronized void doGet(HttpServletRequest req,
                                   HttpServletResponse res) throws IOException {
        BufferedInputStream txtIn = null;
        try {
            logger.debug("doGet called");
            VelocityNetwork net = Start.getNetwork(req);
            String staCode = RevUtil.get("stacode", req);
            float gaussianWidth = RevUtil.getFloat("gaussian",
                                                   req,
                                                   Start.getDefaultGaussian());
            txtIn = RecFuncDB.loadHKStackFileText(net.getWrapped(), staCode, gaussianWidth);
            if (txtIn != null) {
                BufferedOutputStream out = new BufferedOutputStream(res.getOutputStream());
                byte[] b = new byte[512];
                int numRead = txtIn.read(b);
                while (numRead != 0) {
                    out.write(b, 0, numRead);
                    numRead = txtIn.read(b);
                }
                txtIn.close();
                out.close();
                return;
            } else {
                super.doGet(req, res);
            }
        } catch(NotFound e) {
            OutputStreamWriter writer = new OutputStreamWriter(res.getOutputStream());
            writer.write("<html><body><p>No HK stack foundfor  "
                         + req.getParameter("staCode") + "</p></body></html>");
            writer.flush();
        } catch(Throwable e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new RuntimeException(e);
        } finally {
            if (txtIn != null) {txtIn.close();}
        }
    }
    
    void output(SumHKStack sumStack,
                OutputStream out,
                HttpServletRequest req,
                HttpServletResponse res) throws IOException {
        doXYZOutput(sumStack, out, req, res);
    }
    
    public static void doXYZOutput(SumHKStack sumStack,
                                   OutputStream out,
                                   HttpServletRequest req,
                                   HttpServletResponse res) throws IOException {
        res.setContentType("text/plain");
        OutputStreamWriter writer = new OutputStreamWriter(out);
        try {
            printXYZ(sumStack, writer);
        } catch(SocketException e) {
            // oh well, client has closed connection
        } catch(EOFException e) {
            // oh well, client has closed connection
        } finally {
            writer.close();
        }
    }

    public static void printXYZ(SumHKStack sumStack, Writer writer)
            throws IOException {
        float[][] stack = sumStack.getSum().getStack();
        writer.write("# Vp/Vs  H  value\n");
        for(int i = 0; i < stack.length; i++) {
            String h = ""
                    + sumStack.getSum()
                            .getHFromIndex(i)
                            .getValue(UnitImpl.KILOMETER);
            for(int j = 0; j < stack[0].length; j++) {
                writer.write(sumStack.getSum().getKFromIndex(j) + " " + h + " "
                        + stack[i][j] + "\n");
            }
        }
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SumHKStackAsXYZ.class);
}
