import androidx.compose.runtime.Composable

@Composable
expect fun VideoTrimEditor(
    video: RecordedVideo,
    strings: Map<String, String>,
    onDismiss: () -> Unit,
    onConfirmTrim: (startMs: Long, endMs: Long) -> Unit
)