package edu.sc.seis.receiverFunction;

import edu.sc.seis.fissuresUtil.freq.Cmplx;

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

    public static void main(String[] args) throws Exception {
        NativeFFT nFFT = new NativeFFT();
        int length = 8;
        float[] data = new float[length];
        data[length/2] = 1;
        float[] data2 = new float[length];
        System.arraycopy(data, 0, data2, 0, data.length);
        int errorCode;

        for ( int i=0; i<data.length; i++) {
            System.out.println("pre  data["+i+"]="+data[i]);
        } // end of for ()
        errorCode = NativeFFT.realFFT(data);
        if ( errorCode != 0) {
            throw new Exception("Error "+errorCode+" in native fft");
        } // end of if ()

        for ( int i=0; i<data.length; i++) {
            System.out.println("post  data["+i+"]="+data[i]);
        } // end of for ()

        Cmplx[] out2 = Cmplx.fft(data2);
        for ( int i=0; i<out2.length; i++) {
            System.out.println("post  out2["+i+"]="+out2[i].r+", "+out2[i].i);
        } // end of for ()

    } // end of main()


} // NativeFFT
