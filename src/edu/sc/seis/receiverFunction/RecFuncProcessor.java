/**
 * RecFuncProcessor.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.Orientation;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.display.DisplayUtils;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.fissuresUtil.xml.DataSet;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.fissuresUtil.xml.URLDataSetSeismogram;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.LocalSeismogramProcess;
import edu.sc.seis.sod.subsetter.waveFormArm.SacFileProcessor;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

public class RecFuncProcessor extends SacFileProcessor implements LocalSeismogramProcess {

    public RecFuncProcessor(Element config)  throws ConfigurationException {
        super(config);
    }

    /**
     * Processes localSeismograms to calculate receiver functions.
     *
     * @param event an <code>EventAccessOperations</code> value
     * @param network a <code>NetworkAccess</code> value
     * @param channel a <code>Channel</code> value
     * @param original a <code>RequestFilter[]</code> value
     * @param available a <code>RequestFilter[]</code> value
     * @param seismograms a <code>LocalSeismogram[]</code> value
     * @param cookies a <code>CookieJar</code> value
     * @exception Exception if an error occurs
     */
    public LocalSeismogram[] process(EventAccessOperations event,
                                     NetworkAccess network,
                                     Channel channel,
                                     RequestFilter[] original,
                                     RequestFilter[] available,
                                     LocalSeismogram[] seismograms,
                                     CookieJar cookies) throws Exception {
        // save original seismograms
        super.process(event,
                      network,
                      channel,
                      original,
                      available,
                      seismograms,
                      cookies);
        if (recFunc == null) {
            float gwidth = 3.0f;
            tauPTime = new TauP_Time("iasp91");
            recFunc = new RecFunc(tauPTime,
                                  new IterDecon(200, true, .001f, gwidth));
        }
        if ( ! ChannelIdUtil.areEqual(available[0].channel_id, seismograms[0].channel_id)) {
            // fix the dumb -farm or -spyder on pond available_data
            available[0].channel_id = seismograms[0].channel_id;
        }

        DataSet dataset = getDataSet(event);
        DataSetSeismogram[] chGrpSeismograms =
            DisplayUtils.getComponents(dataset, available[0]);

        if (chGrpSeismograms.length < 3) {
            logger.debug("chGrpSeismograms.length = "+chGrpSeismograms.length);
            // must not be all here yet
            return seismograms;
        }

        logger.info("RecFunc for "+ChannelIdUtil.toStringNoDates(channel.get_id()));
        for (int i=0; i<chGrpSeismograms.length; i++) {
            if (chGrpSeismograms[i] == null) {
                // must not be all here yet
                System.out.println("chGrpSeismograms["+i+"] is null");
                return seismograms;
            }
        }

        processor =
            new DataSetRecFuncProcessor(chGrpSeismograms,
                                        event,
                                        recFunc);
        for (int i=0; i<chGrpSeismograms.length; i++) {
            logger.debug("Retrieving for "+chGrpSeismograms[i].getName());
            chGrpSeismograms[i].retrieveData(processor);
        }
        while ( ! processor.isRecFuncFinished()) {
            try {
                //System.out.println("Sleeping "+ChannelIdUtil.toStringNoDates(channel.get_id()));

                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        if (processor.getError() == null) {
            if (processor.getPredicted() != null) {
                for (int i = 0; i < processor.getPredicted().length; i++) {
                    MemoryDataSetSeismogram predicted = processor.getPredicted()[i];
                    dataset.remove(predicted); // to avoid duplicates
                    Channel recFuncChannel = new ChannelImpl(predicted.getRequestFilter().channel_id,
                                                             "receiver function fake channel for "+ChannelIdUtil.toStringNoDates(channel.get_id()),
                                                             new Orientation(0, 0),
                                                             channel.sampling_info,
                                                             channel.effective_time,
                                                             channel.my_site);

                    URLDataSetSeismogram saved =
                        saveInDataSet(event, recFuncChannel, predicted.getCache());
                    Collection aux = predicted.getAuxillaryDataKeys();
                    Iterator it = aux.iterator();
                    while (it.hasNext()) {
                        Object key = it.next();
                        saved.addAuxillaryData(key, predicted.getAuxillaryData(key));
                    }
                    DistAz distAz = DisplayUtils.calculateDistAz(saved);
                    tauPTime.calculate(distAz.delta);
                    Arrival arrival = tauPTime.getArrival(0);
                    // convert radian per sec ray param into km per sec
                    float kmRayParam = (float)(arrival.getRayParam()/tauPTime.getTauModel().getRadiusOfEarth());
                    HKStack stack = new HKStack(6.5f,
                                                kmRayParam,
                                                5, .25f, 240,
                                                1.6f, .0025f, 200,
                                                saved);

                    File outImageFile  = new File(getEventDirectory(event),"stack_"+ChannelIdUtil.toStringNoDates(predicted.getRequestFilter().channel_id)+".png");
                    BufferedImage bufImage = createStackImage(stack);
                    javax.imageio.ImageIO.write(bufImage, "png", outImageFile);
                }
            } else {
                logger.error("problem with recfunc: predicted is null");
            }
        } else {
            // problem occurred
            SeisDataErrorEvent error = processor.getError();
            logger.error("problem with recfunc:", error.getCausalException());
        }
        System.out.println("Done with "+ChannelIdUtil.toStringNoDates(channel.get_id()));
        return seismograms;
    }

    int makeImageable(float min, float max, float val) {
        float absMax = Math.max(Math.abs(min), Math.abs(max));
        return (int)SimplePlotUtil.linearInterp(-1*absMax, 0, absMax, 255, val);
    }

    boolean isDataComplete(LocalSeismogram seis) {
        return processor.isRecFuncFinished();
    }

    public BufferedImage createStackImage(HKStack stack) {
        float[][] stackOut = stack.getStack();
        int dataH = 2*stackOut.length;
        int dataW = 2*stackOut[0].length;
        int fullWidth = dataW+40;
        int fullHeight = dataH+140;
        BufferedImage bufImage = new BufferedImage(fullWidth,
                                                   fullHeight,
                                                   BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bufImage.createGraphics();
        g.setColor(Color.darkGray);
        g.fillRect(0, 0, bufImage.getWidth(), bufImage.getHeight());
        g.translate(0, 5);
        FontMetrics fm = g.getFontMetrics();

        String title = ChannelIdUtil.toStringNoDates(stack.getRecFunc().getRequestFilter().channel_id);
        g.setColor(Color.white);
        g.drawString(title, (fullWidth-fm.stringWidth(title))/2, fm.getHeight());

        g.translate(5, fm.getHeight()+fm.getDescent());

        float min = stackOut[0][0];
        int maxIndexX = 0;
        int maxIndexY = 0;
        int minIndexX = 0;
        int minIndexY = 0;
        float max = min;

        for (int j = 0; j < stackOut.length; j++) {
            for (int k = 0; k < stackOut[j].length; k++) {
                if (stackOut[j][k] < min) {
                    min = stackOut[j][k];
                    minIndexX = k;
                    minIndexY = j;
                }
                if (stackOut[j][k] > max) {
                    max = stackOut[j][k];
                    maxIndexX = k;
                    maxIndexY = j;
                }
            }
        }

        for (int j = 0; j < stackOut.length; j++) {
            //System.out.print(j+" : ");
            for (int k = 0; k < stackOut[j].length; k++) {
                int colorVal = makeImageable(min, max, stackOut[j][k]);
                g.setColor(new Color(colorVal, colorVal, colorVal));
                g.fillRect( 2*k, 2*j, 2, 2);
                //System.out.print(colorVal+" ");
            }
            //System.out.println("");
        }
        g.translate(0, dataH);

        g.setColor(Color.white);
        g.drawString("Min H="+(stack.getMinH()+minIndexY*stack.getStepH()), 0, fm.getHeight());
        g.drawString("    K="+(stack.getMinK()+minIndexX*stack.getStepK()), 0, 2*fm.getHeight());
        g.translate(0, 2*fm.getHeight());
        g.drawString("Max H="+(stack.getMinH()+maxIndexY*stack.getStepH()), 0, fm.getHeight());
        g.drawString("    K="+(stack.getMinK()+maxIndexX*stack.getStepK()), 0, 2*fm.getHeight());
        g.translate(0, 2*fm.getHeight());

        int minColor = makeImageable(min, max, min);
        g.setColor(new Color(minColor, minColor, minColor));
        g.fillRect(0, 0, 15, 15);
        g.setColor(Color.white);
        g.drawString("Min="+min, 0, 15+fm.getHeight());

        int maxColor = makeImageable(min, max, max);
        g.setColor(new Color(maxColor, maxColor, maxColor));
        g.fillRect(dataW-20, 0, 15, 15);
        g.setColor(Color.white);
        String maxString = "Max="+max;
        int stringWidth = fm.stringWidth(maxString);
        g.drawString(maxString, dataW-5-stringWidth, 15+fm.getHeight());

        g.dispose();
        return bufImage;
    }

    RecFunc recFunc;
    DataSetRecFuncProcessor processor;
    TauP_Time tauPTime;

    private static Logger logger = Logger.getLogger(RecFuncProcessor.class);

}


