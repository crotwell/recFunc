package edu.sc.seis.vsnexplorer.task;

import edu.sc.seis.vsnexplorer.*;
import edu.sc.seis.vsnexplorer.task.dataSetBrowser.*;
import edu.sc.seis.vsnexplorer.configurator.*;
import java.util.Map;

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
	this.configParams = params;
	if( params.containsKey("dataSetBrowser") ) {

	    TaskAction taskAction = 
		(TaskAction) CommonAccess.getCommonAccess().getTaskAction((String)params.get("dataSetBrowser"));
	    dataSetBrowser = (DataSetBrowser) taskAction.getTask();
	} else dataSetBrowser = null;
    }

    public void invoke() {

    }

    Map configParams;

    DataSetBrowser dataSetBrowser;

}// RecFuncTask
