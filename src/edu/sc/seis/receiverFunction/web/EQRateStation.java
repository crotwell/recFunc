package edu.sc.seis.receiverFunction.web;

import java.text.DecimalFormat;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.sod.velocity.network.VelocityStation;


public class EQRateStation {
    EQRateStation(VelocityStation sta, MicroSecondDate begin, MicroSecondDate end, int numSucc, int numUnsucc) {
        this.sta = sta;
        this.begin = begin;
        this.end = end;
        this.numSucc = numSucc;
        this.numUnsucc = numUnsucc;
    }
    
    VelocityStation sta;
    MicroSecondDate begin;
    MicroSecondDate end;
    int numSucc;
    int numUnsucc;
    
    public MicroSecondDate getBegin() {
        return begin;
    }
    
    public MicroSecondDate getEnd() {
        return end;
    }
    
    public MicroSecondDate getEndOrNow() {
        MicroSecondDate now = ClockUtil.now();
        if (end.after(now)) {return now; } else {return end;}
    }
    
    public int getNumSucc() {
        return numSucc;
    }
    
    public int getNumUnsucc() {
        return numUnsucc;
    }

    public int getNumTotal() {
        return numUnsucc+numSucc;
    }
    
    public Station getSta() {
        return sta;
    }
    
    public QuantityImpl successPeriod() {
        return getEndOrNow().subtract(getBegin()).divideBy(getNumSucc()).convertTo(UnitImpl.DAY);
    }
    
    public QuantityImpl formatSuccessPeriod() {
        QuantityImpl q = successPeriod();
        q.setFormat(format);
        return q;
    }
    
    public QuantityImpl eqPeriod() {
       return getEndOrNow().subtract(getBegin()).divideBy(getNumTotal()).convertTo(UnitImpl.DAY);
    }
    
    public QuantityImpl formatEqPeriod() {
        QuantityImpl q = eqPeriod();
        q.setFormat(format);
        return q;
    }
    
    public float getPeriodRatio() {
        return (float)((QuantityImpl)eqPeriod().divideBy(successPeriod())).getValue(DAY_PER_DAY);
    }
    
    public String formatPeriodRatio() {
        return format.format(getPeriodRatio());
    }
    
    public String toString() {
        return sta.getCode()+" "+getNumSucc()+"  u="+getNumUnsucc();
    }
    
    static DecimalFormat format = new DecimalFormat("#.0");
    
    static UnitImpl DAY_PER_DAY = UnitImpl.divide(UnitImpl.DAY, UnitImpl.DAY);
}
