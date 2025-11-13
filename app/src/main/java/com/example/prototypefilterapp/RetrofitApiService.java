package com.example.prototypefilterapp;

import com.google.gson.JsonObject;
import retrofit2.http.Body;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface RetrofitApiService {
    @POST("/analyze_filter_recommendation")
    Call<JsonObject> analyzeRecommendation(@Body JsonObject body);

    // Multipart/form-data 전송을 위한 정의
    @Multipart
    @POST("generate_filter_image")
    Call<JsonObject> generateFilterImage(
            @Part MultipartBody.Part image,
            @Part("prompt") RequestBody prompt
    );
}

