package com.example.administrator.usbtest;

import android.util.Log;

import com.decard.NDKMethod.BasicOper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hizha on 2017/7/31.
 */

public class M1CardModel {
    private M1CardListener mListener;

    public M1CardModel(M1CardListener listener){
        mListener = listener;
    }

    public void bt_download(String btDownload, String sectorsNum, int keyType, String newPasswordKey, int spinnerPosition) {
        if ("All".equals(sectorsNum)) {
            List<String> loadKeyList = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                if (!MDSEUtils.isSucceed(BasicOper.dc_load_key_hex(keyType, i, newPasswordKey)))
                    loadKeyList.add(Integer.toString(i));
            }
            mListener.getM1CardResult(ConstUtils.BT_DOWNLOAD,loadKeyList,"","");

        } else {
            String result = BasicOper.dc_load_key_hex(keyType, spinnerPosition - 1, newPasswordKey);
            mListener.getM1CardResult(ConstUtils.BT_DOWNLOAD,null,result,"");
        }
    }

    public void bt_read_card(String btReadCard, int keyType, int firstVisibleItemPosition) {
        int currentSectors = firstVisibleItemPosition - 1;

        List<String> authFailList = new ArrayList<>();
        if (firstVisibleItemPosition == 0) {
            for (int i = 0; i < 16; i++) {
                if (MDSEUtils.isSucceed(BasicOper.dc_authentication(keyType, i))) {
//                    readSectorData(i);
                    mListener.getM1CardResult(btReadCard,null,i+"","");

                } else {
                    BasicOper.dc_card_hex(0);// 如果接口调用失败，需要进行复位才能进行后续的操作
                    authFailList.add(Integer.toString(i));
                }
            }
            mListener.getM1CardResult(btReadCard,authFailList,"","");

        } else {
            boolean authSucceed = MDSEUtils.isSucceed(BasicOper.dc_authentication(keyType, currentSectors));
            String isSucceed;
            if (authSucceed){
                isSucceed = "true";
            }else {
                isSucceed = "false";
            }
            Log.d("tag","isSucceed : " +isSucceed);
            mListener.getM1CardResult(btReadCard,null,isSucceed,currentSectors+"");
//            readSectorData(currentSectors);
        }
    }
}
