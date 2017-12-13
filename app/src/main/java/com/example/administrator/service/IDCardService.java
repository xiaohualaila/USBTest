package com.example.administrator.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.decard.NDKMethod.BasicOper;
import com.example.administrator.activity.MainActivity;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Administrator on 2017/12/13.
 */

public class IDCardService extends Service {
    //身份证
    private Thread thread;
    private boolean isAuto = true;
    private boolean choose = false;//false标准协议,true公安部协议
    private static Lock lock = new ReentrantLock();
    private boolean startReadCard = true;

    private OnIDCardDataListener onDataListener;

    public void setOnProgressListener(OnIDCardDataListener onProgressListener) {
        this.onDataListener = onProgressListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //身份证
        thread = new Thread(task);
        thread.start();
    }

     Runnable task = new Runnable() {
        @Override
        public void run() {
            while (isAuto) {
                lock.lock();
                try {
                        com.decard.entitys.IDCard idCardData;
                        if (!choose) {
                            //标准协议
                            idCardData = BasicOper.dc_get_i_d_raw_info();
                        } else {
                            //公安部协议
                            idCardData = BasicOper.dc_SamAReadCardInfo(1);
                        }
                        if(idCardData!= null){
                            onDataListener.onDataMsg(idCardData);
                            Thread.sleep(1500);
                        }else {
                            Thread.sleep(800);
                        }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    };

    public class IDCardBinder extends Binder {
        public IDCardService getService(){
            return IDCardService.this;
        }
    }

    public interface OnIDCardDataListener {
        void onDataMsg(com.decard.entitys.IDCard data);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new IDCardBinder();
    }
}
