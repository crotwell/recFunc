/**
 * RecFunc.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.log4j.Logger;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.seisFile.fdsnws.stationxml.Channel;
import edu.sc.seis.sod.bag.IncompatibleSeismograms;
import edu.sc.seis.sod.bag.IterDecon;
import edu.sc.seis.sod.bag.IterDeconResult;
import edu.sc.seis.sod.bag.Rotate;
import edu.sc.seis.sod.bag.TauPUtil;
import edu.sc.seis.sod.bag.ZeroPowerException;
import edu.sc.seis.sod.model.common.FissuresException;
import edu.sc.seis.sod.model.common.Location;
import edu.sc.seis.sod.model.common.Orientation;
import edu.sc.seis.sod.model.common.SamplingImpl;
import edu.sc.seis.sod.model.event.CacheEvent;
import edu.sc.seis.sod.model.event.NoPreferredOrigin;
import edu.sc.seis.sod.model.event.OriginImpl;
import edu.sc.seis.sod.model.seismogram.LocalSeismogramImpl;
import edu.sc.seis.sod.model.station.ChannelGroup;
import edu.sc.seis.sod.util.time.ClockUtil;

public class RecFunc {

    /** uses a default shift of 10 seconds. */
    public RecFunc(TauPUtil timeCalc, IterDecon decon, boolean pWave) {
        this(timeCalc, decon, pWave, DEFAULT_SHIFT);

    }

    public RecFunc(TauPUtil timeCalc, IterDecon decon, boolean pWave, Duration shift) {
        this(timeCalc, decon, pWave, shift, Duration.ofSeconds(100));
    }

    public RecFunc(TauPUtil timeCalc, IterDecon decon, boolean pWave, Duration shift, Duration pad) {
        this.timeCalc = timeCalc;
        this.decon = decon;
        this.shift = shift;
        this.pad = pad;
        this.pWave = pWave;
    }

    public IterDeconResult[] process(CacheEvent event,
                                     ChannelGroup channelGroup,
                                     LocalSeismogramImpl[] localSeis)
        throws NoPreferredOrigin, FissuresException, IncompatibleSeismograms, TauModelException, ZeroPowerException {
        return process(event, channelGroup.getChannels(), localSeis);
    }
    
    public IterDeconResult[] process(CacheEvent event,
                                     Channel[] channel,
                                     LocalSeismogramImpl[] localSeis)
        throws NoPreferredOrigin,
        IncompatibleSeismograms,
        FissuresException, TauModelException, ZeroPowerException {

        LocalSeismogramImpl n = null, e = null, z = null;
        String foundChanCodes = "";
        for (int i=0; i<localSeis.length; i++) {
            if (localSeis[i].channel_id.getChannelCode().endsWith("N")) {
                n = localSeis[i];
            } else if (localSeis[i].channel_id.getChannelCode().endsWith("E")) {
                e = localSeis[i];
            }if (localSeis[i].channel_id.getChannelCode().endsWith("Z")) {
                z = localSeis[i];
            }
            foundChanCodes += localSeis[i].channel_id.getChannelCode()+" ";
        }
        if (n == null || e == null || z == null) {
            logger.error("problem one seismogram component is null ");
            throw new NullPointerException("problem one seismogram component is null, "+foundChanCodes+" "+
                                               " "+(n != null)+" "+(e != null)+" "+(z != null));
        }
        Channel nChan = null, eChan = null, zChan = null;
        for(int i = 0; i < channel.length; i++) {
            if (channel[i].getCode().endsWith("N")) {
                nChan = channel[i];
            } else if (channel[i].getCode().endsWith("E")) {
                eChan = channel[i];
            }if (channel[i].getCode().endsWith("Z")) {
                zChan = channel[i];
            }
        }
        if (nChan == null || eChan == null || zChan == null) {
            logger.error("problem one channel component is null ");
            throw new NullPointerException("problem one channel component is null, "+
                                           " "+(nChan != null)+" "+(eChan != null)+" "+(zChan != null));
        }

        Location staLoc = Location.of(zChan);
        OriginImpl origin = event.get_preferred_origin();
        Location evtLoc = origin.getLocation();

        LocalSeismogramImpl[] rotSeis = Rotate.rotateGCP(e, Orientation.of(eChan), n, Orientation.of(nChan), staLoc, evtLoc, "T", "R");
        float[][] rotated = { rotSeis[0].get_as_floats(), rotSeis[1].get_as_floats() };

        // check lengths, trim if needed???
        float[] zdata = z.get_as_floats();
        if (rotated[0].length != zdata.length) {
            logger.error("data is not of same length "+
                             rotated[0].length+" "+zdata.length);
            throw new IncompatibleSeismograms("data is not of same length "+
                                                  rotated[0].length+" "+zdata.length);
        }
        if (zdata.length == 0) {
            throw new IncompatibleSeismograms("data is of zero length ");
        }
        SamplingImpl samp = z.sampling_info;
        double period = ClockUtil.doubleSeconds(samp.getPeriod());

        zdata = IterDecon.makePowerTwo(zdata);
        rotated[0] = IterDecon.makePowerTwo(rotated[0]);
        rotated[1] = IterDecon.makePowerTwo(rotated[1]);

        IterDeconResult ansRadial;
        IterDeconResult ansTangential;
        if (pWave) {
            ansRadial = processComponent(rotated[1],
                                         zdata,
                                         (float)period,
                                         staLoc,
                                         origin);

            ansTangential = processComponent(rotated[0],
                                             zdata,
                                             (float)period,
                                             staLoc,
                                             origin);
        } else {
            // s wave deconvolve horizontal from z, opposite of P wave
            ansRadial = processComponent(zdata,
                                         rotated[1],
                                         (float)period,
                                         staLoc,
                                         origin);

            ansTangential = processComponent(zdata,
                                             rotated[0],
                                             (float)period,
                                             staLoc,
                                             origin);
            
        }
        IterDeconResult[] ans = new IterDeconResult[2];
        ans[0] = ansRadial;
        ans[1] = ansTangential;
        return ans;
    }

    public IterDeconResult processComponent(float[] component,
                                               float[] zdata,
                                               float period,
                                               Location staLoc,
                                               OriginImpl origin)
        throws TauModelException, ZeroPowerException, ZeroPowerException {
        if (component.length == 0) {
            throw new ArrayIndexOutOfBoundsException("Component length is "+component.length);
        }
        if (zdata.length == 0) {
            throw new ArrayIndexOutOfBoundsException("Z component length is "+zdata.length);
        }
        IterDeconResult ans = decon.process(component,
                                            zdata,
                                            period);
        float[] predicted = ans.getPredicted();
        logger.info("predicted.length = "+predicted.length);

        String[] phaseName = pWave ? new String[] {"ttp"} : new String[] {"tts"};
        List<Arrival> pPhases =
            timeCalc.calcTravelTimes(staLoc, origin, phaseName);

        Instant firstP = origin.getOriginTime();
        logger.debug("origin "+firstP);
        firstP = firstP.plus(ClockUtil.durationOfSeconds(pPhases.get(0).getTime()));
        logger.debug("firstP "+firstP);
        if (ClockUtil.floatSeconds(shift) != 0) {
            logger.debug("shifting by "+shift+"  before 0="+predicted[0]);
            predicted = decon.phaseShift(predicted,
            		ClockUtil.floatSeconds(shift),
                                             period);

            logger.debug("shifting by "+shift);
        }

        logger.info("Finished with receiver function processing");
        logger.debug("rec func begin "+firstP.minus(shift));
        ans.predicted = predicted;
        ans.setAlignShift(shift);
        return ans;
    }

    public IterDecon getIterDecon() {
        return decon;
    }

    public TauPUtil getTimeCalc() {
        return timeCalc;
    }

    public Duration getShift() {
        return shift;
    }

    /*
    public static MemoryDataSetSeismogram saveTimeSeries(float[] data,
                                           String name,
                                           String chanCode,
                                           Instant begin,
                                           LocalSeismogramImpl refSeismogram) {
        return saveTimeSeries(data, name, chanCode, begin, refSeismogram, UnitImpl.createUnitImpl(refSeismogram.y_unit));
    }

    public static MemoryDataSetSeismogram saveTimeSeries(float[] data,
                                           String name,
                                           String chanCode,
                                           Instant begin,
                                           LocalSeismogramImpl refSeismogram,
                                          UnitImpl unit) {
        ChannelId recFuncChanId = new ChannelId(refSeismogram.channel_id.network_id,
                                                refSeismogram.channel_id.station_code,
                                                refSeismogram.channel_id.site_code,
                                                chanCode,
                                                refSeismogram.channel_id.begin_time);

        LocalSeismogramImpl predSeis =
            new LocalSeismogramImpl("recFunc/"+chanCode+"/"+refSeismogram.get_id(),
                                    begin,
                                    data.length,
                                    refSeismogram.sampling_info,
                                    unit,
                                    recFuncChanId,
                                    data);
        predSeis.setName(name);
        return new MemoryDataSetSeismogram(predSeis,
                                           name);

    }
    */
    
    TauPUtil timeCalc;

    IterDecon decon;

    Duration shift;

    Duration pad;
    
    boolean pWave;
    
    static public final Duration DEFAULT_SHIFT = Duration.ofSeconds(10);

    public static Duration getDefaultShift() {
        return DEFAULT_SHIFT;   
    }
    
    static Logger logger = Logger.getLogger(RecFunc.class);

}

