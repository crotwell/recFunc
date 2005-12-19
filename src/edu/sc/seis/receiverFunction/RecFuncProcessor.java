/**
 * RecFuncProcessor.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import edu.iris.Fissures.Orientation;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfSeismogramDC.LocalSeismogram;
import edu.iris.Fissures.IfSeismogramDC.RequestFilter;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.cache.EventUtil;
import edu.sc.seis.fissuresUtil.display.DisplayUtils;
import edu.sc.seis.fissuresUtil.xml.DataSet;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.fissuresUtil.xml.SeismogramFileTypes;
import edu.sc.seis.fissuresUtil.xml.URLDataSetSeismogram;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.sod.ChannelGroup;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.SodUtil;
import edu.sc.seis.sod.Start;
import edu.sc.seis.sod.process.waveform.LocalSeismogramTemplateGenerator;
import edu.sc.seis.sod.process.waveform.SaveSeismogramToFile;
import edu.sc.seis.sod.process.waveform.WaveformResult;
import edu.sc.seis.sod.process.waveform.vector.ANDWaveformProcessWrapper;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorProcess;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorResult;
import edu.sc.seis.sod.status.FissuresFormatter;
import edu.sc.seis.sod.status.StringTreeLeaf;

public class RecFuncProcessor extends SaveSeismogramToFile implements WaveformVectorProcess {

    public RecFuncProcessor(Element config)  throws ConfigurationException {
        super(config);
        IterDeconConfig deconConfig = parseIterDeconConfig(config);
        gwidth = deconConfig.gwidth;
        maxBumps = deconConfig.maxBumps;
        tol = deconConfig.tol;
        Element phaseNameElement = SodUtil.getElement(config, "phaseName");
        if (phaseNameElement != null) {
            String phaseName = SodUtil.getNestedText(phaseNameElement);
            if (phaseName.equals("P")) {
                pWave = true;
            } else {
                pWave = false;
            }
        }
        logger.info("Init RecFuncProcessor");
    }
    
    public static IterDeconConfig parseIterDeconConfig(Element config) {
        float gwidth = DEFAULT_GWIDTH;
        int maxBumps = DEFAULT_MAXBUMPS;
        float tol = DEFAULT_TOL;
        Element gElement = SodUtil.getElement(config, "gaussianWidth");
        if (gElement != null) {
            String gwidthStr = SodUtil.getNestedText(gElement);
            gwidth = Float.parseFloat(gwidthStr);
        }
        Element bumpsElement = SodUtil.getElement(config, "maxBumps");
        if (bumpsElement != null) {
            String bumpsStr = SodUtil.getNestedText(bumpsElement);
            maxBumps = Integer.parseInt(bumpsStr);
        }
        Element toleranceElement = SodUtil.getElement(config, "tolerance");
        if (toleranceElement != null) {
            String toleranceStr = SodUtil.getNestedText(toleranceElement);
            tol = Float.parseFloat(toleranceStr);
        }
        return new IterDeconConfig(gwidth, maxBumps, tol);
    }

    /**
     * Processes localSeismograms to calculate receiver functions.
     */
    public WaveformVectorResult process(EventAccessOperations event,
                                        ChannelGroup channelGroup,
                                        RequestFilter[][] original,
                                        RequestFilter[][] available,
                                        LocalSeismogramImpl[][] seismograms,
                                        CookieJar cookieJar) throws Exception {
        // save original seismograms, return value is ignored
        for (int i = 0; i < seismograms.length; i++) {
            WaveformResult saveToFileSeis = super.process(event,
                                                          channelGroup.getChannels()[i],
                                                          original[i],
                                                          available[i],
                                                          seismograms[i],
                                                          cookieJar);
            saveToFileSeis = null;
        }

        if (recFunc == null) {
            tauPTime = TauPUtil.getTauPUtil(modelName);
            recFunc = new RecFunc(tauPTime,
                                  new IterDecon(maxBumps, true, tol, gwidth),
                                  pWave);
        }
        for (int i = 0; i < seismograms.length; i++) {
            if (seismograms[i].length == 0) {
                // maybe no data after cut?
                return new WaveformVectorResult(seismograms, new StringTreeLeaf(this, false, "zero seismograms for "+i+" component"));
            }
            if ( ! ChannelIdUtil.areEqual(available[i][0].channel_id, seismograms[i][0].channel_id)) {
                // fix the dumb -farm or -spyder on pond available_data
                available[i][0].channel_id = seismograms[i][0].channel_id;
            }
        }

        DataSet dataset = prepareDataset(event);
        DataSetSeismogram[] chGrpSeismograms =
            DisplayUtils.getComponents(dataset, available[0][0]);

        if (chGrpSeismograms.length < 3) {
            logger.debug("chGrpSeismograms.length = "+chGrpSeismograms.length);
            // must not be all here yet
            return new WaveformVectorResult(seismograms, new StringTreeLeaf(this, false, "Can't find 3 seismograms for motion vector"));
        }

        logger.info("RecFunc for "+ChannelIdUtil.toStringNoDates(channelGroup.getChannels()[0].get_id()));
        for (int i=0; i<chGrpSeismograms.length; i++) {
            if (chGrpSeismograms[i] == null) {
                // must not be all here yet
                System.out.println("chGrpSeismograms["+i+"] is null");
                return new WaveformVectorResult(seismograms, new StringTreeLeaf(this, false, "seismogram "+i+" is null"));
            }
        }

        processor =
            new DataSetRecFuncProcessor(chGrpSeismograms,
                                        event,
                                        recFunc,
                                        tauPTime);
        for (int i=0; i<chGrpSeismograms.length; i++) {
            logger.debug("Retrieving for "+chGrpSeismograms[i].getName());
            chGrpSeismograms[i].retrieveData(processor);
        }
        Channel zeroChannel = channelGroup.getChannels()[0];
        while ( ! processor.isRecFuncFinished()) {
            try {
                //System.out.println("Sleeping "+ChannelIdUtil.toStringNoDates(zeroChannel.get_id()));

                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        MicroSecondDate before = new MicroSecondDate();
        System.out.println("Before save rec func data");
        if (processor.getError() == null) {
            if (processor.getPredicted() != null) {
                for (int i = 0; i < processor.getPredicted().length; i++) {
                    String ITR_ITT = (i==0?"ITR":"ITT");
                    MemoryDataSetSeismogram predicted = processor.getPredicted()[i];
                    dataset.remove(predicted); // to avoid duplicates
                    Channel recFuncChannel = new ChannelImpl(predicted.getRequestFilter().channel_id,
                                                             "receiver function fake channel for "+ChannelIdUtil.toStringNoDates(zeroChannel.get_id()),
                                                             new Orientation(0, 0),
                                                             zeroChannel.sampling_info,
                                                             zeroChannel.effective_time,
                                                             zeroChannel.my_site);

                    URLDataSetSeismogram saved =
                        saveInDataSet(event, recFuncChannel, predicted.getCache(), SeismogramFileTypes.SAC);
                    Collection aux = predicted.getAuxillaryDataKeys();
                    Iterator it = aux.iterator();
                    while (it.hasNext()) {
                        Object key = it.next();
                        saved.addAuxillaryData(key, predicted.getAuxillaryData(key));
                    }
                    DistAz distAz = DisplayUtils.calculateDistAz(saved);
                    Origin origin = EventUtil.extractOrigin(event);

                    Arrival[] arrivals =
                        tauPTime.calcTravelTimes(recFuncChannel.my_site.my_station, origin, pPhases);

                    // convert radian per sec ray param into km per sec
                    float kmRayParam = (float)(arrivals[0].getRayParam()/tauPTime.getTauModel().getRadiusOfEarth());

                    // find template generator to get directory to output rec func
                    // images
                    if (lSeisTemplateGen == null) {
                        WaveformVectorProcess[] processes = Start.getWaveformArm().getMotionVectorArm().getProcesses();
                        for (int j = 0; j < processes.length; j++) {
                            if (processes[j] instanceof ANDWaveformProcessWrapper) {
                                ANDWaveformProcessWrapper wrapper =
                                    (ANDWaveformProcessWrapper)processes[j];
                                if (wrapper.getProcess() instanceof LocalSeismogramTemplateGenerator) {
                                    lSeisTemplateGen = (LocalSeismogramTemplateGenerator)wrapper.getProcess();
                                    break;
                                }
                            }
                        }
                    }

                    String channelIdString = ChannelIdUtil.toStringNoDates(predicted.getRequestFilter().channel_id);
                    String[] auxStrings = (String[])aux.toArray(new String[0]);
                    cookieJar.put("recFunc_pred_auxData"+ITR_ITT, auxStrings);
                    cookieJar.put("recFunc_percentMatch_"+ITR_ITT, ""+HKStack.getPercentMatch(saved));

                    synchronized(lSeisTemplateGen) {
                        RequestFilter[] zoomP = new RequestFilter[1];
                        MicroSecondDate oTime = new MicroSecondDate(event.get_preferred_origin().origin_time);
                        MicroSecondDate pTime = oTime.add(new TimeInterval(arrivals[0].getTime(), UnitImpl.SECOND));
                        zoomP[0] = new RequestFilter(recFuncChannel.get_id(),
                                                     pTime.subtract(new TimeInterval(5, UnitImpl.SECOND)).getFissuresTime(),
                                                     pTime.add(new TimeInterval(30, UnitImpl.SECOND)).getFissuresTime());
                        lSeisTemplateGen.getSeismogramImageProcess().process(event, recFuncChannel, zoomP,predicted.getCache(), lSeisTemplateGen.getSeismogramImageProcess().PNG, phases, cookieJar);
                        lSeisTemplateGen.getSeismogramImageProcess().process(event, recFuncChannel, zoomP, predicted.getCache(), lSeisTemplateGen.getSeismogramImageProcess().PDF, phases, cookieJar);
                    }
                    RecFuncTemplate rfTemplate =new RecFuncTemplate();
                    File velocityOutFile = new File(getEventDirectory(event),FissuresFormatter.filize("Vel_"+channelIdString+".html"));
                    rfTemplate.process(cookieJar.getContext(), velocityOutFile);

                    if (ITR_ITT.equals("ITR")) {
                        HKStack stack = new HKStack(new QuantityImpl(6.5, UnitImpl.KILOMETER_PER_SECOND),
                                                    kmRayParam, gwidth,
                                                    HKStack.getPercentMatch(saved),
                                                    new QuantityImpl(10, UnitImpl.KILOMETER), new QuantityImpl(.25f, UnitImpl.KILOMETER), 240,
                                                    1.6f, .0025f, 200,
                                                    1, 1, 1,
                                                    saved);

                        String prefix = "HKstack_";
                        String postfix = ".raw";

                        File imageDir = lSeisTemplateGen.getOutputFile(event, zeroChannel).getParentFile();
                        imageDir.mkdirs();
                        File outImageFile  = new File(imageDir, FissuresFormatter.filize(prefix+channelIdString+".png"));
                        BufferedImage bufImage = stack.createStackImage();
                        javax.imageio.ImageIO.write(bufImage, "png", outImageFile);



                        cookieJar.put("recFunc_hkstack_image_"+ITR_ITT, outImageFile.getName());
                        cookieJar.put("stack_"+ITR_ITT, stack);


                        File outHtmlFile  = new File(getEventDirectory(event),
                                                     FissuresFormatter.filize(prefix+channelIdString+".html"));
                        BufferedWriter bw = new BufferedWriter(new FileWriter(outHtmlFile));
                        bw.write("<html>");bw.newLine();
                        bw.write("<head>");bw.newLine();
                        bw.write("<title>"+channelIdString+"</title>");bw.newLine();
                        bw.write("</head>");bw.newLine();
                        bw.write("<body>");bw.newLine();
                        bw.write("</br><pre>");bw.newLine();
                        stack.writeReport(bw);bw.newLine();
                        bw.write("</pre></br>");bw.newLine();
                        it = aux.iterator();
                        while (it.hasNext()) {
                            Object key = it.next();
                            Object val = predicted.getAuxillaryData(key);
                            if (val instanceof Element) {
                                Element e = (Element)val;
                                StringWriter swriter = new StringWriter();
                                edu.sc.seis.fissuresUtil.xml.Writer out = new edu.sc.seis.fissuresUtil.xml.Writer(false, true);
                                out.setOutput(swriter);
                                out.write(e);
                                swriter.close();
                                bw.write("<h3>"+key.toString()+"</h3>");bw.newLine();
                                bw.write("<pre>");bw.newLine();
                                bw.write(swriter.toString());bw.newLine();
                                bw.write("</pre></br>");bw.newLine();
                            } else {
                                // oh well, use toString and hope for the best
                                bw.write(key.toString()+" = "+val.toString()+"</br>");bw.newLine();
                            }
                        }
                        bw.write("<img src="+quote+outImageFile.getName()+quote+"/>");
                        bw.write("</body>");bw.newLine();
                        bw.write("</html>");bw.newLine();
                        bw.close();

                        StackMaximum xyMax = stack.getGlobalMaximum();
                        float max = xyMax.getMaxValue();
                        appendToSummaryPage("<tr><td>"+getEventDirectory(event).getName()+"</td><td>"+channelIdString+"</td><td>"+stack.getPercentMatch()+"</td><td>"+(stack.getMaxValueH())+"</td><td>"+xyMax.getKValue()+"</td></tr>");


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
                                                             zeroChannel.sampling_info,
                                                             zeroChannel.effective_time,
                                                             zeroChannel.my_site);
                    URLDataSetSeismogram saved =
                        saveInDataSet(event, recFuncChannel, ((MemoryDataSetSeismogram)dss).getCache(), SeismogramFileTypes.SAC);
                }
            }
        } else {
            // problem occurred
            SeisDataErrorEvent error = processor.getError();
            logger.error("problem with recfunc:", error.getCausalException());
            return new WaveformVectorResult(seismograms, new StringTreeLeaf(this, false, "problem with recfunc", error.getCausalException()));
        }
        MicroSecondDate after = new MicroSecondDate();
        System.out.println("Save took "+after.subtract(before).convertTo(UnitImpl.SECOND));
        System.out.println("Done with "+ChannelIdUtil.toStringNoDates(zeroChannel.get_id()));
        return new WaveformVectorResult(seismograms, new StringTreeLeaf(this, true));
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
            summaryPage.write("<table>");summaryPage.newLine();
            summaryPage.write("<tr id="+quote+"title"+quote+">");summaryPage.newLine();
            summaryPage.write("<td>Event</td><td>Channel</td><td>Match</td><td>best H</td><td>best K</td>");summaryPage.newLine();
            summaryPage.write("</tr>");summaryPage.newLine();

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
    TauPUtil tauPTime;
    LocalSeismogramTemplateGenerator lSeisTemplateGen = null;

    public static float DEFAULT_GWIDTH = 2.5f;
    public static int DEFAULT_MAXBUMPS = 400;
    public static float DEFAULT_TOL = 0.001f;
    
    protected float gwidth;

    protected float tol;
    
    protected int maxBumps;
    
    protected boolean pWave = true;
    
    String modelName = "iasp91";

    String[] phases = { };
    //String[] phases = { "P", "Pms", "PPvms", "PSvms"};

    String[] pPhases = { "P" };

    static BufferedWriter summaryPage = null;

    public static final char quote = '"';

    private static Logger logger = Logger.getLogger(RecFuncProcessor.class);

}





