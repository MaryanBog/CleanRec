import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun HelpDialog(
    strings: Map<String, String>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CleanRecShapes.Dialog)
                .background(CleanRecColors.SurfaceDark)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringRes(strings, "guide_title", "How it works"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CleanRecColors.TextPrimary
            )

            GuideStepItem(
                badge = "REC",
                badgeColor = CleanRecColors.AccentCyan,
                title = stringRes(strings, "guide_step1_title", "1. Start recording"),
                desc = stringRes(
                    strings,
                    "guide_step1_desc",
                    "Tap START and allow capture. You have 5 seconds to open your game or app."
                )
            )

            GuideStepItem(
                badge = "STOP",
                badgeColor = CleanRecColors.AccentRed,
                title = stringRes(strings, "guide_step2_title", "2. Stop recording"),
                desc = stringRes(
                    strings,
                    "guide_step2_desc",
                    "Pull down the notification shade and tap Stop, or return to CleanRec."
                )
            )

            GuideStepItem(
                badge = "CUT",
                badgeColor = CleanRecColors.AccentSecondary,
                title = stringRes(strings, "guide_step3_title", "3. Trim & Share"),
                desc = stringRes(
                    strings,
                    "guide_step3_desc",
                    "Open Recordings, tap Trim, set start and end points, and save a clean clip."
                )
            )

            CleanRecActionButton(
                text = stringRes(strings, "guide_got_it", "Got it"),
                bgColor = CleanRecColors.AccentCyan,
                textColor = CleanRecColors.BgDark,
                fontSize = 14.sp,
                paddingV = 12.dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun GuideStepItem(
    badge: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clip(CleanRecShapes.Small)
                .background(badgeColor.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CleanRecColors.TextPrimary
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = CleanRecColors.TextSecondary
            )
        }
    }
}
