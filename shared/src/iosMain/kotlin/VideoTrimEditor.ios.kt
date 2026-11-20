import androidx.compose.runtime.Composable

@Composable
actual fun VideoTrimEditor(
    video: RecordedVideo,
    strings: Map<String, String>,
    onDismiss: () -> Unit,
    onConfirmTrim: (startMs: Long, endMs: Long) -> Unit
) {}