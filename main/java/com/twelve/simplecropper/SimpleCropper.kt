package com.twelve.simplecropper

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.theartofdev.edmodo.cropper.CropImageView
import com.twelve.simplecropper.AV_FrameCapture.TAG
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SimpleCropper : AppCompatActivity() {
    lateinit var imageView: CropImageView
    lateinit var cropVideoButton: Button
    var mFrameCapture: AV_FrameCapture? = null
    val USE_MEDIA_META_DATA_RETRIEVER = false
    //number of keys
    var defaultsbMax: Int = 20
    var arrayOfFrames: ArrayList<Bitmap> = ArrayList()
    var rectsArray = Array(defaultsbMax, {IntArray(4)})
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_cropper)
        playselectedvid()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Checks the orientation of the screen
        val imageView: CropImageView = findViewById(R.id.cropImageView)
        val progressPercentage: TextView = findViewById(R.id.progressPercentage)
        progressPercentage.text = "0%"
        if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {
            var params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)
            params.gravity = Gravity.CENTER
            params.weight = 1F
            imageView.setLayoutParams(params)
        }
    }

    fun playselectedvid(){
        val directory = File("sdcard/SimpleCropper/")
        if (!directory.exists()) {
            directory.mkdir()
        }
        cropVideoButton = findViewById(R.id.cropvideo)
        imageView = findViewById(R.id.cropImageView)
        imageView.isAutoZoomEnabled = false
        val sb = findViewById<SeekBar>(R.id.seekBar)
        sb.visibility = View.INVISIBLE
        val path: String = intent.getStringExtra("path").toString()
        var getAllframes: GetAllFrames
        //get video information
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        var width: Int =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        var height: Int =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        val duration: Int =
            Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
        val rotation = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))

        val mmr = FFmpegMediaMetadataRetriever()
        var frameRate = 24
        try {
            //path of the video of which you want frames
            mmr.setDataSource(path)
        } catch (e: java.lang.Exception) {
            println("Exception= $e")
        }
        frameRate = mmr.getMetadata().getDouble("framerate").toInt()
        val numberOfFrames = (duration / frameRate)
        if (rotation==270 || rotation==90){
            var tempvar = width
            width = height
            height = tempvar
            imageView.setImageBitmap(getFrameAtTimeByFrameCapture(path,0,width,height))}
        else{imageView.setImageBitmap(getFrameAtTimeByFrameCapture(path,0,width,height))}
        var params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)
        params.gravity = Gravity.CENTER
        params.weight = 1F
        imageView.setLayoutParams(params)
        val rect = Rect()
        rect.left = 0
        rect.top = 0
        rect.right = 0 + width
        rect.bottom = 0 + height
        imageView.cropRect = rect
        setRects(0,19, intArrayOf(0,0,width,height))
        imageView.setOnSetCropOverlayMovedListener{
            setRects(0,19, intArrayOf(it.left,it.top,it.right,it.bottom))
        }
        getAllframes = GetAllFrames1(path, duration, width, height)
        getAllframes.execute()
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, currentValue: Int, p2: Boolean) {
                imageView.setImageBitmap(arrayOfFrames[currentValue])
                var rect2 = Rect()
                rect2.left = rectsArray[currentValue][0]
                rect2.top = rectsArray[currentValue][1]
                rect2.right = rectsArray[currentValue][2]
                rect2.bottom = rectsArray[currentValue][3]
                imageView.cropRect = rect2
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        var outpath = "sdcard/SimpleCropper/"+"SimpleCropper"+path.substring(path.lastIndexOf("/") + 1)
        while ((File(outpath)).exists()) {
            var fileNumber = 0
            fileNumber++
            outpath = "sdcard/SimpleCropper/"+"SimpleCropper"+fileNumber.toString()+path.substring(path.lastIndexOf("/") + 1)
        }
        cropVideoButton.setOnClickListener{cropVideo(path,outpath,numberOfFrames)}

    }
    fun setRects(beginning: Int, end: Int, specificValues: IntArray){
        for (nums in beginning..end){
            rectsArray[nums] = specificValues
        }
    }

    inner class GetAllFrames1(
        private val path: String,
        private val duration: Int,
        private val width: Int,
        private val height: Int
    ) : GetAllFrames(path,duration,width,height) {
        override fun onPostExecute(result: ArrayList<Bitmap>?) {
            super.onPostExecute(result)
            if (result != null) {
                val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                val seekBar: SeekBar = findViewById(R.id.seekBar)
                arrayOfFrames = result
                progressBar.visibility = View.INVISIBLE
                seekBar.visibility = View.VISIBLE
            }
        }
    }

    fun getFrameAtTimeByMMDR(path: String, time: Long): Bitmap? {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(path)
        val bmp = mmr.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST)
        mmr.release()
        return bmp
    }

    fun getFrameAtTimeByFrameCapture(
        path: String,
        time: Long,
        snapshot_width: Int,
        snapshot_height: Int
    ): Bitmap? {
        var mFrameCapture = AV_FrameCapture()
        mFrameCapture.setDataSource(path)
        mFrameCapture.setTargetSize(snapshot_width, snapshot_height)
        mFrameCapture.init()
        return mFrameCapture.getFrameAtTime(time)
    }

    fun cropVideo(inputPath: String, outputPath: String, frameCount: Int) {
        hideElements()
        val ff = FFmpeg.getInstance(this)
        ff.loadBinary(object : FFmpegLoadBinaryResponseHandler {
            override fun onFinish() {
                Log.e("FFmpegLoad", "onFinish")
            }

            override fun onSuccess() {
                Log.e("FFmpegLoad", "onSuccess")
                val command = arrayOf("-i", inputPath, "-filter:v", "crop="+(rectsArray[0][2]-rectsArray[0][0]).toString()+":"+(rectsArray[0][3]-rectsArray[0][1]).toString()+":"+rectsArray[0][0].toString()+":"+rectsArray[0][0].toString(), "-threads", "5", "-preset", "ultrafast", "-strict", "-2", "-c:v", "libx264", "-c:a", "copy", outputPath)
                try {
                    ff.execute(command, object : ExecuteBinaryResponseHandler() {
                        override fun onSuccess(message: String?) {
                            super.onSuccess(message)
                            Log.e(TAG, "onSuccess: " + message!!)
                        }

                        override fun onProgress(message: String?) {
                            super.onProgress(message)
                            if (message != null) {

                                val messageArray = message.split("frame=")
                                if (messageArray.size >= 2) {
                                    val secondArray = messageArray[1].trim().split(" ")
                                    if (secondArray.isNotEmpty()) {
                                        val framesString = secondArray[0].trim()
                                        try {
                                            val frames = framesString.toInt()
                                            val progress = (frames.toFloat() / frameCount.toFloat()) * 100f
                                            findViewById<TextView>(R.id.progressPercentage).text =
                                                "${(progress.toInt())}%"
                                        } catch (e: Exception) {
                                        }
                                    }
                                }
                            }
                            Log.e(TAG, "onProgress: " + message!!)
                        }

                        override fun onFailure(message: String?) {
                            super.onFailure(message)
                            Log.e(TAG, "onFailure: " + message!!)
                        }

                        override fun onStart() {
                            super.onStart()
                            Log.e(TAG, "onStart: ")
                        }

                        override fun onFinish() {
                            super.onFinish()
                            Log.e(TAG, "onFinish: ")
                            findViewById<ProgressBar>(R.id.progressBar2).visibility = View.INVISIBLE
                            findViewById<TextView>(R.id.progressPercentage).text = "Video Cropped, saved in /SimpleCropper."
                        }
                    })
                } catch (e: FFmpegCommandAlreadyRunningException) {
                }
            }

            override fun onFailure() {
                Log.e("FFmpegLoad", "onFailure")
            }

            override fun onStart() {
            }
        })
    }

    fun hideElements(){
        findViewById<CropImageView>(R.id.cropImageView).visibility = View.INVISIBLE
        findViewById<Button>(R.id.cropvideo).visibility = View.INVISIBLE
        findViewById<LinearLayout>(R.id.frameLayout).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.textView3).visibility = View.INVISIBLE
        findViewById<FrameLayout>(R.id.frameLayout2).visibility = View.INVISIBLE
        findViewById<SeekBar>(R.id.seekBar).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.INVISIBLE
        //
        findViewById<ProgressBar>(R.id.progressBar2).visibility = View.VISIBLE
        findViewById<TextView>(R.id.progressPercentage).visibility = View.VISIBLE
    }
}
