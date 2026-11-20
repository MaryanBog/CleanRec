import android.content.ClipData
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberVideoRepository(): VideoRepository {
    val context = LocalContext.current

    return remember(context) {
        object : VideoRepository {

            override suspend fun getRecordedVideos(): List<RecordedVideo> = withContext(Dispatchers.IO) {
                val result = mutableListOf<RecordedVideo>()
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DATE_ADDED
                )

                val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
                } else {
                    "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
                }
                val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf("%CleanRec%")
                } else {
                    arrayOf("CleanRec_%")
                }

                val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        result.add(
                            RecordedVideo(
                                id = contentUri.toString(),
                                name = cursor.getString(nameCol) ?: "CleanRec.mp4",
                                durationMs = cursor.getLong(durCol),
                                sizeBytes = cursor.getLong(sizeCol),
                                dateAddedSec = cursor.getLong(dateCol)
                            )
                        )
                    }
                }
                result
            }

            override fun playVideo(video: RecordedVideo) {
                val uri = Uri.parse(video.id)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, video.name).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
            }

            override fun shareVideo(video: RecordedVideo) {
                val uri = Uri.parse(video.id)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri(video.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, video.name).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
            }

            override suspend fun deleteVideo(video: RecordedVideo): Boolean = withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(video.id)
                    context.contentResolver.delete(uri, null, null) > 0
                } catch (e: Exception) {
                    false
                }
            }

            override suspend fun trimVideo(
                video: RecordedVideo,
                startMs: Long,
                endMs: Long
            ): Boolean = AndroidVideoTrimmer.trim(context, video, startMs, endMs)
        }
    }
}
