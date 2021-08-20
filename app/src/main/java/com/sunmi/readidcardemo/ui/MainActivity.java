package com.sunmi.readidcardemo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.sunmi.eidlibrary.EidCall;
import com.sunmi.eidlibrary.EidConstants;
import com.sunmi.eidlibrary.EidReadCardCallBack;
import com.sunmi.eidlibrary.EidReader;
import com.sunmi.eidlibrary.EidSDK;
import com.sunmi.eidlibrary.IDCardType;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;
import com.sunmi.readidcardemo.BuildConfig;
import com.sunmi.readidcardemo.R;
import com.sunmi.readidcardemo.bean.BaseInfo;
import com.sunmi.readidcardemo.bean.DecodeRequest;
import com.sunmi.readidcardemo.bean.Result;
import com.sunmi.readidcardemo.bean.ResultInfo;
import com.sunmi.readidcardemo.utils.ByteUtils;
import com.sunmi.readidcardemo.utils.DesUtils;
import com.sunmi.readidcardemo.utils.SignatureUtils;
import com.sunmi.readidcardemo.utils.Utils;
import com.zkteco.android.IDReader.IDCardPhoto;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import sunmi.paylib.SunmiPayKernel;

public class MainActivity extends AppCompatActivity implements EidCall {
    public static final String TAG = "MainActivity";

    @BindView(R.id.state)
    TextView mState;
    @BindView(R.id.version)
    TextView mVer;
    @BindView(R.id.name)
    TextView mName;
    @BindView(R.id.gender)
    TextView mGender;
    @BindView(R.id.race)
    TextView mRace;
    @BindView(R.id.pic)
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
    private EidReader eid;
    private PendingIntent pi;
    private NfcAdapter nfcAdapter;

    public static String appid = "b04f7ede49644a1ebf0b6b4194c8d983";//test
    public static String appkey = "696d52a837a4481ca5a14c8f97b1955c";

