package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.ConfigurationException;


public class ComplexResidualAsXYZ extends ComplexityResidualImage {

    public ComplexResidualAsXYZ() throws SQLException, ConfigurationException, Exception {
        super();
    }
    
    void output(SumHKStack sumStack, OutputStream out, HttpServletRequest req, HttpServletResponse res) throws IOException {
        SumHKStackAsXYZ.doXYZOutput(sumStack, out, req, res);
    }
}
