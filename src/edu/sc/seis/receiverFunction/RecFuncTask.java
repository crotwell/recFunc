package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.bag.Rotate;
import edu.sc.seis.fissuresUtil.chooser.DataSetChannelGrouper;
import edu.sc.seis.fissuresUtil.display.BasicSeismogramDisplay;
import edu.sc.seis.fissuresUtil.xml.DataSet;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.vsnexplorer.CommonAccess;
import edu.sc.seis.vsnexplorer.configurator.ConfigurationException;
import edu.sc.seis.vsnexplorer.task.GlobalToolBar;
import edu.sc.seis.vsnexplorer.task.Task;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
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

public class RecFuncTask  extends MouseAdapter implements Task {
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
                    new DataSetRecFuncProcessor(chGrpSeismograms, currEvent);
                for (int i=0; i<chGrpSeismograms.length; i++) {
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
                                EventAccessOperations event)
            throws NoPreferredOrigin{
            this.seis = seis;
            this.event = event;
            this.origin = event.get_preferred_origin();
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
            DistAz distAz = new DistAz(staLoc.latitude, staLoc.longitude,
                                       evtLoc.latitude, evtLoc.longitude);
            float[][] rotated = Rotate.rotate(e, n, (180+distAz.baz)*Math.PI/180);
            SamplingImpl samp = SamplingImpl.createSamplingImpl(z.sampling_info);
            double period = samp.getPeriod().convertTo(UnitImpl.SECOND).getValue();
            IterDeconResult ans = decon.process(rotated[0],
                                                z.get_as_floats(),
                                                    (float)period);
            float[] predicted = ans.getPredicted();
            logger.info("Finished with receiver funciton processing");
        }
        
        /**
         * Method pushData
         *
         * @param    sdce                a  SeisDataChangeEvent
         *
         */
        public void pushData(SeisDataChangeEvent sdce) {
            localSeis[getIndex(sdce.getSource())] = sdce.getSeismograms()[0];
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
        
        boolean[] finished;
        
        LocalSeismogramImpl[] localSeis;
        
        SeisDataErrorEvent error = null;
        
    }
    
    IterDecon decon;
    
    Logger logger = Logger.getLogger(RecFuncTask.class);
    
}// RecFuncTask
