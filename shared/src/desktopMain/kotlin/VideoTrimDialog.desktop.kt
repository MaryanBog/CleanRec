import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
actual fun VideoTrimEditor(
    video: RecordedVideo,
    strings: Map<String, String>,
    onDismiss: () -> Unit,
    onConfirmTrim: (startMs: Long, endMs: Long) -> Unit
) {
    val totalMs = video.durationMs.coerceAtLeast(1000L)
    var range by remember { mutableStateOf(0f..totalMs.toFloat()) }
    var isPlaying by remember { mutableStateOf(false) }
    var seekTargetMs by remember { mutableStateOf(0L) }
    var seekTick by remember { mutableStateOf(0) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val startMs = range.start.toLong()
    val endMs = range.endInclusive.toLong()
    val previewer = remember(video.id) { DesktopVideoPreviewer(video.id) }

    DisposableEffect(previewer) {
        onDispose {
            isPlaying = false
            previewer.close()
        }
    }

    LaunchedEffect(previewer) {
        previewer.open()
        previewBitmap = previewer.seekAndGrab(0L)
    }

    LaunchedEffect(seekTick) {
        if (seekTick > 0 && !isPlaying) {
            delay(80L)
            previewer.seekAndGrab(seekTargetMs)?.let { previewBitmap = it }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startPos = if (seekTargetMs < startMs || seekTargetMs >= endMs) startMs else seekTargetMs
            previewer.seekAndGrab(startPos)?.let { previewBitmap = it }
            while (isPlaying) {
                val t0 = System.currentTimeMillis()
                val (bmp, posMs) = previewer.grabNextFrame()
                if (posMs < 0L || posMs >= endMs) {
                    seekTargetMs = startMs
                    previewBitmap = previewer.seekAndGrab(startMs)
                    isPlaying = false
                    break
                }
                if (bmp != null) previewBitmap = bmp
                seekTargetMs = posMs
                delay((33L - (System.currentTimeMillis() - t0)).coerceAtLeast(5L))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = CleanRecColors.BgDark) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = CleanRecDimens.ScreenPaddingH, vertical = CleanRecDimens.ScreenPaddingV),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DesktopTrimHeader(
                    strings = strings,
                    onCancel = { isPlaying = false; onDismiss() },
                    onSave = { isPlaying = false; onConfirmTrim(startMs, endMs) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(CleanRecShapes.Dialog)
                        .background(CleanRecColors.SurfaceDark)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    previewBitmap?.let { bmp ->
                        Image(bitmap = bmp, contentDescription = video.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    }
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .clip(CleanRecShapes.Pill)
                                .background(CleanRecColors.ScrimDark)
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = stringRes(strings, "trim_preview", "▶ Preview"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanRecColors.AccentCyan
                            )
                        }
                    }
                }

                DesktopTrimControls(
                    range = range,
                    totalMs = totalMs,
                    strings = strings,
                    onRangeChange = { newRange, targetMs ->
                        range = newRange
                        isPlaying = false
                        seekTargetMs = targetMs
                        seekTick++
                    }
                )
            }
        }
    }
}
