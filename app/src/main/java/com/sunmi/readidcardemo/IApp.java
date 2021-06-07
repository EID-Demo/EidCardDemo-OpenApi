package com.sunmi.readidcardemo;

import android.app.Application;
import android.util.Log;

import com.sunmi.eidlibrary.EidCall;

/**
 * @author Darren(Zeng Dongyang)
 * @date 2019-10-11
 */
public class IApp extends Application implements EidCall {

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onCallData(int code, String msg) {
        Log.d("app", "onCallData: code:" + code + ", msg:" + msg);
    }
}
