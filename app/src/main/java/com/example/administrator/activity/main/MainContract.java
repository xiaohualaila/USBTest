package com.example.administrator.activity.main;

import com.example.administrator.activity.base.IBasePresenter;
import com.example.administrator.activity.base.IBaseView;
import java.io.File;

/**
 * Created by Administrator on 2017/6/3.
 */

public interface MainContract {
    interface View extends IBaseView<Presenter> {
        void doError();
        void doSuccess();
    }

    interface Presenter extends IBasePresenter {
       void load(boolean isNetAble, String device_id, int type, String ticketNum, File newFile);

    }
}
