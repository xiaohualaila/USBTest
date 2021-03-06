package com.example.administrator.usbtest;


import android.serialport.SerialPort;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * Created by admin on 2017/7/19.
 */

public class ScanHelper {
    //每次获取数据之间的休息时间
    private static final int WAIT_TIME = 50;
    //开始获取数据的等待次数
    private static final int GET_FIRST_AVAILABLE_NUM = 3;
    //获取结尾数据判断是否是最后数据等待次数
    private static final int GET_LAST_MSG_NUM = 5;


    private SerialPort serialport;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private ReadThread mReadThread;
    private OnDataReceived onDataReceived;
    private Subscription sub;

    /**
     * 初始化并打开串口
     * @param device
     * @param baudrate
     * @param onDataReceived
     */
    public ScanHelper(String device, int baudrate, OnDataReceived onDataReceived) {
        this.onDataReceived = onDataReceived;
        try {
            this.serialport = new SerialPort(new File(device),baudrate);
            this.mInputStream = this.serialport.getInputStream();
            this.mOutputStream = this.serialport.getOutputStream();
            mReadThread = new ReadThread();
            mReadThread.start();
          //  findCode();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ScanHelper", "can not open device");
        }
    }

    /**
     * 关闭串口
     */
    public void close() {
     //   sub.unsubscribe();
        if (mReadThread != null){
           mReadThread.interrupt();
        }
         if (serialport != null) {
             serialport.close();
             serialport = null;
             }
        serialport = null;
        mInputStream=null;
        mOutputStream=null;
    }

    private void findCode(){
        sub = Observable.interval(1000, 200, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        try {
//                            if (mInputStream == null) return;
//                            int poolNum = 1;
//                            while(true) {
//                                if(mInputStream.available() > 0){
//                                    break;
//                                }
//                                poolNum ++;
//                                if(poolNum > GET_FIRST_AVAILABLE_NUM){
//                                    break;
//                                }
//                            }
//                            int available = mInputStream.available();
//                            if(available > 0) {
//                                while (true) {
//                                    if (mInputStream.available() > available) {
//                                        available = mInputStream.available();
//                                        poolNum = 1;
//                                    } else if (mInputStream.available() == available) {
//                                        poolNum++;
//                                        if (poolNum > GET_LAST_MSG_NUM) {
//                                            break;
//                                        }
//                                    }
//                                }
//                                byte[] buffer = new byte[available];
//                                mInputStream.read(buffer);
//                                onDataReceived.received(buffer, available);
//                            }
                            int poolNum = 1;
                            if (mInputStream == null) return;
                            int available = mInputStream.available();
                                if(available > 0) {
                                while (true) {
                                    if (mInputStream.available() > available) {
                                        available = mInputStream.available();
                                        poolNum = 1;
                                    } else if (mInputStream.available() == available) {
                                        poolNum++;
                                        if (poolNum > GET_LAST_MSG_NUM) {
                                            break;
                                        }
                                    }
                                }
                                byte[] buffer = new byte[available];
                                mInputStream.read(buffer);
                                onDataReceived.received(buffer, available);
                                    mInputStream.reset();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });


    }

    class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while(!isInterrupted()) {
                try {
                    if (mInputStream == null) return;
                    int poolNum = 1;
                    while(true) {
                        Thread.sleep(WAIT_TIME);
                        if(mInputStream.available() > 0){
                            break;
                        }
                        poolNum ++;
                        if(poolNum > GET_FIRST_AVAILABLE_NUM){
                            break;
                        }
                    }
                    int available = mInputStream.available();
                    if(available > 0) {
                        poolNum = 1;
                        while (true) {
                            Thread.sleep(WAIT_TIME);
                            if (mInputStream.available() > available) {
                                available = mInputStream.available();
                                poolNum = 1;
                            } else if (mInputStream.available() == available) {
                                poolNum++;
                                if (poolNum > GET_LAST_MSG_NUM) {
                                    break;
                                }
                            }
                        }
                        byte[] buffer = new byte[available];
                        mInputStream.read(buffer);
                        onDataReceived.received(buffer, available);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public OutputStream getOutputStream() {
        return mOutputStream;
    }

    public interface OnDataReceived{
        void received(byte[] buffer, int size);
    }


}
