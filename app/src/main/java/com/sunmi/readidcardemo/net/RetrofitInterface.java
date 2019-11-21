package com.sunmi.readidcardemo.net;

import com.sunmi.readidcardemo.bean.Result;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;


public interface RetrofitInterface {

    @SuppressWarnings("rawtypes")
    @FormUrlEncoded
    @POST("eid/decode")
    Observable<Result> parse(@Field("request_id") String reqId,
                             @Field("app_id") String appid,
                             @Field("encrypt_factor") String encryptFactor,
                             @Field("sign") String sign);
}
