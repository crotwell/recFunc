package edu.sc.seis.receiverFunction.web;

import java.awt.Paint;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.XYZDataset;

/**
 * Colors the data points based on the Z value in the XYZDataset.
 * 
 * @author crotwell Created on May 4, 2005
 */
public class ZColorXYDotRenderer extends StandardXYItemRenderer {

    public ZColorXYDotRenderer(XYZDataset dataset, GMTColorPalette colors) {
        super(StandardXYItemRenderer.SHAPES);
        setConstructorValues(dataset, colors);
    }
    
    public ZColorXYDotRenderer(int type, XYZDataset dataset, GMTColorPalette colors) {
        super(type);
        setConstructorValues(dataset, colors);
    }

    public ZColorXYDotRenderer(int type, XYToolTipGenerator toolTipGenerator, XYZDataset dataset, GMTColorPalette colors) {
        super(type, toolTipGenerator);
        setConstructorValues(dataset, colors);
    }

    public ZColorXYDotRenderer(int type, XYToolTipGenerator toolTipGenerator,
            XYURLGenerator urlGenerator, XYZDataset dataset, GMTColorPalette colors) {
        super(type, toolTipGenerator, urlGenerator);
        setConstructorValues(dataset, colors);
    }

    private void setConstructorValues(XYZDataset dataset, GMTColorPalette colors) {
        this.dataset = dataset;
        this.colormap = colors;
    }
    
    public Paint getItemPaint(int series, int item) {
        return colormap.getColor(dataset.getZValue(series, item));
    }

    XYZDataset dataset;

    GMTColorPalette colormap;
}