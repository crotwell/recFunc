package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.PhaseCut;
import edu.sc.seis.fissuresUtil.bag.PhaseNonExistent;
import edu.sc.seis.fissuresUtil.bag.RMean;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;

public class PolarityCheck {

    public static boolean checkPolarity(CachedResult result)
            throws FissuresException, TauModelException, PhaseNonExistent {
        LocalSeismogramImpl itr = (LocalSeismogramImpl)result.radial;
        itr = cutter.cut(result.channels[0].my_site.my_station.my_location,
                         result.prefOrigin,
                         itr);
        float[] data = itr.get_as_floats();
        float max = Float.NEGATIVE_INFINITY;
        float min = Float.POSITIVE_INFINITY;
        for(int i = 0; i < data.length; i++) {
            if(data[i] > max) {
                max = data[i];
            }
            if(data[i] < min) {
                min = data[i];
            }
        }
        return max > -1 * min;
    }

    static RMean rMean = new RMean();
    
    static PhaseCut cutter = new PhaseCut(TauPUtil.getTauPUtil(),
                                          "P",
                                          new TimeInterval(-2, UnitImpl.SECOND),
                                          "P",
                                          new TimeInterval(2, UnitImpl.SECOND));
}
