package edu.sc.seis.receiverFunction;



/**
 * NativeFFT.java
 *
 *
 * Created: Mon Apr 14 10:06:06 2003
 *
 * @author <a href="mailto:crotwell@owl.seis.sc.edu">Philip Crotwell</a>
 * @version 1.0
 */
public class NativeFFT {

    static {
        System.loadLibrary("nativeFFT");
    }

    public NativeFFT() {

    } // NativeFFT constructor

    /** Uses the MacOSX built in vDSP library to do the fft using altavec. This
     *  returns the real fft, where only half the values are needed due to
     *  symmetry and the real part of point N/2+1 is put in the imaginary
     *  part of the zeroth element. Data returned are otherwise real,imaginary,
     *  real, imaginary...
     * @retuns 0 for success, other for error code
     */
    private static native int realFFT(float[] realData);

    /** Uses the MacOSX built in vDSP library to do the fft using altavec. This
     *  returns the real fft, where only half the values are needed due to
     *  symmetry and the real part of point N/2+1 is put in the imaginary
     *  part of the zeroth element. Data returned are otherwise real,imaginary,
     *  real, imaginary...
     */
    public static void forward(float[] realData) {
        int errorCode = realFFT(realData);
        if (errorCode != 0) {
            throw new RuntimeException("Trouble with native FFT, error code="+errorCode);
        } // end of if ()
        // vDSP uses opposite sign convention as Num. Rec. FFT, so
        // values are complex congugate of what we want
        for (int i = 3; i < realData.length; i+=2) {
            realData[i] *= -1;
        }
    }

    /**
     * Performs the inverse fft operation of the realFFT call. It also
     * correctly applies the scale factor introduced by the underlying
     * native library implementation.
     * @retuns 0 for success, other for error code
     */
    private static native int realInverseFFT(float[] realData);

    /**
     * Performs the inverse fft operation of the realFFT call. It also
     * correctly applies the scale factor introduced by the underlying
     * native library implementation.
     */
    public static void inverse(float[] realData) {
        // vDSP uses opposite sign convention as Num. Rec. FFT, so
        // values are complex congugate of what we want
        for (int i = 3; i < realData.length; i+=2) {
            realData[i] *= -1;
        }
        int errorCode = realInverseFFT(realData);
        if (errorCode != 0) {
            throw new RuntimeException("Trouble with native FFT, error code="+errorCode);
        } // end of if ()
    }


    public static float[] correlate(float[] x, float[] y) {

        float[] xforward = new float[x.length];
        System.arraycopy(x, 0, xforward, 0, x.length);
        forward(xforward);

        float[] yforward = new float[y.length];
        System.arraycopy(y, 0, yforward, 0, y.length);
        forward(yforward);

        float[] ans = new float[x.length];
        // handle 0 and nyquist
        ans[0] = xforward[0] * yforward[0];
        ans[1] = xforward[1] * yforward[1];

        float a, b, c, d;
        for (int j = 2; j < x.length; j+=2) {
            a = xforward[j];
            b = xforward[j+1];
            c = yforward[j];
            d = yforward[j+1];
            ans[j]   = a * c + b * d;
            ans[j+1] = -a * d + b * c;
        }
        inverse(ans);

        return ans;
    }

    public static float[] convolve(float[] x, float[] y, float delta) {
        float[] xforward = new float[x.length];
        System.arraycopy(x, 0, xforward, 0, x.length);
        forward(xforward);

        float[] yforward = new float[y.length];
        System.arraycopy(y, 0, yforward, 0, y.length);
        forward(yforward);

        float[] ans = new float[x.length];
        // handle 0 and nyquist
        ans[0] = xforward[0] * yforward[0];
        ans[1] = xforward[1] * yforward[1];

        float a, b, c, d;
        for (int j = 2; j < x.length; j+=2) {
            a = xforward[j];
            b = xforward[j+1];
            c = yforward[j];
            d = yforward[j+1];
            ans[j]   = a * c - b * d;
            ans[j+1] = a * d + b * c;
        }
        inverse(ans);

        for (int i = 0; i < ans.length; i++) {
            ans[i] *= delta;
        }
        return ans;
    }


} // NativeFFT
