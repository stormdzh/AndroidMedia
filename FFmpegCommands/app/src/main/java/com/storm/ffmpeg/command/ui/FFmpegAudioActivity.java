package com.storm.ffmpeg.command.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.storm.ffmpeg.command.R;
import com.storm.ffmpeg.command.ffmpeg.AudioCommands;
import com.storm.ffmpeg.command.ffmpeg.FFmpegRun;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FFmpegAudioActivity extends AppCompatActivity {

    private String dir = "/storage/emulated/0/affmpeg/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_audio);
        findViewById(R.id.btnCutAudio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cutAudio();
            }
        });
    }


    private void cutAudio() {
        List<String> wavList = new ArrayList<>();
        wavList.add(dir + "temp/temp/0_p_record.wav");
        wavList.add(dir + "temp/temp/1_p_record.wav");
        wavList.add(dir + "temp/temp/2_p_record.wav");
        wavList.add(dir + "temp/temp/3_p_record.wav");
        wavList.add(dir + "temp/temp/4_p_record.wav");

        File file=new File(wavList.get(0));
        Log.i("test","是否存在：" +file.exists());
        Log.i("test","path：" +wavList.get(0));
        concat(wavList);



//        String src = dir + "bgm.mp3";
//        String dest = dir + "bgm_short.mp3";
//        String[] commands = AudioCommands.cutAudio(src, dest, "00:00:00", 10);
//        FFmpegRun.execute(commands, new FFmpegRun.FFmpegRunListener() {
//            @Override
//            public void onStart() {
//                Log.i("test", "onStart");
//            }
//
//            @Override
//            public void onEnd(int result) {
//                Log.i("test", "onEnd");
//            }
//        });
    }

    private void concat(List<String> wavList) {
        Toast.makeText(this, "开始拼接", Toast.LENGTH_SHORT).show();
        String outmp3 = dir + "temp/result.wav";
//        String outmp3=dir+"temp/result.mp3";
        String[] commons = getConcat2(wavList, outmp3);
        FFmpegRun.execute(commons, new FFmpegRun.FFmpegRunListener() {
            @Override
            public void onStart() {
                Log.i("test", "开始拼接");
            }

            @Override
            public void onEnd(int result) {
                Log.i("test", "结束拼接");
            }
        });

    }

    public static String[] getConcat2(List<String> wavList, String outmp3url) {

//        ffmpeg  -i 1.wav -i 2.wav -i 3.wav -i 4.wav -i 5.wav -filter_complex '[0:0] [1:0]  concat=n=5:v=0:a=1[out]' -map '[out]' concat_1.wav

        ArrayList<String> _commands = new ArrayList<>();
        _commands.add("ffmpeg");
        _commands.add("-i");
        _commands.add(wavList.get(0));
        _commands.add("-i");
        _commands.add(wavList.get(1));
        _commands.add("-i");
        _commands.add(wavList.get(2));
        _commands.add("-i");
        _commands.add(wavList.get(3));
        _commands.add("-i");
        _commands.add(wavList.get(4));
        _commands.add("-filter_complex");
        _commands.add("[0:0] [1:0]  concat=n=5:v=0:a=1[out]");
        _commands.add("-map");
        _commands.add("[out]");

        _commands.add("-ar");
        _commands.add("4000");
        _commands.add("-ab");
        _commands.add("2.4k");
        _commands.add("-ac");
        _commands.add("1");

//        _commands.add("-b:a");
//        _commands.add("16k");

//        _commands.add("-f");

        _commands.add(outmp3url);

        String[] commands = new String[_commands.size()];
        for (int i = 0; i < _commands.size(); i++) {
            commands[i] = _commands.get(i);
        }
        return commands;
    }
}
