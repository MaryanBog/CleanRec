import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DesktopTrimHeader(
    strings: Map<String, String>,
    onCancel: () -> Unit,
    onSave: () -> Unit
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
            onClick = onCancel
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
            onClick = onSave
        )
    }
}
