/**
 * Crust2Profile.java
 *
 * @author Philip Crotwell
 */

package edu.sc.seis.receiverFunction.crust2;

import edu.sc.seis.TauP.VelocityLayer;

public class Crust2Profile {

    public Crust2Profile(String code, String name, VelocityLayer[] layers) {
        if (layers.length != 8) {
            throw new IllegalArgumentException("profiles in crust2.0 have exactly 8 layers");
        }
        this.layers = layers;
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public VelocityLayer getLayer(int i) {
        return layers[i];
    }

    String name;
    String code;
    VelocityLayer[] layers;

}

