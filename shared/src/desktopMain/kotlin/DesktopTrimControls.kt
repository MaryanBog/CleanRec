import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RangeSlider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DesktopTrimControls(
    range: ClosedFloatingPointRange<Float>,
    totalMs: Long,
    strings: Map<String, String>,
    onRangeChange: (ClosedFloatingPointRange<Float>, Long) -> Unit
) {
    val startMs = range.start.toLong()
    val endMs = range.endInclusive.toLong()
    val currentStartMs by rememberUpdatedState(startMs)
    val currentEndMs by rememberUpdatedState(endMs)

    val stepMinus = stringRes(strings, "trim_step_minus", "-1s")
    val stepPlus = stringRes(strings, "trim_step_plus", "+1s")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CleanRecShapes.Dialog)
            .background(CleanRecColors.SurfaceDark)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(CleanRecDimens.ItemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DesktopTimeBadge(stringRes(strings, "trim_start", "Start"), startMs)
            DesktopTimeBadge(stringRes(strings, "trim_total", "Total"), (endMs - startMs).coerceAtLeast(0L), highlight = true)
            DesktopTimeBadge(stringRes(strings, "trim_end", "End"), endMs)
        }

        RangeSlider(
            value = range,
            onValueChange = { newRange ->
                val s = newRange.start.coerceAtMost(newRange.endInclusive - 1000f)
                val e = newRange.endInclusive.coerceAtLeast(s + 1000f)
                val newStartMs = s.toLong()
                val newEndMs = e.toLong()
                val targetSeekMs = if (newStartMs != currentStartMs) newStartMs else newEndMs
                onRangeChange(s..e, targetSeekMs)
            },
            valueRange = 0f..totalMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = CleanRecColors.AccentCyan,
                activeTrackColor = CleanRecColors.AccentCyan,
                inactiveTrackColor = CleanRecColors.BgDark
            )
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CleanRecActionButton(
                    text = stepMinus,
                    shape = CleanRecShapes.Small,
                    paddingV = 6.dp,
                    onClick = {
                        val next = (currentStartMs - 1000L).coerceAtLeast(0L)
                        onRangeChange(next.toFloat()..currentEndMs.toFloat(), next)
                    }
                )
                CleanRecActionButton(
                    text = stepPlus,
                    shape = CleanRecShapes.Small,
                    paddingV = 6.dp,
                    onClick = {
                        val next = (currentStartMs + 1000L).coerceAtMost(currentEndMs - 1000L)
                        onRangeChange(next.toFloat()..currentEndMs.toFloat(), next)
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CleanRecActionButton(
                    text = stepMinus,
                    shape = CleanRecShapes.Small,
                    paddingV = 6.dp,
                    onClick = {
                        val next = (currentEndMs - 1000L).coerceAtLeast(currentStartMs + 1000L)
                        onRangeChange(currentStartMs.toFloat()..next.toFloat(), next)
                    }
                )
                CleanRecActionButton(
                    text = stepPlus,
                    shape = CleanRecShapes.Small,
                    paddingV = 6.dp,
                    onClick = {
                        val next = (currentEndMs + 1000L).coerceAtMost(totalMs)
                        onRangeChange(currentStartMs.toFloat()..next.toFloat(), next)
                    }
                )
            }
        }
    }
}
