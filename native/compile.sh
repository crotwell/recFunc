
javah -classpath ../target/recFunc-1.0beta.jar edu.sc.seis.receiverFunction.NativeFFT

cc -c \
-I/System/Library/Frameworks/JavaVM.framework/Headers \
-I/System/Library/Frameworks/vDSP.framework/Headers \
-I. apple_vDSP.c

cc -dynamiclib -o libnativeFFT.jnilib apple_vDSP.o -framework JavaVM -framework vecLib

