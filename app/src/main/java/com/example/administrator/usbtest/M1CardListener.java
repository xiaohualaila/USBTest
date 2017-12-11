package com.example.administrator.usbtest;

import java.util.List;

/**
 * Created by hizha on 2017/7/31.
 */

public interface M1CardListener {
    void getM1CardResult(String cmd, List<String> list, String result, String resultCode);
}
