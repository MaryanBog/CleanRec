import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun RecorderScreen(
    recorder: ScreenRecorderController,
    strings: Map<String, String>
) {
    val isRecording by recorder.isRecording.collectAsState()

    var recordMic by rememberSaveable { mutableStateOf(true) }
    var qualityHeight by rememberSaveable { mutableStateOf(1080) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        } else {
            elapsedSeconds = 0
        }
    }

    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) CleanRecColors.AccentRed else CleanRecColors.AccentCyan
    )

    val formattedTime = remember(elapsedSeconds) {
        val mins = (elapsedSeconds / 60).toString().padStart(2, '0')
        val secs = (elapsedSeconds % 60).toString().padStart(2, '0')
        "$mins:$secs"
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = CleanRecDimens.ScreenPaddingH,
                vertical = CleanRecDimens.ScreenPaddingV
            )
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    HeaderSection(isRecording = isRecording, strings = strings)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) CleanRecColors.TextPrimary else CleanRecColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RecordButton(
                        size = 116.dp,
                        isRecording = isRecording,
                        buttonColor = buttonColor,
                        strings = strings,
                        onClick = {
                            if (isRecording) {
                                recorder.stopRecording()
                            } else {
                                recorder.startRecording(RecordingConfig(recordMic, qualityHeight))
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    SettingsCard(
                        isRecording = isRecording,
                        recordMic = recordMic,
                        onRecordMicChange = { recordMic = it },
                        qualityHeight = qualityHeight,
                        onQualityChange = { qualityHeight = it },
                        strings = strings
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                HeaderSection(isRecording = isRecording, strings = strings)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) CleanRecColors.TextPrimary else CleanRecColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    RecordButton(
                        size = 160.dp,
                        isRecording = isRecording,
                        buttonColor = buttonColor,
                        strings = strings,
                        onClick = {
                            if (isRecording) {
                                recorder.stopRecording()
                            } else {
                                recorder.startRecording(RecordingConfig(recordMic, qualityHeight))
                            }
                        }
                    )
                }

                SettingsCard(
                    isRecording = isRecording,
                    recordMic = recordMic,
                    onRecordMicChange = { recordMic = it },
                    qualityHeight = qualityHeight,
                    onQualityChange = { qualityHeight = it },
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    isRecording: Boolean,
    strings: Map<String, String>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringRes(strings, "app_name", "CleanRec"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = CleanRecColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isRecording) {
                stringRes(strings, "status_recording", "Recording...")
            } else {
                stringRes(strings, "status_ready", "Ready")
            },
            fontSize = 13.sp,
            color = if (isRecording) CleanRecColors.AccentRed else CleanRecColors.TextSecondary
        )
    }
}

@Composable
private fun RecordButton(
    size: Dp,
    isRecording: Boolean,
    buttonColor: androidx.compose.ui.graphics.Color,
    strings: Map<String, String>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .border(2.dp, buttonColor.copy(alpha = 0.35f), CircleShape)
            .padding(10.dp)
            .clip(CircleShape)
            .background(buttonColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isRecording) {
                stringRes(strings, "action_stop", "STOP")
            } else {
                stringRes(strings, "action_start", "START")
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CleanRecColors.BgDark
        )
    }
}

@Composable
private fun SettingsCard(
    isRecording: Boolean,
    recordMic: Boolean,
    onRecordMicChange: (Boolean) -> Unit,
    qualityHeight: Int,
    onQualityChange: (Int) -> Unit,
    strings: Map<String, String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CleanRecShapes.Dialog)
            .background(CleanRecColors.SurfaceDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringRes(strings, "mic_title", "Microphone"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CleanRecColors.TextPrimary
                )
                Text(
                    text = if (recordMic) {
                        stringRes(strings, "mic_on_desc", "Enabled")
                    } else {
                        stringRes(strings, "mic_off_desc", "Muted")
                    },
                    fontSize = 12.sp,
                    color = CleanRecColors.TextSecondary
                )
            }
            Switch(
                checked = recordMic,
                onCheckedChange = { if (!isRecording) onRecordMicChange(it) },
                enabled = !isRecording,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CleanRecColors.BgDark,
                    checkedTrackColor = CleanRecColors.AccentCyan
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringRes(strings, "quality_title", "Quality"),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CleanRecColors.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(720, 1080).forEach { res ->
                    val selected = qualityHeight == res
                    CleanRecActionButton(
                        text = "${res}p",
                        bgColor = if (selected) CleanRecColors.AccentCyan else CleanRecColors.BgDark,
                        textColor = if (selected) CleanRecColors.BgDark else CleanRecColors.TextSecondary,
                        shape = CleanRecShapes.Button,
                        fontSize = 13.sp,
                        paddingH = 14.dp,
                        paddingV = 8.dp,
                        onClick = { if (!isRecording) onQualityChange(res) }
                    )
                }
            }
        }
    }
}