package com.skypause

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ScreenCaptureActivity : Activity() {
    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SkyPause", "ScreenCaptureActivity created")
        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        try {
            Log.d("SkyPause", "Requesting screen capture permission")
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        } catch (e: Exception) {
            Log.e("SkyPause", "Failed to request screen capture", e)
            Toast.makeText(this, "请求录屏权限失败: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("SkyPause", "Got activity result: requestCode=$requestCode, resultCode=$resultCode")
        
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d("SkyPause", "Screen capture permission granted")
                RecordingService.setMediaProjectionData(resultCode, data)
                
                val intent = Intent(this, RecordingService::class.java).apply {
                    action = "START_RECORDING"
                }
                Log.d("SkyPause", "Starting recording service")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                Log.e("SkyPause", "Screen capture permission denied")
                Toast.makeText(this, "未获得录屏权限", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
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