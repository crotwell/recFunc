/**
 * Crust2Test.java
 *
 * @author Philip Crotwell
 */

package edu.sc.seis.receiverFunction.crust2;

import junit.framework.TestCase;
import java.io.IOException;

public class Crust2Test extends TestCase {

    public void testLoad() throws IOException {
        Crust2 crust2 = new Crust2();
        Crust2Profile profile = crust2.get(-180, 90);
        assertEquals("code", "A2", crust2.getCode(-180, 90));
        assertEquals("-180, 90", 3.81f, profile.getLayer(0).botPVelocity, .01f);

        profile = crust2.get(-176, 88);
        assertEquals("code", "A4", crust2.getCode(-176, 88));
        assertEquals("-180, 90", 1.5f, profile.getLayer(3).botDepth-profile.getLayer(3).topDepth, .01f);
    }

    public void testClosest() {
        int[] out;
        out = Crust2.getClosestLatLon(0, 0);
        assertEquals(" 0, 0", 1, out[0]);
        assertEquals(" 0, 0", 1, out[1]);


        out = Crust2.getClosestLatLon(1, 1);
        assertEquals(" 1, 1", 1, out[1]);
        assertEquals(" 1, 1", 1, out[1]);


        out = Crust2.getClosestLatLon(-180, -88);
        assertEquals(" -180, -88", -179, out[0]);
        assertEquals(" -180, -88", -87, out[1]);
    }
}

