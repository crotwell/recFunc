package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Mar 31, 2005
 */
public class ComparePriorResultTxt extends ComparePriorResult {
    
    
    public ComparePriorResultTxt() throws SQLException, ConfigurationException, Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    protected void setContentType(HttpServletRequest request,
                                  HttpServletResponse response)
    {
        response.setContentType("text/plain");
    }

    public String getVelocityTemplate(HttpServletRequest req) {
        return "comparePriorResultTxt.vm";
    }
}
