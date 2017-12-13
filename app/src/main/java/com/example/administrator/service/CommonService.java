package com.example.administrator.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.decard.NDKMethod.BasicOper;
import com.example.administrator.usbtest.ConstUtils;
import com.example.administrator.usbtest.M1CardListener;
import com.example.administrator.usbtest.M1CardModel;
import com.example.administrator.usbtest.MDSEUtils;
import com.example.administrator.usbtest.SectorDataBean;
import com.example.administrator.usbtest.UltralightCardListener;
import com.example.administrator.usbtest.UltralightCardModel;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Administrator on 2017/12/13.
 */

public class CommonService extends Service implements UltralightCardListener,M1CardListener {
    private int flag = 1;
    //身份证
    private Thread thread;
    private boolean isAuto = true;
    private boolean choose = false;//false标准协议,true公安部协议
    private static Lock lock = new ReentrantLock();
    //UltralightCard读卡
    private UltralightCardModel model;

    //M1
    private M1CardModel model2;
    private boolean isHaveOne = false;

    private boolean uitralight = true;
    private boolean scan = true;
    private boolean idcard = true;

    private OnDataListener onDataListener;

    public void setOnProgressListener(OnDataListener onProgressListener) {
        this.onDataListener = onProgressListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //身份证
        thread = new Thread(task);
        thread.start();
        //UltralightCard
        model = new UltralightCardModel(this);
        //
        model2 = new M1CardModel(this);
    }

     Runnable task = new Runnable() {
        @Override
        public void run() {
            while (isAuto) {
                lock.lock();
                try {
                    if(flag == 1){//UltralightCard
                        if(uitralight){
                            model.bt_seek_card(ConstUtils.BT_SEEK_CARD);
                            Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>UltralightCard");
                            Thread.sleep(500);
                        }else {//M1
                            if (MDSEUtils.isSucceed(BasicOper.dc_card_hex(1))) {
                                final int keyType = 0;// 0 : 4; 密钥套号 0(0套A密钥)  4(0套B密钥)
                                isHaveOne = true;
                                model2.bt_read_card(ConstUtils.BT_READ_CARD,keyType,0);
                            }
                            Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>M1");
                            Thread.sleep(500);
                        }
                        flag = 2;
                    }else if(flag == 2){//身份证
                        Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>身份证");
                        com.decard.entitys.IDCard idCardData;
                        if (!choose) {
                            //标准协议
                            idCardData = BasicOper.dc_get_i_d_raw_info();

                        } else {
                            //公安部协议
                            idCardData = BasicOper.dc_SamAReadCardInfo(1);
                        }
                        if(idCardData!= null){
                            onDataListener.onIDCardMsg(idCardData);
                        }
                        Thread.sleep(500);
                        flag = 1;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    };

    @Override
    public void getUltralightCardResult(String cmd, String result) {
        Log.i("sss","cmd++ "+cmd);
        Log.i("sss","result++ "+result);
        if(!result.equals("1003|无卡或无法寻到卡片")){
            if(!result.equals("0001|操作失败")){
                onDataListener.onUltralightCardMsg(result);
            }
        }
    }

    @Override
    public void getM1CardResult(String cmd, List<String> list, String result, String resultCode) {
        if(isHaveOne){
            isHaveOne = false;
            if (list == null){
                if (result.length() > 2){
                    readSectorData(Integer.parseInt(resultCode));
                }else {
                    readSectorData(Integer.parseInt(result));
                }
            }
        }
    }

    private void readSectorData(int currentSectors) {
        boolean b = true;
        int piece = (currentSectors + 1) * 4;
        SectorDataBean sectorDataBean = new SectorDataBean();
        String[] pieceDatas = new String[4];
        for (int i = piece - 4, j = 0; i < piece; i++, j++) {
            String pieceData = MDSEUtils.returnResult(BasicOper.dc_read_hex(i));
            pieceDatas[j] = pieceData;
        }
        sectorDataBean.pieceZero = pieceDatas[0];

        if (b){
            String string = sectorDataBean.pieceZero.substring(0,8);
            onDataListener.onM1CardMsg(string);
            b = false;
        }

    }


    public class MyBinder extends Binder {
        public CommonService getService(){
            return CommonService.this;
        }

        public void setIntentData(boolean uitralight, boolean scan, boolean idcard) {
            CommonService.this.uitralight = uitralight;
            CommonService.this.scan = scan;
            CommonService.this.idcard = idcard;
        }

        public void stopThread() {
            thread.interrupt();
        }
    }

    public interface OnDataListener {
        void onIDCardMsg(com.decard.entitys.IDCard data);
        void onUltralightCardMsg(String result);
        void onM1CardMsg(String result);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }
}
