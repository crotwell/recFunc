
# see
# http://developer.apple.com/techpubs/macosx/Java/JavaDevelopment/overview/chapter_2_section_8.html
#
#
# to make available without setting this env var, copy to
# /Users/crotwell/Library/Java/Extensions/libnativeFFT.jnilib
#

setenv DYLD_LIBRARY_PATH `pwd`

javah -classpath ../target/recFunc-1.0beta.jar edu.sc.seis.receiverFunction.NativeFFT

cc -c \
-I/System/Library/Frameworks/JavaVM.framework/Headers \
-I/System/Library/Frameworks/vDSP.framework/Headers \
-I. apple_vDSP.c

cc -dynamiclib -o libnativeFFT.jnilib apple_vDSP.o -framework JavaVM -framework vecLib

