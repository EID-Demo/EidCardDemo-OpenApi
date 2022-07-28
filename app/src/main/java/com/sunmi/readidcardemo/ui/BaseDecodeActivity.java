package com.sunmi.readidcardemo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.sunmi.eidlibrary.EidCall;
import com.sunmi.eidlibrary.EidConstants;
import com.sunmi.eidlibrary.EidSDK;
import com.sunmi.eidlibrary.bean.BaseInfo;
import com.sunmi.eidlibrary.bean.ResultInfo;
import com.sunmi.readidcardemo.Constant;
import com.sunmi.readidcardemo.R;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 封装了读卡后的decode动作，这里的逻辑请放在云端，使用云对云方案
 */
public class BaseDecodeActivity extends AppCompatActivity {
    private static final String TAG = "BaseDecodeActivity";
    @BindView(R.id.state)
    TextView mState;
    @BindView(R.id.version)
    TextView mVer;
    @BindView(R.id.delay)
    Button mDelay;
    @BindView(R.id.stop)
    Button mStop;

    @BindView(R.id.ll_travel)
    LinearLayout mLltravel;
    @BindView(R.id.et_id_num)
    EditText etIdNum;
    @BindView(R.id.et_birthday)
    EditText etBirthday;
    @BindView(R.id.et_validity)
    EditText etValidity;
    @BindView(R.id.cb_is_img)
    CheckBox cbIsImg;

    @BindView(R.id.name)
    TextView mName;
    @BindView(R.id.gender)
    TextView mGender;
    @BindView(R.id.race)
    TextView mRace;
    @BindView(R.id.tv_pic)
    ImageView mPic;
    @BindView(R.id.date)
    TextView mDate;
    @BindView(R.id.address)
    TextView mAddress;
    @BindView(R.id.number)
    TextView mNumber;
    @BindView(R.id.office)
    TextView mOffice;
    @BindView(R.id.start)
    TextView mStart;
    @BindView(R.id.end)
    TextView mEnd;
    @BindView(R.id.appeidcode)
    TextView mAppEidCode;
    @BindView(R.id.dn)
    TextView mDn;
    @BindView(R.id.request_id)
    TextView mRequestId;
    @BindView(R.id.app_id)
    TextView mAppId;
    @BindView(R.id.app_key)
    EditText mAppKey;
    @BindView(R.id.tv_read_time)
    TextView mReadTime;

    @BindView(R.id.is_need_picture)
    public CheckBox mIsNeedPicture;

    public long timeMillis;
    public long readCardTimeMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_nfc);
        ButterKnife.bind(this);
        setEditText(mAppId, Constant.APP_ID);
        setEditText(mAppKey, Constant.APP_KEY);
        requestPerm();
        showVersion();
    }

    /**
     * 调用SDK提供的解码方法，存在泄漏key的风险，建议使用云对云方案
     *
     * @param reqId
     */
    @Deprecated
    public void decode(String reqId) {
        //调用SDK的解码，存在泄漏key的风险，建议使用云对云方案
        //传入读卡获取的reqId，商米partner平台上的appkey，以及结果callback
        EidSDK.getIDCardInfo(reqId, Constant.APP_KEY, new EidCall() {
            @Override
            public void onCallData(int code, String data) {
                //EidConstants.DECODE_SUCCESS 解码成功，data为身份证信息的gson格式，可直接解析成SDK中提供的 ResultInfo 实体类
                if (code == EidConstants.DECODE_SUCCESS) {
                    setEditText(mState, String.format(Locale.getDefault(), "身份证解析成功，业务状态：%d", code));
                    ResultInfo result = new Gson().fromJson(data, ResultInfo.class);
                    parseData(result);
                } else {
                    //解码失败，code 为错误吗，data为错误原因
                    setEditText(mState, String.format(Locale.getDefault(), "身份证解析失败，请重试(%d:%s)", code, data));
                }
            }
        });
    }

    protected void parseData(ResultInfo data) {
        //Step 6 解析身份证数据，这里业务方根据自己接口的返回来解析，如需解析身份证图片 可调用 EidSDK.parseCardPhoto() 方法
        try {
            BaseInfo info = data.info;
            setEditText(mName, String.format("姓名：%s", info.name));
            setEditText(mGender, String.format("性别：%s", info.sex));
            setEditText(mRace, String.format("民族：%s", info.nation));
            setEditText(mDate, String.format("出生年月：%s", info.birthDate));
            setEditText(mAddress, String.format("地址：%s", info.address));
            setEditText(mNumber, String.format("身份证号码：%s", info.idnum));
            setEditText(mOffice, String.format("签发机关：%s", info.signingOrganization));
            setEditText(mStart, String.format("有效起始时间：%s", info.beginTime));
            setEditText(mEnd, String.format("有效结束时间：%s", info.endTime));
            setEditText(mAppEidCode, String.format("appeidcode：%s", data.appeidcode));
            setEditText(mDn, String.format("DN码：%s", data.dn));

            //注：这里如果不读取身份证照片，picture会有个默认值，需要特殊处理
            if (data != null && !TextUtils.isEmpty(data.picture) && data.picture.length() != 1) {
                final Bitmap bt = EidSDK.parseCardPhoto(data.picture);
                if (bt != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPic.setVisibility(View.VISIBLE);
                            mPic.setImageBitmap(bt);
                        }
                    });

                }
            }
            setEditText(mReadTime, String.format(Locale.getDefault(), "readTime: 读卡：%dms;请求解码%dms;总：%dms",
                    readCardTimeMillis, (System.currentTimeMillis() - timeMillis - readCardTimeMillis), (System.currentTimeMillis() - timeMillis)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    public void clearData() {
        setEditText(mState, "");
        setEditText(mRequestId, "reqId:");
        setEditText(mName, "姓名：");
        setEditText(mGender, "性别：");
        setEditText(mRace, "民族：");
        setEditText(mDate, "出生年月：");
        setEditText(mAddress, "地址：");
        setEditText(mNumber, "身份证号码：");
        setEditText(mOffice, "签发机关：");
        setEditText(mStart, "有效起始时间：");
        setEditText(mEnd, "有效结束时间：");
        setEditText(mAppEidCode, "appeidcode：");
        setEditText(mDn, "DN码：");
        setEditText(mReadTime, "readTime：");
        mPic.setVisibility(View.GONE);
    }

    public void setEditText(final TextView textView, final String text) {
        Log.e(TAG, text);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    private void showVersion() {
        mVer.setText(String.format("商米SDK.Ver:%s, 读卡模块Ver:%s", EidSDK.getSunmiEidSDKVersion(), EidSDK.getEidSDKVersion()));
    }

    private void requestPerm() {
        //sdk 中需要获取SN
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_PHONE_STATE,
                        }, 81);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 81 && grantResults != null && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {// Permission Granted
                Toast.makeText(this, "无所需权限,请在设置中添加权限", Toast.LENGTH_LONG).show();
            }
        }
    }
}