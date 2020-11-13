/**
 * Crust2Process.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction.crust2;

import org.w3c.dom.Element;

import edu.sc.seis.sod.hibernate.eventpair.MeasurementStorage;
import edu.sc.seis.sod.model.common.Location;
import edu.sc.seis.sod.model.event.CacheEvent;
import edu.sc.seis.sod.model.seismogram.LocalSeismogramImpl;
import edu.sc.seis.sod.model.seismogram.RequestFilter;
import edu.sc.seis.sod.model.station.ChannelGroup;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorProcess;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorResult;
import edu.sc.seis.sod.status.StringTreeLeaf;

public class Crust2Process implements WaveformVectorProcess  {
    public Crust2Process(Element config) {
    }

    @Override
	public WaveformVectorResult accept(CacheEvent event, ChannelGroup channelGroup, RequestFilter[][] original,
			RequestFilter[][] available, LocalSeismogramImpl[][] seismograms, MeasurementStorage cookieJar)
			throws Exception {
        if (crust2 != null) {
            Location loc = Location.of(channelGroup.getChannels()[0]);
            Crust2Profile profile = crust2.getClosest(loc.longitude,
                                                      loc.latitude);
            cookieJar.addMeasurement("Crust2_H", new Float(profile.getLayer(7).getTopDepth()));
            cookieJar.addMeasurement("Crust2_Vp", new Float(profile.getPWaveAvgVelocity()));
            cookieJar.addMeasurement("Crust2_Vs", new Float(profile.getSWaveAvgVelocity()));
            cookieJar.addMeasurement("Crust2_VpVs", new Float(profile.getPWaveAvgVelocity() /
                                                       profile.getSWaveAvgVelocity()));
        }
        return new WaveformVectorResult(seismograms, new StringTreeLeaf(this, true));

    }

    transient static Crust2 crust2 =  new Crust2();

}

