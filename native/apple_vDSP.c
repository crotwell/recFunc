#include <jni.h>
#include "edu_sc_seis_receiverFunction_NativeFFT.h"
#include <stdio.h>

#include <stdlib.h>
#include <string.h>
#include <CoreServices/CoreServices.h>
#include <vecLib/vDSP.h>
#include <sys/param.h>
#include <sys/sysctl.h>


#define kHasAltiVecMask    ( 1 << gestaltPowerPCHasVectorInstructions )  // used in looking for a g4

#include <Carbon/Carbon.h>


/*
 * Class:     edu_sc_seis_receiverFunction_NativeFFT
 * Method:    realFFT
 * Signature: ([F)V
 */
JNIEXPORT jint JNICALL Java_edu_sc_seis_receiverFunction_NativeFFT_realFFT
(JNIEnv * env, jclass class, jfloatArray dataArray) {

      COMPLEX_SPLIT A;
      FFTSetup      setupReal;
      UInt32        log2n;
      UInt32        n, nOver2;
      SInt32        stride;
      UInt32        i;
      jfloat*       originalReal;
      jfloat*        originalData;
      float         scale;
      jboolean      isCopy;
      jsize         len;
      
      // Set the size of FFT.
      len = (*env)->GetArrayLength(env, dataArray);
      log2n = 1;                     // start with 2
      n = 1 << log2n;                // n is 2^log2n.
      while (n < len) {
        log2n++;
        n = 1 << log2n;
      }
      if (n != len) {
        // data is not even power of 2
        return 1;
      }

      stride = 1;
      nOver2 = n / 2;                // half of n as real part and imag part.
      
      //      printf ( "1D real FFT of length log2 ( %d ) = %d\n\n", (unsigned int)n, (unsigned int)log2n );
      
      // Allocate memory for the input operands and check its availability,
      // use the vector version to get 16-byte alignment.
      A.realp = ( float* ) malloc ( nOver2 * sizeof ( float ) );
      A.imagp = ( float* ) malloc ( nOver2 * sizeof ( float ) );
      originalData = (*env)->GetFloatArrayElements(env, dataArray, &isCopy);
      originalReal = ( jfloat* ) malloc ( n * sizeof ( jfloat ) );

      if ( originalReal == NULL || A.realp == NULL || A.imagp == NULL )
      	{
            printf ( "\nmalloc failed to allocate memory for the real FFT section of the sample.\n" );
            exit ( 0 );
      	}

      for (i=0; i<len; i++) {
        originalReal[i] = originalData[i];
      }
      for (i=len; i<n; i++) {
        originalReal[i] = 0;
      }

            
      // Look at the real signal as an interleaved complex vector by casting it.
      // Then call the transformation function ctoz to get a split complex vector,
      // which for a real signal, divides into an even-odd configuration.
      ctoz ( ( COMPLEX * ) originalReal, 2, &A, 1, nOver2 );

      // Set up the required memory for the FFT routines and check its availability.
      setupReal = create_fftsetup ( log2n, FFT_RADIX2);
      if ( setupReal == NULL )
      	{
            printf ( "\nFFT_Setup failed to allocate enough memory for the real FFT.\n" );
            return 2;
      	}
      
      // Carry out a Forward and Inverse FFT transform.
      fft_zrip ( setupReal, &A, stride, log2n, FFT_FORWARD );
      //     fft_zrip ( setupReal, &A, stride, log2n, FFT_INVERSE );
      
      // Verify correctness of the results, but first scale it by 2n.
      scale = (float)1.0/2;
      
      vsmul( A.realp, 1, &scale, A.realp, 1, nOver2 );
      vsmul( A.imagp, 1, &scale, A.imagp, 1, nOver2 );
     
      // The output signal is now in a split real form.  Use the function
      // ztoc to get a split real vector.
      ztoc ( &A, 1, ( COMPLEX * ) originalReal, 2, nOver2 );

      for (i=0; i<len; i++) {
        originalData[i] = originalReal[i];
      }

      if (isCopy == JNI_TRUE) {
        (*env)->ReleaseFloatArrayElements(env, dataArray, originalData, 0);
      }

      destroy_fftsetup ( setupReal );
      free ( originalReal );
      free ( A.realp );
      free ( A.imagp );

      return 0;
}


