/**
 * RecFuncProcessor.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.receiverFunction.server.RecFuncCacheProcessor;
import edu.sc.seis.sod.process.waveform.AbstractSeismogramWriter;
import edu.sc.seis.sod.process.waveform.vector.WaveformVectorProcess;

public class RecFuncProcessor extends RecFuncCacheProcessor implements WaveformVectorProcess {

    public RecFuncProcessor(Element config)  throws Exception {
        super(config);
        cache = new RecFuncCacheImpl(AbstractSeismogramWriter.extractWorkingDir(config));
    }

    private static Logger logger = Logger.getLogger(RecFuncProcessor.class);

}





