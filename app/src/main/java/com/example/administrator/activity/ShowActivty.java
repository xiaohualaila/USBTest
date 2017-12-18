package com.example.administrator.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.decard.NDKMethod.BasicOper;
import com.decard.entitys.IDCard;
import com.example.administrator.service.IDCardService;
import com.example.administrator.service.M1Service;
import com.example.administrator.service.MyService;
import com.example.administrator.usbtest.R;
import com.example.administrator.usbtest.SPUtils;
import com.example.administrator.usbtest.Utils;
import butterknife.ButterKnife;


/**
 * 该页面吧调通的外挂设备单独开启
 */
public class ShowActivty extends AppCompatActivity  {
    private SPUtils settingSp;
    private String USB="";

    private MyService myService;
    private MyService.MsgBinder myBinder;
    private M1Service m1Service;
    private M1Service.MsgBinder myM1Binder;
    private IDCardService idCardService;
    private IDCardService.IDCardBinder idCardBinder;
    private boolean uitralight = true;
    private boolean scan = true;
    private boolean idcard = true;

    private boolean conn1 = false;
    private boolean conn2 = false;
    private boolean conn3 = false;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (MyService.MsgBinder) service;
            myBinder.initReadCard();
            myService = myBinder.getService();
            myService.setOnProgressListener(new MyService.OnDataListener() {
                @Override
                public void onMsg(String result) {
                    BasicOper.dc_beep(5);
                    Log.i("sss",result + ">>>>>>>>>>>>>>>>");
                }
            });
        }
    };
    private ServiceConnection connectionM1 = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myM1Binder = (M1Service.MsgBinder) service;
            myM1Binder.initReadCard();
            m1Service = myM1Binder.getService();
            m1Service.setOnProgressListener(new M1Service.OnDataListener() {
                @Override
                public void onMsg(String code) {
                    BasicOper.dc_beep(5);
                    Log.i("sss",code + ">>>>>>>>>>>>>>>>");
                }
            });
        }
    };

    private ServiceConnection connectionIDCard = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            idCardBinder = (IDCardService.IDCardBinder) service;
            idCardService = idCardBinder.getService();
            idCardService.setOnProgressListener(new IDCardService.OnIDCardDataListener() {
                @Override
                public void onDataMsg(IDCard idCardData) {
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
                        Log.i("sss", BasicOper.dc_getfingerdata());

                    }
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

    }

    //打开设备
    public void onOpenConnectPort(){
        BasicOper.dc_AUSB_ReqPermission(this);
        int portSate = BasicOper.dc_open(USB, this, "", 0);
        if (portSate >= 0) {
            BasicOper.dc_beep(5);
            Log.d("sss", "portSate:" + portSate + "设备已连接");
            if(uitralight){
                //UltralightCard开启服务
                Intent bindIntent1 = new Intent(this, MyService.class);
                bindService(bindIntent1, connection, BIND_AUTO_CREATE);
                conn1 = true;
            }else {
                //M1
                Intent bindIntent2 = new Intent(this, M1Service.class);
                bindService(bindIntent2, connectionM1, BIND_AUTO_CREATE);
                conn2 = true;
            }
            if(idcard){
                Intent bindIntent3 = new Intent(this, IDCardService.class);
                bindService(bindIntent3, connectionIDCard, BIND_AUTO_CREATE);
                conn3 = true;
            }
        }else {
            Toast.makeText(this,"设备没有连接上！",Toast.LENGTH_LONG).show();
        }
    }

    //关闭设备
    public void onDisConnectPort(View view) {
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
        if(conn1){
            unbindService(connection);
        }
        if(conn2){
            unbindService(connectionM1);
        }
        if(conn3){
            unbindService(connectionIDCard);
        }
    }


}
