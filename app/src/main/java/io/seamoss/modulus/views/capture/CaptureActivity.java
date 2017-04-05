package io.seamoss.modulus.views.capture;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import io.seamoss.modulus.Modulus;
import io.seamoss.modulus.R;
import io.seamoss.modulus.base.BaseActivity;
import io.seamoss.modulus.base.nav.BaseNavActivity;
import io.seamoss.modulus.hardware.Doppler;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by Alexander Melton on 3/29/2017.
 */

public class CaptureActivity extends BaseActivity implements CaptureView {

    @BindView(R.id.texture)
    TextureView textureView;

    @BindView(R.id.overlay_capture)
    FrameLayout frameLayout;

    @BindView(R.id.capture_speed)
    TextView speedText;

    private String cameraId;
    private Size imageDimension;
    private List<Double> speeds;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSessions;
    private PublishSubject<SurfaceTexture> onUpdateSubject;
    private Subscription updateSubsription;
    private int frameCount = 0;

    @Inject
    CapturePresenter capturePresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Modulus.getInstance().getAppGraph().inject(this);

        speeds = new ArrayList<>();
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        onUpdateSubject = PublishSubject.create();

        File media = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Test.mp4");
    }

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            frameCount++;
            onUpdateSubject.onNext(surface);
            if(frameCount%5 == 0){
                displayAverageSpeed();
            }
        }
    };

    public void displayAverageSpeed(){
        double average = 0;
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);
        for(Double d: speeds){
            average = d + average;
        }
        if(speeds.size() > 0) average = average/speeds.size();
        speedText.setText(df.format(average) + " MPH");
        speeds.clear();
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Timber.d("Opened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void createCameraPreview(){
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        assert surfaceTexture != null;
        surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
        Surface surface = new Surface(surfaceTexture);
        try{
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getApplicationContext(), "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }

    }

    private void transformImage(int width, int height){
        Matrix matrix = new Matrix();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        RectF textureRectF = new RectF(0, 0, width, height);
        RectF previewRectF = new RectF(0, 0, imageDimension.getHeight(), imageDimension.getWidth());
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();

        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            previewRectF.offset(centerX - previewRectF.centerX(), centerY - previewRectF.centerY());
            matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
            Float scale = Math.max((float)width / (float)imageDimension.getWidth(), (float)height/ (float)imageDimension.getHeight());
            Timber.d("SCALE " + scale);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY );
        }

        Timber.d("TEXT WIDTH " + textureView.getWidth() + " TEX HEIGHT " + textureView.getHeight());
        textureView.setTransform(matrix);
        Timber.d("TEXT WIDTH " + textureView.getWidth() + " TEX HEIGHT " + textureView.getHeight());
        Timber.d("DIM WIDTH " + imageDimension.getWidth() + " DIM HEIGHT " + imageDimension.getHeight());
        Timber.d("WIDTH " + width + " HEIGHT " + height);

    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            Timber.d("updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, capturePresenter.getHandler());

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void openCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            try{
                cameraManager.openCamera(cameraId, stateCallback, null);
            }catch (SecurityException e){
                e.printStackTrace();
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
        transformImage(textureView.getWidth(), textureView.getHeight());
    }


    @Override
    protected void onPause() {
        super.onPause();
        capturePresenter.detachView();
        cameraDevice.close();
        cameraCaptureSessions.close();
        updateSubsription.unsubscribe();
        Doppler.getInstance().stopCapture();
    }

    @Override
    protected void onResume() {
        super.onResume();
        capturePresenter.attachView(this);
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
        updateSubsription = onUpdateSubject.asObservable()
                .subscribeOn(AndroidSchedulers.from(capturePresenter.getHandler().getLooper()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(capturePresenter::fetchSpeedData);
        Doppler.getInstance().beginCapture();
        Doppler.getInstance().getSpeedObservable()
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::speedCallback, e -> e.printStackTrace());
    }

    public void speedCallback(Double speed){
        speeds.add(speed);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_capture;
    }
}
