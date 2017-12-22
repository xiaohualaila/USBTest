package com.example.administrator.retrofit.api;

import org.json.JSONObject;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by xyuxiao on 2016/9/23.
 */
public interface BaseApi {

    //上传照片
    @POST("face.php")
    @Multipart
    Observable<JSONObject> uploadPhoto(
            @Part List<MultipartBody.Part> file
    );

    @POST("api2.php")
    @Multipart
    Observable<JSONObject> uploadPhotoBase(
            @Query("deviceid") String deviceid,
            @Query("ticketid") String ticketid,
            @Part List<MultipartBody.Part> file
    );

    @POST("api.php")
    Observable<JSONObject> uploadData(
            @Query("deviceid") String deviceid,
            @Query("inserttype") int type,
            @Query("ticketid") String ticketNum
    );

    @POST("api.php")
    @Multipart
    Observable<JSONObject> uploadData(
            @Query("deviceid") String deviceid,
            @Query("inserttype") int type,
            @Query("ticketid") String ticketNum,
            @Part List<MultipartBody.Part> file
    );

    @POST("validate.php")
    Observable<JSONObject> checkIp();

}

