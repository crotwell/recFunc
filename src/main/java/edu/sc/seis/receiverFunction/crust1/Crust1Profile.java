package edu.sc.seis.receiverFunction.crust1;

import java.text.DecimalFormat;

import edu.sc.seis.TauP.VelocityLayer;
import edu.sc.seis.sod.model.common.QuantityImpl;
import edu.sc.seis.sod.model.common.UnitImpl;

public class Crust1Profile {

    public Crust1Profile(float lat, float lon, VelocityLayer[] layers) {
        if (layers.length != 8) {
            throw new IllegalArgumentException("profiles in crust1.0 have exactly 8 layers");
        }
        this.layers = layers;
        this.lat = lat;
        this.lon = lon;
    }

    public VelocityLayer getLayer(int i) {
        return layers[i];
    }

    /** Calculated for vertical incidence P wave, assumes layers are constant velocity
     * and ignores the last layer (halfspace). */
    public double getPWaveAvgVelocity() {
        double travelTime = 0;
        for (int i = 0; i < layers.length-1; i++) {
            travelTime += layers[i].getBotPVelocity()*(layers[i].getBotDepth()-layers[i].getTopDepth());
        }
        return travelTime/layers[layers.length-1].getTopDepth();
    }
    
    public String formatPWaveAvgVelocity() {
        return formatter.format(getPWaveAvgVelocity());
    }

    /** Calculated for vertical incidence S wave, assumes layers are constant velocity
     * and ignores the last layer (halfspace). */
    public double getSWaveAvgVelocity() {
        double travelTime = 0;
        for (int i = 0; i < layers.length-1; i++) {
            travelTime += layers[i].getBotSVelocity()*(layers[i].getBotDepth()-layers[i].getTopDepth());
        }
        return travelTime/layers[layers.length-1].getTopDepth();
    }
    
    public String formatSWaveAvgVelocity() {
        return formatter.format(getSWaveAvgVelocity());
    }
    
    public QuantityImpl getCrustThickness() {
        QuantityImpl q = new QuantityImpl(layers[7].getTopDepth(), UnitImpl.KILOMETER);
        q.setFormat(formatter);
        return q;
    }
    
    public double getVpVs() {
        return getPWaveAvgVelocity()/getSWaveAvgVelocity();
    }
    
    public String formatVpVs() {
        return formatter.format(getVpVs());
    }

    float lat;
    float lon;
    VelocityLayer[] layers;
    
    DecimalFormat formatter = new DecimalFormat("0.##");

}

