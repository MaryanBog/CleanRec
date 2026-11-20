import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.myapplication.ScreenRecordService
import kotlinx.coroutines.flow.StateFlow

private fun createCaptureIntent(manager: MediaProjectionManager): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    } else {
        manager.createScreenCaptureIntent()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun rememberScreenRecorder(): ScreenRecorderController {
    val context = LocalContext.current
    var pendingConfig by remember { mutableStateOf(RecordingConfig()) }

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Запускаем сервис СРАЗУ, пока приложение на переднем плане
            val startIntent = Intent(context, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_START
                putExtra(ScreenRecordService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecordService.EXTRA_DATA, result.data)
                putExtra(ScreenRecordService.EXTRA_RECORD_MIC, pendingConfig.recordMic)
                putExtra(ScreenRecordService.EXTRA_QUALITY, pendingConfig.qualityHeight)
            }
            ContextCompat.startForegroundService(context, startIntent)

            // Сворачиваем CleanRec на рабочий стол
            context.findActivity()?.moveTaskToBack(true)
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        mediaProjectionLauncher.launch(createCaptureIntent(projectionManager))
    }

    return remember(context) {
        object : ScreenRecorderController {
            override val isRecording: StateFlow<Boolean> = ScreenRecordService.isRecording

            override fun startRecording(config: RecordingConfig) {
                pendingConfig = config
                val neededPermissions = mutableListOf<String>()

                if (config.recordMic && ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    neededPermissions.add(Manifest.permission.RECORD_AUDIO)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (neededPermissions.isNotEmpty()) {
                    permissionsLauncher.launch(neededPermissions.toTypedArray())
                } else {
                    mediaProjectionLauncher.launch(createCaptureIntent(projectionManager))
                }
            }

            override fun stopRecording() {
                context.startService(Intent(context, ScreenRecordService::class.java).apply {
                    action = ScreenRecordService.ACTION_STOP
                })
            }
        }
    }
}
