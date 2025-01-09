package com.skypause

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class ScreenCaptureActivity : Activity() {
    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                // 保存录屏权限
                RecordingService.setMediaProjectionData(resultCode, data)
                
                // 开始录制
                val intent = Intent(this, RecordingService::class.java)
                intent.action = "START_RECORDING"
                startService(intent)
                
                // 3秒后停止录制
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val stopIntent = Intent(this, RecordingService::class.java)
                    stopIntent.action = "STOP_RECORDING"
                    startService(stopIntent)
                }, 3000)
            }
        }
        finish()
    }
} 