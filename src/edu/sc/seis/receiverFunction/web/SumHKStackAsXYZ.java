package edu.sc.seis.receiverFunction.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Sep 2, 2005
 */
public class SumHKStackAsXYZ extends SummaryHKStackImageServlet {

    public SumHKStackAsXYZ() throws SQLException, ConfigurationException, Exception {
        super();
    }

    void output(SumHKStack sumStack, OutputStream out, HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain");
        OutputStreamWriter writer = new OutputStreamWriter(out);
        float[][] stack = sumStack.getSum().getStack();
        writer.write("# Vp/Vs  H  value\n");
        for(int i = 0; i < stack.length; i++) {
            String h = ""+sumStack.getSum().getHFromIndex(i).getValue(UnitImpl.KILOMETER);
            for(int j = 0; j < stack[0].length; j++) {
                writer.write(sumStack.getSum().getKFromIndex(j)+" "+h+" "+stack[i][j]+"\n");
            }
        }
        writer.close();
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SumHKStackAsXYZ.class);
}
