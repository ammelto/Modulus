package io.seamoss.modulus.views.capture;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;

import io.seamoss.modulus.base.mvp.BasePresenter;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by Alexander Melton on 3/29/2017.
 */

public class CapturePresenter extends BasePresenter<CaptureView> {

    private Handler mBackgroundHandler;
    private HandlerThread captureThread;

    @Override
    public void attachView(CaptureView view) {
        super.attachView(view);
        startBackgroundListener();
    }

    @Override
    public void detachView() {
        super.detachView();
        stopBackgroundListener();
    }

    public Handler getHandler(){
        return mBackgroundHandler;
    }

    protected void startBackgroundListener(){
        captureThread = new HandlerThread("capture");
        captureThread.start();
        mBackgroundHandler = new Handler(captureThread.getLooper());
    }

    protected void stopBackgroundListener(){
        captureThread.quitSafely();
        try{
            captureThread.join();
            captureThread = null;
            mBackgroundHandler = null;
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    protected void fetchSpeedData(SurfaceTexture surface){

    }

}
