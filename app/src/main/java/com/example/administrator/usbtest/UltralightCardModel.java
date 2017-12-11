package com.example.administrator.usbtest;

import android.text.TextUtils;

import com.decard.NDKMethod.BasicOper;

/**
 * Created by hizha on 2017/7/31.
 */

public class UltralightCardModel {
    private UltralightCardListener mListener;

    public UltralightCardModel(UltralightCardListener listener){
        mListener = listener;
    }

    public void bt_seek_card(String btSeekCard) {
        String cardNum = MDSEUtils.returnResult(BasicOper.dc_card_n_hex(0));
        mListener.getUltralightCardResult(btSeekCard,cardNum);
    }

    public void bt_read_card(String btReadCard, String readPieceNum) {
        if (!TextUtils.isEmpty(readPieceNum)) {
            String pieceData = MDSEUtils.returnResult(BasicOper.dc_read_hex(Integer.parseInt(readPieceNum)));
            mListener.getUltralightCardResult(btReadCard,pieceData);
        }
    }

    public void bt_write_card(String btWriteCard, String writePieceNum, String writePieceData) {

        if (!TextUtils.isEmpty(writePieceNum)) {
            String result = BasicOper.dc_write_hex(Integer.parseInt(writePieceNum), writePieceData);
           mListener.getUltralightCardResult(btWriteCard,result);
        }
    }

    public void bt_check_password(String btCheckPassword, String password) {
        if (!TextUtils.isEmpty(password)) {
            String result = BasicOper.dc_auth_ulc_hex(password);
            mListener.getUltralightCardResult(btCheckPassword,result);
        }
    }
}
