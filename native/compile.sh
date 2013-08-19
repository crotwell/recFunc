
# see http://developer.apple.com/unix/crossplatform.html
# in the section Using Java > Extensions and JNI Libraries 
# to see about jni installation on the mac
#
# to make available without setting this env var, copy to
# /Users/crotwell/Library/Java/Extensions/libnativeFFT.jnilib
#

setenv DYLD_LIBRARY_PATH `pwd`

javah -classpath ../target/classes edu.sc.seis.receiverFunction.NativeFFT

cc -c \
-I/System/Library/Frameworks/JavaVM.framework/Headers \
-I/System/Library/Frameworks/vDSP.framework/Headers \
-I. apple_vDSP.c

cc -dynamiclib -o libnativeFFT.jnilib apple_vDSP.o -framework JavaVM -framework vecLib

