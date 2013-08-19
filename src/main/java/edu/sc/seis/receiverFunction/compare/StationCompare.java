package edu.sc.seis.receiverFunction.compare;

import edu.iris.Fissures.IfNetwork.StationId;


/**
 * @author crotwell
 * Created on Dec 6, 2004
 */
public interface StationCompare {

    StationResult getResult(StationId stationId);
}
