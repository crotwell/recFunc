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
import edu.sc.seis.TauP.TauP_Time;
import edu.sc.seis.fissuresUtil.display.DisplayUtils;
import edu.sc.seis.fissuresUtil.xml.DataSet;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.CookieJar;
import edu.sc.seis.sod.LocalSeismogramProcess;
import edu.sc.seis.sod.subsetter.waveFormArm.SacFileProcessor;
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
            TauP_Time tauPTime = new TauP_Time("iasp91");
            recFunc = new RecFunc(tauPTime,
                                  new IterDecon(100, true, .001f, gwidth));
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
        if (processor.getError() == null) {
            if (processor.getPredicted() != null) {
                MemoryDataSetSeismogram predicted = processor.getPredicted();
                dataset.remove(predicted); // to avoid duplicates
                Channel recFuncChannel = new ChannelImpl(predicted.getRequestFilter().channel_id,
                                                         "receiver function fake channel for "+ChannelIdUtil.toStringNoDates(channel.get_id()),
                                                         new Orientation(0, 0),
                                                         channel.sampling_info,
                                                         channel.effective_time,
                                                         channel.my_site);
                
                saveInDataSet(event, recFuncChannel, predicted.getCache());
            } else {
                logger.error("problem with recfunc: predicted is null");
            }
        } else {
            // problem occurred
            SeisDataErrorEvent error = processor.getError();
            logger.error("problem with recfunc:", error.getCausalException());
        }
        return seismograms;
    }
    
    boolean isDataComplete(LocalSeismogram seis) {
        return processor.isRecFuncFinished();
    }
    
    RecFunc recFunc;
    DataSetRecFuncProcessor processor;
    
    static Logger logger = Logger.getLogger(RecFuncProcessor.class);
    
}

