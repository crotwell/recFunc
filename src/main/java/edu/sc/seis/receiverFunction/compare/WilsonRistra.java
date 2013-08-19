package edu.sc.seis.receiverFunction.compare;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;


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
                return new StationResult(NetworkDB.getSingleton().getNetworkById(stationId.network_id),
                                         stationId.station_code,
                                         new QuantityImpl(Float.parseFloat(props.getProperty(prefix+"_H")), UnitImpl.KILOMETER),
                                         Float.parseFloat(props.getProperty(prefix+"_VpVs")),
                                         new QuantityImpl(6, UnitImpl.KILOMETER_PER_SECOND),
                                         new StationResultRef("a","b","c"));
            } catch(NumberFormatException e) {
                return null;
            } catch(NotFound e) {
                return null;
            }
        } else {
            return null;
        }
    }
    
    Properties props = new Properties();
}
