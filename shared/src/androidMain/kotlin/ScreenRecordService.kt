package com.lockrion.cleanrec.common

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentVideoUri: Uri? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var delayedStartRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }
                val recordMic = intent.getBooleanExtra(EXTRA_RECORD_MIC, true)
                val quality = intent.getIntExtra(EXTRA_QUALITY, 1080)

                if (resultCode == Activity.RESULT_OK && data != null) {
                    startForegroundServiceNotification(recordMic, isWaiting = true)

                    delayedStartRunnable?.let { mainHandler.removeCallbacks(it) }
                    delayedStartRunnable = Runnable {
                        startForegroundServiceNotification(recordMic, isWaiting = false)
                        startRecording(resultCode, data, recordMic, quality)
                    }
                    mainHandler.postDelayed(delayedStartRunnable!!, 5_000L)
                }
            }
            ACTION_STOP -> {
                delayedStartRunnable?.let { mainHandler.removeCallbacks(it) }
                delayedStartRunnable = null
                stopRecording()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun loadNotifStrings(): Map<String, String> {
        val lang = Locale.getDefault().language.lowercase()
        val path = if (lang == "en") "values/strings.xml" else "values-$lang/strings.xml"
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: javaClass.classLoader?.getResourceAsStream("values/strings.xml")
            ?: return emptyMap()

        val xmlText = stream.bufferedReader().use { it.readText() }
        val regex = Regex("""<string name="([^"]+)">([^<]*)</string>""")
        return regex.findAll(xmlText).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun startForegroundServiceNotification(recordMic: Boolean, isWaiting: Boolean) {
        val channelId = "cleanrec_recording_channel_v2"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val strings = loadNotifStrings()

        val channelName = strings["notif_channel_name"] ?: "CleanRec Screen Recording"
        val title = if (isWaiting) {
            strings["notif_wait_title"] ?: "CleanRec: Starting in 5s..."
        } else {
            strings["notif_rec_title"] ?: "CleanRec: Recording screen"
        }
        val body = if (isWaiting) {
            strings["notif_wait_body"] ?: "Open your target app — recording will start automatically"
        } else {
            strings["notif_rec_body"] ?: "Tap Stop to finish and save the video"
        }
        val stopLabel = strings["notif_action_stop"] ?: "Stop"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(android.R.drawable.ic_media_pause, stopLabel, stopPendingIntent)
            .setOngoing(true)

        if (!isWaiting) {
            builder
                .setWhen(System.currentTimeMillis())
                .setUsesChronometer(true)
        }

        val notification: Notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (recordMic) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRecording(resultCode: Int, data: Intent, recordMic: Boolean, maxHeight: Int) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopRecording()
                stopSelf()
            }
        }, Handler(Looper.getMainLooper()))

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val aspectRatio = metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat()
        val targetHeight = minOf(metrics.heightPixels, maxHeight).let { it - (it % 2) }
        val targetWidth = (targetHeight * aspectRatio).toInt().let { it - (it % 2) }
        val density = metrics.densityDpi

        val fileName = "CleanRec_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/CleanRec")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        currentVideoUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        fileDescriptor = currentVideoUri?.let { contentResolver.openFileDescriptor(it, "w") }

        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            if (recordMic) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(fileDescriptor?.fileDescriptor)
            setVideoSize(targetWidth, targetHeight)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (recordMic) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
            }
            setVideoEncodingBitRate(6_000_000)
            setVideoFrameRate(30)
            prepare()
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "CleanRecDisplay",
            targetWidth,
            targetHeight,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )

        mediaRecorder?.start()
        _isRecording.value = true
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
        } finally {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        }

        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
        fileDescriptor?.close()
        fileDescriptor = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            currentVideoUri?.let { uri ->
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, values, null, null)
            }
        }
        _isRecording.value = false
    }
    override fun onDestroy() {
        delayedStartRunnable?.let { mainHandler.removeCallbacks(it) }
        super.onDestroy()
    }
    companion object {
        const val ACTION_START = "com.cleanrec.app.ACTION_START"
        const val ACTION_STOP = "com.cleanrec.app.ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val EXTRA_RECORD_MIC = "EXTRA_RECORD_MIC"
        const val EXTRA_QUALITY = "EXTRA_QUALITY"
        private const val NOTIFICATION_ID = 1001
        private val _isRecording = MutableStateFlow(false)
        val isRecording = _isRecording.asStateFlow()
    }
}