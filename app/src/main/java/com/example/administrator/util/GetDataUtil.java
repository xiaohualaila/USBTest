package com.example.administrator.util;

import android.text.TextUtils;
import android.util.Log;

import com.example.administrator.bean.WhiteList;
import com.example.administrator.greendaodemo.greendao.GreenDaoManager;
import com.example.administrator.greendaodemo.greendao.gen.WhiteListDao;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import jxl.Sheet;
import jxl.Workbook;


/**
 * Created by Administrator on 2017/11/20.
 */

public class GetDataUtil {

    /**
     * 获取 excel 表格中的数据,不能在主线程中调用
     *
     * @param xlsName excel 表格的名称
     * @param index   第几张表格中的数据
     */
    public static Boolean getXlsData(String xlsName, int index) {
        boolean saveSuccess = false;
        try {
            File file =new File(xlsName);
            InputStream in=new FileInputStream(file);
            Workbook workbook = Workbook.getWorkbook(in);
            Sheet sheet = workbook.getSheet(index);
            int sheetNum = workbook.getNumberOfSheets();
            int sheetRows = sheet.getRows();
            int sheetColumns = sheet.getColumns();

//            Log.d(TAG, "the num of sheets is " + sheetNum);
//            Log.d(TAG, "the name of sheet is  " + sheet.getName());
//            Log.d(TAG, "total rows is 行=" + sheetRows);
//            Log.d(TAG, "total cols is 列=" + sheetColumns);

            WhiteList whiteList =null;
             String num;//编号0
             String name;//姓名1
             String cardCode;//卡号2
             String idCardNo;//身份证号3
             String company;//公司4
             String work;//职位5
             String certificates;//证件6
            WhiteListDao whiteListDao = GreenDaoManager.getInstance().getSession().getWhiteListDao();
            whiteListDao.deleteAll();
            for (int i = 0; i < sheetRows; i++) {
                num = sheet.getCell(0, i).getContents();
                name = sheet.getCell(1, i).getContents();
                cardCode = sheet.getCell(2, i).getContents();
                idCardNo = sheet.getCell(3, i).getContents();
                company = sheet.getCell(4, i).getContents();
                work = sheet.getCell(5, i).getContents();
                certificates = sheet.getCell(6, i).getContents();
                Log.i("xxx",num +" "+ name +" "+cardCode+" "+idCardNo+" "+company+" "+work+" "+certificates);
                if(TextUtils.isEmpty(num) && TextUtils.isEmpty(name)&& TextUtils.isEmpty(cardCode)&&
                    TextUtils.isEmpty(idCardNo)&& TextUtils.isEmpty(company)){
                    break;
                }
                whiteList = new WhiteList(num,name.trim(),cardCode.toUpperCase().trim(),idCardNo,company,work,certificates);
                whiteListDao.insert(whiteList);
            }
            workbook.close();
            saveSuccess = true;
        } catch (Exception e) {
            saveSuccess =false;
        }
        return saveSuccess;
    }

    public  static WhiteList getDataBooean (String code){
                WhiteListDao whiteListDao = GreenDaoManager.getInstance().getSession().getWhiteListDao();
                WhiteList whiteList =  whiteListDao.queryBuilder().where(WhiteListDao.Properties.CardCode.eq(code)).build().unique();
                if(whiteList != null){
                    return whiteList;
                }else {
                    return null;
                }
    }

}
