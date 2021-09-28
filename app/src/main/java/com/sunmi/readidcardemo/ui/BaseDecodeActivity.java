package com.sunmi.readidcardemo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
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
import com.sunmi.eidlibrary.EidSDK;
import com.sunmi.readidcardemo.BuildConfig;
import com.sunmi.readidcardemo.Constant;
import com.sunmi.readidcardemo.R;
import com.sunmi.readidcardemo.bean.BaseInfo;
import com.sunmi.readidcardemo.bean.DecodeRequest;
import com.sunmi.readidcardemo.bean.Result;
import com.sunmi.readidcardemo.bean.ResultInfo;
import com.sunmi.readidcardemo.utils.DesUtils;
import com.sunmi.readidcardemo.utils.SignatureUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

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
    EditText mAppId;
    @BindView(R.id.app_key)
    EditText mAppKey;
    @BindView(R.id.tv_read_time)
    TextView mReadTime;

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
     * 模拟接入方服务器发起请求，仅做演示，非示例代码
     *
     * @param reqId
     */
    @Deprecated
    public void mockServerDecode(String reqId) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("http", message));
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .addNetworkInterceptor(logging)
                .build();
        String json;
        Gson gson = new Gson();
        DecodeRequest requestBean = new DecodeRequest();
        requestBean.request_id = reqId;
        // 实际开发 随机8位加密因子
        requestBean.encrypt_factor = "abc12345";
        json = gson.toJson(requestBean);
        RequestBody body = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), json);
        long timeStamp = System.currentTimeMillis() / 1000;
        // 实际开发 6位随机数
        String nonce = "111111";
        String stringA = json + Constant.APP_ID + timeStamp + nonce;
        String sign = SignatureUtils.generateHashWithHmac256(stringA, Constant.APP_KEY);
        Request request = new Request
                .Builder()
                .url(BuildConfig.OPEN_API_HOST + "v2/eid/eid/idcard/decode")
                .addHeader("Sunmi-Timestamp", timeStamp + "")
                .addHeader("Sunmi-Sign", sign)
                .addHeader("Sunmi-Appid", Constant.APP_ID)
                .addHeader("Sunmi-Nonce", nonce)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                setEditText(mState, String.format(Locale.getDefault(), "身份证解析失败，请重试:" + e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                Result result = new Result();

                JSONObject object;
                try {
                    object = new JSONObject(response.body().string());
                    result.code = object.getInt("code");
                    if (object.has("msg")) {
                        result.msg = object.getString("msg");
                    }
                    if (result.code == 1) {
                        JSONObject object1 = object.getJSONObject("data");
                        Result.Data data = new Result.Data();
                        data.info = object1.getString("info");
                        result.data = data;
                    } else {
                        setEditText(mState, String.format(Locale.getDefault(), "身份证解析失败，请重试(%d:%s)", result.code, result.msg));
                        return;
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    setEditText(mState, String.format(Locale.getDefault(), "身份证解析失败，请重试:" + e.getMessage()));
                    return;
                }
//                    result = gson.fromJson(response.body().string(), Result.class);
                setEditText(mState, String.format(Locale.getDefault(), "身份证解析成功，业务状态：%d:%s", result.code, result.msg));
                byte[] stringA = Base64.decode(result.data.info.getBytes(), Base64.DEFAULT);
                String stringB;
                try {
                    stringB = new String(DesUtils.decode(Constant.APP_KEY.substring(0, 8), stringA, requestBean.encrypt_factor));
                } catch (Exception e) {
                    e.printStackTrace();
                    setEditText(mState, String.format(Locale.getDefault(), "身份证解析失败，请重试:" + e.getMessage()));
                    return;
                }
                ResultInfo finalInfo = gson.fromJson(stringB, ResultInfo.class);
                parseData(finalInfo);
            }
        });

    }

    private void parseData(ResultInfo data) {
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

            if (data != null && !TextUtils.isEmpty(data.picture)) {
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