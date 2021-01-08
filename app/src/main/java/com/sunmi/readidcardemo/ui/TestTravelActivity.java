package com.sunmi.readidcardemo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sunmi.eidlibrary.EidCall;
import com.sunmi.eidlibrary.EidConstants;
import com.sunmi.eidlibrary.EidReader;
import com.sunmi.eidlibrary.EidSDK;
import com.sunmi.readidcardemo.R;
import com.sunmi.readidcardemo.bean.BaseInfo;
import com.sunmi.readidcardemo.bean.ResultInfo;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 读取旅行证件:护照，港澳通行证
 */
public class TestTravelActivity extends AppCompatActivity implements EidCall {
    public static final String TAG = "TestTravelActivity";

    @BindView(R.id.state)
    TextView mState;
    @BindView(R.id.version)
    TextView mVer;
    @BindView(R.id.read)
    Button mRead;
    @BindView(R.id.delay)
    Button mDelay;
    @BindView(R.id.name)
    TextView mName;
    @BindView(R.id.gender)
    TextView mGender;
    @BindView(R.id.race)
    TextView mRace;
    @BindView(R.id.tv_pic)
    TextView mPic;
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
    @BindView(R.id.tv_read_time)
    TextView mReadTime;
    @BindView(R.id.et_id_num)
    EditText etIdNum;
    @BindView(R.id.et_birthday)
    EditText etBirthday;
    @BindView(R.id.et_validity)
    EditText etValidity;
    @BindView(R.id.cb_is_img)
    CheckBox cbIsImg;

    private EidReader eid;
    //nfc相关
    private PendingIntent pi;
    private NfcAdapter nfcAdapter;
    private static IntentFilter[] mFilters;
    private static String[][] mTechLists;
    private static final int READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
            | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V;

    private NfcAdapter.ReaderCallback mReaderCallback = new NfcAdapter.ReaderCallback() {
        @Override
        public void onTagDiscovered(Tag tag) {
            Log.d(TAG, "process-------->onTagDiscovered");
            readTravel(tag);
        }
    };

