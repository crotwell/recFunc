/**
 * RecFunc.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import org.apache.log4j.Logger;

import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.IncompatibleSeismograms;
import edu.sc.seis.fissuresUtil.bag.Rotate;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;

public class RecFunc {

    /** uses a default shift of 10 seconds. */
    public RecFunc(TauPUtil timeCalc, IterDecon decon, boolean pWave) {
        this(timeCalc, decon, pWave, DEFAULT_SHIFT);

    }

    public RecFunc(TauPUtil timeCalc, IterDecon decon, boolean pWave, TimeInterval shift) {
        this(timeCalc, decon, pWave, shift, new TimeInterval(100, UnitImpl.SECOND));
    }

    public RecFunc(TauPUtil timeCalc, IterDecon decon, boolean pWave, TimeInterval shift, TimeInterval pad) {
        this.timeCalc = timeCalc;
        this.decon = decon;
        this.shift = shift;
        this.pad = pad;
        this.pWave = pWave;
    }

    public IterDeconResult[] process(EventAccessOperations event,
                                     ChannelGroup channelGroup,
                                     LocalSeismogramImpl[] localSeis)
        throws NoPreferredOrigin, FissuresException, IncompatibleSeismograms, TauModelException, RecFuncException {
        return process(event, channelGroup.getChannels(), localSeis);
    }
    
    public IterDeconResult[] process(EventAccessOperations event,
                                     Channel[] channel,
                                     LocalSeismogramImpl[] localSeis)
        throws NoPreferredOrigin,
        IncompatibleSeismograms,
        FissuresException, TauModelException, RecFuncException {

        LocalSeismogramImpl n = null, e = null, z = null;
        String foundChanCodes = "";
        for (int i=0; i<localSeis.length; i++) {
            if (localSeis[i].channel_id.channel_code.endsWith("N")) {
                n = localSeis[i];
            } else if (localSeis[i].channel_id.channel_code.endsWith("E")) {
                e = localSeis[i];
            }if (localSeis[i].channel_id.channel_code.endsWith("Z")) {
                z = localSeis[i];
            }
            foundChanCodes += localSeis[i].channel_id.channel_code+" ";
        }
        if (n == null || e == null || z == null) {
            logger.error("problem one seismogram component is null ");
            throw new NullPointerException("problem one seismogram component is null, "+foundChanCodes+" "+
                                               " "+(n != null)+" "+(e != null)+" "+(z != null));
        }
        Channel nChan = null, eChan = null, zChan = null;
        for(int i = 0; i < channel.length; i++) {
            if (channel[i].get_id().channel_code.endsWith("N")) {
                nChan = channel[i];
            } else if (channel[i].get_id().channel_code.endsWith("E")) {
                eChan = channel[i];
            }if (channel[i].get_id().channel_code.endsWith("Z")) {
                zChan = channel[i];
            }
        }
        if (nChan == null || eChan == null || zChan == null) {
            logger.error("problem one channel component is null ");
            throw new NullPointerException("problem one channel component is null, "+
                                           " "+(nChan != null)+" "+(eChan != null)+" "+(zChan != null));
        }

        Location staLoc = zChan.my_site.my_station.my_location;
        Origin origin = event.get_preferred_origin();
        Location evtLoc = origin.my_location;

        LocalSeismogramImpl[] rotSeis = Rotate.rotateGCP(e, eChan.an_orientation, n, nChan.an_orientation, staLoc, evtLoc, "T", "R");
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
        SamplingImpl samp = SamplingImpl.createSamplingImpl(z.sampling_info);
        double period = samp.getPeriod().convertTo(UnitImpl.SECOND).getValue();

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
                                               Origin origin)
        throws TauModelException, RecFuncException {
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
        Arrival[] pPhases =
            timeCalc.calcTravelTimes(staLoc, origin, phaseName);

        MicroSecondDate firstP = new MicroSecondDate(origin.origin_time);
        logger.debug("origin "+firstP);
        firstP = firstP.add(new TimeInterval(pPhases[0].getTime(), UnitImpl.SECOND));
        logger.debug("firstP "+firstP);
        //TimeInterval shift = firstP.subtract(z.getBeginTime());
        shift = (TimeInterval)shift.convertTo(UnitImpl.SECOND);
        if (shift.getValue() != 0) {
            logger.debug("shifting by "+shift+"  before 0="+predicted[0]);
            predicted = decon.phaseShift(predicted,
                                             (float)shift.getValue(),
                                             period);

            logger.debug("shifting by "+shift);
        }

        logger.info("Finished with receiver function processing");
        logger.debug("rec func begin "+firstP.subtract(shift));
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

    public TimeInterval getShift() {
        return shift;
    }

    public static MemoryDataSetSeismogram saveTimeSeries(float[] data,
                                           String name,
                                           String chanCode,
                                           MicroSecondDate begin,
                                           LocalSeismogramImpl refSeismogram) {
        return saveTimeSeries(data, name, chanCode, begin, refSeismogram, UnitImpl.createUnitImpl(refSeismogram.y_unit));
    }

    public static MemoryDataSetSeismogram saveTimeSeries(float[] data,
                                           String name,
                                           String chanCode,
                                           MicroSecondDate begin,
                                           LocalSeismogramImpl refSeismogram,
                                          UnitImpl unit) {
        ChannelId recFuncChanId = new ChannelId(refSeismogram.channel_id.network_id,
                                                refSeismogram.channel_id.station_code,
                                                refSeismogram.channel_id.site_code,
                                                chanCode,
                                                refSeismogram.channel_id.begin_time);

        LocalSeismogramImpl predSeis =
            new LocalSeismogramImpl("recFunc/"+chanCode+"/"+refSeismogram.get_id(),
                                    begin.getFissuresTime(),
                                    data.length,
                                    refSeismogram.sampling_info,
                                    unit,
                                    recFuncChanId,
                                    data);
        predSeis.setName(name);
        return new MemoryDataSetSeismogram(predSeis,
                                           name);

    }
    
    TauPUtil timeCalc;

    IterDecon decon;

    TimeInterval shift;

    TimeInterval pad;
    
    boolean pWave;
    
    static public final TimeInterval DEFAULT_SHIFT = new TimeInterval(10, UnitImpl.SECOND);

    public static TimeInterval getDefaultShift() {
        return DEFAULT_SHIFT;   
    }
    
    static Logger logger = Logger.getLogger(RecFunc.class);

}

