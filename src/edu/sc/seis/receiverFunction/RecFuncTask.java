package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.display.BasicSeismogramDisplay;
import edu.sc.seis.fissuresUtil.display.DisplayUtils;
import edu.sc.seis.fissuresUtil.display.mouse.SDMouseAdapter;
import edu.sc.seis.fissuresUtil.display.mouse.SDMouseEvent;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.xml.DataSet;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.gee.configurator.ConfigurationException;
import edu.sc.seis.gee.task.GUITask;
import edu.sc.seis.gee.task.GlobalToolBar;
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

public class RecFuncTask  extends SDMouseAdapter implements GUITask {
    public RecFuncTask (){

    }

    public void configure(java.util.Map params) throws ConfigurationException {
        configParams = params;
        float gwidth = 3.0f;
        try {
            taupUtil = new TauPUtil("iasp91");
            recFunc = new RecFunc(taupUtil,
                new IterDecon(100, true, .001f, gwidth));
        } catch (TauModelException e) {
            throw new ConfigurationException("Problem with TauP", e);
        }
    }

    public void invoke() {
        GlobalToolBar.add(this);
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

    public void mouseClicked(SDMouseEvent me){
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
                    DisplayUtils.getComponents(seis);
                DataSetRecFuncProcessor processor =
                    new DataSetRecFuncProcessor(chGrpSeismograms,
                                                currEvent,
                                                recFunc,
                                                taupUtil
                                               );
                for (int i=0; i<chGrpSeismograms.length; i++) {
                    logger.debug("Retrieveing for "+chGrpSeismograms[i].getName());
                    chGrpSeismograms[i].retrieveData(processor);
                }
            }
        } catch(NoPreferredOrigin e) {
            GlobalExceptionHandler.handle(e);
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

    TauPUtil taupUtil;

    IterDecon decon;

    RecFunc recFunc;

    Logger logger = Logger.getLogger(RecFuncTask.class);

}// RecFuncTask
