package com.sunmi.readidcardemo.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.eidlink.idocr.sdk.listener.OnGetDelayListener;
import com.sunmi.eidlibrary.EidCall;
import com.sunmi.eidlibrary.EidConstants;
import com.sunmi.eidlibrary.EidSDK;
import com.sunmi.readidcardemo.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butterknife.OnClick;

/**
 * 读旅行证件页面，流程同读卡页面一样
 * 1.业务方如果是独立的读卡页面 建议在onResume()中调用startCheckCard()、在onPause()中调用stopCheckCard()
 * 2.业务方如果需要更加精准的控制刷卡，可以在需要刷卡时调用startCheckCard()，不需要刷卡时调用stopCheckCard()
 */
public class ReadTravelActivity extends BaseDecodeActivity {

    private static final String TAG = "ReadTravelActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLltravel.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //独立刷卡页面可在此处这样调用
//        startCheckCard();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //独立刷卡页面可在此处这样调用
//        EidSDK.stopCheckCard(ReadTravelActivity.this);
    }

    @OnClick({R.id.start_read_card, R.id.delay, R.id.stop})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.start_read_card:
                clearData();
                startCheckCard();
                break;
            case R.id.stop:
                EidSDK.stopCheckCard(ReadTravelActivity.this);
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
        //*** 旅行证件需要传入以下参数(必传) ***
        Map params = new HashMap();
        //是否是旅行证件
        params.put(EidSDK.PARAMS_IS_TRAVEL, true);
        //证件号码 旅行证件的证件号
        params.put(EidSDK.PARAMS_TRAVEL_ID, etIdNum.getText().toString());
        //生日日期 旅行证件的生日日期 格式：yyMMdd 例：950528
        params.put(EidSDK.PARAMS_TRAVEL_BIRTHDAY, etBirthday.getText().toString());
        //有效期至 旅行证件的有效期至 格式：yyMMdd 例：950528
        params.put(EidSDK.PARAMS_TRAVEL_VALIDITY, etValidity.getText().toString());
        //是否读取照片 true 读取照片 false 不读取照片 如护照，照片较大读取时间较长，NFC 读取中，请不要移动读卡设备和护照。
        params.put(EidSDK.PARAMS_TRAVEL_NEED_READ_IMG, cbIsImg.isChecked());
        EidSDK.startCheckCard(this, new EidCall() {
            @Override
            public void onCallData(int code, String msg) {
                Log.e(TAG, "onCallData-" + code + " , " + msg);
                switch (code) {
                    case EidConstants.ERR_NFC_NOT_SUPPORT:
                        Log.e(TAG, "机器不支持NFC");
                        // 该机器不支持NFC功能，无法使用SDK
                        break;
                    case EidConstants.ERR_NETWORK_NOT_CONNECTED:
                        Log.e(TAG, "网络未连接，连接网络后重新调用 startCheckCard 方法");
                        setEditText(mState, String.format(Locale.getDefault(), "网络未连接，请联网后重试"));
                        // *** 异常处理： 连接网络后，需要重新调用 startCheckCard 方法（手动触发，非自动） ***
                        break;
                    case EidConstants.ERR_NFC_CLOSED:
                        Log.e(TAG, "NFC 未打开，打开后重试 ：" + msg);
                        setEditText(mState, String.format(Locale.getDefault(), "NFC未打开，请打开后重试"));
                        //  *** 异常处理： 打开NFC后，需要重新调用 startCheckCard 方法（手动触发，非自动）***
                        break;
                    case EidConstants.READ_CARD_READY:
                        //Step 3 读卡准备完成 -> 业务方可以引导用户开始进行刷卡操作
                        Log.e(TAG, "SDK准备完成，请刷卡");
                        clearData();
                        setEditText(mState, "请刷卡，刷卡时请勿移动卡片");
                        break;
                    case EidConstants.READ_CARD_START:
                        //Step 4 读卡中 -> 业务方可以提醒用户"读卡中，请勿移动卡片"
                        Log.e(TAG, "开始读卡，请勿移动");
                        clearData();
                        setEditText(mState, "开始读卡，请勿移动");
                        break;
                    case EidConstants.READ_CARD_SUCCESS:
                        //Step 5 读卡成功 -> 返回的msg为reqId，通过 reqId 业务方走云对云方案获取身份证信息
                        //注：如不需要循环读卡，可在此处调用stopCheckCard方法
                        Log.e(TAG, "读卡成功，reqId：" + msg);
                        setEditText(mRequestId, String.format("reqId:%s", msg));
                        mockServerDecode(msg);
                        break;
                    case EidConstants.READ_CARD_FAILED:
                        //*** 异常处理： 读卡失败，请重新读卡 ***
                        Log.e(TAG, "读卡失败：" + msg);
                        setEditText(mState, String.format(Locale.getDefault(), "读卡错误,请重新贴卡：%s", msg));
                        break;
                    default:
                        //*** 异常处理： 其他失败 - code为错误码，msg为详细错误原因 需要重新调用 startCheckCard 方法（手动触发，非自动） ***
                        Log.e(TAG, "读卡失败：code:" + code + ",msg:" + msg);
                        setEditText(mState, String.format(Locale.getDefault(), "其他错误：%d,%s", code, msg));
                        break;
                }
            }
        }, params);
    }
}