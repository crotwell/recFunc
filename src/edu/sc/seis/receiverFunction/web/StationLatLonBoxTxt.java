package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import edu.sc.seis.sod.ConfigurationException;


/**
 * @author crotwell
 * Created on Sep 6, 2005
 */
public class StationLatLonBoxTxt extends StationLatLonBox {

    /**
     *
     */
    public StationLatLonBoxTxt() throws SQLException, ConfigurationException,
            Exception {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     *
     */
    public StationLatLonBoxTxt(String databaseURL, String dataloc)
            throws SQLException, ConfigurationException, Exception {
        super(databaseURL, dataloc);
        // TODO Auto-generated constructor stub
    }
    
    
    public String getVelocityTemplate(HttpServletRequest req) {
        return "stationLatLonBoxTxt.vm";
    }
}
