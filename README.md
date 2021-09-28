# ReadIDCardDemo

> 新版身份证云识别分支为：develop_v2_new

### 新版接口相对老版本的改进:

- 开发者不再需要关注Nfc原生刷卡接口，直接使用新版封装好的接口
- 开发者不再需要关注在商米金融机具上，有关商米的金融刷卡接口的使用，直接使用新版封装好的接口
- 新增由于权限或者rom版本问题获取不到SN相关提示

### 包引用

```
//新版SDK 只需要集成这一个包即可完成所有流程
implementation "com.sunmi:SunmiEID-SDK:1.3.2"
```

### 接口说明：

1. 设置为调试模式

   > 设置为调试模式会输出更多log , 正式环境不要使用

   `EidSDK.setDebuggable(true);`

2. 初始化

   > 初始化SDK,主要用于保存appid或者授权操作
   >
   > 初始化成功回调返回：`EidConstants.EID_INIT_SUCCESS`

​      `EidSDK.init(Context context, String appId, EidCall call)`

3. 开始识别身份证

   > 开始识别身份证，对应的回调code如下
   >
   > `EidConstants.READ_CARD_START` 已经准备好，可以刷身份证
   >
   > `EidConstants.READ_CARD_START` 刷身份证中，请勿移动身份证
   >
   > `EidConstants.READ_CARD_SUCCESS` 刷身份证成功获取到`reqId`，通过OpenApi获取身份证具体信息
   >
   > 其他错误码见demo源码

​     `EidSDK.startCheckCard(Activity act, EidCall call)`

4. 解密获取身份证后的图片

   > 图片是加密的需要解密

   `Bitmap EidSDK.parseCardPhoto(String photo)`

5. 停止读身份证

   > 停止读身份证，关闭nfc或者金融读读卡接口

​     `EidSDK.stopCheckCard(Activity act)`

6. SDK资源销毁，资源释放

   > 释放对象引用，appid等，调用此接口后，再使用需要重新初始化

​     `EidSDK.destroy()`

### 接口使用示例代码

1. 调用初始化

   ```java
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
   ```

2. 开始读身份证

   ```java
   private void startCheckCard() {
       //Step 2 开启读卡 -> 调用startCheckCard方法，通过回调结果处理业务逻辑
       //注：默认循环读卡，只会回调一次EidConstants.READ_CARD_READY
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
                       //*** 异常处理： 其他失败 - code为错误码，msg为详细错误原因 需要重新调用 startCheckCard 方法 （手动触发，非自动）***
                       Log.e(TAG, "读卡失败：code:" + code + ",msg:" + msg);
                       setEditText(mState, String.format(Locale.getDefault(), "其他错误：%d,%s", code, msg));
                       break;
               }
           }
       });
   }
   ```

3. 停止读身份证

   ```java
   EidSDK.stopCheckCard(ReadCardActivity.this);
   ```

4. 释放SDK资源

   ```java
   @Override
   protected void onDestroy() {
       super.onDestroy();
       EidSDK.destroy();
   }
   ```

