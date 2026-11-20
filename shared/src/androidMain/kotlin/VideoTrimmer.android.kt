import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AndroidVideoTrimmer {

    suspend fun trim(
        context: Context,
        video: RecordedVideo,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        if (endMs - startMs < 500L) return@withContext false

        val sourceUri = Uri.parse(video.id)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outName = "CleanRec_${timestamp}_trim.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, outName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/CleanRec")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val outUri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return@withContext false

        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            val inputPfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
                ?: return@withContext false
            val outputPfd = context.contentResolver.openFileDescriptor(outUri, "w")
                ?: run {
                    inputPfd.close()
                    return@withContext false
                }

            inputPfd.use { inFd ->
                outputPfd.use { outFd ->
                    extractor = MediaExtractor().apply {
                        setDataSource(inFd.fileDescriptor)
                    }
                    muxer = MediaMuxer(
                        outFd.fileDescriptor,
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                    )

                    val trackCount = extractor!!.trackCount
                    val trackMap = mutableMapOf<Int, Int>()
                    var maxBufferSize = 1024 * 1024

                    for (i in 0 until trackCount) {
                        val format = extractor!!.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                        if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                            extractor!!.selectTrack(i)
                            val dstIndex = muxer!!.addTrack(format)
                            trackMap[i] = dstIndex
                            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                                val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                                if (size > maxBufferSize) maxBufferSize = size
                            }
                        }
                    }

                    if (trackMap.isEmpty()) return@withContext false

                    val startUs = startMs * 1000L
                    val endUs = endMs * 1000L

                    extractor!!.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    val actualStartUs = extractor!!.sampleTime.coerceAtLeast(0L)

                    muxer!!.start()

                    val buffer = ByteBuffer.allocate(maxBufferSize)
                    val bufferInfo = MediaCodec.BufferInfo()

                    while (true) {
                        val sampleSize = extractor!!.readSampleData(buffer, 0)
                        if (sampleSize < 0) break

                        val sampleTime = extractor!!.sampleTime
                        if (sampleTime > endUs) break

                        if (sampleTime >= actualStartUs) {
                            val srcTrack = extractor!!.sampleTrackIndex
                            val dstTrack = trackMap[srcTrack]
                            if (dstTrack != null) {
                                bufferInfo.offset = 0
                                bufferInfo.size = sampleSize
                                bufferInfo.presentationTimeUs = sampleTime - actualStartUs
                                bufferInfo.flags = extractor!!.sampleFlags
                                muxer!!.writeSampleData(dstTrack, buffer, bufferInfo)
                            }
                        }
                        extractor!!.advance()
                    }
                    muxer!!.stop()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(outUri, doneValues, null, null)
            }
            true
        } catch (e: Exception) {
            context.contentResolver.delete(outUri, null, null)
            false
        } finally {
            try {
                muxer?.release()
            } catch (e: Exception) {
            }
            try {
                extractor?.release()
            } catch (e: Exception) {
            }
        }
    }
}
