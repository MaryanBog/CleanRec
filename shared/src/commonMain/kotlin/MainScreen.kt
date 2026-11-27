import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen() {
    val recorder = rememberScreenRecorder()
    val repository = rememberVideoRepository()

    val initialLang = defaultSystemLanguage()
    var currentLang by rememberSaveable { mutableStateOf(initialLang) }
    val strings = rememberAppStrings(selectedLang = currentLang)

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    val (showOnboarding, completeOnboarding) = rememberShouldShowOnboarding()
    var showHelpManually by rememberSaveable { mutableStateOf(false) }

    if (showOnboarding || showHelpManually) {
        HelpDialog(
            strings = strings,
            onDismiss = {
                if (showOnboarding) completeOnboarding()
                showHelpManually = false
            }
        )
    }

    CleanRecTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CleanRecColors.BgDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = CleanRecDimens.ScreenPaddingH,
                            vertical = 8.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CleanRecActionButton(
                        text = "?",
                        bgColor = CleanRecColors.SurfaceDark,
                        textColor = CleanRecColors.TextSecondary,
                        shape = CleanRecShapes.Button,
                        paddingH = 12.dp,
                        paddingV = 6.dp,
                        onClick = { showHelpManually = true }
                    )

                    CleanRecActionButton(
                        text = currentLang.uppercase(),
                        bgColor = CleanRecColors.SurfaceDark,
                        textColor = CleanRecColors.AccentCyan,
                        shape = CleanRecShapes.Button,
                        paddingH = 14.dp,
                        paddingV = 6.dp,
                        onClick = { currentLang = nextLanguage(currentLang) }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        RecorderScreen(
                            recorder = recorder,
                            strings = strings
                        )
                    } else {
                        GalleryScreen(
                            repository = repository,
                            strings = strings
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = CleanRecDimens.ScreenPaddingH,
                            vertical = CleanRecDimens.ScreenPaddingV
                        )
                        .clip(CleanRecShapes.Card)
                        .background(CleanRecColors.SurfaceDark)
                        .padding(CleanRecDimens.SmallSpacing),
                    horizontalArrangement = Arrangement.spacedBy(CleanRecDimens.SmallSpacing)
                ) {
                    TabButton(
                        title = stringRes(strings, "tab_record", "Record"),
                        selected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabButton(
                        title = stringRes(strings, "tab_gallery", "Recordings"),
                        selected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CleanRecActionButton(
        text = title,
        bgColor = if (selected) CleanRecColors.AccentCyan else CleanRecColors.SurfaceDark,
        textColor = if (selected) CleanRecColors.BgDark else CleanRecColors.TextSecondary,
        shape = CleanRecShapes.Tab,
        fontSize = 14.sp,
        paddingV = 12.dp,
        modifier = modifier,
        onClick = onClick
    )
}
