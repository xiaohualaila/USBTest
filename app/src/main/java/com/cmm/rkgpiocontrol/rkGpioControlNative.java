package com.cmm.rkgpiocontrol;

public class rkGpioControlNative {

	static {
        System.loadLibrary("rkGpioControl_jni");
    }
	
	public static native int init();
	public static native int ControlGpio(int which,int state); 
	public static native int ReadGpio(int which);
	public static native void close();
	
}
