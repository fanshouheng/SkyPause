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
import android.widget.Toast
import android.util.Log

class PauseAccessibilityService : AccessibilityService() {
    private var isShowingDialog = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 移除切换逻辑
    }
    
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "PERFORM_GLOBAL_ACTION") {
            try {
                if (!isShowingDialog) {
                    isShowingDialog = true
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("开始录制")
                        .setMessage("将录制3秒视频，确定开始吗？")
                        .setPositiveButton("开始") { _, _ ->
                            startSlowMotionRecording()
                            isShowingDialog = false
                        }
                        .setNegativeButton("取消") { _, _ ->
                            isShowingDialog = false
                        }
                        .setOnCancelListener {
                            isShowingDialog = false
                        }
                        .create()
                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    dialog.show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isShowingDialog = false
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startSlowMotionRecording() {
        try {
            Log.d("SkyPause", "Starting screen capture activity")
            val intent = Intent(this, ScreenCaptureActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("SkyPause", "Failed to start recording", e)
            Toast.makeText(this, "启动录制失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowingDialog = false
    }
} 