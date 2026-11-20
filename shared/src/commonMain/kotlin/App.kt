import androidx.compose.runtime.Composable

@Composable
fun App() {
    MainScreen()
}

expect fun getPlatformName(): String
