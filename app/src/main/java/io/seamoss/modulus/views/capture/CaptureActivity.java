package io.seamoss.modulus.views.capture;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
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
import android.media.MediaRecorder;
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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

    @BindView(R.id.capture_record)
    ImageView recordButton;

    @BindView(R.id.overlay_capture)
    FrameLayout frameLayout;

    @BindView(R.id.capture_speed)
    TextView speedText;

    @BindView(R.id.capture_speed_max)
    TextView speedTextMax;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private Surface recorderSurface;
    private String filePath;
    private String cameraId;
    private Size imageDimension;
    private List<Double> speeds;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSessions;
    private PublishSubject<SurfaceTexture> onUpdateSubject;
    private Subscription updateSubsription;
    private Subscription speedSubscription;
    private double maxSpeed = 0;
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

        speedTextMax.setOnClickListener(this::speedTextMaxListener);
        recordButton.setOnClickListener(this::onRecordClick);

    }

    public void speedTextMaxListener(View view){
        speedTextMax.setText("MAX: 00 MPH");
        maxSpeed = 0;
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

    public void onRecordClick(View view){
        if(isRecording) stopRecording();
        else beginRecording();
    }

    public void displayAverageSpeed(){
        double average = 0;
        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);
        int zeros = 0;
        for(Double d: speeds){
            average = d + average;
            if(d == 0) zeros++;
        }
        if(speeds.size() > 0 && zeros != speeds.size()){
            average = average/(speeds.size() - zeros);
            speedText.setText(df.format(average) + " MPH");
            if(average > maxSpeed){
                speedTextMax.setText("MAX: " + df.format(average) + " MPH");
                maxSpeed = average;
            }
        }else{
            speedText.setText("00 MPH");
        }
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

        textureView.setTransform(matrix);

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
            mediaRecorder = new MediaRecorder();
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
        if(!isRecording) {
            cameraDevice.close();
            cameraCaptureSessions.close();

            updateSubsription.unsubscribe();
            updateSubsription = null;
            Doppler.getInstance().stopCapture();
            speedSubscription.unsubscribe();
            speedSubscription = null;
            capturePresenter.detachView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isRecording) {
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
            speedSubscription = Doppler.getInstance().getSpeedObservable()
                    .onBackpressureDrop()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(this::speedCallback, e -> e.printStackTrace());
        }


    }

    private void closePreview(){
        if(cameraCaptureSessions != null){
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
        }
    }

    private void setupMediaRecorder() throws IOException{
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if(filePath == null || filePath.isEmpty()){
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES);
            File file = new File(path, System.currentTimeMillis() + ".mp4");
            filePath = file.getAbsolutePath();
            //file.createNewFile();
        }
        mediaRecorder.setOutputFile(filePath);
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1920, 1080);
        //mediaRecorder.setVideoSize(imageDimension.getWidth(), imageDimension.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        mediaRecorder.setOrientationHint(rotation);
        mediaRecorder.prepare();
    }

    private void beginRecording(){
        if(cameraDevice == null || !textureView.isAvailable() || imageDimension == null) return;
        isRecording = true;

        try{
            closePreview();
            setupMediaRecorder();
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            assert surfaceTexture != null;
            surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(surfaceTexture);
            surfaces.add(previewSurface);
            captureRequestBuilder.addTarget(previewSurface);

            recorderSurface = mediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            captureRequestBuilder.addTarget(recorderSurface);

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI
                            recordButton.setBackgroundColor(Color.RED);

                            // Start recording
                            mediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    isRecording = false;
                    createFailToast();
                }
            }, capturePresenter.getHandler());
        }catch (CameraAccessException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void stopRecording(){
        isRecording = false;
        recordButton.setBackgroundColor(Color.WHITE);
        try{
            mediaRecorder.stop();
        }catch (RuntimeException e){
            e.printStackTrace();
        }
        mediaRecorder.reset();

        Toast.makeText(this, "Video saved: " + filePath , Toast.LENGTH_SHORT).show();
        filePath = null;

    }

    public void createFailToast(){
        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
    }

    public void speedCallback(Double speed){
        Timber.d(speed + " ");
        speeds.add(speed);
    }

    private String getVideoFilePath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath().substring(8);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_capture;
    }
}
