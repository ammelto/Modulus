package io.seamoss.modulus.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaSync;
import android.media.audiofx.AutomaticGainControl;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.fft.DoubleFFT_2D;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by Alexander Melton on 4/1/2017.
 */

public class Doppler extends BroadcastReceiver{
    private static Doppler instance;
    private int N;
    private Observable<Double> speedObservable = Observable.create(this::readSpeed);
    private AudioRecord recorder = null;
    private AudioTrack track = null;

    public static Doppler getInstance() {
        if(instance == null) instance = new Doppler();
        return instance;
    }

    private Doppler(){

    }

    public Observable<Double> getSpeedObservable(){
        return speedObservable;
    }

    /*
     * Initialize buffer to hold continuously recorded audio data, start recording, and start
     * playback.
     */
    public void beginCapture(){
        N = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
        recorder.startRecording();

    }

    private void readSpeed(Subscriber subscriber){
        short[][]   buffers  = new short[256][160];
        int ix = 0;

        try
        {
            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            DoubleFFT_1D doubleFFT = new DoubleFFT_1D(256);
            double movingAvg = 0;
            while(true)
            {
                double[] bufferD = new double[256];
                short[] buffer = buffers[ix++ % buffers.length];

                N = recorder.read(buffer,0,buffer.length);
                track.write(buffer, 0, buffer.length);
                for (int i = 0; i < buffer.length; ++i) {
                    bufferD[i] = buffer[i];
                }

                doubleFFT.realForward(bufferD);
                double maxMag = 0;
                double maxFreq = 0;
                double average = 0;
                for (int i = 0; i < bufferD.length/2; i++) {
                    double d = Math.sqrt(bufferD[i*2] * bufferD[i*2] + bufferD[i*2+1] * bufferD[i*2+1]);
                    double m = (i * 16000 / 256);
                    average += d;
                    if(d > maxMag){
                        maxMag = d;
                        maxFreq = m;
                    }
                }
                average = average/(bufferD.length/2);
                double speed = (maxFreq * 3*Math.pow(10,8))/(24*Math.pow(10,9) * 2);
                speed = speed/0.44704;
                if(movingAvg == 0) movingAvg = average;
                else{
                    if(maxMag > movingAvg + 160000 || true){
                        if(speed > 15 || true) subscriber.onNext(speed);
                        Timber.d("Freq: " + maxFreq + " Mag:" + maxMag + " Avg: " + movingAvg + " Speed: " + speed + "mph");
                    }
                    movingAvg = (average + movingAvg)/2;
                }
            }
        }
        catch(Throwable x)
        {
            x.printStackTrace();
        }
    }

    public void stopCapture(){
        //speedObservable = null;
        recorder.stop();
        recorder.release();
        track.stop();
        track.release();
        recorder = null;
        track = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
            Toast.makeText(context, "NOISE", Toast.LENGTH_SHORT);
        }
    }
}
