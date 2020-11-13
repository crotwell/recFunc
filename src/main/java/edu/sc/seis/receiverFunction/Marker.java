package edu.sc.seis.receiverFunction;

import java.awt.Color;
import java.text.DecimalFormat;

import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.sod.model.common.QuantityImpl;


/**
 * @author crotwell
 * Created on Mar 2, 2005
 */
public class Marker {
    public Marker(StationResult result, Color color) {
        this.result = result;
        this.color = color;
    }
    public Color getColor() {
        return color;
    }
    public QuantityImpl getDepth() {
        return result.getH();
    }
    public String getName() {
        return result.getRef().getName();
    }
    public double getVpvs() {
        return result.getVpVs();
    }
    public String formatDepth() {
        return depthFormat.format(getDepth());
    }
    public String formatVpvs() {
        return vpvsFormat.format(getVpvs());
    }
    private StationResult result;
    private Color color;
    
    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");
    private static DecimalFormat depthFormat = new DecimalFormat("0.##");
}
