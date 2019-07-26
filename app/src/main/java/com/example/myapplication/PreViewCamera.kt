package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import java.lang.Exception
import java.util.concurrent.Semaphore
import android.hardware.camera2.params.RggbChannelVector
import kotlin.math.ln
import kotlin.math.pow
import android.hardware.camera2.CameraCharacteristics
import com.google.api.services.drive.Drive
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.security.auth.callback.Callback


class PreviewCamera :Thread{
    private var cameraDevice:CameraDevice? = null
    private lateinit var previewSize:Size
    private var cameraManger:CameraManager? = null
    private var textureView:TextureView? = null
    private lateinit var previewBuilder:CaptureRequest.Builder
    private var previewSession:CameraCaptureSession? = null
    private var appleButton:Button? = null
    private var backgroundHandler:Handler? = null
    private var photoSaver:PhotoSaver? = null
    private var helper:DriveServiceHelper? = null

    @SuppressLint("ClickableViewAccessibility")
    constructor(activity: Activity, textureView: TextureView) {
        cameraManger = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        this.textureView = textureView
        photoSaver = PhotoSaver(activity)
        appleButton = activity.findViewById<Button>(R.id.apple_button)
        backgroundHandler = getBackgroundHandler()
        appleButton!!.setOnClickListener {
            savePhoto()
        }
    }

    private fun sendImage(path:String){
        Thread{
            val file = File(path)
            val requestFile:RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
            val body:MultipartBody.Part = MultipartBody.Part.createFormData("image", file.name, requestFile)
            val receiver = SocketManagement().getRetrofitService().sendImage(body).execute()
            Log.e("result", receiver.body().toString())
        }.start()
    }

    fun setDrive(drive: Drive?){
        helper = DriveServiceHelper(drive)
    }

    private fun savePhoto(){
        var fileOutputStream: FileOutputStream? = null
        try{
            val galleryFolder:java.io.File = photoSaver!!.createImageGallery()
            Log.e("path", galleryFolder.absolutePath)
            val file = photoSaver!!.createImageFile(galleryFolder)
            fileOutputStream = FileOutputStream(file)
            textureView!!.bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            val imagePath:String= galleryFolder.absolutePath+"/"+file.name
            sendImage(imagePath)
            helper!!
                .saveFile(file.name, imagePath)
                .addOnFailureListener { exception -> Log.e("exception", exception.toString()) }
        }catch (e:Exception){
            e.printStackTrace()
        }
        finally {
            fileOutputStream?.close()
        }
    }

    private fun getBehindCameraId():String{
        for (cameraId in cameraManger!!.cameraIdList){
            var lens:CameraCharacteristics = getCameraCharacter(cameraId)
            if(isLensFacingBack(lens)){
                setPreviewSize(lens)
                return cameraId
            }
        }
        return ""
    }


    private fun getCameraCharacter(cameraId:String):CameraCharacteristics = cameraManger!!.getCameraCharacteristics(cameraId)
    private fun isLensFacingBack(lens:CameraCharacteristics):Boolean = lens.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK

    private fun setPreviewSize(lens:CameraCharacteristics){
        val map:StreamConfigurationMap? = lens.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = map!!.getOutputSizes(SurfaceTexture::class.java)
        previewSize = sizes[0]

        for (size:Size in sizes)
            if (size.width > previewSize.width)
                previewSize = size
    }



    @SuppressLint("MissingPermission")
    private fun playCamera(){
        try{
            cameraManger!!.openCamera(getBehindCameraId(), getCameraStateListener(), null)
        }catch (exception:Exception){
            exception.printStackTrace()
        }
    }

    private fun getCameraStateListener()= object:CameraDevice.StateCallback(){
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }

    private fun startPreview(){
        if(isCanPlayCamera())
            Log.e("Error: ", "please ready Camera")
        val texture = textureView!!.surfaceTexture
        setPreviewBufferSize(texture)

        val surface:Surface = Surface(texture)

        previewBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewBuilder.addTarget(surface)

        cameraDevice!!.createCaptureSession(listOf(surface), getCameraSessionCallback(), backgroundHandler)
    }

    private fun setPreviewBufferSize(texture:SurfaceTexture) = texture.setDefaultBufferSize(previewSize.width, previewSize.height)

    private fun isCanPlayCamera():Boolean = cameraDevice == null || !textureView!!.isAvailable //함수이름 물어보

    private fun getCameraSessionCallback() = object: CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
        }

        override fun onConfigured(session: CameraCaptureSession) {
            previewSession = session
            updatedPreview()
        }
    }



    fun updatedPreview(){
//        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)//자동 화이트 베이스 옵션 끄기
        previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
//        previewBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
        //previewBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, colorTemperature(2300)) //wb설정
        // previewBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 2) ISO설정
        previewSession!!.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler)
    }

    private fun colorTemperature(whiteBalance: Int): RggbChannelVector {
        val temperature = (whiteBalance / 100).toFloat()
        var red: Float
        var green: Float
        var blue: Float

        if (temperature <= 66)
            red = 255f
        else {
            red = temperature - 60
            red = (329.698727446 * red.toDouble().pow(-0.1332047592)).toFloat()
            if (red < 0)
                red = 0f
            if (red > 255)
                red = 255f
        }

        if (temperature <= 66) {
            green = temperature
            green = (99.4708025861 * ln(green.toDouble()) - 161.1195681661).toFloat()
            if (green < 0)
                green = 0f
            if (green > 255)
                green = 255f
        } else {
            green = temperature - 60
            green = (288.1221695283 * green.toDouble().pow(-0.0755148492)).toFloat()
            if (green < 0)
                green = 0f
            if (green > 255)
                green = 255f
        }

        if (temperature >= 66)
            blue = 255f
        else if (temperature <= 19)
            blue = 0f
        else {
            blue = temperature - 10
            blue = (138.5177312231 * ln(blue.toDouble()) - 305.0447927307).toFloat()
            if (blue < 0)
                blue = 0f
            if (blue > 255)
                blue = 255f
        }

        return RggbChannelVector(red / 255 * 2, green / 255, green / 255, blue / 255 * 2)
    }

    private fun getBackgroundHandler(): Handler{
        val handlerThread = HandlerThread("CameraPreview")
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

    private fun setSurfaceTextureListener() {
        textureView!!.surfaceTextureListener = getSurfaceTextureListener()
    }

    private fun getSurfaceTextureListener() = object: TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            playCamera()
        }
    }

    fun onResume(){
        setSurfaceTextureListener()
    }

    private val cameraOpenCloseLock:Semaphore = Semaphore(1)

    fun onPause(){
        try {
            cameraOpenCloseLock.acquire()
            if(cameraDevice!=null){
                cameraDevice!!.close()
                cameraDevice = null
            }
        }catch (e:InterruptedException){
        }finally {
            cameraOpenCloseLock.release()
        }
    }
}


