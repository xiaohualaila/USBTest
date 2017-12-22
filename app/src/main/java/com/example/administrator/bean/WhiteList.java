package com.example.administrator.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;

/**
 * Created by Administrator on 2017/11/30.
 */
@Entity
public class WhiteList {
    @Id
    private Long _id;
    private String num;//编号
    private String name;//姓名
    private String cardCode;//卡号
    private String idCardNo;//身份证号
    private String company;//公司
    private String work;//职位
    private String certificates;//证件

    public WhiteList() {
    }

    @Keep
    @Generated(hash = 2050529862)
    public WhiteList(String num, String name, String cardCode, String idCardNo, String company, String work, String certificates) {
        this.num = num;
        this.name = name;
        this.cardCode = cardCode;
        this.idCardNo = idCardNo;
        this.company = company;
        this.work = work;
        this.certificates = certificates;
    }
    @Keep
    @Generated(hash = 73662917)
    public WhiteList(String num, String name, String cardCode, String idCardNo, String company) {
        this.num = num;
        this.name = name;
        this.cardCode = cardCode;
        this.idCardNo = idCardNo;
        this.company = company;

    }

    @Generated(hash = 2044499616)
    public WhiteList(Long _id, String num, String name, String cardCode, String idCardNo, String company, String work,
                     String certificates) {
        this._id = _id;
        this.num = num;
        this.name = name;
        this.cardCode = cardCode;
        this.idCardNo = idCardNo;
        this.company = company;
        this.work = work;
        this.certificates = certificates;
    }
    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public String getCertificates() {
        return certificates;
    }

    public void setCertificates(String certificates) {
        this.certificates = certificates;
    }

    public Long get_id() {
        return this._id;
    }

    public void set_id(Long _id) {
        this._id = _id;
    }
}
