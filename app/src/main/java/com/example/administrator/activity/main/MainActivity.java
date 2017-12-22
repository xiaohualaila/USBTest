package com.example.administrator.activity.main;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;
import com.decard.entitys.IDCard;
import com.example.administrator.activity.base.BaseAppCompatActivity;
import com.example.administrator.service.LifecycleService;
import com.example.administrator.usbtest.ConvertUtils;
import com.example.administrator.usbtest.R;
import com.example.administrator.usbtest.SPUtils;
import com.example.administrator.usbtest.ScanHelper;
import com.example.administrator.usbtest.Utils;
import com.example.administrator.util.FileUtil;
import com.example.administrator.util.MyUtil;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import butterknife.BindView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;



public class MainActivity extends BaseAppCompatActivity  implements MainContract.View {
    private MainContract.Presenter presenter;
    @BindView(R.id.img)
    ImageView img;
    private SPUtils settingSp;
    private String USB="";
    private boolean isOpenDoor = false;
    private LifecycleService myService;
    private LifecycleService.MyBinder myBinder;
    private Handler handler = new Handler();

    private boolean uitralight = true;
    private boolean scan = true;
    private boolean idcard = false;
    private boolean isHaveThree = false;

    //串口
    protected ScanHelper mScanHelper;
    private Subscription sub;
    private boolean isReading = false;

    private String device_id;
    /**
     * 3 身份证,1 Ultralight,4 M1,2串口
     */
    private int type;
    private String ticketNum;
    private File newFile;

    @Override
    protected void init() {
        new MainPressenter(this);
        device_id = MyUtil.getDeviceID(this);//获取设备号
        Intent intent = getIntent();
        uitralight = intent.getBooleanExtra("uitralight",false);
        scan = intent.getBooleanExtra("scan",true);
        idcard = intent.getBooleanExtra("idcard",false);
        isHaveThree = intent.getBooleanExtra("isHaveThree",false);
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
        img.setImageResource(R.drawable.welcome);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.main;
    }

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
                                    Bitmap idcard = ConvertUtils.bytes2Bitmap(ConvertUtils.hexString2Bytes(idCardData.getPhotoDataHexStr()));

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
                    if(str.trim().length() > 4){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                type = 2;
                                ticketNum = str.trim();
                                upload();
                            }
                        });
                    }
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
         //   Log.i("sss","设备关闭");//关闭设备
        }else {
           // Log.i("sss","Port has closed");//设备已关闭
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(scan){
            mScanHelper.close();
            sub.unsubscribe();
        }
        if(isHaveThree){
            myBinder.stopThread();
            unbindService(connection);
            onDisConnectPort();
        }
        adcNative.close(0);
        adcNative.close(2);
        rkGpioControlNative.close();

    }


    private void  upload(){
        Log.i("xxxx","type >>" + type +"" +" ticketNum>>" + ticketNum);
        boolean isNetAble = MyUtil.isNetworkAvailable(this);
            if(type == 3){
                if(!newFile.exists()){
                    isReading = false;
                    return;
                }
            }
        presenter.load(isNetAble,device_id,type,ticketNum,newFile);
    }

    @Override
    public void setPresenter(MainContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void doError() {
        isReading = false;
        img.setImageResource(R.drawable.not_pass);
        welcome();
    }

    @Override
    public void doSuccess() {
        isReading = false;
        img.setImageResource(R.drawable.pass);
        openDoor();
        welcome();
    }

    private void welcome(){
       Observable.interval(1000, TimeUnit.MILLISECONDS)
                .take(1)
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        img.setImageResource(R.drawable.welcome);
                    }
                });
    }
}

