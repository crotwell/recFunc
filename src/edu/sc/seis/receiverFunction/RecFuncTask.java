package edu.sc.seis.vsnexplorer.task;

import edu.sc.seis.vsnexplorer.*;
import edu.sc.seis.vsnexplorer.task.dataSetBrowser.*;
import edu.sc.seis.vsnexplorer.configurator.*;
import edu.sc.seis.fissuresUtil.display.*;
import edu.sc.seis.fissuresUtil.chooser.*;
import edu.sc.seis.fissuresUtil.xml.*;
import edu.iris.Fissures.IfEvent.*;
import edu.iris.Fissures.IfNetwork.*;
import edu.iris.Fissures.seismogramDC.*;
import edu.sc.seis.fissuresUtil.bag.*;
import java.util.*;

/**
 * RecFuncTask.java
 *
 *
 * Created: Wed Jul  3 21:22:35 2002
 *
 * @author <a href="mailto:">Philip Crotwell</a>
 * @version
 */

public class RecFuncTask {
    public RecFuncTask (){
	
    }
    
    public void configure(java.util.Map params) throws ConfigurationException {
	configParams = params;

	if(params.containsKey("colSeisDisplay")){
	    String colSeisId = (String)params.get("colSeisDisplay");
	    colSeis = 
		((ColumnSeismogramTask)CommonAccess.getCommonAccess().getTaskAction(colSeisId).getTask());
	}
    }

    public void invoke() {
	if (colSeis != null) {
	    HashSet stations = new HashSet();
	    HashMap events = new HashMap();
	    LinkedList displays = 
		colSeis.getDisplays();
	    Iterator it = displays.iterator();
	    while (it.hasNext()) {
		BasicSeismogramDisplay currDisp = 
		    ((BasicSeismogramDisplay)it.next());
		DataSetSeismogram[] seismograms = 
		    currDisp.getSeismograms();
		for (int i=0; i<seismograms.length; i++) {
		     
		    DataSetSeismogram currDS = seismograms[i];
		    XMLDataSet dataSet =(XMLDataSet)currDS.getDataSet();
		    EventAccessOperations currEvent = dataSet.getEvent();
		    LocalSeismogramImpl seis = currDS.getSeismogram();
		    ChannelId[] channelGroup = 
			DataSetChannelGrouper.retrieveGrouping(dataSet, 
							       seis.getChannelID());
		    DataSetSeismogram[] chGrpSeismograms = new DataSetSeismogram[3];
		    for(int counter = 0; counter < channelGroup.length; counter++) {
			String name =
			    DisplayUtils.getSeismogramName(channelGroup[counter], 
							   dataSet, 
	new edu.iris.Fissures.TimeRange(seis.getBeginTime().getFissuresTime(), 
					seis.getEndTime().getFissuresTime()));
			chGrpSeismograms[counter] = new DataSetSeismogram(dataSet.getSeismogram(name), dataSet);
		    }

		    try {
			 
		    LocalSeismogramImpl[] tmpSeis = new LocalSeismogramImpl[3];
		    tmpSeis[0] = chGrpSeismograms[0].getSeismogram();
		    tmpSeis[1] = chGrpSeismograms[1].getSeismogram();
		    tmpSeis[2] = chGrpSeismograms[2].getSeismogram();
		    LocalMotionVectorImpl motionVec = 
			MotionVectorUtil.create(tmpSeis);
		    } catch (IncompatibleSeismograms e) {
			
		    } // end of try-catch
		    
		    
		} // end of for (int i=0; i<seismograms.length; i++)
		
	    }
	}
    }

    Map configParams;

    DataSetBrowser dataSetBrowser;

    ColumnSeismogramTask colSeis;

}// RecFuncTask
