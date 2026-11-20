import androidx.compose.runtime.*
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

private val StringRegex = Regex("""<string name="([^"]+)">([^<]*)</string>""")

val SUPPORTED_LANGUAGES = listOf("en", "ru", "zh")

fun nextLanguage(current: String): String {
    val idx = SUPPORTED_LANGUAGES.indexOfFirst { it.equals(current, ignoreCase = true) }
    return if (idx == -1) {
        SUPPORTED_LANGUAGES.first()
    } else {
        SUPPORTED_LANGUAGES[(idx + 1) % SUPPORTED_LANGUAGES.size]
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberAppStrings(selectedLang: String): Map<String, String> {
    var strings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(selectedLang) {
        val code = selectedLang.lowercase().substringBefore("-")
        val path = if (code == "en") "values/strings.xml" else "values-$code/strings.xml"

        val xmlBytes = try {
            resource(path).readBytes()
        } catch (e: Exception) {
            resource("values/strings.xml").readBytes()
        }
        val xmlText = xmlBytes.decodeToString()
        strings = StringRegex.findAll(xmlText).associate { match ->
            match.groupValues[1] to match.groupValues[2]
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
