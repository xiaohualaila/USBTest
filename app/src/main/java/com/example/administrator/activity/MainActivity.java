package com.example.administrator.activity;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.decard.NDKMethod.BasicOper;
import com.example.administrator.usbtest.AssistBean;
import com.example.administrator.usbtest.ComBean;
import com.example.administrator.usbtest.ConstUtils;
import com.example.administrator.usbtest.M1CardListener;
import com.example.administrator.usbtest.M1CardModel;
import com.example.administrator.usbtest.MDSEUtils;
import com.example.administrator.R;
import com.example.administrator.usbtest.SPUtils;
import com.example.administrator.usbtest.SectorDataBean;
import com.example.administrator.usbtest.SerialHelper;
import com.example.administrator.usbtest.UltralightCardListener;
import com.example.administrator.usbtest.UltralightCardModel;
import com.example.administrator.usbtest.Utils;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements UltralightCardListener,M1CardListener {
    private SPUtils settingSp;
    private String USB="";
    private String deviceType = "";
    private UltralightCardModel model;
    private M1CardModel model2;
    private TextView textView2;

    //m1
    private int index = 0;
    private boolean isHaveOne = false;

    //身份证
    private Thread thread;
    private boolean isAuto = true;
    private boolean choose = false;//false标准协议,true公安部协议
    private static Lock lock = new ReentrantLock();
    private IDCardHandler idCardHandler;
    private boolean startReadCard = false;

   //串口
    SerialControl ComA;
    DispQueueThread DispQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        textView2 = (TextView)findViewById(R.id.textView2);
        Utils.init(getApplicationContext());
        settingSp = new SPUtils(getString(R.string.settingSp));
       //UltralightCard初始化
        model = new UltralightCardModel(this);
       //m1
        model2 = new M1CardModel(this);
        //身份证
        idCardHandler = new IDCardHandler();
        //串口
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();
//        AssistData = getAssistData();
//        setControls();
    }

    protected void onResume() {
        super.onResume();
        USB = settingSp.getString(getString(R.string.usbKey), getString(R.string.androidUsb));
        //身份证
        thread = new Thread(task);
        thread.start();
    }

    //UltralightCard打开设备
    public void onOpen(){
        BasicOper.dc_AUSB_ReqPermission(this);
        int portSate = BasicOper.dc_open(USB, this, "", 0);
        if (portSate >= 0) {
            BasicOper.dc_beep(5);
            Log.d("sss", "portSate:" + portSate + "设备已连接");
        }else {
            Log.d("sss", "portSate:" + portSate);
        }
    }

    //UltralightCard关闭设备
    public void onDisConnectPort(View view) {
        int close_status = BasicOper.dc_exit();
        if(close_status>=0){
            Log.i("sss","设备关闭");
        }else {
            Log.i("sss","Port has closed");
        }
    }


    //UltralightCard读卡
    public void seek_card(View view) {
        model.bt_seek_card(ConstUtils.BT_SEEK_CARD);
    }

    //UltralightCard读卡
    @Override
    public void getUltralightCardResult(String cmd, String result) {
       Log.i("sss",result + ">>>>>>>>>>>>>>>>");
        textView2.setText(result);
    }

    //M1读卡
    public void m1_read(View view) {
        if (!MDSEUtils.isSucceed(BasicOper.dc_card_hex(1))) {
            Toast.makeText(this,"M1卡不存在！",Toast.LENGTH_LONG).show();
            return;
        }
        final int keyType = 0;// 0 : 4; 密钥套号 0(0套A密钥)  4(0套B密钥)
        index = 0;
        isHaveOne = true;
        model2.bt_read_card(ConstUtils.BT_READ_CARD,keyType,0);

    }
    //M1读卡
    @Override
    public void getM1CardResult(String cmd, List<String> list, String result, String resultCode) {
        if(isHaveOne){
            isHaveOne = false;
            index ++;
            Log.i("xxx",index + "++++");
            if (list == null){
                if (result.length() > 2){
                    readSectorData(Integer.parseInt(resultCode));
                }else {
                    readSectorData(Integer.parseInt(result));
                }
            }
        }
    }
    //M1读卡
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
            textView2.setText(string);
            b = false;
        }

        Log.i("xxx",pieceDatas[0]);
        Log.i("xxx",pieceDatas[1]);
        Log.i("xxx",pieceDatas[2]);
        Log.i("xxx",pieceDatas[3]);
    }



    //身份证读卡
    public void idcard_read(View view) {
//        idCardHandler.post(runnable);
        startReadCard = true;
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(2000);
                Message msg = Message.obtain();
                com.decard.entitys.IDCard idCardData;
                if (!choose) {
                    //标准协议
                    Log.d("TAG", "dc_get_i_d_raw_info");
                    idCardData = BasicOper.dc_get_i_d_raw_info();
                } else {
                    //公安部协议
                    Log.d("TAG", "dc_SamAReadCardInfo");
                    idCardData = BasicOper.dc_SamAReadCardInfo(1);
                }
                msg.obj = idCardData;
                idCardHandler.sendMessage(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable task = new Runnable() {
        @Override
        public void run() {
            while (isAuto) {
                lock.lock();
                try {
                    Thread.sleep(2000);
                    if (startReadCard) {

                        Message msg = Message.obtain();
                        com.decard.entitys.IDCard idCardData;
                        if (!choose) {
                            //标准协议
                            Log.d("TAG","dc_get_i_d_raw_info");
                            idCardData = BasicOper.dc_get_i_d_raw_info();
                        } else {
                            //公安部协议
                            Log.d("TAG","dc_SamAReadCardInfo");
                            idCardData = BasicOper.dc_SamAReadCardInfo(1);
                        }
                        msg.obj = idCardData;
                        idCardHandler.sendMessage(msg);
                        startReadCard = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    };


    @SuppressLint("HandlerLeak")
    private class IDCardHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            com.decard.entitys.IDCard idCardData = (com.decard.entitys.IDCard) msg.obj;
            readIDCardData(idCardData);
        }
    }

    private void readIDCardData(com.decard.entitys.IDCard idCardData) {
        if (idCardData != null) {
            Log.i("IIII",idCardData.getName());
            Log.i("IIII",idCardData.getSex());
            Log.i("IIII",idCardData.getNation());
            Log.i("IIII",idCardData.getBirthday());
            Log.i("IIII",idCardData.getAddress());
            Log.i("IIII",idCardData.getId());
            Log.i("IIII",idCardData.getOffice());
            Log.i("IIII",idCardData.getEndTime());
            Log.i("IIII",idCardData.getName());
            Log.i("IIII",BasicOper.dc_getfingerdata());

//            tvName.setText(idCardData.getName());
//            tvSex.setText(idCardData.getSex());
//            tvNation.setText(idCardData.getNation());
//            tvBirthday.setText(idCardData.getBirthday());
//            tvHomeAddress.setText(idCardData.getAddress());
//            tvIdNum.setText(idCardData.getId());
//            tvIssueOrgan.setText(idCardData.getOffice());
//            tvPeriodValidity.setText(idCardData.getEndTime());
//            ivPhoto.setImageBitmap(ConvertUtils.bytes2Bitmap(ConvertUtils.hexString2Bytes(idCardData.getPhotoDataHexStr())));
//            tvFingerprintInfo.setText(BasicOper.dc_getfingerdata());

//            tvSucceedCount.setText(Integer.toString(succeedCount));
        } else {
//            tvName.setText("");
//            tvSex.setText("");
//            tvNation.setText("");
//            tvBirthday.setText("");
//            tvHomeAddress.setText("");
//            tvIdNum.setText("");
//            tvIssueOrgan.setText("");
//            tvPeriodValidity.setText("");
//            ivPhoto.setImageDrawable(getHoldingActivity().getResources().getDrawable(R.drawable.identity_image));
//            tvFingerprintInfo.setText("");

//            tvFailCount.setText(Integer.toString(failCount));
        }
        startReadCard = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isAuto = false;
        startReadCard = false;
    }



    //打开串口
    public void openErWeiMa(View view) {
        ComA.setPort("/dev/ttyS4");
        ComA.setBaudRate("115200");
        OpenComPort(ComA);
    }

    private void OpenComPort(SerialHelper ComPort){
        try
        {
            ComPort.open();
        } catch (SecurityException e) {
      //     Log.i("xxx","SecurityException" + e.toString());
        } catch (IOException e) {
         //   Log.i("xxx","IOException" + e.toString());
        } catch (InvalidParameterException e) {
       //     Log.i("xxx","InvalidParameterException" + e.toString());
        }
    }

    public void closeErWeiMa(View view) {
        CloseComPort(ComA);
    }

    private void CloseComPort(SerialHelper ComPort){
        if (ComPort!=null){
            ComPort.stopSend();
            ComPort.close();
        }
    }

    private class SerialControl extends SerialHelper{

        public SerialControl(){
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData)
        {
            DispQueue.AddQueue(ComRecData);

        }
    }

    private class DispQueueThread extends Thread{
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();
        @Override
        public void run() {
            super.run();
            while(!isInterrupted()) {
                final ComBean ComData;
                while((ComData=QueueList.poll())!=null)
                {
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            DispRecData(ComData);
                        }
                    });
                    try
                    {
                        Thread.sleep(300);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        String str = "";
        boolean kk = true;
        private void DispRecData(ComBean ComRecData){

            if(kk){
                 str = new String(ComRecData.bRec);
                 kk = false;
            }else {
                Log.i("xxx",str+ new String(ComRecData.bRec) + "");
                kk = true;
            }

        }



        public synchronized void AddQueue(ComBean ComData){
            QueueList.add(ComData);
        }
    }


}
