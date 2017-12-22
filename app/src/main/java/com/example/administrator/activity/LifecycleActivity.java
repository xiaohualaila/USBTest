package com.example.administrator.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;
import com.decard.entitys.IDCard;
import com.example.administrator.bean.WhiteList;
import com.example.administrator.retrofit.Api;
import com.example.administrator.retrofit.ConnectUrl;
import com.example.administrator.service.LifecycleService;
import com.example.administrator.usbtest.ConvertUtils;
import com.example.administrator.usbtest.R;
import com.example.administrator.usbtest.SPUtils;
import com.example.administrator.usbtest.ScanHelper;
import com.example.administrator.usbtest.Utils;
import com.example.administrator.util.FileUtil;
import com.example.administrator.util.GetDataUtil;
import com.example.administrator.util.MyUtil;

import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;



public class LifecycleActivity extends AppCompatActivity  {
    @BindView(R.id.name_tv)
    TextView name_tv;
    @BindView(R.id.sex_tv)
    TextView sex_tv;
    @BindView(R.id.native_tv)
    TextView native_tv;
    @BindView(R.id.age_tv)
    TextView age_tv;
    @BindView(R.id.address_tv)
    TextView address_tv;
    @BindView(R.id.idcard_num_tv)
    TextView idcard_num_tv;

    @BindView(R.id.code_type)
    TextView code_type;
    @BindView(R.id.code_tv)
    TextView code_tv;
    @BindView(R.id.ivPhoto)
    ImageView ivPhoto;
    private SPUtils settingSp;
    private String USB="";
    private boolean isOpenDoor = false;
    private LifecycleService myService;
    private LifecycleService.MyBinder myBinder;
    private Handler handler = new Handler();

    private boolean uitralight = true;
    private boolean scan = true;
    private boolean idcard = true;
    private boolean isHaveThree = true;

    //串口
    protected ScanHelper mScanHelper;
    private Subscription sub;
    private boolean isReading = false;

    private String device_id;

    private int type;//3 身份证,1 Ultralight,4 M1,2串口
    private String ticketNum;
    private File newFile;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (LifecycleService.MyBinder) service;
            myService = myBinder.getService();
            myBinder.setIntentData(uitralight,idcard);
            myService.setOnProgressListener(new LifecycleService.OnDataListener() {
                @Override
                public void onIDCardMsg(final IDCard idCardData) {//身份证
                    if(!isReading) {
                        isReading = true;
                        if (idCardData != null) {
                            BasicOper.dc_beep(5);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    name_tv.setText(idCardData.getName().trim());
                                    sex_tv.setText(idCardData.getSex().trim());
                                    native_tv.setText(idCardData.getNation().trim());
                                    age_tv.setText(idCardData.getBirthday().trim());
                                    address_tv.setText(idCardData.getAddress().trim());
                                    idcard_num_tv.setText(idCardData.getId().trim());

                                    Bitmap idcard = ConvertUtils.bytes2Bitmap(ConvertUtils.hexString2Bytes(idCardData.getPhotoDataHexStr()));
                                    ivPhoto.setImageBitmap(idcard);
                                    try {
                                        if (newFile != null){
                                            if(newFile.exists()){
                                                newFile.delete();
                                            }
                                        }

                                         newFile = FileUtil.saveFile(idcard, FileUtil.getPath() + File.separator  + "photo", FileUtil.getTime() + ".jpeg");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    type = 3;
                                    ticketNum = idCardData.getId().trim();
                                    upload();
                                }
                            });
                        }

                    }
                }

