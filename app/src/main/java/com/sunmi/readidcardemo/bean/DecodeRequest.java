package com.sunmi.readidcardemo.bean;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DecodeRequest implements Serializable {
    //android端生成的 request_id
    @SerializedName("request_id")
    public String request_id;
    //加密因子（8 位大小写字母和数字组成的的随机字符串，建议每次访问随机生成）
    @SerializedName("encrypt_factor")
    public String encrypt_factor;

}
