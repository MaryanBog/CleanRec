import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RangeSlider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
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
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val startMs = range.start.toLong()
    val endMs = range.endInclusive.toLong()
    val selectedDurationMs = (endMs - startMs).coerceAtLeast(0L)

    LaunchedEffect(isPlaying, endMs) {
        while (isPlaying) {
            val currentPos = videoViewRef?.currentPosition?.toLong() ?: 0L
            if (currentPos >= endMs) {
                videoViewRef?.pause()
                videoViewRef?.seekTo(startMs.toInt())
                isPlaying = false
            }
            delay(100L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CleanRecColors.BgDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = CleanRecDimens.ScreenPaddingH,
                        vertical = CleanRecDimens.ScreenPaddingV
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CleanRecActionButton(
                        text = stringRes(strings, "trim_cancel", "Cancel"),
                        bgColor = CleanRecColors.BgDark,
                        textColor = CleanRecColors.TextSecondary,
                        shape = CleanRecShapes.Small,
                        fontSize = 14.sp,
                        paddingH = 8.dp,
                        paddingV = 6.dp,
                        onClick = onDismiss
                    )

                    Text(
                        text = stringRes(strings, "trim_title", "Trim video"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanRecColors.TextPrimary
                    )

                    CleanRecActionButton(
                        text = stringRes(strings, "trim_save", "Save"),
                        bgColor = CleanRecColors.AccentCyan,
                        textColor = CleanRecColors.BgDark,
                        shape = CleanRecShapes.Button,
                        fontSize = 13.sp,
                        paddingH = 14.dp,
                        paddingV = 8.dp,
                        onClick = {
                            videoViewRef?.pause()
                            onConfirmTrim(startMs, endMs)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(CleanRecShapes.Dialog)
                        .background(CleanRecColors.SurfaceDark)
                        .clickable {
                            val vv = videoViewRef ?: return@clickable
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                if (vv.currentPosition < startMs || vv.currentPosition >= endMs) {
                                    vv.seekTo(startMs.toInt())
                                }
                                vv.start()
                                isPlaying = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(video.id))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = false
                                    seekTo(1)
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CleanRecShapes.Dialog)
                        .background(CleanRecColors.SurfaceDark)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(CleanRecDimens.ItemSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeBadge(
                            label = stringRes(strings, "trim_start", "Start"),
                            ms = startMs
                        )
                        TimeBadge(
                            label = stringRes(strings, "trim_total", "Total"),
                            ms = selectedDurationMs,
                            highlight = true
                        )
                        TimeBadge(
                            label = stringRes(strings, "trim_end", "End"),
                            ms = endMs
                        )
                    }

                    RangeSlider(
                        value = range,
                        onValueChange = { newRange ->
                            val minGap = 1000f
                            val clampedStart = newRange.start.coerceAtMost(newRange.endInclusive - minGap)
                            val clampedEnd = newRange.endInclusive.coerceAtLeast(clampedStart + minGap)
                            val oldStart = range.start
                            range = clampedStart..clampedEnd

                            videoViewRef?.pause()
                            isPlaying = false
                            val seekTarget = if (clampedStart != oldStart) clampedStart else clampedEnd
                            videoViewRef?.seekTo(seekTarget.toInt())
                        },
                        valueRange = 0f..totalMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = CleanRecColors.AccentCyan,
                            activeTrackColor = CleanRecColors.AccentCyan,
                            inactiveTrackColor = CleanRecColors.BgDark
                        )
                    )

                    val stepMinusLabel = stringRes(strings, "trim_step_minus", "-1s")
                    val stepPlusLabel = stringRes(strings, "trim_step_plus", "+1s")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CleanRecActionButton(
                                text = stepMinusLabel,
                                shape = CleanRecShapes.Small,
                                paddingV = 6.dp,
                                onClick = {
                                    val next = (startMs - 1000L).coerceAtLeast(0L)
                                    range = next.toFloat()..endMs.toFloat()
                                    videoViewRef?.seekTo(next.toInt())
                                }
                            )
                            CleanRecActionButton(
                                text = stepPlusLabel,
                                shape = CleanRecShapes.Small,
                                paddingV = 6.dp,
                                onClick = {
                                    val next = (startMs + 1000L).coerceAtMost(endMs - 1000L)
                                    range = next.toFloat()..endMs.toFloat()
                                    videoViewRef?.seekTo(next.toInt())
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CleanRecActionButton(
                                text = stepMinusLabel,
                                shape = CleanRecShapes.Small,
                                paddingV = 6.dp,
                                onClick = {
                                    val next = (endMs - 1000L).coerceAtLeast(startMs + 1000L)
                                    range = startMs.toFloat()..next.toFloat()
                                    videoViewRef?.seekTo(next.toInt())
                                }
                            )
                            CleanRecActionButton(
                                text = stepPlusLabel,
                                shape = CleanRecShapes.Small,
                                paddingV = 6.dp,
                                onClick = {
                                    val next = (endMs + 1000L).coerceAtMost(totalMs)
                                    range = startMs.toFloat()..next.toFloat()
                                    videoViewRef?.seekTo(next.toInt())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun TimeBadge(
    label: String,
    ms: Long,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = CleanRecColors.TextSecondary
        )
        Text(
            text = formatTimeMs(ms),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) CleanRecColors.AccentCyan else CleanRecColors.TextPrimary
        )
    }
}
private fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).toInt()
    val mins = (totalSec / 60).toString().padStart(2, '0')
    val secs = (totalSec % 60).toString().padStart(2, '0')
    return "$mins:$secs"
}