                @Override
                public void onUltralightCardMsg(final String result) {
                    if(!isReading){
                        isReading = true;
                         BasicOper.dc_beep(5);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                code_type.setText("Ultralight卡");
                                code_tv.setText(result);
                                type = 1;
                                ticketNum = result.trim() + "00";
                                upload();
                            }
                        });

                    }
                }

                @Override
                public void onM1CardMsg(final String code) {
                    if(!isReading) {
                        BasicOper.dc_beep(5);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                code_type.setText("M1卡");
                                code_tv.setText(code);
                                type = 4;
                                ticketNum = code.trim();
                                upload();
                            }
                        });

                    }
                }
            });
        }
    };

    public void doErWeiMaLifecycle(){
        sub = Observable.interval(2000, 500, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if(!isReading){
                            initScanSerialPort();
                        }
                    }
                });
    }

    private void initScanSerialPort() {
        try {
            mScanHelper = new ScanHelper("/dev/ttyS4", 115200, new ScanHelper.OnDataReceived() {
                @Override
                public void received(byte[] buffer, int size) {
                    final String str =new String(buffer);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            code_type.setText("二维码");
                            code_tv.setText(str.trim());
                            type = 2;
                            ticketNum = str.trim();
                            upload();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void openDoor(){
        if(!isOpenDoor){
            isOpenDoor = true;
            rkGpioControlNative.ControlGpio(1, 0);//开门
            handler.postDelayed(runnable,500);
            Log.i("xxxx",">>>>openDoor>>>>>>>");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        ButterKnife.bind(this);
        device_id = MyUtil.getDeviceID(this);//获取设备号
        Intent intent = getIntent();
        uitralight = intent.getBooleanExtra("uitralight",true);
        scan = intent.getBooleanExtra("scan",true);
        idcard = intent.getBooleanExtra("idcard",true);
        isHaveThree = intent.getBooleanExtra("isHaveThree",true);
        Utils.init(getApplicationContext());
        settingSp = new SPUtils(getString(R.string.settingSp));
        USB = settingSp.getString(getString(R.string.usbKey), getString(R.string.androidUsb));
        if(isHaveThree){
            onOpenConnectPort();
        }
        rkGpioControlNative.init();
        //串口
        if(scan){
            doErWeiMaLifecycle();
        }


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
            Intent bindIntent1 = new Intent(this, LifecycleService.class);
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
        if(isHaveThree){
            myBinder.stopThread();
            unbindService(connection);
            onDisConnectPort();
        }
        adcNative.close(0);
        adcNative.close(2);
        rkGpioControlNative.close();
        if(scan){
            sub.unsubscribe();
        }
    }


    private void  upload(){
        Log.i("xxxx","type >>" + type +"" +" ticketNum>>" + ticketNum);
        boolean isNetAble = MyUtil.isNetworkAvailable(this);
        if(isNetAble){
            if(type == 2||type == 4||type == 1){
                Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                        .uploadData(device_id,type,ticketNum)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<JSONObject>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.i("xxxx",e.toString());
                                isReading = false;
                            }

                            @Override
                            public void onNext(JSONObject jsonObject) {
                                jsonObjectResult(jsonObject);
                            }
                        });
            }else {
                if(!newFile.exists()){
                    isReading = false;
                    return;
                }
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), newFile);
                builder.addFormDataPart("idCardPhoto", newFile.getName(), requestBody);
                Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                        .uploadData(device_id,type,ticketNum,builder.build().parts())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<JSONObject>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.i("xxxx",e.toString());
                                isReading = false;
                            }

                            @Override
                            public void onNext(JSONObject jsonObject) {
                                jsonObjectResult(jsonObject);
                            }
                        });
                isReading = false;
            }
        }else {
            findCount();
        }
    }

    private void jsonObjectResult(JSONObject jsonObject){
        Log.i("xxxx",jsonObject.toString());
        if(jsonObject !=null){
            String result = jsonObject.optString("Result");
            if(!TextUtils.isEmpty(result)){
                if(result.equals("1")){
                    openDoor();

                    String imageStr = jsonObject.optString("Face_path");
                }else if(result.equals("2")){
                    Toast.makeText(LifecycleActivity.this,"正常票卡，已经入场，不可重复入场!",Toast.LENGTH_SHORT).show();
                }else if(result.equals("3")){
                    Toast.makeText(LifecycleActivity.this,"正常票卡，入场口错误，不可入场",Toast.LENGTH_SHORT).show();
                }else if(result.equals("4")){
                    Toast.makeText(LifecycleActivity.this,"正常票卡，入场频繁，稍后入场",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(LifecycleActivity.this,"无效票！",Toast.LENGTH_SHORT).show();
                }

            }
        }
        isReading = false;
    }

    private void findCount() {
        String sStr = ticketNum.toUpperCase().trim();
        WhiteList whiteList =  GetDataUtil.getDataBooean(sStr);
        if(whiteList != null){
            openDoor();
        }else {
            code_tv.setText("无效票！");
        }
    }


}

