package com.stormdzh.lamedemo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import com.stormdzh.lame4android.R

/**
 * 选择 边录音边转码 还是 录完之后再转码
 */
class SelectActivity : AppCompatActivity() {

    private lateinit var btnRecordingToMp3: Button
    private lateinit var btnRecordedToMp3: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        btnRecordingToMp3 = findViewById(R.id.btnRecordingToMp3)
        btnRecordedToMp3 = findViewById(R.id.btnRecordedToMp3)

        //kt点击事件
        btnRecordedToMp3.setOnClickListener {
            //先录音后转码
            startActivity(Intent(this, LameActivity::class.java))
        }

        btnRecordingToMp3.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

}