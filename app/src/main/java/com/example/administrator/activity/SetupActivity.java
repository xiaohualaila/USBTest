package com.example.administrator.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.example.administrator.greendaodemo.greendao.GreenDaoManager;
import com.example.administrator.greendaodemo.greendao.gen.WhiteListDao;
import com.example.administrator.usbtest.R;
import com.example.administrator.util.FileUtil;
import com.example.administrator.util.GetDataUtil;
import java.io.File;
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
    @BindView(R.id.choose_switch)
    Switch choose_switch;
    @BindView(R.id.m1_ll)
    RelativeLayout m1_ll;
    @BindView(R.id.idcard_ll)
    RelativeLayout idcard_ll;
    @BindView(R.id.add_excel)
    TextView add_excel;
    private boolean isUitralight = true;
    private boolean isScan = true;
    private boolean isIdcard = true;
    private boolean isHaveThree = true;

    private String path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        ButterKnife.bind(this);
        // 添加监听
        idcard_switch.setOnCheckedChangeListener(this);
        scan_switch.setOnCheckedChangeListener(this);
        choose_switch.setOnCheckedChangeListener(this);
        FileUtil.getPath();
    }


    @OnClick({R.id.rb_uitralight,R.id.rb_m1,R.id.finish,R.id.add_excel})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rb_uitralight:
                isUitralight = true;
                break;
            case R.id.rb_m1:
                isUitralight = false;
                break;
            case R.id.finish:

                Intent intent = new Intent(this,LifecycleActivity.class);
                intent.putExtra("uitralight",isUitralight);
                intent.putExtra("scan",isScan);
                intent.putExtra("idcard",isIdcard);
                intent.putExtra("isHaveThree",isHaveThree);
                startActivity(intent);
                finish();
                break;
            case R.id.add_excel:
                getExcel();
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
            case R.id.choose_switch:
                if(isChecked){
                    m1_ll.setVisibility(View.VISIBLE);
                    idcard_ll.setVisibility(View.VISIBLE);
                    isHaveThree = true;
                }else {
                    m1_ll.setVisibility(View.GONE);
                    idcard_ll.setVisibility(View.GONE);
                    isHaveThree = false;
                }
                break;
        }

    }

    //判断Excel文件是否存在
    private void getExcel() {
        path = FileUtil.getPath()+ File.separator +"door.xls";
        File file = new File(path);
        if(!file.exists()){
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_msg)//dialog_msg
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
            dialog.show();
            return;
        }else {//存在
            add_excel.setText("正在导入Excel表！");
            add_excel.setEnabled(false);
            new ExcelDataLoader().execute(path);
        }
    }

    //在异步方法中 调用
    private class ExcelDataLoader extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... params) {

            return GetDataUtil.getXlsData(params[0], 0);
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {

            if(isSuccess){
                WhiteListDao whiteListDao = GreenDaoManager.getInstance().getSession().getWhiteListDao();
                //存在数据
                add_excel.setText("加载成功！共"+whiteListDao.loadAll().size() + "条记录");
            }else {
                //加载失败
                add_excel.setText(R.string.load_fail);
            }
            add_excel.setEnabled(true);
        }
    }
}
