import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import java.awt.Desktop
import java.io.File

@Composable
actual fun rememberVideoRepository(): VideoRepository {
    return remember {
        object : VideoRepository {
            private val videosDir = File(System.getProperty("user.home"), "Videos/CleanRec").apply { mkdirs() }

            override suspend fun getRecordedVideos(): List<RecordedVideo> = withContext(Dispatchers.IO) {
                val files = videosDir.listFiles { file ->
                    file.isFile && file.extension.equals("mp4", ignoreCase = true)
                } ?: return@withContext emptyList()

                files.map { file ->
                    val durationMs = getVideoDurationMs(file)
                    RecordedVideo(
                        id = file.absolutePath,
                        name = file.name,
                        durationMs = durationMs,
                        sizeBytes = file.length(),
                        dateAddedSec = file.lastModified() / 1000L
                    )
                }.sortedByDescending { it.dateAddedSec }
            }

            private fun getVideoDurationMs(file: File): Long {
                return try {
                    val grabber = FFmpegFrameGrabber(file)
                    grabber.start()
                    val durationUs = grabber.lengthInTime
                    grabber.stop()
                    grabber.release()
                    if (durationUs > 0) durationUs / 1000L else 0L
                } catch (_: Exception) {
                    0L
                }
            }

            override fun playVideo(video: RecordedVideo) {
                val file = File(video.id)
                if (file.exists() && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file)
                }
            }

            override fun shareVideo(video: RecordedVideo) {
                val file = File(video.id)
                if (file.exists() && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file.parentFile)
                }
            }

            override suspend fun deleteVideo(video: RecordedVideo): Boolean = withContext(Dispatchers.IO) {
                val file = File(video.id)
                if (file.exists()) file.delete() else false
            }

            override suspend fun trimVideo(video: RecordedVideo, startMs: Long, endMs: Long): Boolean = withContext(Dispatchers.IO) {
                val inputFile = File(video.id)
                if (!inputFile.exists() || endMs <= startMs) return@withContext false

                val outputFile = File(videosDir, "${inputFile.nameWithoutExtension}_trimmed_${System.currentTimeMillis()}.mp4")
                val grabber = FFmpegFrameGrabber(inputFile)
                var recorder: FFmpegFrameRecorder? = null

                try {
                    grabber.start()
                    val audioChannels = if (grabber.audioChannels > 0) grabber.audioChannels else 0
                    recorder = FFmpegFrameRecorder(outputFile.absolutePath, grabber.imageWidth, grabber.imageHeight, audioChannels).apply {
                        videoCodec = avcodec.AV_CODEC_ID_H264
                        format = "mp4"
                        frameRate = if (grabber.frameRate > 0) grabber.frameRate else 30.0
                        videoBitrate = if (grabber.videoBitrate > 0) grabber.videoBitrate else 4_000_000
                        setVideoOption("preset", "ultrafast")
                        if (audioChannels > 0) {
                            audioCodec = avcodec.AV_CODEC_ID_AAC
                            sampleRate = if (grabber.sampleRate > 0) grabber.sampleRate else 44100
                            audioBitrate = 128_000
                        }
                    }
                    recorder.start()

                    val startUs = startMs * 1000L
                    val endUs = endMs * 1000L
                    grabber.setTimestamp(startUs)

                    while (true) {
                        val frame = grabber.grab() ?: break
                        if (grabber.timestamp > endUs) break
                        recorder.record(frame)
                    }
                    true
                } catch (_: Exception) {
                    outputFile.delete()
                    false
                } finally {
                    try {
                        recorder?.stop()
                        recorder?.release()
                    } catch (_: Exception) {}
                    try {
                        grabber.stop()
                        grabber.release()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
