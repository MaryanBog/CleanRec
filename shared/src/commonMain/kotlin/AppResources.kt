import androidx.compose.runtime.*
import androidx.compose.ui.text.intl.Locale
import cleanrec.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private val StringRegex = Regex("""<string name="([^"]+)">([^<]*)</string>""")

val SUPPORTED_LANGUAGES = listOf("en", "ru", "zh")

fun nextLanguage(current: String): String {
    val idx = SUPPORTED_LANGUAGES.indexOfFirst { it.equals(current, ignoreCase = true) }
    return if (idx == -1) SUPPORTED_LANGUAGES.first()
    else SUPPORTED_LANGUAGES[(idx + 1) % SUPPORTED_LANGUAGES.size]
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberAppStrings(selectedLang: String): Map<String, String> {
    var strings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(selectedLang) {
        val code = selectedLang.lowercase().substringBefore("-")
        val path = if (code == "en") "files/values/strings.xml" else "files/values-$code/strings.xml"

        val xmlBytes = try {
            Res.readBytes(path)
        } catch (_: Exception) {
            Res.readBytes("files/values/strings.xml")
        }
        val xmlText = xmlBytes.decodeToString()
        strings = StringRegex.findAll(xmlText).associate { match ->
            match.groupValues.get(1) to match.groupValues.get(2)
        }
    }

    return strings
}

@Composable
fun defaultSystemLanguage(): String {
    return remember {
        val sysLang = Locale.current.language.lowercase().substringBefore("-")
        if (SUPPORTED_LANGUAGES.contains(sysLang)) sysLang else "en"
    }
}

@Composable
fun stringRes(strings: Map<String, String>, key: String, fallback: String = key): String {
    return strings[key] ?: fallback
}
