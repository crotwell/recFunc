package edu.sc.seis.receiverFunction;

import java.util.Iterator;

import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;

public class BazIterator implements Iterator<ReceiverFunctionResult> {

    public BazIterator(Iterator<ReceiverFunctionResult> it, float minBaz, float maxBaz) {
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

    public ReceiverFunctionResult next() {
        checkNext();
        ReceiverFunctionResult temp = next;
        next = null;
        return temp;
    }

    public void remove() {
        throw new RuntimeException("Not implemented");
    }

    void checkNext() {
        while(next == null && it.hasNext()) {
            ReceiverFunctionResult temp = it.next();
            DistAz distAz = new DistAz(temp.getChannelGroup().getChannel1(), temp.getEvent());
            double baz = distAz.getBaz();
            if (baz < 0) {baz += 360;}
            if(baz >= minBaz && baz <= maxBaz) {
                next = temp;
                return;
            }
        }
    }

    Iterator<ReceiverFunctionResult> it;

    float minBaz;

    float maxBaz;

    ReceiverFunctionResult next = null;
}
