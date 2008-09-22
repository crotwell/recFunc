package edu.sc.seis.receiverFunction.web;

import edu.iris.Fissures.model.MicroSecondDate;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.SodConfigNotFound;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.sod.status.FissuresFormatter;


/**
 * @author crotwell
 * Created on Feb 24, 2005
 */
public class VelocityCachedResult {

    /**
     *
     */
    public VelocityCachedResult(ReceiverFunctionResult result) {
        this.result = result;
    }

    public String getradialMatch() {
        return ""+result.getRadialMatch();
    }
    
    public String getradialBump() {
        return ""+result.getRadialBump();
    }
    
    public String gettransverseMatch() {
        return ""+result.getTransverseMatch();
    }
    
    public String gettransverseBump() {
        return ""+result.getTransverseBump();
    }
    
    public String getsodConfigId() {
        return ""+result.getSodConfig().getDbid();
    }
    
    public String getinsertTime() {
        return FissuresFormatter.formatDate(new MicroSecondDate(result.getInsertTime()));
    }
    
    public String getgwidth() {
        return ""+result.getGwidth();
    }
    
    public String getmaxBumps() {
        return ""+result.getMaxBumps();
    }
    
    public String gettol() {
        return ""+result.getTol();
    }
    
    ReceiverFunctionResult result;
}
