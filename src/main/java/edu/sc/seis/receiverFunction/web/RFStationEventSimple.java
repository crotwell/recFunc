package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import edu.sc.seis.sod.ConfigurationException;

public class RFStationEventSimple extends RFStationEvent {

    public RFStationEventSimple() throws SQLException, ConfigurationException,
            Exception {
        super();
        velocityFile = "rfStationEventSimple.vm";
    }
}
