package edu.sc.seis.receiverFunction.compare;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.iris.Fissures.network.StationIdUtil;


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
            return new StationResult(stationId,
                                     Float.parseFloat(props.getProperty(prefix+"_H")),
                                     Float.parseFloat(props.getProperty(prefix+"_VpVs")));
        } else {
            return null;
        }
    }
    
    Properties props = new Properties();
}
