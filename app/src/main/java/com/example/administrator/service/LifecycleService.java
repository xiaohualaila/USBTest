package com.example.administrator.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
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
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2017/12/13.
 */

public class LifecycleService extends Service implements UltralightCardListener,M1CardListener {
    private int flag = 1;
    //身份证
    private boolean choose = false;//false标准协议,true公安部协议
    //UltralightCard读卡
    private UltralightCardModel model;

    //M1
    private M1CardModel model2;
    private boolean isHaveOne = false;

    private boolean uitralight = true;
    private boolean idcard = true;
    private Subscription sub;
    private OnDataListener onDataListener;

    public void setOnProgressListener(OnDataListener onProgressListener) {
        this.onDataListener = onProgressListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //UltralightCard
        model = new UltralightCardModel(this);
        //M1
        model2 = new M1CardModel(this);

        todoLifecycle();
    }

    public void todoLifecycle(){
         sub = Observable.interval(2000, 800, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if(flag == 1){//UltralightCard
                            if(uitralight){
                                model.bt_seek_card(ConstUtils.BT_SEEK_CARD);
                             //   Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>UltralightCard");
                            }else {//M1
                                if (MDSEUtils.isSucceed(BasicOper.dc_card_hex(1))) {
                                    final int keyType = 0;// 0 : 4; 密钥套号 0(0套A密钥)  4(0套B密钥)
                                    isHaveOne = true;
                                    model2.bt_read_card(ConstUtils.BT_READ_CARD,keyType,0);
                                }
                              //  Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>M1");
                            }
                            flag = 2;
                        }else if(flag == 2){//身份证
                            if(idcard){
                             //   Log.i("sss",">>>>>>>>>>>>>>>>>>>>>>身份证");
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
                            }
                            flag = 1;
                        }
                    }
                });
    }



    @Override
    public void getUltralightCardResult(String cmd, String result) {
        if(!result.equals("1003|无卡或无法寻到卡片")){
            if(!result.equals("0001|操作失败")){
                if(!result.equals("FFFF|操作失败")){
                    onDataListener.onUltralightCardMsg(result);
                }
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
        public LifecycleService getService(){
            return LifecycleService.this;
        }

        public void setIntentData(boolean uitralight, boolean idcard) {
            LifecycleService.this.uitralight = uitralight;
            LifecycleService.this.idcard = idcard;
        }

        public void stopThread() {
            sub.unsubscribe();
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
