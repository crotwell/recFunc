/**
 * RecFuncTemplateTest.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import edu.sc.seis.mockFissures.IfEvent.MockEventAccessOperations;
import edu.sc.seis.mockFissures.IfNetwork.MockChannel;
import java.util.Collection;
import java.util.HashMap;
import junit.framework.TestCase;
import org.apache.velocity.VelocityContext;

public class RecFuncTemplateTest extends TestCase {

    public void testProcess() throws Exception {
        RecFuncTemplate template = new RecFuncTemplate();
        VelocityContext context = new VelocityContext();

        context.put( "name", new String("Velocity") );
        context.put("sod_event", MockEventAccessOperations.createEvent());
        context.put("sod_channel", MockChannel.createChannel());
        context.put("recFunc_hkstack_image", "hkstack.png");
        HashMap aux = new HashMap();
        aux.put("testA", "A");
        aux.put("testB", "B");
        context.put("recFunc_pred_auxData", aux);
        template.process(context);
    }
}

