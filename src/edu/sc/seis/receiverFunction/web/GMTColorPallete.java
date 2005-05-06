package edu.sc.seis.receiverFunction.web;

import java.awt.Color;

/**
 * @author crotwell Created on May 5, 2005
 */
public class GMTColorPallete {

    /**
     *
     */
    public GMTColorPallete(ColorRange[] range) {
        this.range = range;
    }
    
    public Color getColor(double val) {
        for(int i = 0; i < range.length; i++) {
            if (range[i].isInRange(val)) {
                return range[i].getColor(val);
            }
        }
        if (val < range[0].min) { return smallColor; }
        if (val > range[range.length-1].max) { return largeColor; }
        // assume NaN
        return NaNColor;
    }
    
    public static GMTColorPallete getDefault(double min, double max) {
        return new GMTColorPallete(new ColorRange[] { new ColorRange(min, Color.red, max, Color.BLUE) });
    }

    ColorRange[] range;
    Color smallColor = Color.CYAN, largeColor=Color.MAGENTA, NaNColor=Color.BLACK;
    
    static class ColorRange {

        ColorRange(double min, Color minColor, double max, Color maxColor) {
            this.min = min;
            this.minColor = minColor;
            this.max = max;
            this.maxColor = maxColor;
        }

        boolean isInRange(double val) {
            return (min <= val && val < max);
        }

        Color getColor(double val) {
            logger.debug("get for "+val+"  "+linearInterp(min, minColor.getRed(), max, maxColor.getRed(), val)+", "+
                             linearInterp(min, minColor.getGreen(), max, maxColor.getGreen(), val)+", "+
                             linearInterp(min, minColor.getBlue(), max, maxColor.getBlue(), val));
            return new Color(linearInterp(min, minColor.getRed(), max, maxColor.getRed(), val)/256f,
                             linearInterp(min, minColor.getGreen(), max, maxColor.getGreen(), val)/256f,
                             linearInterp(min, minColor.getBlue(), max, maxColor.getBlue(), val)/256f);
        }

        public float linearInterp(double Xa,
                                   double Ya,
                                   double Xb,
                                   double Yb,
                                   double val) {
            if (val == Ya) {
                return (float)Xa;
            }
            return (float)(Ya + (Yb - Ya) * (val - Xa) / (Xb - Xa));
        }

        double min, max;

        Color minColor, maxColor;
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GMTColorPallete.class);
}