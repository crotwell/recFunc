/**
 * RecFuncTemplateTest.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.sc.seis.fissuresUtil.mockFissures.IfEvent.MockEventAccessOperations;
import edu.sc.seis.fissuresUtil.mockFissures.IfNetwork.MockChannel;
import edu.sc.seis.sod.CookieJar;
import java.util.HashMap;
import java.util.HashSet;
import junit.framework.TestCase;
import org.apache.velocity.VelocityContext;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.sc.seis.fissuresUtil.mockFissures.IfEvent.MockEventAccessOperations;
import edu.sc.seis.fissuresUtil.mockFissures.IfNetwork.MockChannel;

public class RecFuncTemplateTest extends TestCase {

    public void testProcess() throws Exception {
        RecFuncTemplate template = new RecFuncTemplate();

        VelocityContext context = new VelocityContext();

        context.put("sod_event", MockEventAccessOperations.createEvent());
        context.put("sod_channel", MockChannel.createChannel());
        context.put("channelIdToString", ChannelIdUtil.toStringNoDates(MockChannel.createChannel().get_id()));
        context.put("recFunc_hkstack_image", "hkstack.png");
        context.put("ChannelIdUtil", new ChannelIdUtil());

        HKStack stack = new HKStack(new QuantityImpl(1, UnitImpl.KILOMETER_PER_SECOND), 2, 2.5f, 90, new QuantityImpl(10, UnitImpl.KILOMETER), new QuantityImpl(1, UnitImpl.KILOMETER), 50, 1.6f, .025f, 50, 1, 1, 1);
        context.put("stack", stack);
        HashMap aux = new HashMap();
        aux.put("testA", "A");
        aux.put("testB", "B");
        context.put("recFunc_pred_auxData", aux);
        HashSet set = new HashSet();
        set.add("setA");
        set.add("setB");
        context.put("set", set);

        template.process(context, "testTemplate.html");
    }
}

