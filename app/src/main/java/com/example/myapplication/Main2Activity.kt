package com.example.myapplication

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_main2.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File

class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val filePath = intent.getStringExtra("file")
        val location = intent.getStringExtra("spinner")
        sendButton.setOnClickListener {
            Log.e("file", filePath+location)
            sendImage(filePath, location)
        }
    }

    private fun getBrixAverage():Float = (StringToFloat(getEditText(brix1)) + StringToFloat(getEditText(brix2)) + StringToFloat(getEditText(brix3)))/3

    private fun getEditText(brix:EditText) =brix.text.toString()

    private fun StringToFloat(data:String) = data.toFloat()

    private fun sendImage(path:String, location:String){
        Thread{
            val file = File(path)
            val requestFile: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
            val body: MultipartBody.Part = MultipartBody.Part.createFormData("image", file.name, requestFile)
            val brix:String = getBrixAverage().toString()
            Log.e("brix", brix)
//            val receiver = SocketManagement().getRetrofitService().sendImage(location, body).execute()
            SocketManagement().getRetrofitService().sendImage("$location,$brix", body).enqueue(object : retrofit2.Callback<IsSuccessSendImageFile> {
                override fun onFailure(call: Call<IsSuccessSendImageFile>, t: Throwable) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
                override fun onResponse(
                    call: Call<IsSuccessSendImageFile>,
                    response: Response<IsSuccessSendImageFile>
                ) {
                    Log.e("server Response", response.body().toString())
                }

            })
        }.start()
        this.finish()
    }
}
