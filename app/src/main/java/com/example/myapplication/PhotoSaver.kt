package com.example.myapplication

import com.example.myapplication.R
import android.app.Activity
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class PhotoSaver(private var activity: Activity) {
    private var galleryFolder:File? = null

    fun createImageGallery():File {
        val storageDirectory = getExternalStoragePublicDirectory(DIRECTORY_PICTURES)
        galleryFolder = File(storageDirectory, activity.resources.getString(R.string.app_name))
        if (!galleryFolder!!.exists()) {
            val wasCreated = galleryFolder!!.mkdirs()
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory")
            }
        }
        return galleryFolder as File
    }


    fun createImageFile(galleryFolder: File): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "image_" + timeStamp + "_"
        return File.createTempFile(imageFileName, ".jpg", galleryFolder)
    }
}