    private boolean init;
    private int readType = IDCardType.IDCARD;
    private IsoDep isodep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mAppId.setText(appid);
        mAppKey.setText(appkey);
        requestPerm();
        initNfc();
        showVersion();
        connectPayService();
    }

    private void showVersion() {
        mVer.setText(String.format("商米SDK.Ver:%s, 读卡模块Ver:%s", EidSDK.getSunmiEidSDKVersion(), EidSDK.getEidSDKVersion()));
    }

    /**
     * Android 标准初始化
     */
    private void initNfc() {
        Log.e("TAG", "initNfc  1 ");
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            mState.setText("设备不支持NFC，金融设备请使金融读卡");
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            mState.setText("请在系统设置中先启用NFC功能");
            return;
        }
        pi = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Log.e("tag", "onResume nfcAdapter  " + nfcAdapter);
            if (null != nfcAdapter) {
                nfcAdapter.enableForegroundDispatch(this, pi, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != nfcAdapter) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (init) {
            try {
                // 释放金融-SDK
                if (Utils.isAppInstalled(this, "com.sunmi.pay.hardware_v3")) {
                    SunmiPayKernel.getInstance().mReadCardOptV2.cancelCheckCard();
                    SunmiPayKernel.getInstance().mReadCardOptV2.cardOff(AidlConstantsV2.CardType.NFC.getValue());
                }
                EidSDK.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void requestPerm() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, 81);
            }
        }
    }


    @SuppressLint("MissingSuperCall")
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 81 && grantResults != null && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {// Permission Granted
                Toast.makeText(this, "无所需权限,请在设置中添加权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    @OnClick({R.id.init, R.id.finance_read_card, R.id.clear, R.id.delay, R.id.destroy})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.init:
                appid = mAppId.getText().toString();
                appkey = mAppKey.getText().toString();
                if (appid == null || appid.isEmpty()) {
                    Toast.makeText(this, "请检查appid", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (appkey == null || appkey.isEmpty()) {
                    Toast.makeText(this, "请检查appkey", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    //设置就走测试环境，不设置走正式环境
//                    EidSDK.setDebug(EidSDK.TEST_MODE);
                    //初始化；初始化成功后回调onCallData  code=1
                    EidSDK.init(getApplicationContext(), appid, this);
                } catch (Exception e) {
                    mState.setText(e.getMessage());
                }
                break;
            case R.id.finance_read_card:
                if (init) {
                    if (Utils.isAppInstalled(this, "com.sunmi.pay.hardware_v3")) {
                        try {
                            SunmiPayKernel.getInstance().mReadCardOptV2.cancelCheckCard();
                            SunmiPayKernel.getInstance().mReadCardOptV2.cardOff(AidlConstantsV2.CardType.NFC.getValue());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        checkCard();
                    } else {
                        mState.setText("当前设备非金融机具");
                        Toast.makeText(this, "当前设备非金融机具！", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mState.setText("请先初始化SDK");
                    Toast.makeText(this, "请先初始化SDK", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.clear:
                clearData();
                break;
            case R.id.delay:
                if (init) {
                    EidSDK.getDelayTime(this, 3);
                } else {
                    mState.setText("请先初始化SDK");
                    Toast.makeText(this, "请先初始化SDK", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.destroy:
                if (init) {
                    EidSDK.destroy();
                    init = false;
                } else {
                    mState.setText("请先初始化SDK");
                    Toast.makeText(this, "请先初始化SDK", Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }

    private void initEidReader() {
        try {
            eid = EidSDK.getEidReaderForNfc(3, this);
        } catch (Exception e) {
            mState.setText(e.getMessage());
        }
    }

    @Override
    public void onCallData(int code, String msg) {
        switch (code) {
            case EidConstants.READ_CARD_START:
                Log.i(TAG, "开始读卡，请勿移动");
                setEditText(mState, "开始读卡，请勿移动");
                break;
            case EidConstants.READ_CARD_SUCCESS:
                closeNFCReader();//电子身份证需要关闭

                Log.d(TAG, "process-------->READ_CARD_SUCCESS\n" +
                        "解析卡信息请使用sunmi云对云方案：https://docs.sunmi.com/eidapi/3/");
                //通过card_id请求识读卡片的信息
                Log.d(TAG, "onCallData: reqId:" + msg);
                setEditText(mRequestId, "reqId:" + msg);
                //==== 读卡成功后，解析卡信息请使用sunmi云对云方案：https://docs.sunmi.com/eidapi/3/" ====
                //==== 读卡成功后，解析卡信息请使用sunmi云对云方案：https://docs.sunmi.com/eidapi/3/"=====
                //==== 读卡成功后，解析卡信息请使用sunmi云对云方案：https://docs.sunmi.com/eidapi/3/"=====
                // *** 这里代码只为展示后续流程，实际开发需接入方后台根据文档自行开发 ***
                mockServerDecode(msg);
                break;
            case EidConstants.READ_CARD_FAILED:
                closeNFCReader();//电子身份证需要关闭
                Log.i(TAG, String.format(Locale.getDefault(), "读卡错误,请重新贴卡：%s", msg));
                setEditText(mState, String.format(Locale.getDefault(), "读卡错误,请重新贴卡：%s", msg));
                break;
            case EidConstants.READ_CARD_DELAY:
                Log.i(TAG, String.format(Locale.getDefault(), "延迟 %sms", msg));
                setEditText(mState, String.format(Locale.getDefault(), "延迟 %sms", msg));
                break;
            //初始化成功
            case EidConstants.EID_INIT_SUCCESS:
                init = true;
                initEidReader();
            case EidConstants.ERR_ACCOUNT_EXCEPTION://请检查套餐
            case EidConstants.ERR_APP_ID_NULL://appId参数错误/未初始化
            case EidConstants.ERR_DNS_EXCEPTION://解析域名异常
            case EidConstants.ERR_NETWORK_EXCEPTION://网络连接异常
            default:
                setEditText(mState, code + ":" + msg);
                break;
        }
    }

    /**
     * 模拟接入方服务器发起请求，仅做演示，非示例代码
     *
     * @param reqId
     */
    @Deprecated
    private void mockServerDecode(String reqId) {
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
        String stringA = json + appid + timeStamp + nonce;
        String sign = SignatureUtils.generateHashWithHmac256(stringA, appkey);
        Request request = new Request
                .Builder()
                .url(BuildConfig.OPEN_API_HOST + "v2/eid/eid/idcard/decode")
                .addHeader("Sunmi-Timestamp", timeStamp + "")
                .addHeader("Sunmi-Sign", sign)
                .addHeader("Sunmi-Appid", appid)
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
                    stringB = new String(DesUtils.decode(appkey.substring(0, 8), stringA, requestBean.encrypt_factor));
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

    private void setEditText(final TextView textView, final String text) {
        Log.i(TAG, text);
        runOnUiThread(() -> textView.setText(text));
    }

    private void parseData(ResultInfo data) {
        if (data == null) return;
        runOnUiThread(() -> {
            try {
                BaseInfo info = data.info;
                mName.setText(String.format("姓名：%s", info.name));
                mGender.setText(String.format("性别：%s", info.sex));
                mRace.setText(String.format("民族：%s", info.nation));
                mDate.setText(String.format("出生年月：%s", info.birthDate));
                mAddress.setText(String.format("地址：%s", info.address));
                mNumber.setText(String.format("身份证号码：%s", info.idnum));
                mOffice.setText(String.format("签发机关：%s", info.signingOrganization));
                mStart.setText(String.format("有效起始时间：%s", info.beginTime));
                mEnd.setText(String.format("有效结束时间：%s", info.endTime));
                mAppEidCode.setText(String.format("appeidcode：%s", data.appeidcode));
                mDn.setText(String.format("DN码：%s", data.dn));
                if (!TextUtils.isEmpty(data.picture)) {
                    Bitmap photo = IDCardPhoto.getIDCardPhoto(data.picture);
                    mPic.setVisibility(View.VISIBLE);
                    mPic.setImageBitmap(photo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void clearData() {
        mState.setText("请读卡");
        mRequestId.setText("reqId:");
        mName.setText("姓名：");
        mGender.setText("性别：");
        mRace.setText("民族：");
        mDate.setText("出生年月：");
        mAddress.setText("地址：");
        mNumber.setText("身份证号码：");
        mOffice.setText("签发机关：");
        mStart.setText("有效起始时间：");
        mEnd.setText("有效结束时间：");
        mAppEidCode.setText("appeidcode：");
        mDn.setText("DN码：");
        mPic.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(1, 1, 1, "通用卡类型");
        menu.add(2, 2, 2, "身份证等ID卡");
        menu.add(3, 3, 3, "电子身份证");
        menu.add(4, 4, 4, "旅行证件");
        return true;
    }

    /**
     * 读取类型 通用类型{@link IDCardType#CARD}，身份证等ID卡{@link IDCardType#IDCARD}，电子证照{@link IDCardType#ECCARD}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                readType = IDCardType.CARD;
                mState.setText("请读卡");
                break;
            case 2:
                readType = IDCardType.IDCARD;
                mState.setText("请读身份证等ID卡");
                break;
            case 3:
                readType = IDCardType.ECCARD;
                mState.setText("请读电子证照");
                break;
            case 4:
                //旅行证件
                startActivity(new Intent(this, TestTravelActivity.class));
                break;
        }
        return true;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (init) {
            Log.d(TAG, "onNewIntent: " + intent.getAction());
            if (readType == IDCardType.CARD || readType == IDCardType.IDCARD) {
                Log.d(TAG, "onNewIntent: 普通身份证或通用类型");
                eid.nfcReadCard(intent);
            } else if (readType == IDCardType.ECCARD) {
                Log.d(TAG, "onNewIntent: 电子证照");
                try {
                    Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    try {
                        isodep = IsoDep.get(tagFromIntent);
                        isodep.connect();
                        if (isodep.isConnected()) {
                            eid.readCard(IDCardType.ECCARD, new EidReadCardCallBack() {
                                @Override
                                public byte[] transceiveTypeB(byte[] data) {
                                    return data;
                                }

                                @Override
                                public byte[] transceiveTypeA(byte[] data) {
                                    byte[] outData = new byte[data.length];
                                    try {
                                        outData = isodep.transceive(data);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return outData;
                                }
                            });
                        } else {
                            isodep.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            mState.setText("请先初始化SDK");
            Toast.makeText(this, "请先初始化SDK", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeNFCReader() {
        if (isodep != null) {
            try {
                isodep.close();
                isodep = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // ---------------------------------------------- 金融设备读卡 ------------------------------

    private void connectPayService() {
        if (Utils.isAppInstalled(this, "com.sunmi.pay.hardware_v3")) {
            SunmiPayKernel payKernel = SunmiPayKernel.getInstance();
            payKernel.initPaySDK(this, mConnectCallback);
        }
    }


    private SunmiPayKernel.ConnectCallback mConnectCallback = new SunmiPayKernel.ConnectCallback() {
        @Override
        public void onConnectPaySDK() {
            Log.e(TAG, "onConnectPaySDK");
        }

        @Override
        public void onDisconnectPaySDK() {
            Log.e(TAG, "onDisconnectPaySDK");
        }
    };

    private CheckCardCallbackV2 mReadCardCallback = new CheckCardCallbackV2.Stub() {
        @Override
        public void findMagCard(Bundle bundle) throws RemoteException {
            Log.e(TAG, "findMagCard,bundle:" + bundle);
        }

        @Override
        public void findICCard(String atr) throws RemoteException {
            Log.e(TAG, "findICCard, atr:" + atr);
        }

        @Override
        public void findRFCard(String uuid) throws RemoteException {
            Log.e(TAG, "findRFCard, uuid:" + uuid);
            readCard();
        }

        @Override
        public void onError(final int code, final String msg) throws RemoteException {
            Log.e(TAG, "check card error,code:" + code + "message:" + msg);
        }

        @Override
        public void findICCardEx(Bundle bundle) throws RemoteException {
            Log.e(TAG, "findICCard, bundle:" + bundle);
        }

        @Override
        public void findRFCardEx(Bundle bundle) throws RemoteException {
            Log.e(TAG, "findRFCard, bundle:" + bundle);
            //readCard();
        }

        @Override
        public void onErrorEx(Bundle bundle) throws RemoteException {
            Log.e(TAG, "check card error, bundle:" + bundle);
        }
    };

    private void readCard() {
        try {
            Log.e(TAG, "操作读卡...");
            eid.readCard(readType, new EidReadCardCallBack() {
                @Override
                public byte[] transceiveTypeB(byte[] bytes) {
                    Log.e(TAG, "金融-NFC-身份证");
                    try {
                        byte[] out = new byte[260];
                        int code = SunmiPayKernel.getInstance().mReadCardOptV2.smartCardExChangePASS(AidlConstants.CardType.NFC.getValue(), bytes, out);
                        if (code < 0) {
                            Log.e(TAG, "读卡失败..code:" + code);
                            return new byte[0];
                        }
                        int len = ByteUtils.unsignedShort2IntBE(out, 0);
                        byte[] valid = Arrays.copyOfRange(out, 2, len + 4);
                        return valid;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return new byte[0];
                }

                @Override
                public byte[] transceiveTypeA(byte[] bytes) {
                    Log.e(TAG, "金融-NFC-电子身份证");
                    try {
                        byte[] out = new byte[255];
                        int code = SunmiPayKernel.getInstance().mReadCardOptV2.transmitApdu(AidlConstants.CardType.NFC.getValue(), bytes, out);
                        if (code < 0) {
                            Log.e(TAG, "读卡失败..code:" + code);
                            return new byte[0];
                        }
                        byte[] valid = Arrays.copyOfRange(out, 0, code);
                        return valid;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return new byte[0];
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 刷卡
     */
    private void checkCard() {
        try {
            SunmiPayKernel.getInstance().mReadCardOptV2.checkCard(AidlConstants.CardType.NFC.getValue(), mReadCardCallback, 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