/*
 * Class:     edu_sc_seis_receiverFunction_NativeFFT
 * Method:    realInverseFFT
 * Signature: ([F)V
 */
JNIEXPORT jint JNICALL Java_edu_sc_seis_receiverFunction_NativeFFT_realInverseFFT
(JNIEnv * env, jclass class, jfloatArray dataArray) {

      COMPLEX_SPLIT A;
      FFTSetup      setupReal;
      UInt32        log2n;
      UInt32        n, nOver2;
      SInt32        stride;
      UInt32        i;
      jfloat*       originalReal;
      jfloat*        originalData;
      float         scale;
      jboolean      isCopy;
      jsize         len;
      
      // Set the size of FFT.
      len = (*env)->GetArrayLength(env, dataArray);
      log2n = 1;                     // start with 2
      n = 1 << log2n;                // n is 2^log2n.
      while (n < len) {
        log2n++;
        n = 1 << log2n;
      }
      if (n != len) {
        return 1;
      }

      stride = 1;
      nOver2 = n / 2;                // half of n as real part and imag part.
      
      //      printf ( "1D real FFT of length log2 ( %d ) = %d\n\n", (unsigned int)n, (unsigned int)log2n );
      
      // Allocate memory for the input operands and check its availability,
      // use the vector version to get 16-byte alignment.
      A.realp = ( float* ) malloc ( nOver2 * sizeof ( float ) );
      A.imagp = ( float* ) malloc ( nOver2 * sizeof ( float ) );
      originalData = (*env)->GetFloatArrayElements(env, dataArray, &isCopy);
      originalReal = ( jfloat* ) malloc ( n * sizeof ( jfloat ) );

      if ( originalReal == NULL || A.realp == NULL || A.imagp == NULL )
      	{
            printf ( "\nmalloc failed to allocate memory for the real FFT section of the sample.\n" );
            return 2;
      	}

      for (i=0; i<len; i++) {
        originalReal[i] = originalData[i];
      }
      for (i=len; i<n; i++) {
        originalReal[i] = 0;
      }

            
      // Look at the real signal as an interleaved complex vector by casting it.
      // Then call the transformation function ctoz to get a split complex vector,
      // which for a real signal, divides into an even-odd configuration.
      ctoz ( ( COMPLEX * ) originalReal, 2, &A, 1, nOver2 );

      // Set up the required memory for the FFT routines and check its availability.
      setupReal = create_fftsetup ( log2n, FFT_RADIX2);
      if ( setupReal == NULL )
      	{
            printf ( "\nFFT_Setup failed to allocate enough memory for the real FFT.\n" );
            exit ( 0 );
      	}
      
      // Carry out a Forward and Inverse FFT transform.
      //fft_zrip ( setupReal, &A, stride, log2n, FFT_FORWARD );
      fft_zrip ( setupReal, &A, stride, log2n, FFT_INVERSE );
      
      // Verify correctness of the results, but first scale it by 2n.
      scale = (float)1.0/n;
      
      vsmul( A.realp, 1, &scale, A.realp, 1, nOver2 );
      vsmul( A.imagp, 1, &scale, A.imagp, 1, nOver2 );
     
      // The output signal is now in a split real form.  Use the function
      // ztoc to get a split real vector.
      ztoc ( &A, 1, ( COMPLEX * ) originalReal, 2, nOver2 );

      for (i=0; i<len; i++) {
        originalData[i] = originalReal[i];
      }

      if (isCopy == JNI_TRUE) {
        (*env)->ReleaseFloatArrayElements(env, dataArray, originalData, 0);
      }

      destroy_fftsetup ( setupReal );
      free ( originalReal );
      free ( A.realp );
      free ( A.imagp );

      return 0;
}
