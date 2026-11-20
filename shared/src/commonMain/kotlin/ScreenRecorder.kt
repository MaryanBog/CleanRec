import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

data class RecordingConfig(
    val recordMic: Boolean = true,
    val qualityHeight: Int = 1080
)

interface ScreenRecorderController {
    val isRecording: StateFlow<Boolean>
    fun startRecording(config: RecordingConfig)
    fun stopRecording()
}

@Composable
expect fun rememberScreenRecorder(): ScreenRecorderController
