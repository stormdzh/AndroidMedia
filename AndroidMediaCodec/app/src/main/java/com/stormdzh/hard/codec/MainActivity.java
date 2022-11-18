package com.stormdzh.hard.codec;

import android.Manifest;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.stormdzh.hard.codec.util.audio.AudioEncoder;
import com.stormdzh.hard.codec.util.audio.AudioRecordUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private AudioEncoder mAudioEncoder;
    private AudioRecordUtils au;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }

        findViewById(R.id.btnStart).setOnClickListener(this);
        findViewById(R.id.btnEnd).setOnClickListener(this);
        findViewById(R.id.btnPlay).setOnClickListener(this);
        findViewById(R.id.btnPlayPcm).setOnClickListener(this);
        findViewById(R.id.btnPlayWav).setOnClickListener(this);


        initRecord();

//        au=new AudioRecordUtils();
//        au.init();
    }

    private void initRecord() {
        mAudioEncoder = new AudioEncoder();
        //如果使用传统的方式，先存为pcm则使用 /storage/emulated/0/ffmpeg/test_audio_origin.pcm
        String srcPath = "/storage/emulated/0/ffmpeg/test_audio_origin.pcm";
        File file = new File(srcPath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mAudioEncoder.setSavePath(file.getAbsolutePath());
        try {
            mAudioEncoder.prepare();
            Log.i("test", "mAudioEncoder.prepare 成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("test", "mAudioEncoder.prepare 失败");
        }

    }


    /**
     * 播放pcm
     *
     * @param file
     */
    private void playPcm(File file) {
        DataInputStream dis = null;
//        File file = new File(parent, "audio.pcm");
        int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int sampleRate = 44100;
        int audioSource = MediaRecorder.AudioSource.MIC;

        try {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            AudioTrack audioTrack = new AudioTrack(audioSource, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
            byte[] datas = new byte[bufferSize];
            audioTrack.play();
            while (true) {
                int i = 0;
                try {
                    while (dis.available() > 0 && i < datas.length) {
                        datas[i] = dis.readByte();
                        i++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                audioTrack.write(datas, 0, datas.length);
                //表示读取完了
                if (i != bufferSize) {
                    audioTrack.stop();
                    audioTrack.release();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void playWav(File fileWav) {

        int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int sampleRate = 44100;
        int audioSource = MediaRecorder.AudioSource.MIC;

        DataInputStream dis = null;
        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        AudioTrack mAudioTrack = new AudioTrack(audioSource, sampleRate, channelConfig, audioFormat, bufferSizeInBytes, AudioTrack.MODE_STREAM);
//        File fileWav = new File(parent, "audio.wav");
//        File fileWav = new File(parent, "audio.wav");
        try {
            dis = new DataInputStream(new FileInputStream(fileWav));
            readWavHeader(dis);
            new Thread(ReadDataRunnable).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readWavHeader(DataInputStream dis) {
        try {
            byte[] byteIntValue = new byte[4];
            byte[] byteShortValue = new byte[2];
            //读取四个
            String mChunkID = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte();
            Log.e("Wav_Header", "mChunkID:" + mChunkID);
            dis.read(byteIntValue);
            int chunkSize = byteArrayToInt(byteIntValue);
            Log.e("Wav_Header", "chunkSize:" + chunkSize);
            String format = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte();
            Log.e("Wav_Header", "format:" + format);
            String subchunk1ID = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte();
            Log.e("Wav_Header", "subchunk1ID:" + subchunk1ID);
            dis.read(byteIntValue);
            int subchunk1Size = byteArrayToInt(byteIntValue);
            Log.e("Wav_Header", "subchunk1Size:" + subchunk1Size);
            dis.read(byteShortValue);
            short audioFormat = byteArrayToShort(byteShortValue);
            Log.e("Wav_Header", "audioFormat:" + audioFormat);
            dis.read(byteShortValue);
            short numChannels = byteArrayToShort(byteShortValue);
            Log.e("Wav_Header", "numChannels:" + numChannels);
            dis.read(byteIntValue);
            int sampleRate = byteArrayToInt(byteIntValue);
            Log.e("Wav_Header", "sampleRate:" + sampleRate);
            dis.read(byteIntValue);
            int byteRate = byteArrayToInt(byteIntValue);
            Log.e("Wav_Header", "byteRate:" + byteRate);
            dis.read(byteShortValue);
            short blockAlign = byteArrayToShort(byteShortValue);
            Log.e("Wav_Header", "blockAlign:" + blockAlign);
            dis.read(byteShortValue);
            short btsPerSample = byteArrayToShort(byteShortValue);
            Log.e("Wav_Header", "btsPerSample:" + btsPerSample);
            String subchunk2ID = "" + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte() + (char) dis.readByte();
            Log.e("Wav_Header", "subchunk2ID:" + subchunk2ID);
            dis.read(byteIntValue);
            int subchunk2Size = byteArrayToInt(byteIntValue);
            Log.e("subchunk2Size", "subchunk2Size:" + subchunk2Size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int byteArrayToInt(byte[] byteIntValue) {

        return ByteBuffer.wrap(byteIntValue).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private short byteArrayToShort(byte[] byteShortValue) {

        return ByteBuffer.wrap(byteShortValue).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }


    private Runnable ReadDataRunnable = new Runnable() {
        @Override
        public void run() {
//            byte[] buffer = new byte[1024 * 2];
//            while (readData(buffer, 0, buffer.length) > 0) {
//                if (mAudioTrack.write(buffer, 0, buffer.length) != buffer.length) {
//                }
//
//                mAudioTrack.play();
//            }
//            mAudioTrack.stop();
//            mAudioTrack.release();
//            try {
//                if (dis != null) {
//                    dis.close();
//                    dis = null;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    };

    public int readData(byte[] buffer, int offset, int count) {
//        try {
//            int nbytes = dis.read(buffer, offset, count);
//            if (nbytes == -1) {
//                return 0;
//            }
//            return nbytes;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return -1;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                try {
                    mAudioEncoder.start();
                    Log.i("test", "mAudioEncoder.start 成功");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.i("test", "mAudioEncoder.start 失败");
                }
//                au.start();
//                Log.i("test", "start 成功");
                break;
            case R.id.btnEnd:
                mAudioEncoder.stop();
//                au.stop();
//                Log.i("test", "stop 成功");
//
////                PcmToWavUtil pcmToWavUtil=new PcmToWavUtil(44100,1,AudioFormat.ENCODING_PCM_16BIT);
////                pcmToWavUtil.pcmToWav("/storage/emulated/0/ffmpeg/test_audio_origin_a.pcm"
////                        ,"/storage/emulated/0/ffmpeg/test_audio_origin_b.wav");
//                au.copyWaveFile("/storage/emulated/0/ffmpeg/test_audio_origin_a.pcm"
//                        ,"/storage/emulated/0/ffmpeg/test_audio_origin_b.wav");
                break;

            case R.id.btnPlay:
                startActivity(new Intent(this, DecodeActivity.class));
                break;
            case R.id.btnPlayPcm:
                File file = new File("/storage/emulated/0/ffmpeg/test_audio_origin.pcm");
                playPcm(file);
                break;
            case R.id.btnPlayWav:
                //改方法没有完全实现，需要报关联的方法里面的注释去掉，再修改代码
                File filev = new File("/storage/emulated/0/ffmpeg/test_audio_origin.pcm");
                playWav(filev);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}
