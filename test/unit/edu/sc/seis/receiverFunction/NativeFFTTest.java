/**
 * NativeFFTTest.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import edu.sc.seis.fissuresUtil.freq.Cmplx;
import junit.framework.TestCase;
import junitx.framework.ArrayAssert;

public class NativeFFTTest extends TestCase {

    public void testForward() {
        float[] data = new float[128];
        data[40] = 1;

        float[] nativeData = new float[data.length];
        float[] javaData = new float[data.length];

        System.arraycopy(data, 0, nativeData, 0, data.length);
        System.arraycopy(data, 0, javaData, 0, data.length);

        NativeFFT.forward(nativeData);

        Cmplx[] cData = Cmplx.fft(javaData);


        for (int i = 0; i < cData.length/2; i++) {
            System.out.println("real i="+i+" "+ (float)cData[i].real()+"      "+  nativeData[2*i]);
            System.out.println("imag i="+i+" "+ (float)cData[i].imag()+"      "+  nativeData[2*i+1]);
        }
        for (int i = cData.length/2; i < cData.length; i++) {
            System.out.println("real i="+i+" "+ (float)cData[i].real());
            System.out.println("imag i="+i+" "+ (float)cData[i].imag());
        }

        // check 0, and F_n/2 is in imaginary of F_0
        assertEquals("real F0", nativeData[0], cData[0].real(), 0.00001f);
        assertEquals("real F_n/2", nativeData[1], cData[cData.length/2].real(), 0.00001f);
        for (int i = 1; i < cData.length/2; i++) {
            assertEquals("real i="+i, (float)cData[i].real(), nativeData[2*i], 0.001f);
            assertEquals("imag i="+i, (float)cData[i].imag(), nativeData[2*i+1], 0.001f);
        }
    }

    public void testRoundTrip() {

        float[] data = new float[128];
        data[40] = 1;

        float[] nativeData = new float[data.length];
        System.arraycopy(data, 0, nativeData, 0, data.length);

        NativeFFT.forward(nativeData);
        NativeFFT.inverse(nativeData);
        ArrayAssert.assertEquals("round trip", data, nativeData, 0.001f);
    }
}

