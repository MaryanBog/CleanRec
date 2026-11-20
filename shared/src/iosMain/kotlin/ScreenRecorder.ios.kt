import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
actual fun rememberScreenRecorder(): ScreenRecorderController {
    return object : ScreenRecorderController {
        private val _isRecording = MutableStateFlow(false)
        override val isRecording: StateFlow<Boolean> = _isRecording

        override fun startRecording(config: RecordingConfig) {
            _isRecording.value = true
        }

        override fun stopRecording() {
            _isRecording.value = false
        }
    }
}
