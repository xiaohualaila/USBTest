package com.example.administrator.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.administrator.usbtest.ConstUtils;
import com.example.administrator.usbtest.UltralightCardListener;
import com.example.administrator.usbtest.UltralightCardModel;

/**
 * 读卡
 */

public class MyService extends Service implements UltralightCardListener {
    //UltralightCard读卡
    private final int TIME = 500;
    private UltralightCardModel model;

    private OnDataListener onDataListener;

    public void setOnProgressListener(OnDataListener onProgressListener) {
        this.onDataListener = onProgressListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initReadCard();
    }

    private void initReadCard() {
        model = new UltralightCardModel(this);

    }

    Handler handler=new Handler();

    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            model.bt_seek_card(ConstUtils.BT_SEEK_CARD);
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return new MsgBinder();
    }

    @Override
    public void getUltralightCardResult(String cmd, String result) {
        Log.i("sss","cmd++ "+cmd);
        Log.i("sss","result++ "+result);
        if(!result.equals("1003|无卡或无法寻到卡片")){
            if(!result.equals("0001|操作失败")){
                onDataListener.onMsg(result);
                handler.postDelayed(runnable, 2000);
            }else {
                handler.postDelayed(runnable, TIME);
            }
        }else {
            handler.postDelayed(runnable, TIME);
        }

    }

    public class MsgBinder extends Binder {

        public MyService getService(){
            return MyService.this;
        }
        public void initReadCard() {
            handler.postDelayed(runnable, TIME);
        }

    }

    public interface OnDataListener {
        void onMsg(String code);
    }

}
