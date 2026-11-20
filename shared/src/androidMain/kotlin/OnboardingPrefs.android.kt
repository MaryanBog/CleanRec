import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShouldShowOnboarding(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("cleanrec_prefs", Context.MODE_PRIVATE)
    }
    var shouldShow by remember {
        mutableStateOf(!prefs.getBoolean("onboarding_done", false))
    }

    val markDone: () -> Unit = {
        prefs.edit().putBoolean("onboarding_done", true).apply()
        shouldShow = false
    }

    return shouldShow to markDone
}
