package edu.sc.seis.receiverFunction.web;

import java.awt.Paint;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYZDataset;


/**
 * @author crotwell
 * Created on May 4, 2005
 */
public class ZColorXYDotRenderer extends StandardXYItemRenderer {

    /**
     *
     */
    public ZColorXYDotRenderer(XYZDataset dataset, GMTColorPallete colors) {
        super(StandardXYItemRenderer.SHAPES);
        this.dataset = dataset;
        this.colormap = colors;
    }
    
    public Paint getItemPaint(int series, int item) {
        logger.debug("getItemPaint "+item+"  color="+colormap.getColor(dataset.getZValue(series, item))+"  val="+dataset.getZValue(series, item));
        return colormap.getColor(dataset.getZValue(series, item));
    }
    
    XYZDataset dataset;
    
    GMTColorPallete colormap;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ZColorXYDotRenderer.class);
}
