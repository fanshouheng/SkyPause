package com.skypause

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.os.Build
import android.app.ActivityManager
import android.content.Context
import android.provider.Settings

class FloatingButtonService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: View
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var lastClickTime: Long = 0
    private val DOUBLE_CLICK_TIME_DELTA: Long = 300 // 双击间隔时间（毫秒）

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 创建悬浮按钮
        floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button, null)

        // 设置悬浮窗参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        // 设置按钮点击和拖动事件
        floatingButton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingButton, params)
                    true
                }
                else -> false
            }
        }

        // 设置按钮点击事件
        floatingButton.findViewById<ImageButton>(R.id.floatingBtn).setOnClickListener {
            // 检查辅助功能服务是否已启用
            if (!isAccessibilityServiceEnabled()) {
                // 如果未启用，打开辅助功能设置
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } else {
                // 如果已启用，执行暂停操作
                val intent = Intent(this, PauseAccessibilityService::class.java)
                intent.action = "PERFORM_GLOBAL_ACTION"
                startService(intent)
            }
        }

        // 添加悬浮按钮到窗口
        windowManager.addView(floatingButton, params)
    }

    private fun pauseGame() {
        // 使用 Accessibility Service 来模拟按下最近任务键
        // 这样可以让游戏暂停但不会返回桌面
        try {
            val cmd = "input keyevent KEYCODE_APP_SWITCH"
            Runtime.getRuntime().exec(cmd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingButton)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            services?.let {
                return it.contains("${packageName}/${PauseAccessibilityService::class.java.name}")
            }
        }
        return false
    }
} 