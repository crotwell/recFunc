/**
 * RecFuncTemplateTest.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import java.util.HashMap;
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
        context.put( "channelIdToString", ChannelIdUtil.toStringNoDates(MockChannel.createChannel().get_id()));
        context.put("recFunc_hkstack_image", "hkstack.png");
        context.put("ChannelIdUtil", new ChannelIdUtil());

        HKStack stack = new HKStack(1, 2, 90, 10, 1, 50, 1.6f, .025f, 50);
        context.put("stack", stack);
        HashMap aux = new HashMap();
        aux.put("testA", "A");
        aux.put("testB", "B");
        context.put("recFunc_pred_auxData", aux);
        template.process(context, "testTemplate.html");
    }
}

