package com.cloudlab.soundlessaudioclassification.framework.retrofit.services

import com.cloudlab.soundlessaudioclassification.framework.retrofit.entities.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @GET("ping")
    fun ping(): Call<String>

    @Multipart
    @POST("uploadSingle")
    fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("labels") labels: RequestBody
    ): Call<ApiResponse>
}