package com.sunmi.readidcardemo.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.eidlink.idocr.sdk.listener.OnGetDelayListener;
import com.sunmi.eidlibrary.EidCall;
import com.sunmi.eidlibrary.EidConstants;
import com.sunmi.eidlibrary.EidSDK;
import com.sunmi.readidcardemo.Constant;
import com.sunmi.readidcardemo.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butterknife.OnClick;

/**
 * 读卡页面
 * 1.业务方如果是独立的读卡页面 建议在onResume()中调用startCheckCard()、在onPause()中调用stopCheckCard()
 * 2.业务方如果需要更加精准的控制刷卡，可以在需要刷卡时调用startCheckCard()，不需要刷卡时调用stopCheckCard()
 */
public class ReadCardActivity extends BaseDecodeActivity {
    private static final String TAG = "ReadCardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsNeedPicture.setVisibility(View.VISIBLE);
        mStop.setText("停止检卡");
        mAppId.setText(Constant.APP_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //独立刷卡页面可在此处这样调用
//        startCheckCard()
    }

    @Override
    protected void onPause() {
        super.onPause();
        //独立刷卡页面可在此处这样调用
//        EidSDK.stopCheckCard(this);
    }

    @OnClick({R.id.start_read_card, R.id.delay, R.id.stop})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.start_read_card:
                if (!TextUtils.isEmpty(mAppKey.getText().toString())) {
                    Constant.APP_KEY = mAppKey.getText().toString();
                }
                startCheckCard();
                break;
            case R.id.stop:
                EidSDK.stopCheckCard(ReadCardActivity.this);
                clearData();
                break;
            case R.id.delay:
                EidSDK.getDelayTime(3, new OnGetDelayListener() {

                    @Override
                    public void onSuccess(long l) {
                        setEditText(mState, String.format(Locale.getDefault(), "延迟 %dms", l));
                    }

                    @Override
                    public void onFailed(int i) {
                        setEditText(mState, String.format(Locale.getDefault(), "读卡错误,请重新贴卡：%d", i));
                    }
                });
                break;
        }
    }

    private void startCheckCard() {
        //Step 2 开启读卡 -> 调用startCheckCard方法，通过回调结果处理业务逻辑
        //注：默认循环读卡，只会回调一次EidConstants.READ_CARD_READY
        //是否需要获取图片，不获取图片要快些，大约300-500ms
        Map<String, Object> map = new HashMap<>();
        map.put(EidSDK.PARAMS_IS_READ_PICTURE, mIsNeedPicture.isChecked());
        EidSDK.startCheckCard(this, new EidCall() {
            @Override
            public void onCallData(int code, String msg) {
                Log.e(TAG, "onCallData-" + code + " , " + msg);
                switch (code) {
                    case EidConstants.ERR_NFC_NOT_SUPPORT:
                        Log.e(TAG, "机器不支持NFC");
                        // 该机器不支持NFC功能，无法使用SDK
                        setEditText(mState, String.format(Locale.getDefault(), "机器不支持NFC"));
                        break;
                    case EidConstants.ERR_NETWORK_NOT_CONNECTED:
                        Log.e(TAG, "网络未连接，连接网络后重新调用 startCheckCard 方法");
                        setEditText(mState, String.format(Locale.getDefault(), "网络未连接，请联网后重试"));
                        // *** 异常处理： 连接网络后，需要重新调用 startCheckCard 方法 （手动触发，非自动）***
                        break;
                    case EidConstants.ERR_NFC_CLOSED:
                        Log.e(TAG, "NFC 未打开，打开后重试 ：" + msg);
                        setEditText(mState, String.format(Locale.getDefault(), "NFC未打开，请打开后重试"));
                        //  *** 异常处理： 打开NFC后，需要重新调用 startCheckCard 方法 （手动触发，非自动）***
                        break;
                    case EidConstants.READ_CARD_READY:
                        //Step 3 读卡准备完成 -> 业务方可以引导用户开始进行刷卡操作
                        Log.e(TAG, "SDK准备完成，请刷卡");
                        clearData();
                        setEditText(mState, "请刷卡，刷卡时请勿移动卡片");
                        break;
                    case EidConstants.READ_CARD_START:
                        //Step 4 读卡中 -> 业务方可以提醒用户"读卡中，请勿移动卡片"
                        timeMillis = System.currentTimeMillis();
                        Log.e(TAG, "开始读卡，请勿移动");
                        clearData();
                        setEditText(mState, "开始读卡，请勿移动");
                        break;
                    case EidConstants.READ_CARD_SUCCESS:
                        readCardTimeMillis = (System.currentTimeMillis() - timeMillis);
                        //Step 5 读卡成功 -> 返回的msg为reqId，通过 reqId 业务方走云对云方案获取身份证信息
                        //注：如不需要循环读卡，可在此处调用stopCheckCard方法
                        Log.e(TAG, "读卡成功，reqId：" + msg);
                        setEditText(mRequestId, String.format("reqId:%s", msg));
                        decode(msg);
                        break;
                    case EidConstants.READ_CARD_FAILED:
                        //*** 异常处理： 读卡失败，请重新读卡 ***
                        Log.e(TAG, "读卡失败：" + msg);
                        setEditText(mState, String.format(Locale.getDefault(), "读卡错误,请重新贴卡：%s", msg));
                        break;
                    default:
                        //*** 异常处理： 其他失败 - code为错误码，msg为详细错误原因 需要重新调用 startCheckCard 方法 （手动触发，非自动）***
                        Log.e(TAG, "读卡失败：code:" + code + ",msg:" + msg);
                        setEditText(mState, String.format(Locale.getDefault(), "其他错误：%d,%s", code, msg));
                        break;
                }

            }
        }, map);
    }
}