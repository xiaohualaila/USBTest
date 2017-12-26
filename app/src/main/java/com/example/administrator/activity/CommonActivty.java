package com.example.administrator.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;
import com.decard.entitys.IDCard;
import com.example.administrator.retrofit.Api;
import com.example.administrator.retrofit.ConnectUrl;
import com.example.administrator.service.CommonService;
import com.example.administrator.usbtest.ComBean;
import com.example.administrator.usbtest.ConvertUtils;
import com.example.administrator.usbtest.R;
import com.example.administrator.usbtest.SPUtils;
import com.example.administrator.usbtest.SerialHelper;
import com.example.administrator.usbtest.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2017/12/12.
 */

public class CommonActivty extends AppCompatActivity  {
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
    private CommonService myService;
    private CommonService.MyBinder myBinder;
    private Handler handler = new Handler();

    private boolean uitralight = true;
    private boolean scan = true;
    private boolean idcard = true;

    //串口
    SerialControl ComA;
    DispQueueThread DispQueue;

    private boolean isReading = false;


    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (CommonService.MyBinder) service;
            myService = myBinder.getService();
            myBinder.setIntentData(uitralight,idcard);
            myService.setOnProgressListener(new CommonService.OnDataListener() {
                @Override
                public void onIDCardMsg(final IDCard idCardData) {//身份证
                    if(!isReading) {
                        isReading = true;
                        if (idCardData != null) {
                           // BasicOper.dc_beep(5);
                            openDoor();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    name_tv.setText(idCardData.getName().trim());
                                    sex_tv.setText(idCardData.getSex().trim());
                                    native_tv.setText(idCardData.getNation().trim());
                                    age_tv.setText(idCardData.getBirthday().trim());
                                    address_tv.setText(idCardData.getAddress().trim());
                                    idcard_num_tv.setText(idCardData.getId().trim());
                                    ivPhoto.setImageBitmap(ConvertUtils.bytes2Bitmap(ConvertUtils.hexString2Bytes(idCardData.getPhotoDataHexStr())));
                                }
                            });
                        }
                        upload();
                    }
                }

                @Override
                public void onUltralightCardMsg(final String result) {
                    if(!isReading){
                        isReading = true;
                       // BasicOper.dc_beep(5);
                        openDoor();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                code_type.setText("Ultralight卡");
                                code_tv.setText(result);
                            }
                        });
                        upload();
                    }
                }

                @Override
                public void onM1CardMsg(final String code) {
                    if(!isReading) {
                      //  BasicOper.dc_beep(5);
                        openDoor();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                code_type.setText("M1卡");
                                code_tv.setText(code);
                            }
                        });
                        upload();
                    }
                }
            });
        }
    };


   private void  upload(){
       isReading = false;
   }

     public void openDoor(){
         if(!isOpenDoor){
             isOpenDoor = true;
             rkGpioControlNative.ControlGpio(1, 0);//开门
             handler.postDelayed(runnable,500);
         }
     }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        ButterKnife.bind(this);
        Intent intent = getIntent();
        uitralight = intent.getBooleanExtra("uitralight",true);
        scan = intent.getBooleanExtra("scan",true);
        idcard = intent.getBooleanExtra("idcard",true);

        Utils.init(getApplicationContext());
        settingSp = new SPUtils(getString(R.string.settingSp));
        USB = settingSp.getString(getString(R.string.usbKey), getString(R.string.androidUsb));
        onOpenConnectPort();
        rkGpioControlNative.init();
        //串口
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();
        if(scan){
            openErWeiMa();
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
        if(scan){
            closeErWeiMa();
        }
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
                                openDoor();
                                String scan_code = str + new String(ComData.bRec);
                                Log.i("sss",scan_code);
                                code_type.setText("二维码");
                                code_tv.setText(scan_code.trim());
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

    /**
     * 上传信息
     */
    private void uploadPhoto() {
        File file = new File("");
        if(!file.exists()){

            return;
        }
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        builder.addFormDataPart("photoImgFiles", file.getName(), requestBody);
        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                .uploadPhotoBase("1","",1,builder.build().parts())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        //   Log.i("xxx",e.toString());
                        isReading = false;
                    }

                    @Override
                    public void onNext(JSONObject jsonObject) {
                        Log.i("xxx",jsonObject.toString());
//                            try {
//                                if(jsonObject != null) {
//                                    String Face_path = jsonObject.optString("Face_path");
//                                    if(!TextUtils.isEmpty(Face_path)){
//                                        Glide.with(CameraActivity.this).load(Face_path).error(R.drawable.img_bg).into(img_server);
//                                    }
//                                    String result = jsonObject.optString("Result");
//                                    if (result.equals("1")||result.equals("6")) {
//                                        if(result.equals("1")){
//                                            text_card.setText(R.string.open_door_1);
//                                        }else {
//                                            text_card.setText(R.string.open_door_6);
//                                        }
//                                        isOpenDoor = true;
//                                        rkGpioControlNative.ControlGpio(1, 0);//开门
//                                        flag_tag.setImageResource(R.drawable.flag_green);
//                                    } else if (result.equals("2")) {
//                                        text_card.setText(R.string.open_door_2);
//                                        flag_tag.setImageResource(R.drawable.flag_red);
//                                    }else if (result.equals("3")){
//                                        text_card.setText(R.string.open_door_3);
//                                        flag_tag.setImageResource(R.drawable.flag_red);
//                                    }else if (result.equals("4")){
//                                        text_card.setText(R.string.open_door_4);
//                                        flag_tag.setImageResource(R.drawable.flag_red);
//                                    }else if(result.equals("5")){
//                                        text_card.setText(R.string.open_door_5);
//                                        flag_tag.setImageResource(R.drawable.flag_red);
//                                    } else if(result.equals("99")){
//                                        text_card.setText(R.string.open_door_99);
//                                        flag_tag.setImageResource(R.drawable.flag_red);
//                                    } else{
//                                        text_card.setText(R.string.open_door_other);
//                                        flag_tag.setImageResource(R.drawable.flag_red);
//                                    }
//                                }
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }finally {
//                                uploadFinish();
//                            }
                        isReading = false;
                    }
                });
    }
}
