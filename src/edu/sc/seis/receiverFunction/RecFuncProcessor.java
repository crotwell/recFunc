/**
 * RecFuncProcessor.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import edu.sc.seis.fissuresUtil.xml.*;

import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.Orientation;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.display.DisplayUtils;
import edu.sc.seis.fissuresUtil.display.SimplePlotUtil;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.process.waveFormArm.LocalSeismogramProcess;
import edu.sc.seis.sod.process.waveFormArm.SaveSeismogramToFile;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class RecFuncProcessor extends SaveSeismogramToFile implements LocalSeismogramProcess {

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
                System.out.println("Sleeping "+ChannelIdUtil.toStringNoDates(channel.get_id()));

                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        MicroSecondDate before = new MicroSecondDate();
        System.out.println("Before save rec func data");
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
                        saveInDataSet(event, recFuncChannel, predicted.getCache(), SeismogramFileTypes.SAC);
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
                                                HKStack.getPercentMatch(saved),
                                                5, .25f, 240,
                                                1.6f, .0025f, 200,
                                                saved);

                    String prefix = "HKstack_";
                    String postfix = ".raw";
                    String channelIdString = ChannelIdUtil.toStringNoDates(predicted.getRequestFilter().channel_id);
                    File stackOutFile = new File(getEventDirectory(event),prefix+channelIdString+postfix);
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(stackOutFile)));
                    stack.write(dos);
                    dos.close();

                    File outImageFile  = new File(getEventDirectory(event),prefix+channelIdString+".png");
                    BufferedImage bufImage = stack.createStackImage();
                    javax.imageio.ImageIO.write(bufImage, "png", outImageFile);

                    char quote = '"';
                    File outHtmlFile  = new File(getEventDirectory(event),prefix+channelIdString+".html");
                    BufferedWriter bw = new BufferedWriter(new FileWriter(outHtmlFile));
                    bw.write("<html>");bw.newLine();
                    bw.write("<head>");bw.newLine();
                    bw.write("<title>"+channelIdString+"</title>");bw.newLine();
                    bw.write("</head>");bw.newLine();
                    bw.write("<body>");bw.newLine();
                    bw.write("</br><pre>");bw.newLine();
                    stack.writeReport(bw);bw.newLine();
                    bw.write("</pre></br>");bw.newLine();
                    bw.write("<img src="+quote+outImageFile+quote+"/>");
                    bw.write("</body>");bw.newLine();
                    bw.write("</html>");bw.newLine();
                    bw.close();

                    appendToSummaryPage(getEventDirectory(event).getName()+" "+channelIdString+" "+stack.getPercentMatch());

                    // now update global per channel stack
                    SumHKStack sum = SumHKStack.load(getParentDirectory(),
                                                     predicted.getRequestFilter().channel_id,
                                                     prefix,
                                                     postfix,
                                                     90);
                    if (sum != null) {
                        // at least on event meet the min percent match
                        File sumStackOutFile = new File(getParentDirectory(),"SumHKStack_"+ChannelIdUtil.toStringNoDates(predicted.getRequestFilter().channel_id)+postfix);
                        if (sumStackOutFile.exists()) {sumStackOutFile.delete();}
                        DataOutputStream sumdos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(sumStackOutFile)));
                        sum.write(sumdos);
                        sumdos.close();

                        File outSumImageFile  = new File(getParentDirectory(),"SumHKStack_"+ChannelIdUtil.toStringNoDates(predicted.getRequestFilter().channel_id)+".png");
                        if (outSumImageFile.exists()) {outSumImageFile.delete();}
                        BufferedImage bufSumImage = sum.createStackImage();
                        javax.imageio.ImageIO.write(bufSumImage, "png", outSumImageFile);
                    }

                }
            } else {
                logger.error("problem with recfunc: predicted is null");
            }
            String[] names = dataset.getDataSetSeismogramNames();
            for (int i = 0; i < names.length; i++) {
                DataSetSeismogram dss = dataset.getDataSetSeismogram(names[i]);
                if (dss instanceof MemoryDataSetSeismogram) {
                    dataset.remove(dss); // avoid duplicates
                    Channel recFuncChannel = new ChannelImpl(dss.getRequestFilter().channel_id,
                                                             "receiver function fake channel for "+ChannelIdUtil.toStringNoDates(dss.getRequestFilter().channel_id),
                                                             new Orientation(0, 0),
                                                             channel.sampling_info,
                                                             channel.effective_time,
                                                             channel.my_site);
                    URLDataSetSeismogram saved =
                        saveInDataSet(event, recFuncChannel, ((MemoryDataSetSeismogram)dss).getCache(), SeismogramFileTypes.SAC);
                }
            }
        } else {
            // problem occurred
            SeisDataErrorEvent error = processor.getError();
            logger.error("problem with recfunc:", error.getCausalException());
        }
        MicroSecondDate after = new MicroSecondDate();
        System.out.println("Save took "+after.subtract(before).convertTo(UnitImpl.SECOND));
        System.out.println("Done with "+ChannelIdUtil.toStringNoDates(channel.get_id()));
        return seismograms;
    }

    public synchronized void appendToSummaryPage(String val) throws IOException {
        if (summaryPage == null) {
            File summaryFile = new File(getParentDirectory(), "summary.html");
            summaryPage = new BufferedWriter(new FileWriter(summaryFile));
            summaryPage.write("<html>");summaryPage.newLine();
            summaryPage.write("<head>");summaryPage.newLine();
            summaryPage.write("<title>Receiver Function Summary</title>");summaryPage.newLine();
            summaryPage.write("</head>");summaryPage.newLine();
            summaryPage.write("<body>");summaryPage.newLine();
            // we don't close the tags so that we can append to the file easily
        }
        summaryPage.write(val);summaryPage.newLine();
        summaryPage.flush();
    }

    boolean isDataComplete(LocalSeismogram seis) {
        return processor.isRecFuncFinished();
    }

    RecFunc recFunc;
    DataSetRecFuncProcessor processor;
    TauP_Time tauPTime;

    static BufferedWriter summaryPage = null;

    private static Logger logger = Logger.getLogger(RecFuncProcessor.class);

}


