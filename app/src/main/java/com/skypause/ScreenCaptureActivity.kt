package com.skypause

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import androidx.core.app.NotificationCompat

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
            if (resultCode == Activity.RESULT_OK && data != null) {
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
        finish()  // 确保Activity关闭
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "recording_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recording Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(1, notification)
    }
} 