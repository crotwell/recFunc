package edu.sc.seis.receiverFunction;

import java.awt.Color;
import java.text.DecimalFormat;


/**
 * @author crotwell
 * Created on Mar 2, 2005
 */
public class Marker {
    public Marker(String name, double vpvs, double depth, Color color) {
        this.name = name;
        this.vpvs = vpvs;
        this.depth = depth;
        this.color = color;
    }
    public Color getColor() {
        return color;
    }
    public double getDepth() {
        return depth;
    }
    public String getName() {
        return name;
    }
    public double getVpvs() {
        return vpvs;
    }
    public String formatDepth() {
        return depthFormat.format(getDepth());
    }
    public String formatVpvs() {
        return vpvsFormat.format(getVpvs());
    }
    private String name;
    private double vpvs;
    private double depth;
    private Color color;
    
    private static DecimalFormat vpvsFormat = new DecimalFormat("0.00");
    private static DecimalFormat depthFormat = new DecimalFormat("0.##");
}
