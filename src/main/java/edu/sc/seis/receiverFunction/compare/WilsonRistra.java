package edu.sc.seis.receiverFunction.compare;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import edu.sc.seis.sod.model.common.QuantityImpl;
import edu.sc.seis.sod.model.common.UnitImpl;
import edu.sc.seis.sod.model.station.StationId;
import edu.sc.seis.sod.model.station.StationIdUtil;


/**
 * @author crotwell
 * Created on Dec 6, 2004
 */
public class WilsonRistra implements StationCompare {

    public WilsonRistra() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("edu/sc/seis/receiverFunction/compare/WilsonRistra.prop");
        in = new BufferedInputStream(in);
        props.load(in);
    }
    
    public StationResult getResult(StationId stationId) {
        String prefix = StationIdUtil.toStringNoDates(stationId);
        if (props.containsKey(prefix+"_H")) {
            try {
                return new StationResult(stationId.getNetworkId(),
                                         stationId.getStationCode(),
                                         new QuantityImpl(Float.parseFloat(props.getProperty(prefix+"_H")), UnitImpl.KILOMETER),
                                         Float.parseFloat(props.getProperty(prefix+"_VpVs")),
                                         new QuantityImpl(6, UnitImpl.KILOMETER_PER_SECOND),
                                         new StationResultRef("a","b","c"));
            } catch(NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }
    
    Properties props = new Properties();
}
