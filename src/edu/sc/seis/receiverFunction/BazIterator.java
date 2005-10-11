package edu.sc.seis.receiverFunction;

import java.util.Iterator;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.sc.seis.fissuresUtil.bag.DistAz;

public class BazIterator implements Iterator {

    public BazIterator(Iterator it, float minBaz, float maxBaz) {
        this.it = it;
        // make sure 0 <= minBaz < 360
        // assume maxBaz is in same convention
        while(minBaz < 0) {
            minBaz += 360;
            maxBaz += 360;
        }
        while(minBaz >= 360) {
            minBaz -= 360;
            maxBaz -= 360;
        }
        this.minBaz = minBaz;
        this.maxBaz = maxBaz;
    }

    public boolean hasNext() {
        checkNext();
        return (next != null);
    }

    public Object next() {
        checkNext();
        HKStack temp = next;
        next = null;
        return temp;
    }

    public void remove() {
        throw new RuntimeException("Not implemented");
    }

    void checkNext() {
        while(next == null && it.hasNext()) {
            HKStack temp = (HKStack)it.next();
            DistAz distAz = new DistAz(temp.chan, temp.getOrigin());
            double baz = distAz.getBaz();
            if (baz < 0) {baz += 360;}
            if(baz >= minBaz && baz <= maxBaz) {
                next = temp;
                System.out.println("Found Baz in range: "+baz);
                return;
            }
        }
    }

    Iterator it;

    float minBaz;

    float maxBaz;

    HKStack next = null;
}
