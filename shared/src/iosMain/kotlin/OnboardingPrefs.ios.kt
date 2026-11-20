import androidx.compose.runtime.*
import platform.Foundation.NSUserDefaults

@Composable
actual fun rememberShouldShowOnboarding(): Pair<Boolean, () -> Unit> {
    val defaults = remember { NSUserDefaults.standardUserDefaults }
    var shouldShow by remember {
        mutableStateOf(!defaults.boolForKey("onboarding_done"))
    }

    val markDone: () -> Unit = {
        defaults.setBool(true, forKey = "onboarding_done")
        shouldShow = false
    }

    return shouldShow to markDone
}
