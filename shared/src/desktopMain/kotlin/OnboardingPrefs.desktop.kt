import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences

@Composable
actual fun rememberShouldShowOnboarding(): Pair<Boolean, () -> Unit> {
    return remember {
        val prefs = Preferences.userRoot().node("cleanrec_prefs")
        val shouldShow = prefs.getBoolean("show_onboarding", true)
        val markCompleted: () -> Unit = {
            prefs.putBoolean("show_onboarding", false)
        }
        shouldShow to markCompleted
    }
}
