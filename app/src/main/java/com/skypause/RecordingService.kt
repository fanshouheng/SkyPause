package com.skypause

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Environment
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.view.Gravity
import android.graphics.PixelFormat

class RecordingService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1
        private var resultCode: Int = 0
        private var resultData: Intent? = null

        fun setMediaProjectionData(code: Int, data: Intent) {
            resultCode = code
            resultData = data
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null
    private var speedControlView: View? = null
    private var currentSpeed = 1.0f

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_RECORDING" -> {
                try {
                    startRecording()
                } catch (e: Exception) {
                    e.printStackTrace()
                    showNotification("录制失败", "请重试")
                }
            }
            "STOP_RECORDING" -> {
                try {
                    stopRecording()
                    showNotification("录制完成", "视频已保存")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun showSpeedControl() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        speedControlView = LayoutInflater.from(this).inflate(R.layout.speed_control, null)

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
        
        params.gravity = Gravity.TOP or Gravity.END
        params.y = 100

        speedControlView?.findViewById<SeekBar>(R.id.speedSeekBar)?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // 将进度转换为 0.05x 到 2.0x 的速度
                    currentSpeed = if (progress < 5) {
                        // 前5%的进度对应 0.05x 到 0.1x
                        0.05f + (progress / 5f) * 0.05f
                    } else {
                        // 剩余95%的进度对应 0.1x 到 2.0x
                        0.1f + ((progress - 5) / 95f) * 1.9f
                    }
                    
                    speedControlView?.findViewById<TextView>(R.id.speedText)?.text = 
                        String.format("%.2fx", currentSpeed)
                    
                    // 调整录制参数
                    adjustRecordingSpeed(currentSpeed)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        windowManager.addView(speedControlView, params)
    }

    private fun adjustRecordingSpeed(speed: Float) {
        currentSpeed = speed
        val newFrameRate = (60 * speed).toInt().coerceIn(1, 60)
        mediaRecorder?.apply {
            setVideoFrameRate(newFrameRate)
        }
    }

    private fun startRecording() {
        try {
            // 先启动前台服务
            val notification = NotificationCompat.Builder(this, "recording_channel")
                .setContentTitle("正在录制慢动作")
                .setContentText("录制中...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
            startForeground(NOTIFICATION_ID, notification)

            // 然后开始录制
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData!!)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "SKY_SLOW_$timestamp.mp4"
            outputFile = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), filename)

            mediaRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(20000000)  // 高码率
                setVideoFrameRate(60)  // 使用60fps
                setVideoSize(720, 1280)
                setOutputFile(outputFile?.absolutePath)
                prepare()
            }

            // 初始化时间戳调整器
            adjustRecordingSpeed(currentSpeed)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecording",
                720, 1280, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface, null, null
            )

            showSpeedControl() // 显示调速控制
            
            mediaRecorder?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            showNotification("录制失败", "错误: ${e.message}")
        }
    }

    private fun stopRecording() {
        try {
            // 移除调速控制
            speedControlView?.let {
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager.removeView(it)
                speedControlView = null
            }
            
            mediaRecorder?.apply {
                stop()
                release()
            }
            virtualDisplay?.release()
            mediaProjection?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
} 