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
        .baseUrl("http://ec2-18-216-201-243.us-east-2.compute.amazonaws.com:5000/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    private val retrofitService = mretrofit.create(RetrofitAPI::class.java)
    fun getRetrofitService(): RetrofitAPI = retrofitService
}