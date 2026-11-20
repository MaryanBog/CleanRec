import androidx.compose.runtime.Composable

@Composable
actual fun rememberVideoRepository(): VideoRepository {
    return object : VideoRepository {
        override suspend fun getRecordedVideos(): List<RecordedVideo> = emptyList()
        override fun playVideo(video: RecordedVideo) {}
        override fun shareVideo(video: RecordedVideo) {}
        override suspend fun deleteVideo(video: RecordedVideo): Boolean = false
        override suspend fun trimVideo(video: RecordedVideo, startMs: Long, endMs: Long): Boolean = false
    }
}
