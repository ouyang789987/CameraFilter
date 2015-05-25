package me.relex.camerafilter.camera;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import me.relex.camerafilter.gles.FullFrameRect;
import me.relex.camerafilter.gles.Texture2dProgram;
import me.relex.camerafilter.widget.CameraSurfaceView;

public class CameraSurfaceRenderer implements GLSurfaceView.Renderer {

    private final CameraSurfaceView.CameraHandler mCameraHandler;
    private int mTextureId = -1;
    private FullFrameRect mFullScreen;
    private SurfaceTexture mSurfaceTexture;
    private final float[] mSTMatrix = new float[16];

    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth, mIncomingHeight;
    private int mSurfaceWidth, mSurfaceHeight;

    public CameraSurfaceRenderer(CameraSurfaceView.CameraHandler cameraHandler) {
        mCameraHandler = cameraHandler;
        mIncomingSizeUpdated = false;
        mIncomingWidth = mIncomingHeight = -1;
    }

    public void setCameraPreviewSize(int width, int height) {
        mIncomingWidth = width;
        mIncomingHeight = height;
        mIncomingSizeUpdated = true;

        Log.e("CameraSurfaceRenderer", "setCameraPreviewSize  pw = "
                + width
                + " ; py = "
                + height
                + " ; sw = "
                + mSurfaceWidth
                + " ; sy = "
                + mSurfaceHeight);

        // TODO 处理 画面缩放等

        float scaleHeight = mSurfaceWidth / (width * 1f / height * 1f);
        float surfaceHeight = mSurfaceHeight;

        if (mFullScreen != null) {
            mFullScreen.scaleMatrix(1f, scaleHeight / surfaceHeight);
        }
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //Log.e("CameraSurfaceRenderer", "onSurfaceCreated");
        mFullScreen =
                new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mTextureId = mFullScreen.createTextureObject();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        //Log.e("CameraSurfaceRenderer",
        //        "onSurfaceChanged width = " + width + "; height = " + height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        gl.glViewport(0, 0, width, height);

        mCameraHandler.sendMessage(
                mCameraHandler.obtainMessage(CameraSurfaceView.CameraHandler.SETUP_CAMERA, width,
                        height, mSurfaceTexture));
    }

    @Override public void onDrawFrame(GL10 gl) {

        mSurfaceTexture.updateTexImage();

        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }

        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mFullScreen.drawFrame(mTextureId, mSTMatrix);
    }

    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        if (mFullScreen != null) {
            mFullScreen.release(false);     // assume the GLSurfaceView EGL context is about
            mFullScreen = null;             //  to be destroyed
        }

        mIncomingWidth = mIncomingHeight = -1;
    }
}