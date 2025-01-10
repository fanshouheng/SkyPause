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

class PauseAccessibilityService : AccessibilityService() {
    private var isGamePaused = false
    private var gamePackageName = "com.tgc.sky.android"
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
                if (!isShowingDialog) {  // 防止重复显示对话框
                    isShowingDialog = true
                    val options = arrayOf("截图", "录制慢动作", "取消")
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("选择操作")
                        .setItems(options) { _, which ->
                            when (which) {
                                0 -> takeScreenshot()
                                1 -> startSlowMotionRecording()
                                2 -> {
                                    // 什么都不做，直接关闭对话框
                                }
                            }
                            isShowingDialog = false
                        }
                        .setOnCancelListener {
                            isShowingDialog = false
                        }
                        .setOnDismissListener {
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

    private fun takeScreenshot() {
        // 使用 MediaProjection API 截图
        // ... 截图代码
    }

    private fun startSlowMotionRecording() {
        val intent = Intent(this, ScreenCaptureActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowingDialog = false
        isGamePaused = false
    }
} 