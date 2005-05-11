package edu.sc.seis.receiverFunction.web;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;

/**
 * Color pallette modelled on the cpt files of GMT.
 * 
 * @see http://gmt.soest.hawaii.edu/gmt/doc/html/GMT_Docs/node51.html
 * @author crotwell Created on May 5, 2005
 */
public class GMTColorPalette {

    public GMTColorPalette(ColorRange[] range) {
        this(range, DEF_SMALL_COLOR, DEF_LARGE_COLOR, DEF_NAN_COLOR);
    }

    public GMTColorPalette(ColorRange[] range, Color smallColor,
            Color largeColor, Color NaNColor) {
        this.range = range;
        this.smallColor = smallColor;
        this.largeColor = largeColor;
        this.nanColor = NaNColor;
    }
    
    public GMTColorPalette renormalize(double min, double max) {
    return renormalize(min, max, DEF_SMALL_COLOR, DEF_LARGE_COLOR, DEF_NAN_COLOR);
    }
    
    public GMTColorPalette renormalize(double min, double max, Color smallColor,
                                       Color largeColor, Color NaNColor) {
        ColorRange[] range = getColorRange();
        double oldMin = range[0].min;
        double oldMax = range[range.length-1].max;
        ColorRange[] outRange = new ColorRange[range.length];
        for(int i = 0; i < outRange.length; i++) {
            outRange[i] = new ColorRange(SimplePlotUtil.linearInterp(oldMin, min, oldMax, max, range[i].min), range[i].minColor,
                                         SimplePlotUtil.linearInterp(oldMin, min, oldMax, max, range[i].max), range[i].maxColor);
        }
        return new GMTColorPalette(outRange, smallColor, largeColor, nanColor);
    }
    
    public static GMTColorPalette load(Reader in) throws IOException {
        StreamTokenizer tokenIn = new StreamTokenizer(in);
        tokenIn.commentChar('#');         // '#' means ignore to end of line
        tokenIn.eolIsSignificant(true);   // end of line is important
        tokenIn.parseNumbers();           /* Differentiate between words and
                                             numbers. Note 1.1e3 is considered
                                             a string instead of a number.
                                           */
        ArrayList colorRanges = new ArrayList();
        Color smallColor = Color.CYAN;
        Color largeColor = Color.MAGENTA;
        Color nanColor = Color.BLACK;
        while (tokenIn.nextToken() != StreamTokenizer.TT_EOF ) {
            if (tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                // assume color range line
                colorRanges.add(readLine(tokenIn));
            } else if (tokenIn.ttype == StreamTokenizer.TT_WORD) {
                // must be one of F, B, N for colors out of range
                if (tokenIn.sval.equals("F")) {
                    largeColor = readColor(tokenIn);
                } else if (tokenIn.sval.equals("F")) {
                    largeColor = readColor(tokenIn);
                } else if (tokenIn.sval.equals("F")) {
                    largeColor = readColor(tokenIn);
                } else {
                    throw new IOException("Expected on of B, F, N but found "+tokenIn.sval);
                }
            } else if (tokenIn.ttype == StreamTokenizer.TT_EOL) {
                // ignore, more to next token
            }
            if ( ! (tokenIn.ttype != StreamTokenizer.TT_EOF || tokenIn.ttype == StreamTokenizer.TT_EOL)) {
                throw new IOException("Expected EOF or EOL but found "+(tokenIn.ttype==StreamTokenizer.TT_NUMBER?""+tokenIn.nval:tokenIn.sval));
            }
        }
        GMTColorPalette palette = new GMTColorPalette((ColorRange[])colorRanges.toArray(new ColorRange[0]), smallColor, largeColor, nanColor);
        return palette;
    }
    
    public static ColorRange readLine(StreamTokenizer tokenIn) throws IOException {
        double min = tokenIn.nval;
        tokenIn.nextToken();
        Color minColor = readColor(tokenIn);

        double max = tokenIn.nval;
        tokenIn.nextToken();
        Color maxColor = readColor(tokenIn);
        
        return new ColorRange(min, minColor, max, maxColor);
    }
    
    public static Color readColor(StreamTokenizer tokenIn) throws IOException {
        int red = (int)tokenIn.nval;
        tokenIn.nextToken();
        int green = (int)tokenIn.nval;
        tokenIn.nextToken();
        int blue = (int)tokenIn.nval;
        tokenIn.nextToken();
        return new Color(red, green, blue);
    }

    public ColorRange[] getColorRange() {
        return range;
    }

    public Color getSmallColor() {
        return smallColor;
    }

    public Color getLargeColor() {
        return largeColor;
    }

    public Color getNanColor() {
        return nanColor;
    }

    public Color getColor(double val) {
        for(int i = 0; i < range.length; i++) {
            if(range[i].isInRange(val)) { return range[i].getColor(val); }
        }
        // check equals top of last range, above does <=
        if (range[range.length-1].max == val) {
            return range[range.length-1].maxColor;
        }
        if(val < range[0].min) { return smallColor; }
        if(val > range[range.length - 1].max) { return largeColor; }
        // assume NaN, probably never happens...
        return nanColor;
    }

    public static GMTColorPalette getDefault(double min, double max) {
        return new GMTColorPalette(new ColorRange[] {new ColorRange(min,
                                                                    Color.RED,
                                                                    max,
                                                                    Color.BLUE)});
    }

    ColorRange[] range;

    Color smallColor, largeColor, nanColor;
    
    public static Color DEF_SMALL_COLOR = Color.CYAN, DEF_LARGE_COLOR = Color.MAGENTA,
    DEF_NAN_COLOR = Color.BLACK;

    public static class ColorRange {

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
            return new Color(linearInterp(min,
                                          minColor.getRed(),
                                          max,
                                          maxColor.getRed(),
                                          val) / 256f,
                             linearInterp(min,
                                          minColor.getGreen(),
                                          max,
                                          maxColor.getGreen(),
                                          val) / 256f,
                             linearInterp(min,
                                          minColor.getBlue(),
                                          max,
                                          maxColor.getBlue(),
                                          val) / 256f);
        }

        public float linearInterp(double Xa,
                                  double Ya,
                                  double Xb,
                                  double Yb,
                                  double val) {
            if(val == Ya) { return (float)Xa; }
            return (float)(Ya + (Yb - Ya) * (val - Xa) / (Xb - Xa));
        }

        double min, max;

        Color minColor, maxColor;
    }
}