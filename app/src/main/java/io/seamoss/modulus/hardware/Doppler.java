package io.seamoss.modulus.hardware;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.fft.DoubleFFT_2D;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by Alexander Melton on 4/1/2017.
 */

public class Doppler {
    private static Doppler instance;
    private MediaRecorder mediaRecorder;
    private String file;
    private Audio audio;

    public static Doppler getInstance() {
        if(instance == null) instance = new Doppler();
        return instance;
    }

    private Doppler(){

    }

    public Observable<Double> getSpeedObservable(){
        return audio.getSubject().asObservable();
    }

    public void setFile(String s){
        file = s;
    }

    public void beginCapture(){
        audio = new Audio();
        audio.start();
        /*
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        //mediaRecorder.setOutputFile(file);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try{
            mediaRecorder.prepare();
        }catch (IOException e){
            e.printStackTrace();
        }
        mediaRecorder.start();
        */
    }

    public void stopCapture(){
        audio.close();
        audio = null;
        /*
        mediaRecorder.start();
        mediaRecorder.release();
        mediaRecorder = null;
        */
    }

    /*
 * Thread to manage live recording/playback of voice input from the device's microphone.
 */
    private class Audio extends Thread
    {
        private boolean stopped = false;
        private PublishSubject<Double> speedSubject = PublishSubject.create();

        /**
         * Give the thread high priority so that it's not canceled unexpectedly, and start it
         */
        private Audio()
        {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        }

        @Override
        public void run()
        {
            Log.i("Audio", "Running Audio Thread");
            AudioRecord recorder = null;
            AudioTrack track = null;
            short[][]   buffers  = new short[256][160];
            int ix = 0;

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
            try
            {
                int N = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10);
                track = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
                recorder.startRecording();

            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
                DoubleFFT_1D doubleFFT = new DoubleFFT_1D(256);
                double movingAvg = 0;
                while(!stopped)
                {
                    double[] bufferD = new double[256];
                    double[] bufferFFT = new double[512];
                    short[] buffer = buffers[ix++ % buffers.length];

                    N = recorder.read(buffer,0,buffer.length);
                    track.write(buffer, 0, buffer.length);
                    String s = "";
                    String mag = "";
                    for (int i = 0; i < buffer.length; ++i) {
                        bufferD[i] = buffer[i];
                        //s = s + "," + buffer[i];
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
                        if(maxMag > movingAvg + 700000){
                            speedSubject.onNext(speed);
                            //Timber.d("Freq: " + maxFreq + " Mag:" + maxMag + " Avg: " + movingAvg + " Speed: " + speed + "mph");
                        }
                        movingAvg = (average + movingAvg)/2;
                    }
                }
            }
            catch(Throwable x)
            {
                Log.w("Audio", "Error reading voice audio", x);
            }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
            finally
            {
                recorder.stop();
                recorder.release();
                track.stop();
                track.release();
            }
        }

        public PublishSubject<Double> getSubject(){
            return speedSubject;
        }

        /**
         * Called from outside of the thread in order to stop the recording/playback loop
         */
        private void close()
        {
            stopped = true;
        }

    }
}
