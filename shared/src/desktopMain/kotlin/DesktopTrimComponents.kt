import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun DesktopTimeBadge(
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
            text = formatDesktopTimeMs(ms),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) CleanRecColors.AccentCyan else CleanRecColors.TextPrimary
        )
    }
}

fun formatDesktopTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).toInt()
    val mins = (totalSec / 60).toString().padStart(2, '0')
    val secs = (totalSec % 60).toString().padStart(2, '0')
    return "$mins:$secs"
}
