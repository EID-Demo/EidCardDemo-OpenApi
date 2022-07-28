package com.sunmi.readidcardemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sunmi.eidlibrary.EidCall;
import com.sunmi.eidlibrary.EidSDK;
import com.sunmi.readidcardemo.Constant;
import com.sunmi.readidcardemo.R;

public class MainListActivity extends AppCompatActivity {

    EditText mAppidEt;
    TextView mStatusTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        mAppidEt = findViewById(R.id.et_appid);
        mStatusTv = findViewById(R.id.tv_status);
        mAppidEt.setText(Constant.APP_ID);
    }

    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_init:
                EidSDK.setDebuggable(true);
//                EidSDK.setDebug(EidSDK.TEST_MODE);
                if (TextUtils.isEmpty(mAppidEt.getText().toString())) {
                    mStatusTv.setText("请输入APPID");
                } else {
                    Constant.APP_ID = mAppidEt.getText().toString();
                    //这里可在application里初始化
                    EidSDK.init(getApplicationContext(), mAppidEt.getText().toString(), new EidCall() {
                        @Override
                        public void onCallData(int code, String msg) {
                            mStatusTv.setText("初始化：" + code + ",msg: " + msg);
                        }
                    });
                }
                break;
            case R.id.btn_card:
                startActivity(new Intent(this, ReadCardActivity.class));
                break;
            case R.id.btn_travel:
                startActivity(new Intent(this, ReadTravelActivity.class));
                break;
            case R.id.btn_destory:
                EidSDK.destroy();
                mStatusTv.setText("未初始化");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EidSDK.destroy();
    }
}