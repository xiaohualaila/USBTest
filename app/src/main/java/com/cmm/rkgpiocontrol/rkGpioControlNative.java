package com.cmm.rkgpiocontrol;

public class rkGpioControlNative {

	static {
        System.loadLibrary("rkGpioControl_jni");
    }
	
	public static native int init();
	/**
	 *  
	 * @param type IO�ڵ�����
	 * @param which ��һ��IO
	 * @param state �߻��ߵ�
	 * @return
	 */
	public static native int ControlGpio(int which,int state); 
	public static native int ReadGpio(int which);
	public static native void close();
	
}
