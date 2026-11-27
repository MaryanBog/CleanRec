import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.io.File

class DesktopVideoPreviewer(filePath: String) {
    private val file = File(filePath)
    private val framesBySec = mutableMapOf<Int, ImageBitmap>()
    private var maxSec = 0

    suspend fun open() = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext
        framesBySec.clear()
        maxSec = 0
        val grabber = FFmpegFrameGrabber(file)
        try {
            grabber.start()
            val totalDurationMs = grabber.lengthInTime / 1000L
            val totalFrames = grabber.lengthInVideoFrames
            var frameIndex = 0
            var lastSec = -1

            while (true) {
                val frame = grabber.grabImage() ?: break
                val currentMs = if (totalFrames > 0 && totalDurationMs > 0) {
                    (frameIndex.toLong() * totalDurationMs) / totalFrames
                } else {
                    grabber.timestamp / 1000L
                }
                val sec = (currentMs / 1000L).toInt().coerceAtLeast(0)
                if (sec != lastSec) {
                    val uniqueConverter = Java2DFrameConverter()
                    uniqueConverter.convert(frame)?.toComposeImageBitmap()?.let {
                        framesBySec[sec] = it
                        if (sec > maxSec) maxSec = sec
                    }
                    lastSec = sec
                }
                frameIndex++
            }
        } catch (_: Exception) {
        } finally {
            try {
                grabber.stop()
                grabber.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun seekAndGrab(targetMs: Long): ImageBitmap? {
        if (framesBySec.isEmpty()) return null
        val sec = (targetMs / 1000L).toInt().coerceIn(0, maxSec)
        for (s in sec downTo 0) {
            framesBySec[s]?.let { return it }
        }
        return framesBySec.values.firstOrNull()
    }

    suspend fun grabNextFrame(): Pair<ImageBitmap?, Long> = null to -1L

    fun close() {
        framesBySec.clear()
    }
}
