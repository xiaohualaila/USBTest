package com.example.administrator.activity.main;

import android.text.TextUtils;
import android.util.Log;
import com.example.administrator.activity.base.BasePresenter;
import com.example.administrator.bean.WhiteList;
import com.example.administrator.retrofit.Api;
import com.example.administrator.retrofit.ConnectUrl;
import com.example.administrator.util.GetDataUtil;
import org.json.JSONObject;
import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


/**
 * Created by Administrator on 2017/6/3.
 */

public class MainPressenter extends BasePresenter implements MainContract.Presenter{
    private MainContract.View view;

    public MainPressenter(MainContract.View view) {
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {

    }

    @Override
    public void load(boolean isNetAble, String device_id, int type, String ticketNum, File newFile) {
        if (isNetAble) {
            if (type == 2 || type == 4 || type == 1) {
                Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                        .uploadData(device_id, type, ticketNum)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<JSONObject>() {
                            @Override
                            public void call(JSONObject object) {
                                jsonObjectResult(object);

                                Log.i("sss",object.toString());
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.i("sss",throwable.toString());
                                view.doError();
                            }
                        });
            } else {
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), newFile);
                builder.addFormDataPart("idCardPhoto", newFile.getName(), requestBody);
                Api.getBaseApiWithOutFormat(ConnectUrl.URL)
                        .uploadData(device_id,type,ticketNum,builder.build().parts())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<JSONObject>() {
                               @Override
                               public void call(JSONObject jsonObject) {
                                   jsonObjectResult(jsonObject);
                                   Log.i("sss",jsonObject.toString());
                               }
                           }, new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {
                                   Log.i("sss",throwable.toString());
                                   view.doError();
                               }
                           }
                        );

            }

        } else {//不联网
            String sStr = ticketNum.toUpperCase().trim();
            WhiteList whiteList =  GetDataUtil.getDataBooean(sStr);
            if(whiteList != null){
                view.doSuccess();
            }else {
                view.doError();
            }
        }

    }


    private void jsonObjectResult(JSONObject jsonObject){
        Log.i("xxxx",jsonObject.toString());
        if(jsonObject !=null){
            String result = jsonObject.optString("Result");
            if(!TextUtils.isEmpty(result)){
                if(result.equals("1")){
                    view.doSuccess();
                //    String imageStr = jsonObject.optString("Face_path");
                }else{
                    view.doError();
                }
            }else {
                view.doError();
            }
        }else {
            view.doError();
        }
    }

}
