import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CleanRecColors {
    val BgDark = Color(0xFF0B0F14)
    val SurfaceDark = Color(0xFF131922)
    val SurfaceElevated = Color(0xFF1A222E)
    val AccentCyan = Color(0xFF00E5FF)
    val AccentSecondary = Color(0xFF6C63FF)
    val AccentRed = Color(0xFFFF4D6D)
    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF9AA4B2)
    val ScrimDark = Color(0xBF0B0F14)
}

object CleanRecShapes {
    val Small = RoundedCornerShape(8.dp)
    val Button = RoundedCornerShape(10.dp)
    val Tab = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(16.dp)
    val Dialog = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(50)
}

object CleanRecDimens {
    val ScreenPaddingH = 20.dp
    val ScreenPaddingV = 12.dp
    val CardPadding = 14.dp
    val ItemSpacing = 12.dp
    val SmallSpacing = 6.dp
}

private val CleanRecDarkPalette = darkColors(
    primary = CleanRecColors.AccentCyan,
    secondary = CleanRecColors.AccentSecondary,
    background = CleanRecColors.BgDark,
    surface = CleanRecColors.SurfaceDark,
    onPrimary = CleanRecColors.BgDark,
    onSecondary = CleanRecColors.TextPrimary,
    onBackground = CleanRecColors.TextPrimary,
    onSurface = CleanRecColors.TextPrimary
)

private val CleanRecMaterialShapes = Shapes(
    small = CleanRecShapes.Button,
    medium = CleanRecShapes.Card,
    large = CleanRecShapes.Dialog
)

@Composable
fun CleanRecTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = CleanRecDarkPalette,
        shapes = CleanRecMaterialShapes,
        content = content
    )
}

/**
 * Единый стиль компактных и обычных кнопок CleanRec
 */
@Composable
fun CleanRecActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color = CleanRecColors.BgDark,
    textColor: Color = CleanRecColors.TextPrimary,
    shape: Shape = CleanRecShapes.Button,
    fontSize: TextUnit = 12.sp,
    paddingH: Dp = 12.dp,
    paddingV: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = paddingH, vertical = paddingV),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textColor
        )
    }
}
