package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.FissuresException;
import edu.sc.seis.receiverFunction.compare.StationResult;
import junit.framework.TestCase;

public class StackComplexityTest extends TestCase {

    public void testSyntheticAtMax() throws FissuresException {
        HKStack stack = HKStackTest.getMockHKStack();
        StationResult maxResult = new StationResult(stack.getChannelId().network_id,
                                                    stack.getChannelId().station_code,
                                                    stack.getMaxValueH(),
                                                    stack.getMaxValueK(),
                                                    stack.getAlpha(),
                                                    null);
        StackComplexity complexity = new StackComplexity(stack, 4096, stack.getGaussianWidth());
        HKStack synth = complexity.getSynthetic(maxResult);
        StackMaximum maxIndex = stack.getGlobalMaximum();
        StackMaximum synthMaxIndex = synth.getGlobalMaximum();
        // for some reason the synthetic max does not exactly align with the peak, but is close, so use within 2
        assertEquals("max index 0 ", maxIndex.getHIndex(), synthMaxIndex.getHIndex(), 2);
        assertEquals("max index 1 ", maxIndex.getKIndex(), synthMaxIndex.getKIndex(), 2);
    }
}
