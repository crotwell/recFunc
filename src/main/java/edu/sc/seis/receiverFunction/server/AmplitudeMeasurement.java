package edu.sc.seis.receiverFunction.server;


public class AmplitudeMeasurement {

    float ampP;
    float ampPs;
    float ampPpPs;
    float ampPsPs;
    
    public AmplitudeMeasurement(float ampP, float ampPs, float ampPpPs, float ampPsPs) {
        this.ampP = ampP;
        this.ampPs = ampPs;
        this.ampPpPs = ampPpPs;
        this.ampPsPs = ampPsPs;
    }

    public float getAmpP() {
        return ampP;
    }

    
    public float getAmpPpPs() {
        return ampPpPs;
    }

    
    public float getAmpPs() {
        return ampPs;
    }

    
    public float getAmpPsPs() {
        return ampPsPs;
    }
    
    
}
