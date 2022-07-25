package com.twelve.simplecropper

import android.graphics.Bitmap
import android.os.AsyncTask

open class GetAllFrames(path: String, duration: Int, width: Int, height: Int) : AsyncTask<Void, Void, ArrayList<Bitmap>>() {

    var mPath = path
    var mDuration = duration
    var mWidth = width
    var mHeight = height
    var mArrayList: ArrayList<Bitmap> = ArrayList()
    override fun doInBackground(vararg p0: Void?): ArrayList<Bitmap> {
        var i = 0
        while (i < mDuration*1000) {
            getFrameAtTimeByFrameCapture(mPath, i.toLong(), mWidth, mHeight)?.let {
                mArrayList.add(
                    it
                )
            }
            i += mDuration*1000 / 20
        }
        return mArrayList
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
}