package edu.sc.seis.receiverFunction.web;

import edu.iris.Fissures.model.MicroSecondDate;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.SodConfigNotFound;
import edu.sc.seis.sod.status.FissuresFormatter;


/**
 * @author crotwell
 * Created on Feb 24, 2005
 */
public class VelocityCachedResult {

    /**
     *
     */
    public VelocityCachedResult(CachedResult result) {
        this.result = result;
    }

    public String getradialMatch() {
        return ""+result.radialMatch;
    }
    
    public String getradialBump() {
        return ""+result.radialBump;
    }
    
    public String gettransverseMatch() {
        return ""+result.transverseMatch;
    }
    
    public String gettransverseBump() {
        return ""+result.transverseBump;
    }
    
    public String getsodConfigId() {
        return ""+result.sodConfigId;
    }
    
    public String getinsertTime() {
        return FissuresFormatter.formatDate(new MicroSecondDate(result.insertTime));
    }
    
    public String getgwidth() {
        return ""+result.config.gwidth;
    }
    
    public String getmaxBumps() {
        return ""+result.config.maxBumps;
    }
    
    public String gettol() {
        return ""+result.config.tol;
    }
    
    CachedResult result;
}
