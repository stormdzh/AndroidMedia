package com.stormdzh.hard.codec.util.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioEncoder2 implements Runnable{
    private String mime = "audio/mp4a-latm";
    private MediaCodec mEnc;
    private int rate=256000;
//    private int rate=44100;

    private AudioRecord mRecorder;
    //录音设置
    private int sampleRate=44100;   //采样率，默认44.1k
    private int channelCount=1;     //音频采样通道，默认2通道
//    private int channelConfig=AudioFormat.CHANNEL_IN_STEREO;        //通道设置，默认立体声
    private int audioFormat=AudioFormat.ENCODING_PCM_16BIT;     //设置采样数据格式，默认16比特PCM
    private FileOutputStream fos;

    private byte[] buffer;
    private boolean isRecording;
    private Thread mThread;
    private int bufferSize;

    private String mSavePath;

    public AudioEncoder2(){

    }

    public void setMime(String mime){
        this.mime=mime;
    }

    public void setRate(int rate){
        this.rate=rate;
    }

    public void setSampleRate(int sampleRate){
        this.sampleRate=sampleRate;
    }

    public void setSavePath(String path){
        this.mSavePath=path;
    }

    public void prepare() throws IOException {
        fos=new FileOutputStream(mSavePath);
        //音频编码相关
        MediaFormat format=MediaFormat.createAudioFormat(mime,sampleRate,channelCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//        format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
        format.setInteger(MediaFormat.KEY_BIT_RATE, rate);
        mEnc=MediaCodec.createEncoderByType(mime);
        mEnc.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

        //音频录制相关
//        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)*2;
//        buffer=new byte[bufferSize];
//        mRecorder=new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,
//                audioFormat,bufferSize);
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelCount, audioFormat);
        buffer=new byte[bufferSize];
        mRecorder=new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelCount,
                audioFormat,bufferSize);
    }

    public void start() throws InterruptedException {
        mEnc.start();
        mRecorder.startRecording();
        if(mThread!=null&&mThread.isAlive()){
            isRecording=false;
            mThread.join();
        }
        isRecording=true;
        mThread=new Thread(this);
        mThread.start();
    }

    private ByteBuffer getInputBuffer(int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEnc.getInputBuffer(index);
        }else{
            return mEnc.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEnc.getOutputBuffer(index);
        }else{
            return mEnc.getOutputBuffers()[index];
        }
    }

    //TODO Add End Flag
    private void readOutputData() throws IOException{
        int index=mEnc.dequeueInputBuffer(-1);
        if(index>=0){
            final ByteBuffer buffer=getInputBuffer(index);
            buffer.clear();
            int length=mRecorder.read(buffer,bufferSize);
            Log.e("test","length-->"+length);
            if(length>0){
                mEnc.queueInputBuffer(index,0,length,System.nanoTime()/1000,0);
            }else{
                Log.e("test","length-->"+length);
            }
        }
        MediaCodec.BufferInfo mInfo=new MediaCodec.BufferInfo();
        int outIndex;
        do{
            outIndex=mEnc.dequeueOutputBuffer(mInfo,0);
            Log.e("test","audio flag---->"+mInfo.flags+"/"+outIndex);
            if(outIndex>=0){
                ByteBuffer buffer=getOutputBuffer(outIndex);
                buffer.position(mInfo.offset);
                byte[] temp=new byte[mInfo.size+7];
                buffer.get(temp,7,mInfo.size);
                addADTStoPacket(temp,temp.length);
                fos.write(temp);
                mEnc.releaseOutputBuffer(outIndex,false);
            }else if(outIndex ==MediaCodec.INFO_TRY_AGAIN_LATER){

            }else if(outIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){

            }
        }while (outIndex>=0);
    }

    /**
     * 给编码出的aac裸流添加adts头字段
     * @param packet 要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        packet[0] = (byte)0xFF;
        packet[1] = (byte)0xF9;
        packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
        packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
        packet[4] = (byte)((packetLen&0x7FF) >> 3);
        packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
        packet[6] = (byte)0xFC;
    }

    /**
     * 停止录制
     */
    public void stop(){
        try {
            isRecording=false;
            mThread.join();
            mRecorder.stop();
            mEnc.stop();
            mEnc.release();
            fos.flush();
            fos.close();

//             pcm需要添加头信息才可以播放
//            copyWaveFile("/storage/emulated/0/ffmpeg/test_audio_origin.pcm"
//                    ,"/storage/emulated/0/ffmpeg/test_audio_origin_b.wav");

            Log.i("test", "mAudioEncoder.stop 成功");
        } catch (Exception e){
            Log.i("test", "mAudioEncoder.stop 失败");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (isRecording){
            try {
                readOutputData();
//                Log.i("test", "while write bufferSize:"+bufferSize);
                //普通的处理方法，是把录制的文件信息写到文件里面
//                int length=mRecorder.read(buffer,0,bufferSize);
//                fos.write(buffer,0,length);
//                Log.i("test", "while write length:"+length+"  bufferSize:"+bufferSize);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("test", "while write 异常:");
            }
        }
    }


    // 这里得到可播放的音频文件
    public void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRate;
//        int channels = 2;
        int channels = channelCount;
        long byteRate = 16 * sampleRate * channels / 8;
        byte[] data = new byte[bufferSize];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加入wav文件头
     */
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        // RIFF/WAVE header
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        //WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // 'fmt ' chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // 4 bytes: size of 'fmt ' chunk
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // format = 1
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // block align
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        // bits per sample
        header[34] = 16;
        header[35] = 0;
        //data
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

}
