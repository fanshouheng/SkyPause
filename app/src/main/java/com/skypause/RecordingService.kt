package com.skypause

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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.app.NotificationChannel
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat

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
    
    private fun initMediaProjection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData!!)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_RECORDING" -> {
                try {
                    initMediaProjection()
                    startRecording()
                } catch (e: Exception) {
                    e.printStackTrace()
                    showNotification("录制失败", "请重试")
                }
            }
            "STOP_RECORDING" -> {
                try {
                    stopRecording()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        // 创建输出文件
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "SKY_$timestamp.mp4"
        outputFile = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), filename)

        mediaRecorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(10000000) // 10Mbps
            setVideoFrameRate(60) // 高帧率录制
            setVideoSize(1080, 1920) // 根据屏幕分辨率调整
            setOutputFile(outputFile?.absolutePath)
            prepare()
        }

        // 创建虚拟显示
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecording",
            1080, 1920, 1,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )

        mediaRecorder?.start()
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        virtualDisplay?.release()
        mediaProjection?.stop()
    }

    private fun slowDownVideo(inputFile: File) {
        val outputFile = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), 
            inputFile.nameWithoutExtension + "_slow.mp4")
        
        // 使用 MediaCodec 和 MediaMuxer 进行视频变速
        // 将视频帧间隔扩大到原来的2倍或4倍
        // 这里需要具体实现视频处理逻辑
        
        // 处理完成后通知用户
        val intent = Intent("VIDEO_PROCESSED")
        intent.putExtra("output_path", outputFile.absolutePath)
        sendBroadcast(intent)
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

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopRecording()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 