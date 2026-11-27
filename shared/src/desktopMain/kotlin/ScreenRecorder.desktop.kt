import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import java.awt.Toolkit
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread

@Composable
actual fun rememberScreenRecorder(): ScreenRecorderController {
    return remember {
        object : ScreenRecorderController {
            private val _isRecording = MutableStateFlow(false)
            override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
            private var videoThread: Thread? = null
            private var audioThread: Thread? = null

            override fun startRecording(config: RecordingConfig) {
                if (_isRecording.value) return
                _isRecording.value = true

                val outputDir = File(System.getProperty("user.home"), "Videos/CleanRec").apply { mkdirs() }
                val outputFile = File(outputDir, "CleanRec_${System.currentTimeMillis()}.mp4")

                val screenSize = Toolkit.getDefaultToolkit().screenSize
                val aspect = screenSize.width.toDouble() / screenSize.height.toDouble()
                val targetHeight = if (config.qualityHeight > 0) config.qualityHeight else screenSize.height
                val rawWidth = (targetHeight * aspect).toInt()
                val targetWidth = if (rawWidth % 2 == 0) rawWidth else rawWidth + 1
                val evenHeight = if (targetHeight % 2 == 0) targetHeight else targetHeight + 1

                val audioFormat = AudioFormat(44100.0f, 16, 1, true, false)
                val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
                val hasMic = config.recordMic && AudioSystem.isLineSupported(info)

                val grabber = FFmpegFrameGrabber("desktop").apply {
                    format = "gdigrab"
                    frameRate = 30.0
                    setOption("framerate", "30")
                    setOption("draw_mouse", "1")
                }
                grabber.start()

                val recorder = FFmpegFrameRecorder(outputFile.absolutePath, targetWidth, evenHeight, if (hasMic) 1 else 0).apply {
                    videoCodec = avcodec.AV_CODEC_ID_H264
                    format = "mp4"
                    pixelFormat = avutil.AV_PIX_FMT_YUV420P
                    frameRate = 30.0
                    gopSize = 30
                    videoBitrate = if (evenHeight >= 1080) 6_000_000 else 3_000_000
                    setVideoOption("preset", "ultrafast")
                    setVideoOption("tune", "zerolatency")
                    if (hasMic) {
                        audioCodec = avcodec.AV_CODEC_ID_AAC
                        sampleRate = 44100
                        audioBitrate = 128_000
                    }
                }
                recorder.start()
                val recorderLock = Any()

                if (hasMic) {
                    audioThread = thread(isDaemon = true) {
                        val line = AudioSystem.getLine(info) as TargetDataLine
                        line.open(audioFormat)
                        line.start()
                        val buffer = ByteArray(4096)
                        try {
                            while (_isRecording.value) {
                                val bytesRead = line.read(buffer, 0, buffer.size)
                                if (bytesRead > 0) {
                                    val shorts = ShortArray(bytesRead / 2)
                                    ByteBuffer.wrap(buffer, 0, bytesRead)
                                        .order(ByteOrder.LITTLE_ENDIAN)
                                        .asShortBuffer()
                                        .get(shorts)
                                    synchronized(recorderLock) {
                                        if (_isRecording.value) {
                                            recorder.recordSamples(44100, 1, ShortBuffer.wrap(shorts))
                                        }
                                    }
                                }
                            }
                        } finally {
                            line.stop()
                            line.close()
                        }
                    }
                }

                videoThread = thread(isDaemon = true) {
                    try {
                        while (_isRecording.value) {
                            val frame = grabber.grabImage() ?: continue
                            synchronized(recorderLock) {
                                if (_isRecording.value) {
                                    recorder.record(frame)
                                }
                            }
                        }
                    } finally {
                        audioThread?.join(1000)
                        synchronized(recorderLock) {
                            recorder.stop()
                            recorder.release()
                        }
                        grabber.stop()
                        grabber.release()
                    }
                }
            }

            override fun stopRecording() {
                if (!_isRecording.value) return
                _isRecording.value = false
                videoThread?.join(3000)
                videoThread = null
                audioThread = null
            }
        }
    }
}
