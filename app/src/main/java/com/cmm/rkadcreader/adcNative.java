package com.cmm.rkadcreader;

public class adcNative {

	static {
        System.loadLibrary("rkAdcReader_jni");
    }
	
	public static native int open(int which);
	public static native int readAdc(int which);
	public static native void close(int which);
	
}
