import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun GalleryScreen(
    repository: VideoRepository,
    strings: Map<String, String>
) {
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<RecordedVideo>>(emptyList()) }
    var trimmingVideo by remember { mutableStateOf<RecordedVideo?>(null) }

    LaunchedEffect(Unit) {
        videos = repository.getRecordedVideos()
    }

    trimmingVideo?.let { videoToTrim ->
        VideoTrimEditor(
            video = videoToTrim,
            strings = strings,
            onDismiss = { trimmingVideo = null },
            onConfirmTrim = { startMs, endMs ->
                trimmingVideo = null
                scope.launch {
                    if (repository.trimVideo(videoToTrim, startMs, endMs)) {
                        videos = repository.getRecordedVideos()
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CleanRecDimens.ScreenPaddingH,
                vertical = CleanRecDimens.ScreenPaddingV
            )
    ) {
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringRes(strings, "gallery_empty", "No recordings yet"),
                    fontSize = 15.sp,
                    color = CleanRecColors.TextSecondary
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(CleanRecDimens.ItemSpacing),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoItemCard(
                        video = video,
                        strings = strings,
                        onPlay = { repository.playVideo(video) },
                        onTrim = { trimmingVideo = video },
                        onShare = { repository.shareVideo(video) },
                        onDelete = {
                            scope.launch {
                                if (repository.deleteVideo(video)) {
                                    videos = videos.filterNot { it.id == video.id }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoItemCard(
    video: RecordedVideo,
    strings: Map<String, String>,
    onPlay: () -> Unit,
    onTrim: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val isTrimmed = remember(video.name) {
        video.name.contains("_trim", ignoreCase = true)
    }

    val displayTitle = remember(video.name) {
        formatCleanRecTitle(video.name)
    }

    val durationText = remember(video.durationMs) {
        val totalSec = (video.durationMs / 1000L).toInt()
        val mins = (totalSec / 60).toString().padStart(2, '0')
        val secs = (totalSec % 60).toString().padStart(2, '0')
        "$mins:$secs"
    }

    val sizeMbText = remember(video.sizeBytes) {
        val tenths = (video.sizeBytes * 10L) / (1024L * 1024L)
        "${tenths / 10}.${tenths % 10} MB"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CleanRecShapes.Card)
            .background(CleanRecColors.SurfaceDark)
            .padding(CleanRecDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CleanRecColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isTrimmed) {
                    Box(
                        modifier = Modifier
                            .clip(CleanRecShapes.Small)
                            .background(CleanRecColors.AccentCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringRes(strings, "badge_trimmed", "Trimmed"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = CleanRecColors.AccentCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$durationText • $sizeMbText",
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                color = CleanRecColors.TextSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CleanRecDimens.SmallSpacing)
        ) {
            CleanRecActionButton(
                text = stringRes(strings, "action_play", "Play"),
                bgColor = CleanRecColors.AccentCyan,
                textColor = CleanRecColors.BgDark,
                fontSize = 11.sp,
                paddingH = 4.dp,
                modifier = Modifier.weight(1f),
                onClick = onPlay
            )
            CleanRecActionButton(
                text = stringRes(strings, "action_trim", "Trim"),
                bgColor = CleanRecColors.BgDark,
                textColor = CleanRecColors.AccentCyan,
                fontSize = 11.sp,
                paddingH = 4.dp,
                modifier = Modifier.weight(1f),
                onClick = onTrim
            )
            CleanRecActionButton(
                text = stringRes(strings, "action_share", "Share"),
                bgColor = CleanRecColors.BgDark,
                textColor = CleanRecColors.TextPrimary,
                fontSize = 11.sp,
                paddingH = 4.dp,
                modifier = Modifier.weight(1f),
                onClick = onShare
            )
            CleanRecActionButton(
                text = stringRes(strings, "action_delete", "Delete"),
                bgColor = CleanRecColors.BgDark,
                textColor = CleanRecColors.AccentRed,
                fontSize = 11.sp,
                paddingH = 4.dp,
                modifier = Modifier.weight(1f),
                onClick = onDelete
            )
        }
    }
}

private val DateStampRegex = Regex("""(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})""")

private fun formatCleanRecTitle(rawName: String): String {
    val match = DateStampRegex.find(rawName)
    if (match != null) {
        val (year, month, day, hour, min) = match.destructured
        return "$day.$month.$year • $hour:$min"
    }
    return rawName
        .removeSuffix(".mp4")
        .removeSuffix("_trim")
        .removePrefix("CleanRec_")
}
