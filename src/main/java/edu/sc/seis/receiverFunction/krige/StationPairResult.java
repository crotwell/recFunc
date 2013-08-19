package edu.sc.seis.receiverFunction.krige;

import java.util.Map;

import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.receiverFunction.SumHKStack;


public class StationPairResult {
    
    public StationPairResult(SumHKStack a, SumHKStack b, Map<SumHKStack, StationImpl> staMap) {
        this.a = a;
        
        this.b = b;
        this.dist = new DistAz(staMap.get(a).getLocation(), staMap.get(b).getLocation()).getDelta();
    }
    
    public float getDeltaCrustalThickness() {
        return Math.abs(a.getBest().getHkm()-b.getBest().getHkm());
    }
    
    double dist;
    SumHKStack a;
    SumHKStack b;
    
    public double getDist() {
        return dist;
    }

    
    public SumHKStack getA() {
        return a;
    }

    
    public SumHKStack getB() {
        return b;
    }
}
