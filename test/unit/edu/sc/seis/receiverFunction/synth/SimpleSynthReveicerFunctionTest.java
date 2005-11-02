package edu.sc.seis.receiverFunction.synth;

import junit.framework.TestCase;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.SamplingImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.mockFissures.IfNetwork.MockChannel;
import edu.sc.seis.fissuresUtil.mockFissures.IfNetwork.MockNetworkId;
import edu.sc.seis.receiverFunction.compare.StationResult;

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
    
    public void testJordiBasicTerms() {
        assertEquals("rsr0", rsr0, simple.rsr0, 0.00001);
        assertEquals("zsz0", zsz0, simple.zsz0, 0.00001);
        assertEquals("rpz0", rpz0, simple.rpz0, 0.00001);
    }
    
    public void testJordiRefTransPP0() {
        assertEquals("PP0", -0.94872421, simple.downgoingRFCoeff.getFreePtoPRefl(flatRP), 0.00001);
    }
    public void testJordiRefTransPS0() {
        assertEquals("PS0", 0.411624223, simple.downgoingRFCoeff.getFreePtoSVRefl(flatRP), 0.00001);
    }
    public void testJordiRefTransSP0() {
        assertEquals("SP0", 0.242751762, simple.downgoingRFCoeff.getFreeSVtoPRefl(flatRP), 0.00001);
    }
    
    public void testJordiRefTransPS2() {
        assertEquals("PS2", -0.0896627381, simple.downgoingRFCoeff.getPtoSVRefl(flatRP), 0.00001);
    }
    
    public void testJordiRefTransPP2() {
        assertEquals("PP2", 0.205925092, simple.downgoingRFCoeff.getPtoPRefl(flatRP), 0.00001);
    }
    
    public void testJordiRefTransSP2() {
        assertEquals("PS2", -0.0528778173, simple.downgoingRFCoeff.getSVtoPRefl(flatRP), 0.00001);
    }
    
    public void testJordiRefTransSS2() {
        assertEquals("PP2", -0.217589691, simple.downgoingRFCoeff.getSVtoSVRefl(flatRP), 0.00001);
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

    double vpCrust = 6.0;

    double vsCrust = 3.5;

    double rhoCrust = 3.0;

    double vpMantle = 7;

    double vsMantle = 4.2;

    double rhoMantle = 4.0;

    float flatRP = 0.03f;

    double[] a = new double[] {0.213547871,  0.0486433432,  0.0977657735, -0.0919868574};
    
    double rsr0 = 4.734231;
    double zsz0 = -0.124569595; 
    double rpz0 = 0.213547871;
}
