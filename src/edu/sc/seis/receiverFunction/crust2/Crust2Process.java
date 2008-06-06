/**
 * Crust2Process.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction.crust2;

import java.io.IOException;

import org.w3c.dom.Element;

import edu.iris.Fissures.Location;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorProcess;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorResult;
import edu.sc.seis.sod.status.StringTreeLeaf;

public class Crust2Process implements WaveformVectorProcess  {
    public Crust2Process(Element config) {
    }

    public WaveformVectorResult process(CacheEvent event,
                                        ChannelGroup channelGroup,
                                        RequestFilter[][] original,
                                        RequestFilter[][] available,
                                        LocalSeismogramImpl[][] seismograms,
                                        CookieJar cookieJar) throws Exception {
        if (crust2 != null) {
            Location loc = channelGroup.getChannels()[0].my_site.my_station.my_location;
            Crust2Profile profile = crust2.getClosest(loc.longitude,
                                                      loc.latitude);
            cookieJar.put("Crust2_H", new Float(profile.getLayer(7).getTopDepth()));
            cookieJar.put("Crust2_Vp", new Float(profile.getPWaveAvgVelocity()));
            cookieJar.put("Crust2_Vs", new Float(profile.getSWaveAvgVelocity()));
            cookieJar.put("Crust2_VpVs", new Float(profile.getPWaveAvgVelocity() /
                                                       profile.getSWaveAvgVelocity()));
        }
        return new WaveformVectorResult(seismograms, new StringTreeLeaf(this, true));

    }

    transient static Crust2 crust2 = null;
    static {
        try {
            crust2 = new Crust2();
        } catch (IOException e) {
            GlobalExceptionHandler.handle("Couldn't load Crust2.0", e);
        }
    }

}

