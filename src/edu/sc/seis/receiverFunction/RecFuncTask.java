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
        recFunc = new RecFunc(CommonAccess.getCommonAccess().getTravelTimeCalc(),
                              new IterDecon(100, true, .001f, gwidth));
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
                DataSetSeismogram[] chGrpSeismograms =
                    DataSetChannelGrouper.retrieveSeismogramGrouping(dataSet,
                                                                     seis);
                
                DataSetRecFuncProcessor processor =
                    new DataSetRecFuncProcessor(chGrpSeismograms,
                                                currEvent,
                                                recFunc);
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
    
    
    
    IterDecon decon;
    
    RecFunc recFunc;
    
    Logger logger = Logger.getLogger(RecFuncTask.class);
    
}// RecFuncTask
