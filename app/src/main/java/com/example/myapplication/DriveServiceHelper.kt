package com.example.myapplication

import android.telecom.Call
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.api.services.drive.Drive

import java.util.Collections
import java.util.concurrent.Executors
import com.google.android.gms.tasks.Task
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executor


internal class DriveServiceHelper(private val driveService: Drive?) {
    var mExecutor: Executor = Executors.newSingleThreadExecutor()

    fun saveFile(name: String, path:String): Task<String> {
        return Tasks.call(mExecutor, Callable {
            val fileMetaData = File()
            fileMetaData.name = name
            val filePath = java.io.File(path)
            val mediaContent = FileContent("image/jpg", filePath)
            driveService!!.files().create(fileMetaData, mediaContent as AbstractInputStreamContent?)
                .setFields("id")
                .execute()
            ""
        })
    }

}