import androidx.compose.runtime.Composable

data class RecordedVideo(
    val id: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long
)

interface VideoRepository {
    suspend fun getRecordedVideos(): List<RecordedVideo>
    fun playVideo(video: RecordedVideo)
    fun shareVideo(video: RecordedVideo)
    suspend fun deleteVideo(video: RecordedVideo): Boolean
    suspend fun trimVideo(video: RecordedVideo, startMs: Long, endMs: Long): Boolean
}

@Composable
expect fun rememberVideoRepository(): VideoRepository
