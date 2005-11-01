package edu.sc.seis.receiverFunction.synth;

import edu.iris.Fissures.Sampling;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.mockFissures.IfNetwork.MockChannel;
import edu.sc.seis.fissuresUtil.mockFissures.IfNetwork.MockNetworkId;
import edu.sc.seis.receiverFunction.compare.StationResult;
import junit.framework.TestCase;

public class SimpleSynthReveicerFunctionTest extends TestCase {

    public SimpleSynthReveicerFunctionTest() {
        this("SimpleSynthReveicerFunctionTest");
    }

    public SimpleSynthReveicerFunctionTest(String arg0) {
        super(arg0);
        simple = new SimpleSynthReceiverFunction(new StationResult(MockNetworkId.createNetworkID(),
                                                                   "test",
                                                                   new QuantityImpl(30, UnitImpl.KILOMETER),
                                                                   (float)(vpCrust / vsCrust),
                                                                   new QuantityImpl(vpCrust,
                                                                                    UnitImpl.KILOMETER_PER_SECOND),
                                                                   null),
                                                 new SamplingImpl(1, new TimeInterval(1, UnitImpl.SECOND)),
                                                 4096,
                                                 rhoCrust,
                                                 vpMantle,
                                                 vsMantle,
                                                 rhoMantle);
        simple.calculate(flatRP,
                         ClockUtil.now().getFissuresTime(),
                         new TimeInterval(0, UnitImpl.SECOND),
                         MockChannel.createChannel().get_id(),
                         5);
    }

    public void testVersusJordi_P() {
        assertEquals("P", a[0], simple.getAmpP(flatRP), 0.00001);
    }

    public void testVersusJordi_Ps() {
        assertEquals("Ps", a[1], simple.getAmpPs(flatRP), 0.00001);
    }

    public void testVersusJordi_PpPs() {
        assertEquals("PpPs", a[2], simple.getAmpPpPs(flatRP), 0.00001);
    }

    public void testVersusJordi_PsPs() {
        assertEquals("PsPs", a[3], simple.getAmpPsPs(flatRP), 0.00001);
    }

    SimpleSynthReceiverFunction simple;

    double vpCrust = 6.5;

    double vsCrust = 3.75300002;

    double rhoCrust = 2.8499999;

    double vpMantle = 8.10000038;

    double vsMantle = 4.67600012;

    double rhoMantle = 3.36199999;

    float flatRP = 0.03f;

    double[] a = new double[] {0.229568452, 0.0611278005, 0.0885273293, -0.0796554536};
}
