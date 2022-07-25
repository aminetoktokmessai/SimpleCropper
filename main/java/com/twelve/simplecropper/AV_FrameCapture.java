package com.twelve.simplecropper;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.twelve.simplecropper.AV_GLHelper;
import com.twelve.simplecropper.AV_VideoDecoder;

public class AV_FrameCapture {
    final static String TAG = "AV_FrameCapture";

    private HandlerThread mGLThread = null;
    private Handler mGLHandler = null;
    private AV_GLHelper mGLHelper = null;

    private int mDefaultTextureID = 10001;

    private int mWidth = 1920;
    private int mHeight = 1080;

    private String mPath = null;

    public AV_FrameCapture() {
        mGLHelper = new AV_GLHelper();
        mGLThread = new HandlerThread("AV_FrameCapture");

        mGLThread.start();
        mGLHandler = new Handler(mGLThread.getLooper());
    }

    public void setDataSource(String path) {
        mPath = path;
    }

    public void setTargetSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void init() {
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                SurfaceTexture st = new SurfaceTexture(mDefaultTextureID);
                st.setDefaultBufferSize(mWidth, mHeight);
                mGLHelper.init(st);
            }
        });
    }

    public void release() {
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                mGLHelper.release();
                mGLThread.quit();
            }
        });
    }

    private Object mWaitBitmap = new Object();
    private Bitmap mBitmap = null;

    public Bitmap getFrameAtTime(final long frameTime) {
        if (null == mPath || mPath.isEmpty()) {
            throw new RuntimeException("Illegal State");
        }

        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                getFrameAtTimeImpl(frameTime);
            }
        });

        synchronized (mWaitBitmap) {
            try {
                mWaitBitmap.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return mBitmap;
    }

    @SuppressLint("SdCardPath")
    public void getFrameAtTimeImpl(long frameTime) {
        final int textureID = mGLHelper.createOESTexture();
        final SurfaceTexture st = new SurfaceTexture(textureID);
        final Surface surface = new Surface(st);
        final AV_VideoDecoder vd = new AV_VideoDecoder(mPath, surface);
        st.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                Log.i(TAG, "onFrameAvailable");
                mGLHelper.drawFrame(st, textureID);
                mBitmap = mGLHelper.readPixels(mWidth, mHeight);
                synchronized (mWaitBitmap) {
                    mWaitBitmap.notify();
                }

                vd.release();
                st.release();
                surface.release();
            }
        });

        if (!vd.prepare(frameTime)) {
            mBitmap = null;
            synchronized (mWaitBitmap) {
                mWaitBitmap.notify();
            }
        }
    }
}
