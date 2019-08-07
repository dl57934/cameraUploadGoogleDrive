package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.SensorManager.getOrientation
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
import android.view.MotionEvent
import android.view.View
import android.widget.Spinner
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
import kotlin.math.roundToInt
import kotlin.math.sqrt


class PreviewCamera{
    private var cameraDevice:CameraDevice? = null
    private lateinit var previewSize:Size
    private var cameraManger:CameraManager? = null
    private var textureView:TextureView? = null
    private var activity:Activity? = null
    private lateinit var previewBuilder:CaptureRequest.Builder
    private var previewSession:CameraCaptureSession? = null
    private var appleButton:Button? = null
    private var spinner: Spinner? = null
    private var backgroundHandler:Handler? = null
    private var photoSaver:PhotoSaver? = null
    private var helper:DriveServiceHelper? = null

    //Lock
    private val STATE_PREVIEW = 0
    private val state = STATE_PREVIEW
    private val STATE_WAITING_LOCK = 1
    private val STATE_WAITING_PRECAPTURE = 2
    private val STATE_WAITING_NON_PRECAPTURE = 3
    private val STATE_PICTURE_TAKEN = 4

    //zooming
    private var fingerSpacing = 0
    private var zoomLevel = 1f
    private var maximumZoomLevel: Float = 0.toFloat()
    private var zoom: Rect? = null

    @SuppressLint("ClickableViewAccessibility")
    constructor(activity: Activity, textureView: TextureView) {
        cameraManger = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        this.textureView = textureView
        this.activity = activity
        maximumZoomLevel = (getCameraCharacter(getBehindCameraId()).get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))
        photoSaver = PhotoSaver(activity)
        spinner = activity.findViewById(R.id.locationSpinner)
        appleButton = activity.findViewById<Button>(R.id.apple_button)
        backgroundHandler = getBackgroundHandler()
        appleButton!!.setOnClickListener {
            savePhoto()
        }
        textureView.setOnTouchListener { v, event ->
            zoomEvent(event)
        }
    }

    private fun zoomEvent(event: MotionEvent):Boolean{
        try {
            val rect =
                getCameraCharacter(getBehindCameraId()).get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                    ?: return false
            var currentFingerSpacing:Float = 0.0f

            if (event.pointerCount == 2) { //Multi touch.
                currentFingerSpacing = getFingerSpacing(event)
                var delta:Float = 0.05f
                if (fingerSpacing != 0) {
                    if (currentFingerSpacing > fingerSpacing) {
                        if ((maximumZoomLevel - zoomLevel) <= delta) {
                            delta = maximumZoomLevel - zoomLevel;
                        }
                        zoomLevel = zoomLevel + delta;
                    } else if (currentFingerSpacing < fingerSpacing){
                        if ((zoomLevel - delta) < 1f) {
                            delta = zoomLevel - 1f;
                        }
                        zoomLevel -= delta;
                    }
                    var ratio = 1 / zoomLevel
                    var croppedWidth = rect.width() - (rect.width() * ratio).roundToInt();
                    var croppedHeight = rect.height() - (rect.height () * ratio).roundToInt()
                    zoom = Rect(croppedWidth/2, croppedHeight/2,
                            rect.width() - croppedWidth/2, rect.height() - croppedHeight/2)
                    previewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom)
                }
                fingerSpacing = currentFingerSpacing.toInt()
            } else {
                return true
            }
            updatedPreview()
            return true
        } catch (e:Exception){
            e.printStackTrace()
        }
        return true
    }

    private fun getFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    fun setDrive(drive: Drive?){
        helper = DriveServiceHelper(drive)
    }

    private fun savePhoto(){
        var fileOutputStream: FileOutputStream? = null
        try{
            val galleryFolder:java.io.File = photoSaver!!.createImageGallery()
            val file:File = photoSaver!!.createImageFile(galleryFolder)
            fileOutputStream = FileOutputStream(file)
            textureView!!.bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            val imagePath:String= galleryFolder.absolutePath+"/"+file.name
            val intent:Intent = Intent(activity!!.applicationContext, Main2Activity::class.java)
            intent.putExtra("file", file.path)
            intent.putExtra("spinner", spinner!!.selectedItem.toString())
            helper!!
                .saveFile(file.name, imagePath)
                .addOnFailureListener { exception -> Log.e("exception", exception.toString()) }
            activity!!.startActivity(intent)

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
        val size:Size = Size(3024, 3024)
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

    private fun getCameraStateListener() = object:CameraDevice.StateCallback(){
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
        previewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom)
        previewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
        previewBuilder.addTarget(surface)

        cameraDevice!!.createCaptureSession(listOf(surface), getCameraSessionCallback(), backgroundHandler)
    }

    private fun setPreviewBufferSize(texture:SurfaceTexture) = texture.setDefaultBufferSize(previewSize.width, previewSize.height)

    private fun isCanPlayCamera():Boolean = cameraDevice == null || !textureView!!.isAvailable //함수이름 물어보

    private fun getCameraSessionCallback():CameraCaptureSession.StateCallback = object: CameraCaptureSession.StateCallback() {
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


