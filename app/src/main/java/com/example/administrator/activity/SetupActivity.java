package com.example.administrator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.example.administrator.usbtest.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * Created by Administrator on 2017/12/11.
 */

public class SetupActivity  extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    @BindView(R.id.idcard_switch)
    Switch idcard_switch;
    @BindView(R.id.scan_switch)
    Switch scan_switch;
    private boolean isUitralight = true;
    private boolean isScan = true;
    private boolean isIdcard = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        ButterKnife.bind(this);
        // 添加监听
        idcard_switch.setOnCheckedChangeListener(this);
        scan_switch.setOnCheckedChangeListener(this);

    }


    @OnClick({R.id.rb_uitralight,R.id.rb_m1,R.id.finish})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rb_uitralight:
                isUitralight = true;
                break;
            case R.id.rb_m1:
                isUitralight = false;
                break;
            case R.id.finish:
                Intent intent = new Intent(this,CommonActivty.class);
                intent.putExtra("uitralight",isUitralight);
                intent.putExtra("scan",isScan);
                intent.putExtra("idcard",isIdcard);
                startActivity(intent);
                finish();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
            case R.id.idcard_switch:
                isIdcard = isChecked;
                break;
            case R.id.scan_switch:
                isScan = isChecked;
                break;
        }

    }
}
