package com.sunmi.readidcardemo;

import android.app.Application;

import com.sunmi.eidlibrary.EidCall;
import com.sunmi.eidlibrary.EidConstants;
import com.sunmi.eidlibrary.EidSDK;


/**
 * @author Darren(Zeng Dongyang)
 * @date 2019-10-11
 */
public class IApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Step 1 初始化SDK 传入AppId
        EidSDK.init(this, Constant.APP_ID, new EidCall() {
            @Override
            public void onCallData(int code, String msg) {
                switch (code) {
                    case EidConstants.EID_INIT_SUCCESS:
                        //初始化成功
                        break;
                    default:
                        //初始化失败，只有参数错误时会失败，回调其他code请检查传入参数。
                        break;
                }
            }
        });
    }

}
