package com.example.myapplication

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Part

interface RetrofitAPI{
    @GET("sendImage")
    fun sendImage(@Part image:MultipartBody.Part):Call<IsSuccessSendImageFile>
}