    private String appid = "";
    private String appkey = "";
    private boolean init;
    private IsoDep isodep;
    private long timeMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_travel);
        ButterKnife.bind(this);

        appid = MainActivity.appid;
        appkey = MainActivity.appkey;

        requestPerm();
        initNfc();
        showVersion();
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
            setEditText(mState, "设备不支持NFC");
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            setEditText(mState, "请在系统设置中先启用NFC功能");
            return;
        }
        pi = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mFilters = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)};

        // Setup a teach list for all NfcX tags
        mTechLists = new String[][]{
                new String[]{IsoDep.class.getName()},
                new String[]{NfcA.class.getName()},
                new String[]{NfcB.class.getName()},
                new String[]{NfcF.class.getName()},
                new String[]{NfcV.class.getName()}};
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            nfcResume();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void nfcResume() {
        Log.e(TAG, "process-------->onResume nfcAdapter  " + nfcAdapter);
        if (null != nfcAdapter) {
            if (Build.VERSION.SDK_INT == 28) {
                nfcAdapter.enableForegroundDispatch(this, pi, mFilters, mTechLists);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Bundle options = new Bundle();
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 300);
                nfcAdapter.enableReaderMode(this, mReaderCallback, READER_FLAGS, options);

            } else {
                nfcAdapter.enableForegroundDispatch(this, pi, mFilters, mTechLists);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "process-------->onNewIntent");
        if (init) {
            //intent.getAction()的值，根据NFC初始化参数不同，导致返回值不同.
            //一般返回NfcAdapter.ACTION_TECH_DISCOVERED和NfcAdapter.ACTION_TAG_DISCOVERED
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
                try {
                    Log.d(TAG, "process-------->888888 222");
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    readTravel(tag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            setEditText(mState, "请先初始化SDK");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            nfcPause();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void nfcPause() {
        Log.e(TAG, "process-------->nfcPause nfcAdapter  " + nfcAdapter);
        if (null != nfcAdapter) {
            if (Build.VERSION.SDK_INT == 28) {
                nfcAdapter.disableForegroundDispatch(this);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                nfcAdapter.disableReaderMode(this);
            } else {
                nfcAdapter.disableForegroundDispatch(this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (init) {
            try {
                EidSDK.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void requestPerm() {
        if (Build.VERSION.SDK_INT >= 23) {
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

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 81 && grantResults != null && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {// Permission Granted
//                finish();
                Toast.makeText(this, "无所需权限,请在设置中添加权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    @OnClick({R.id.read, R.id.delay, R.id.init, R.id.destroy})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.read:
                clearData();
                break;
            case R.id.delay:
                if (init) {
                    EidSDK.getDelayTime(this, 3);
                } else {
                    setEditText(mState, "请先初始化SDK");
                }
                break;
            case R.id.init:
                if (appid == null || appid.isEmpty()) {
                    Toast.makeText(this, "请检查appid", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (appkey == null || appkey.isEmpty()) {
                    Toast.makeText(this, "请检查appkey", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Log.d(TAG, "process-------->init");
//                    EidSDK.setDebug(EidSDK.TEST_MODE);
                    EidSDK.init(getApplicationContext(), appid, this);
                } catch (Exception e) {
                    setEditText(mState, "e.getMessage()");
                }
                break;
            case R.id.destroy:
                if (init) {
                    EidSDK.destroy();
                    init = false;
                } else {
                    setEditText(mState, "请先初始化SDK");
                }
                break;
        }
    }

    private void initEidReader() {
        try {
            eid = EidSDK.getEidReaderForNfc(1, this);
        } catch (Exception e) {
            setEditText(mState, "e.getMessage()");
        }
    }

    private void readTravel(Tag tag) {
        if (TextUtils.isEmpty(etIdNum.getText().toString())
                || TextUtils.isEmpty(etBirthday.getText().toString())
                || TextUtils.isEmpty(etValidity.getText().toString())) {
            setEditText(mState, "参数不能为空");
        } else {
            Log.d(TAG, "process-------->nfcReadTravel");
            /*读取旅行证件方法
             * 读取旅行证件需求三要素
             * 1 证件号
             * 2 生日日期
             * 3 有效期至
             * 日期格式统一为: 950328
             * */
            eid.nfcReadTravel(tag, etIdNum.getText().toString(), etBirthday.getText().toString(), etValidity.getText().toString(), cbIsImg.isChecked());
        }
    }

    @Override
    public void onCallData(int code, String msg) {
        switch (code) {
            case EidConstants.READ_CARD_START:
                Log.d(TAG, "process-------->READ_CARD_START");
                setEditText(mReadTime, "readTime: ");
                timeMillis = System.currentTimeMillis();
                setEditText(mState, "开始读卡，请勿移动");
                break;
            case EidConstants.READ_CARD_SUCCESS:
                Log.d(TAG, "process-------->READ_CARD_SUCCESS");
                setEditText(mState, "正在获取身份信息，请稍等...");
                setEditText(mReadTime, "readTime: 读卡：" + (System.currentTimeMillis() - timeMillis) + "ms,  请求解码：");
                //通过card_id请求识读卡片的信息
                setEditText(mRequestId, "reqId:" + msg);
                getTravelCardInfo(msg);
                break;
            case EidConstants.READ_CARD_FAILED:
                Log.d(TAG, "process-------->READ_CARD_FAILED");
                setEditText(mState, String.format(Locale.getDefault(), "读卡错误,请重新贴卡：%s", msg));
                break;
            case EidConstants.READ_CARD_DELAY:
                Log.d(TAG, "process-------->READ_CARD_DELAY");
                setEditText(mState, String.format(Locale.getDefault(), "延迟 %sms", msg));
                break;
            //初始化成功
            case 1:
                Log.d(TAG, "process-------->初始化成功");
                init = true;
                initEidReader();
            case EidConstants.ERR_ACCOUNT_EXCEPTION://请检查套餐
            case EidConstants.ERR_APP_ID_NULL://appId参数错误/未初始化
            case EidConstants.ERR_DNS_EXCEPTION://解析域名异常
            case EidConstants.ERR_NETWORK_EXCEPTION://网络连接异常
            default:
                Log.d(TAG, "process-------->default" + code + ":" + msg);
                setEditText(mState, code + ":" + msg);
                break;
        }
    }

    private void setEditText(final TextView textView, final String text) {
        Log.e("TAG", text);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    private void getTravelCardInfo(String id) {
        if (appid == null || appkey == null || !init) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TestTravelActivity.this, "请先初始化", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        final long readStart = System.currentTimeMillis();
        EidSDK.getIDCardInfo(id, appid, appkey, new EidCall() {
            @Override
            public void onCallData(int code, String msg) {
                if (code == 10000) {
                    ResultInfo result = new Gson().fromJson(msg, ResultInfo.class);
                    Log.e(TAG, "888888 resultcode 200,result = " + result.toString());
                    setEditText(mReadTime, String.format(Locale.getDefault(), "%s%dms", mReadTime.getText().toString(),
                            (System.currentTimeMillis() - readStart)));
                    if (result.code == 0) {
                        setEditText(mState, String.format(Locale.getDefault(), "身份证解析成功，业务状态：%d:%s", result.code, result.msg));
                        parseData(result);
                    } else {
                        setEditText(mState, String.format(Locale.getDefault(), "身份证解析失败，请重试(%d:%s)", result.code, result.msg));
                    }
                }
            }
        });
//        ReadCardServer.getInstance()
//                .parse(id, appid, appkey)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Subscriber<ResultInfo>() {
//                    @Override
//                    public void onCompleted() {
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        setEditText(mState, "身份证解析失败：" + e.getMessage());
//                        if (mReadTime.getText().toString().endsWith("ms")) {
//                            long rTine = System.currentTimeMillis() - timeMillis;
//                            mReadTime.setText(mReadTime.getText().toString() + rTine + "ms");
//                        }
//                        e.printStackTrace();
//                    }
//
//                    @Override
//                    public void onNext(ResultInfo result) {
//                        long rTine = System.currentTimeMillis() - timeMillis;
//                        mReadTime.setText(mReadTime.getText().toString() + rTine + "ms");
//                        if (result != null && result.code == 0) {
//                            setEditText(mState, "身份证解析成功，业务状态：" + result.code + ":" + result.msg);
//                            Log.i(TAG, "onNext: " + result.toString());
//                            parseData(result);
//                        } else {
//                            if (result != null) {
//                                Log.i(TAG, "onNext: " + result.toString());
//                                setEditText(mState, "身份证解析失败，请重试(" + result.code + ":" + result.msg + ")");
//                            } else {
//                                setEditText(mState, "身份证解析失败，请重试");
//                            }
//                        }
//                    }
//                });
    }

    private void parseData(final ResultInfo data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
                    mPic.setText(String.format("图像数据：%s", data.picture));
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        mReadTime.setText("readTime：");
        mPic.setText("图像数据：");
    }

}
