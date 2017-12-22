package com.example.administrator;


import android.app.Application;
import android.content.Context;

/**
 * Created by xyuxiao on 2016/9/23.
 */
public class MyApplication extends Application {
    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }
    public static Context getContext() {
        return mContext;
    }


}
