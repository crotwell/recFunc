package edu.sc.seis.receiverFunction.compare;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import edu.iris.Fissures.IfNetwork.StationId;


/**
 * @author crotwell
 * Created on Dec 6, 2004
 */
public class WilsonRistra implements StationCompare {

    public WilsonRistra() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("edu/sc/seis/receiverFunction/compare/WilsonRistra.prop");
        in = new BufferedInputStream(in);
        Properties props = new Properties();
        props.load(in);
    }
    
    /**
     *
     */
    public float getVpVs(StationId stationId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     *
     */
    public float getH(StationId stationId) {
        // TODO Auto-generated method stub
        return 0;
    }
}
