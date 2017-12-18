package com.example.administrator.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cmm.rkadcreader.adcNative;
import com.cmm.rkgpiocontrol.rkGpioControlNative;
import com.decard.NDKMethod.BasicOper;
import com.decard.entitys.IDCard;
import com.example.administrator.retrofit.Api;
import com.example.administrator.retrofit.ConnectUrl;
import com.example.administrator.service.CommonService;
import com.example.administrator.usbtest.ComBean;
import com.example.administrator.usbtest.R;
import com.example.administrator.usbtest.SPUtils;
import com.example.administrator.usbtest.SerialHelper;
import com.example.administrator.usbtest.Utils;
import com.example.administrator.util.FileUtil;

import org.json.JSONObject;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;



/**
 * Created by dhht on 16/9/29.
 */

public class CameraActivity extends Activity implements SurfaceHolder.Callback {

    @BindView(R.id.camera_sf)
    SurfaceView camera_sf;
    @BindView(R.id.text_card)
    TextView text_card;
    @BindView(R.id.img1)
    ImageView img1;
    @BindView(R.id.img_server)
    ImageView img_server;
    @BindView(R.id.card_no)
    TextView card_no;
    @BindView(R.id.flag_tag)
    ImageView flag_tag;
    private Camera camera;
    private String filePath;
    private SurfaceHolder holder;
    private boolean isFrontCamera = true;
    private boolean safephoto  =true;
    private int width = 640;
    private int height = 480;
    private String strs;
    private boolean oo =true;
    int index = 0;

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

    /**
     * 测试用按钮
     * @param view
     */
    @OnClick({R.id.take_photo})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.take_photo:
//                if(oo){
//                    rkGpioControlNative.ControlGpio(1, 0);//开门
//                    oo = false;
//                }else {
//                    rkGpioControlNative.ControlGpio(1, 1);//关门
//                    oo = true;
//                }
                takePhoto();
                break;
        }
    }

;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        holder = camera_sf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        Intent bindIntent = new Intent(this, CommonService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);


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


    private void takePhoto(){
           if(!isOpenDoor){
                if (safephoto) {
                    safephoto = false;
                  //  FileUtil.deleteFile(filePath);
                    camera.takePicture(null, null, jpeg);
                    text_card.setText("拍摄人脸照片");
                }
           }
    }

    private Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            stopPreview();
            filePath = FileUtil.getPath() + File.separator + FileUtil.getTime() + ".jpeg";
            Matrix matrix = new Matrix();
            matrix.reset();
            matrix.postRotate(180);
            BitmapFactory.Options factory = new BitmapFactory.Options();
            factory = setOptions(factory);
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length,factory);
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            BufferedOutputStream bos = null;
            try {
                File file = new File(filePath);
                if(!file.exists()){
                    file.createNewFile();
                }
                bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
                bm1.compress(Bitmap.CompressFormat.JPEG,30, bos);
                bos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bos != null){
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                bm.recycle();
                bm1.recycle();
                startPreview();
                uploadPhoto();
            }
        }
    };

    /**
     * 上传信息
     */
    private void uploadPhoto() {
        File  file = new File(filePath);
        if(!file.exists()){
            uploadFinish();
            return;
        }
        Glide.with(CameraActivity.this).load(filePath).error(R.drawable.img_bg).into(img1);
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        builder.addFormDataPart("photoImgFiles", file.getName(), requestBody);
        Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                .uploadPhotoBase("1",strs,builder.build().parts())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                     //   Log.i("xxx",e.toString());
                        text_card.setText("数据请求失败！");
                        flag_tag.setImageResource(R.drawable.flag_red);
                        uploadFinish();
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
                    }
                });
    }

    /**
     * 0.5秒关门
     */
    private void uploadFinish() {
        safephoto = true;
        handler.postDelayed(runnable,500);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(isOpenDoor){
                rkGpioControlNative.ControlGpio(1, 1);//关门
                isOpenDoor = false;
                takePhoto();
            }
        }
    };

    public static BitmapFactory.Options setOptions(BitmapFactory.Options opts) {
        opts.inJustDecodeBounds = false;
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = 1;
        return opts;
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = openCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myBinder.stopThread();
        closeCamera();
        FileUtil.deleteDir(FileUtil.getPath());
        unbindService(connection);
        adcNative.close(0);
        adcNative.close(2);
        rkGpioControlNative.close();
        onDisConnectPort();
        closeErWeiMa();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException exception) {
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    private Camera openCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
            } catch (Exception e) {
                camera = null;
                e.printStackTrace();
            }
        }
        return camera;
    }
    private void startPreview() {
        Camera.Parameters para;
        if (null != camera) {
            para = camera.getParameters();
        } else {
            return;
        }
        para.setPreviewSize(width, height);
        setPictureSize(para,640 , 480);
        para.setPictureFormat(ImageFormat.JPEG);//设置图片格式
        setCameraDisplayOrientation(isFrontCamera ? 0 : 1, camera);
        camera.setParameters(para);
        camera.startPreview();
    }

    /* 停止预览 */
    private void stopPreview() {
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void setCameraDisplayOrientation(int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
         rotation = 2;
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void setPictureSize(Camera.Parameters para, int width, int height) {
        int absWidth = 0;
        int absHeight = 0;
        List<Camera.Size> supportedPictureSizes = para.getSupportedPictureSizes();
        for (Camera.Size size : supportedPictureSizes) {
            if (Math.abs(width - size.width) < Math.abs(width - absWidth)) {
                absWidth = size.width;
            }
            if (Math.abs(height - size.height) < Math.abs(height - absHeight)) {
                absHeight = size.height;
            }
        }
        para.setPictureSize(absWidth,absHeight);
    }

    private void closeCamera() {
        if (null != camera) {
            try {
                camera.setPreviewDisplay(null);
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
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
