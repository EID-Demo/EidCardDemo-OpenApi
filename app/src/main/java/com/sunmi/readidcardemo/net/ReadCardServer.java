package com.sunmi.readidcardemo.net;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.sunmi.readidcardemo.bean.ResultInfo;
import com.sunmi.readidcardemo.utils.Utils;

import rx.Observable;

public class ReadCardServer {

    private static ReadCardServer instance;
    private final RetrofitInterface mNetService;
    private Gson mGson = new Gson();

    private ReadCardServer() {
        RetrofitWrapper retrofitWrapper = RetrofitWrapper.getInstance();
        mNetService = retrofitWrapper.getNetService(RetrofitInterface.class);
    }

    public static ReadCardServer getInstance() {
        if (instance == null) {
            instance = new ReadCardServer();
        }
        return instance;
    }

    public Observable<ResultInfo> parse(@NonNull String reqId, @NonNull String appid, @NonNull String appkey) {
        String encryptFactor = Utils.createKey();
        String sign = "app_id=" + appid + "&encrypt_factor=" + encryptFactor + "&request_id=" + reqId + appkey;
        sign = Utils.md5(sign).toUpperCase();
        return mNetService.parse(reqId, appid, encryptFactor, sign)
                .map(result -> {
                    if ("10000".equals(result.code)) {
                        if (result.data != null && result.data.info != null && !result.data.info.isEmpty()) {
                            String json = Utils.decode(encryptFactor, result.data.info);
                            return mGson.fromJson(json, ResultInfo.class);
                        } else {
                            throw new RuntimeException("Error result.data:" + result.data.toString());
                        }
                    } else {
                        throw new RuntimeException(result.code + ":" + result.msg);
                    }
                });
    }
}
