/**
 * DataSetRecFuncProcessor.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;


import edu.iris.Fissures.AuditInfo;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.bag.PhaseCut;
import edu.sc.seis.fissuresUtil.bag.Rotate;
import edu.sc.seis.fissuresUtil.display.BasicSeismogramDisplay;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.vsnexplorer.CommonAccess;
import edu.sc.seis.vsnexplorer.configurator.ConfigurationException;
import org.apache.log4j.Logger;

public class DataSetRecFuncProcessor implements SeisDataChangeListener {
    DataSetRecFuncProcessor(DataSetSeismogram[] seis,
                            EventAccessOperations event,
                            RecFunc recFunc)
        throws NoPreferredOrigin{
        this.seis = seis;
        this.event = event;
        this.origin = event.get_preferred_origin();
        this.recFunc = recFunc;
        finished = new boolean[seis.length];
        localSeis = new LocalSeismogramImpl[seis.length];
    }
    
    
    /**
     * Method error
     *
     * @param    sdce                a  SeisDataErrorEvent
     *
     */
    public void error(SeisDataErrorEvent sdce) {
        error = sdce;
    }
    
    /**
     * Method finished
     *
     * @param    sdce                a  SeisDataChangeEvent
     *
     */
    public void finished(SeisDataChangeEvent sdce) {
        try {
            logger.debug("finished for "+sdce.getSource().getName());
            finished[getIndex(sdce.getSource())] = true;
            for (int i=0; i<finished.length; i++) {
                if ( ! finished[i]) {
                    // not all finished yet
                    return;
                }
            }
            // must be all finished
            if (error != null) {
                logger.error("problem: ", error.getCausalException());
                return;
            }
            
            Channel[] channel = new Channel[3];
            for (int i = 0; i < seis.length; i++) {
                channel[i] =
                    seis[i].getDataSet().getChannel(seis[i].getRequestFilter().channel_id);
            }
            
            ans = recFunc.process(event,
                                  channel,
                                  localSeis);
            
            Channel chan = channel[0];
            Location staLoc = chan.my_site.my_station.my_location;
            Origin origin = event.get_preferred_origin();
            Location evtLoc = origin.my_location;
            
            Arrival[] pPhases = CommonAccess.getCommonAccess().getTravelTimes(evtLoc, staLoc, "ttp");
            MicroSecondDate firstP = new MicroSecondDate(origin.origin_time);
            logger.debug("origin "+firstP);
            firstP = firstP.add(new TimeInterval(pPhases[0].getTime(), UnitImpl.SECOND));
            logger.debug("firstP "+firstP);
            
            TimeInterval shift = recFunc.getShift();
            float[] predicted = ans.getPredicted();
            
            logger.info("Finished with receiver function processing");
            logger.debug("rec func begin "+firstP.subtract(shift));
            LocalSeismogramImpl predSeis =
                new LocalSeismogramImpl("recFunc/"+localSeis[0].get_id(),
                                        firstP.subtract(shift).getFissuresTime(),
                                        predicted.length,
                                        localSeis[0].sampling_info,
                                        localSeis[0].y_unit,
                                        localSeis[0].channel_id,
                                        predicted);
            predSeis.setName("receiver function "+localSeis[0].channel_id.station_code);
            predictedDSS =
                new MemoryDataSetSeismogram(predSeis,
                                            "receiver function "+localSeis[0].channel_id.station_code);
            AuditInfo[] audit = new AuditInfo[1];
            audit[0] =
                new AuditInfo("Calculated receiver function",
                              System.getProperty("user.name"));
            sdce.getSource().getDataSet().addDataSetSeismogram(predictedDSS,
                                                               audit);
            
        } catch (ConfigurationException ce) {
            logger.warn("Unable to get travel time calculator", ce);
            CommonAccess.getCommonAccess().handleException("Unable to get travel time calculator", ce);
        } catch (Exception ee) {
            logger.warn("Problem shifting receiver function to align P wave", ee);
        } finally {
            recFuncFinished = true;
        }
    }
    
    /**
     * Method pushData
     *
     * @param    sdce                a  SeisDataChangeEvent
     *
     */
    public void pushData(SeisDataChangeEvent sdce) {
        if (sdce.getSeismograms().length != 0) {
            localSeis[getIndex(sdce.getSource())] = sdce.getSeismograms()[0];
        } else {
            logger.info("pushData event has zero length localSeismogram array");
        }
    }
    
    int getIndex(DataSetSeismogram s) {
        for (int i=0; i<seis.length; i++) {
            if (seis[i] == s) {
                return i;
            }
        }
        throw new IllegalArgumentException("Can't find index for this seismogram");
    }

    public boolean isRecFuncFinished() {
        return recFuncFinished;
    }
    
    public MemoryDataSetSeismogram getPredicted() {
        return predictedDSS;
    }
    
    public SeisDataErrorEvent getError() {
        return error;
    }
    
    DataSetSeismogram[] seis;
    
    EventAccessOperations event;
    
    Origin origin;
    
    boolean[] finished;
    
    LocalSeismogramImpl[] localSeis;
    
    RecFunc recFunc;
    
    IterDeconResult ans = null;
    
    MemoryDataSetSeismogram predictedDSS = null;
    
    boolean recFuncFinished = false;
    
    SeisDataErrorEvent error = null;
    
    static Logger logger = Logger.getLogger(DataSetRecFuncProcessor.class);
    
}
