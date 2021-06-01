package com.sunmi.readidcardemo.bean;

import java.io.Serializable;

/**
 * 与后台交互的返回结果,封装单个实体
 */
public class Result implements Serializable {

    /**
     * 返回码
     */
    public String code;
    /**
     * 结果信息
     */
    public String msg = "";
    /**
     * 结果的实体
     */
    public Data data;

    public class Data {

        public String info = "";

        public String sub_code;//5001 权限错误：原因可能是添加了白名单

        public String sub_msg;

        @Override
        public String toString() {
            return "Data{" +
                    "info='" + info + '\'' +
                    ", sub_code='" + sub_code + '\'' +
                    ", sub_msg='" + sub_msg + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + (data == null ? "null" : data.toString()) +
                '}';
    }
}
