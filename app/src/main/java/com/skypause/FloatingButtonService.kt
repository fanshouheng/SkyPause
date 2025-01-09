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
import android.widget.SeekBar
import android.widget.TextView

class FloatingButtonService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: View
    private lateinit var speedControl: View
    private var isSpeedControlVisible = false
    private var currentSpeed = 1.0f
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 创建悬浮按钮
        floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
        
        // 创建速度控制视图
        speedControl = LayoutInflater.from(this).inflate(R.layout.speed_control, null)
        
        // 设置速度控制滑块监听
        speedControl.findViewById<SeekBar>(R.id.speedSeekBar).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    // 将 0-100 映射到 0.1-2.0
                    currentSpeed = 0.1f + (progress / 100f) * 1.9f
                    speedControl.findViewById<TextView>(R.id.speedText).text = 
                        String.format("%.1fx", currentSpeed)
                    
                    // 发送速度更改广播
                    sendBroadcast(Intent("SPEED_CHANGED").apply {
                        putExtra("speed", currentSpeed)
                    })
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )

        // 设置按钮点击事件
        floatingButton.findViewById<ImageButton>(R.id.floatingBtn).setOnClickListener {
            if (!isSpeedControlVisible) {
                // 显示速度控制
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
                params.gravity = Gravity.CENTER
                windowManager.addView(speedControl, params)
                isSpeedControlVisible = true
            } else {
                // 隐藏速度控制
                windowManager.removeView(speedControl)
                isSpeedControlVisible = false
            }
        }

        // 添加悬浮按钮到窗口
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
        if (isSpeedControlVisible) {
            windowManager.removeView(speedControl)
        }
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