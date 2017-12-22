package com.example.administrator.activity.base;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import butterknife.ButterKnife;

/**
 * Created by xyuxiao on 2016/9/23.
 */
public abstract class BaseAppCompatActivity extends FragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        ButterKnife.bind(this);
        init();
        Log.e("Activity Name:", getClass().getSimpleName());
    }

    protected abstract void init();

    protected abstract @LayoutRes
    int getLayoutId();

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
