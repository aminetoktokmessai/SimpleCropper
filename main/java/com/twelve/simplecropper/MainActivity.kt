package com.twelve.simplecropper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import java.lang.Thread.sleep

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_STORAGE_PERMISSION1 = 10
        private const val REQUEST_STORAGE_PERMISSION2 = 11
    }
    lateinit var tempIntent: Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (isStoragePermissionGranted()) {

        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION1)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION2)
        }
        fun initialize() {
            val ffmpeg = FFmpeg.getInstance(this.applicationContext)
            try {
                ffmpeg.loadBinary(object : LoadBinaryResponseHandler() {
                    override fun onFinish() {
                        super.onFinish()
                    }

                    override fun onSuccess() {
                        super.onSuccess()
                    }

                    override fun onFailure() {
                        super.onFailure()
                    }

                    override fun onStart() {
                        super.onStart()
                    }
                })
            } catch (e: FFmpegNotSupportedException) {
                Toast.makeText(this,"Device not supported",Toast.LENGTH_LONG).show()
                sleep(900)
                this.finish()
            }
        }
        initialize()
        val simpleCrop = findViewById<Button>(R.id.simpleCrop)
        simpleCrop.setOnClickListener(View.OnClickListener { selectVideo(Intent(this, SimpleCropper::class.java)) })
    }
    private fun selectVideo(intent2: Intent) {
        //intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        tempIntent = intent2
        intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, 1)}
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1) {

            val selectedMediaUri = data!!.getData()
            if (selectedMediaUri.toString().contains("video")) {
                //handle video

                // Get selected gallery image
                val selectedVideo = data.getData()
                // Get and resize profile image
                val filePathColumn = arrayOf(MediaStore.Video.Media.DATA)
                val cursor = applicationContext.contentResolver.query(
                    selectedVideo!!,
                    filePathColumn,
                    null,
                    null,
                    null
                )
                cursor!!.moveToFirst()

                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                val videoStoragePath = cursor.getString(columnIndex)
                cursor.close()

                var intent = tempIntent
                intent.putExtra("path", videoStoragePath)
                startActivity(intent)
            }
        }
    }
    private fun isStoragePermissionGranted(): Boolean {
        val selfPermission = ContextCompat.checkSelfPermission(baseContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val selfPermission2 = ContextCompat.checkSelfPermission(baseContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        return selfPermission == PackageManager.PERMISSION_GRANTED  && selfPermission2 == PackageManager.PERMISSION_GRANTED
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION1) {
            if (isStoragePermissionGranted()) {

            } else {
                Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}