package edu.sc.seis.receiverFunction;

import edu.sc.seis.fissuresUtil.xml.*;

import edu.iris.Fissures.AuditInfo;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.bag.PhaseCut;
import edu.sc.seis.fissuresUtil.bag.Rotate;
import edu.sc.seis.fissuresUtil.chooser.DataSetChannelGrouper;
import edu.sc.seis.fissuresUtil.display.BasicSeismogramDisplay;
import edu.sc.seis.vsnexplorer.CommonAccess;
import edu.sc.seis.vsnexplorer.configurator.ConfigurationException;
import edu.sc.seis.vsnexplorer.task.GUITask;
import edu.sc.seis.vsnexplorer.task.GlobalToolBar;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.apache.log4j.Logger;

/**
 * RecFuncTask.java
 *
 *
 * Created: Wed Jul  3 21:22:35 2002
 *
 * @author <a href="mailto:">Philip Crotwell</a>
 * @version
 */

public class RecFuncTask  extends MouseAdapter implements GUITask {
    public RecFuncTask (){
        
    }
    
    public void configure(java.util.Map params) throws ConfigurationException {
        configParams = params;
        float gwidth = 3.0f;
        decon = new IterDecon(100, true, .001f, gwidth);
    }
    
    public void invoke() {
        GlobalToolBar.setMouseListener(this);
    }
    
    
    /** Gets the GUI for this Task. Used for interacting with the user
     before and after invoking this Task's action.
     */
    public JComponent getGUI() throws Exception {
        return new JPanel();
    }
    
    /** True if this GUI has a "more options" funtionality. More options
     *  appear in a separate panel below the main gui, and can be shown/hidden
     *  with a "Show More Options" and "Hide More Options" button that is
     *  provided automatically is this return true.
     */
    public boolean hasMoreOptions() {
        // TODO
        return false;
    }
    
    /** Gets the "More Options" GUI component.
     */
    public JComponent getMoreOptionsGUI() {
        // TODO
        return null;
    }
    
    public void destroy(){}
    
    public void mouseClicked(MouseEvent me) {
        try {
            if(me.getComponent() instanceof BasicSeismogramDisplay){
                BasicSeismogramDisplay display =
                    (BasicSeismogramDisplay)me.getComponent();
                DataSetSeismogram[] seismograms =
                    display.getSeismograms();
                DataSetSeismogram seis = seismograms[0];
                DataSet dataSet = seis.getDataSet();
                EventAccessOperations currEvent = dataSet.getEvent();
                ChannelId[] channelGroup =
                    DataSetChannelGrouper.retrieveGrouping(dataSet,
                                                           seis.getRequestFilter().channel_id);
                DataSetSeismogram[] chGrpSeismograms = new DataSetSeismogram[3];
                String[] allSeisNames = dataSet.getDataSetSeismogramNames();
                for(int counter = 0; counter < channelGroup.length; counter++) {
                    for (int i=0; i < allSeisNames.length; i++) {
                        DataSetSeismogram allSeis =
                            dataSet.getDataSetSeismogram(allSeisNames[i]);
                        if (ChannelIdUtil.areEqual(channelGroup[counter], allSeis.getRequestFilter().channel_id) &&
                            seis.getBeginTime().date_time.equals(allSeis.getBeginTime().date_time) &&
                            seis.getEndTime().date_time.equals(allSeis.getEndTime().date_time)) {
                            // found a match
                            chGrpSeismograms[counter] = allSeis;
                            break;
                        }
                    }
                }
                
                DataSetRecFuncProcessor processor =
                    new DataSetRecFuncProcessor(chGrpSeismograms, currEvent, display);
                for (int i=0; i<chGrpSeismograms.length; i++) {
                    logger.debug("Retrieveing for "+chGrpSeismograms[i].getName());
                    chGrpSeismograms[i].retrieveData(processor);
                }
            }
        } catch(NoPreferredOrigin e) {
            CommonAccess.getCommonAccess().handleException(e);
        }
    }
    
    void temp() {
        //        try {
        //
        //            LocalSeismogramImpl[] tmpSeis = new LocalSeismogramImpl[3];
        //            tmpSeis[0] = chGrpSeismograms[0].getSeismogram();
        //            tmpSeis[1] = chGrpSeismograms[1].getSeismogram();
        //            tmpSeis[2] = chGrpSeismograms[2].getSeismogram();
        //            LocalMotionVectorImpl motionVec =
        //                MotionVectorUtil.create(tmpSeis);
        //        } catch (IncompatibleSeismograms e) {
        //
        //        } // end of try-catch
        
    }
    
    Map configParams;
    
