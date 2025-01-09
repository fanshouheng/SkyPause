package com.skypause

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.app.AlertDialog
import android.view.WindowManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

class PauseAccessibilityService : AccessibilityService() {
    private var isGamePaused = false
    private var gamePackageName = "com.tgc.sky.android"
    private var mediaProjectionManager: MediaProjectionManager? = null
    private val REQUEST_MEDIA_PROJECTION = 1

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val currentPackage = event.packageName?.toString()
            if (currentPackage != gamePackageName && isGamePaused) {
                // 如果在暂停状态，保持在最近任务视图
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
        }
    }
    
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
        
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "PERFORM_GLOBAL_ACTION") {
            try {
                isGamePaused = !isGamePaused
                if (isGamePaused) {
                    performGlobalAction(GLOBAL_ACTION_RECENTS)
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        val options = arrayOf("截图", "录制慢动作", "取消")
                        val dialog = AlertDialog.Builder(this)
                            .setTitle("选择操作")
                            .setItems(options) { _, which ->
                                when (which) {
                                    0 -> takeScreenshot()
                                    1 -> startSlowMotionRecording()
                                    2 -> {
                                        isGamePaused = false
                                        performGlobalAction(GLOBAL_ACTION_BACK)
                                    }
                                }
                            }
                            .create()
                        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                        dialog.show()
                    }, 100)
                } else {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun takeScreenshot() {
        // 使用 MediaProjection API 截图
        // ... 截图代码
    }

    private fun startSlowMotionRecording() {
        val intent = Intent(this, ScreenCaptureActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK && data != null) {
            // 保存录屏权限
            RecordingService.setMediaProjectionData(resultCode, data)
            
            // 开始录制
            val intent = Intent(this, RecordingService::class.java)
            intent.action = "START_RECORDING"
            startService(intent)
            
            // 3秒后停止录制
            Handler(Looper.getMainLooper()).postDelayed({
                val stopIntent = Intent(this, RecordingService::class.java)
                stopIntent.action = "STOP_RECORDING"
                startService(stopIntent)
                
                showNotification("录制完成", "视频已保存，可以在相册中查看并以慢动作播放")
            }, 3000)
        }
    }

    private fun showNotification(title: String, message: String) {
        // 实现通知功能
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