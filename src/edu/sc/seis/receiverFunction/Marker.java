package edu.sc.seis.receiverFunction;

import java.awt.Color;


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
    private String name;
    private double vpvs;
    private double depth;
    private Color color;
}
