package com.example.myapplication

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SocketManagement{
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    private val mretrofit:Retrofit = Retrofit
        .Builder()
        .baseUrl("http://10.0.0.50:5000/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    private val retrofitService = mretrofit.create(RetrofitAPI::class.java)
    fun getRetrofitService(): RetrofitAPI = retrofitService
}