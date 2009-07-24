package edu.sc.seis.receiverFunction.web;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;


public class SynthHKAsXYZ extends SynthHKImage {

    public SynthHKAsXYZ() throws SQLException, ConfigurationException, Exception {
        super();
    }

    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    void output(SumHKStack sumStack, BufferedOutputStream out, HttpServletRequest req, HttpServletResponse res) throws IOException {
        SumHKStackAsXYZ.doXYZOutput(sumStack, out, req, res);
    }
    
}
