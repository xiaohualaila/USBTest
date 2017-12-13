package com.example.administrator.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.decard.NDKMethod.BasicOper;
import com.example.administrator.usbtest.ConstUtils;
import com.example.administrator.usbtest.M1CardListener;
import com.example.administrator.usbtest.M1CardModel;
import com.example.administrator.usbtest.MDSEUtils;
import com.example.administrator.usbtest.SectorDataBean;
import java.util.List;

/**
 * 读卡
 */

public class M1Service extends Service implements M1CardListener {
    private final int TIME = 500;
    private M1CardModel model2;

    //m1
    private boolean isHaveOne = false;
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
        model2 = new M1CardModel(M1Service.this);
    }

    Handler handler=new Handler();

    Runnable runnable=new Runnable() {
        @Override
        public void run() {
                try {
                    if (!MDSEUtils.isSucceed(BasicOper.dc_card_hex(1))) {
                        Log.i("sss","M1卡不存在！");
                        handler.postDelayed(runnable, TIME);
                    }else {
                        final int keyType = 0;// 0 : 4; 密钥套号 0(0套A密钥)  4(0套B密钥)
                        isHaveOne = true;
                        model2.bt_read_card(ConstUtils.BT_READ_CARD,keyType,0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

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
    public void getM1CardResult(String cmd, List<String> list, String result, String resultCode) {
        if(isHaveOne){
            handler.postDelayed(runnable, 2000);
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
        sectorDataBean.pieceOne = pieceDatas[1];
        sectorDataBean.pieceTwo = pieceDatas[2];
        sectorDataBean.pieceThree = pieceDatas[3];

        if (b){
            String string = sectorDataBean.pieceZero.substring(0,8);
            onDataListener.onMsg(string);
            b = false;
        }

        Log.i("xxx",pieceDatas[0]);
        Log.i("xxx",pieceDatas[1]);
        Log.i("xxx",pieceDatas[2]);
        Log.i("xxx",pieceDatas[3]);
    }

    public class MsgBinder extends Binder {
        public M1Service getService(){
            return M1Service.this;
        }
        public void initReadCard() {
                handler.postDelayed(runnable, TIME);
        }
    }

    public interface OnDataListener {
        void onMsg(String code);
    }

}
