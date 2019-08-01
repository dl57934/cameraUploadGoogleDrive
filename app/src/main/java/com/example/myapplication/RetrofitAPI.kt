package com.example.myapplication

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitAPI{
    @Multipart
    @POST("/sendImage")
    fun sendImage(@Query("location") location:String, @Part image:MultipartBody.Part):Call<IsSuccessSendImageFile>
}