package com.example.administrator.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;
import com.decard.entitys.IDCard;
import com.example.administrator.service.CommonService;
import com.example.administrator.usbtest.ComBean;
import com.example.administrator.usbtest.R;
import com.example.administrator.usbtest.SPUtils;
import com.example.administrator.usbtest.SerialHelper;
import com.example.administrator.usbtest.Utils;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.Queue;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/12/12.
 */

public class CommonActivty extends AppCompatActivity  {
    private SPUtils settingSp;
    private String USB="";
    private boolean isOpenDoor = false;
    private CommonService myService;
    private CommonService.MyBinder myBinder;
    private Handler handler = new Handler();

    private boolean uitralight = true;
    private boolean scan = true;
    private boolean idcard = true;

    //串口
    SerialControl ComA;
    DispQueueThread DispQueue;


    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (CommonService.MyBinder) service;
            myService = myBinder.getService();
            myBinder.setIntentData(uitralight,scan,idcard);
            myService.setOnProgressListener(new CommonService.OnDataListener() {
                @Override
                public void onIDCardMsg(IDCard idCardData) {//身份证
                    if (idCardData != null) {
                        BasicOper.dc_beep(5);
                        Log.i("sss", idCardData.getName());
                        Log.i("sss", idCardData.getSex());
                        Log.i("sss", idCardData.getNation());
                        Log.i("sss", idCardData.getBirthday());
                        Log.i("sss", idCardData.getAddress());
                        Log.i("sss", idCardData.getId());
                        Log.i("sss", idCardData.getOffice());
                        Log.i("sss", idCardData.getEndTime());
                        Log.i("sss", idCardData.getName());
                    }
                }

                @Override
                public void onUltralightCardMsg(String result) {
                    BasicOper.dc_beep(5);
                    Log.i("sss",result + ">>>>>>>>>>>>>>>>");
                    if(!isOpenDoor){
                        isOpenDoor = true;
                        rkGpioControlNative.ControlGpio(1, 0);//开门
                        handler.postDelayed(runnable,500);
                    }
                }

                @Override
                public void onM1CardMsg(String code) {
                    BasicOper.dc_beep(5);
                    Log.i("sss",code + ">>>>>>>>>>>>>>>>");
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        ButterKnife.bind(this);
        Intent intent = getIntent();
        uitralight = intent.getBooleanExtra("uitralight",true);
        scan = intent.getBooleanExtra("scan",false);
        idcard = intent.getBooleanExtra("idcard",false);

        Utils.init(getApplicationContext());
        settingSp = new SPUtils(getString(R.string.settingSp));
        USB = settingSp.getString(getString(R.string.usbKey), getString(R.string.androidUsb));
        onOpenConnectPort();
        rkGpioControlNative.init();
        //串口
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();
        openErWeiMa();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(isOpenDoor){
                rkGpioControlNative.ControlGpio(1, 1);//关门
                isOpenDoor = false;
            }
        }
    };

    //打开设备
    public void onOpenConnectPort(){
        BasicOper.dc_AUSB_ReqPermission(this);
        int portSate = BasicOper.dc_open(USB, this, "", 0);
        if (portSate >= 0) {
            BasicOper.dc_beep(5);
            Log.d("sss", "portSate:" + portSate + "设备已连接");
                Intent bindIntent1 = new Intent(this, CommonService.class);
                bindService(bindIntent1, connection, BIND_AUTO_CREATE);

        }else {
            Toast.makeText(this,"设备没有连接上！",Toast.LENGTH_LONG).show();
        }
    }

    //关闭设备
    public void onDisConnectPort() {
        int close_status = BasicOper.dc_exit();
        if(close_status>=0){
            Log.i("sss","设备关闭");
        }else {
            Log.i("sss","Port has closed");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myBinder.stopThread();
         unbindService(connection);
        onDisConnectPort();
        adcNative.close(0);
        adcNative.close(2);
        rkGpioControlNative.close();
        closeErWeiMa();
    }

    //打开串口
    public void openErWeiMa() {
        ComA.setPort("/dev/ttyS4");
        ComA.setBaudRate("115200");
        OpenComPort(ComA);
    }

    private void OpenComPort(SerialHelper ComPort){
        try
        {
            ComPort.open();
        } catch (SecurityException e) {
                Log.i("xxx","SecurityException" + e.toString());
        } catch (IOException e) {
                Log.i("xxx","IOException" + e.toString());
        } catch (InvalidParameterException e) {
                Log.i("xxx","InvalidParameterException" + e.toString());
        }
    }

    public void closeErWeiMa() {
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

    String str = "";
    boolean kk = true;
    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (kk) {
                                str = new String(ComData.bRec);
                                kk = false;
                            } else {
                                Log.i("xxx", str + new String(ComData.bRec) + "");
                                kk = true;
                            }
                        }
                    });
                    try {
                        Thread.sleep(300);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }


        public synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }
}