    class DataSetRecFuncProcessor implements SeisDataChangeListener {
        DataSetRecFuncProcessor(DataSetSeismogram[] seis,
                                EventAccessOperations event,
                                BasicSeismogramDisplay display)
            throws NoPreferredOrigin{
            this.seis = seis;
            this.event = event;
            this.origin = event.get_preferred_origin();
            this.display = display;
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
            // must be no errors
            LocalSeismogramImpl n = null, e = null, z = null;
            for (int i=0; i<localSeis.length; i++) {
                if (localSeis[i].channel_id.channel_code.endsWith("N")) {
                    n = localSeis[i];
                } else if (localSeis[i].channel_id.channel_code.endsWith("E")) {
                    e = localSeis[i];
                }if (localSeis[i].channel_id.channel_code.endsWith("Z")) {
                    z = localSeis[i];
                }
            }
            if (n == null || e == null || z == null) {
                logger.error("problem one seismogram component is null ");
                return;
            }
            
            
            Channel chan =
                seis[0].getDataSet().getChannel(seis[0].getRequestFilter().channel_id);
            Location staLoc = chan.my_site.my_station.my_location;
            Location evtLoc = origin.my_location;
            
            PhaseCut phaseCut;
            try {
                phaseCut =
                    new PhaseCut(CommonAccess.getCommonAccess().getTravelTimeCalc(),
                                 "P", new TimeInterval(-30, UnitImpl.SECOND),
                                 "P", new TimeInterval(30, UnitImpl.SECOND));
                
                n = phaseCut.cut(staLoc, origin, n);
                e = phaseCut.cut(staLoc, origin, e);
                z = phaseCut.cut(staLoc, origin, z);
                
                DistAz distAz = new DistAz(staLoc.latitude, staLoc.longitude,
                                           evtLoc.latitude, evtLoc.longitude);
                float[][] rotated = Rotate.rotate(e, n, (180+distAz.baz)*Math.PI/180);
                
                // check lengths, trim if needed???
                float[] zdata = z.get_as_floats();
                if (rotated[0].length != zdata.length) {
                    logger.error("data is not of same length "+
                                     rotated[0].length+" "+zdata.length);
                    return;
                }
                zdata = decon.makePowerTwo(zdata);
                rotated[0] = decon.makePowerTwo(rotated[0]);
                rotated[1] = decon.makePowerTwo(rotated[1]);
                
                SamplingImpl samp = SamplingImpl.createSamplingImpl(z.sampling_info);
                double period = samp.getPeriod().convertTo(UnitImpl.SECOND).getValue();
                IterDeconResult ans = decon.process(rotated[0],
                                                    zdata,
                                                        (float)period);
                float[] predicted = ans.getPredicted();
                
                Arrival[] pPhases = CommonAccess.getCommonAccess().getTravelTimes(evtLoc, staLoc, "ttp");
                MicroSecondDate firstP = new MicroSecondDate(origin.origin_time);
                logger.debug("origin "+firstP);
                firstP = firstP.add(new TimeInterval(pPhases[0].getTime(), UnitImpl.SECOND));
                logger.debug("firstP "+firstP);
                //TimeInterval shift = firstP.subtract(z.getBeginTime());
                //shift = (TimeInterval)shift.convertTo(UnitImpl.SECOND);
                TimeInterval shift = new TimeInterval(10, UnitImpl.SECOND);
                
                predicted = decon.phaseShift(predicted,
                                                 (float)shift.getValue(),
                                                 (float)period);
                
                logger.info("Finished with receiver function processing");
                logger.debug("rec func begin "+firstP.subtract(shift));
                LocalSeismogramImpl predSeis =
                    new LocalSeismogramImpl("recFunc/"+z.get_id(),
                                            firstP.subtract(shift).getFissuresTime(),
                                            predicted.length,
                                            z.sampling_info,
                                            z.y_unit,
                                            z.channel_id,
                                            predicted);
                predSeis.setName("receiver function "+z.channel_id.station_code);
                DataSetSeismogram predDSS =
                    new MemoryDataSetSeismogram(predSeis,
                                                "receiver function");
                AuditInfo[] audit = new AuditInfo[1];
                audit[0] =
                    new AuditInfo("Calculated receiver function",
                                  System.getProperty("user.name"));
                sdce.getSource().getDataSet().addDataSetSeismogram(predDSS,
                                                                   audit);
                
            } catch (ConfigurationException ce) {
                CommonAccess.getCommonAccess().handleException("Unable to get travel time calculator", ce);
            } catch (Exception ee) {
                logger.warn("Problem shifting receiver function to align P wave", ee);
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
        
        DataSetSeismogram[] seis;
        
        EventAccessOperations event;
        
        Origin origin;
        
        BasicSeismogramDisplay display;
        
        boolean[] finished;
        
        LocalSeismogramImpl[] localSeis;
        
        SeisDataErrorEvent error = null;
        
    }
    
    IterDecon decon;
    
    Logger logger = Logger.getLogger(RecFuncTask.class);
    
}// RecFuncTask
