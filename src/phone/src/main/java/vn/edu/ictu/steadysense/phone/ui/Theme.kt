package vn.edu.ictu.steadysense.phone.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Coral = Color(0xFFFF6B6B)
val CoralSoft = Color(0xFFFFE7E5)
val Sky = Color(0xFF3C8DFF)
val SkySoft = Color(0xFFE6F0FF)
val Mint = Color(0xFF31C7A3)
val MintSoft = Color(0xFFDDF8EF)
val Sun = Color(0xFFFFC857)
val Ink = Color(0xFF20263A)
val Muted = Color(0xFF697089)
val Canvas = Color(0xFFF7F9FF)
val White = Color(0xFFFFFFFF)

private val SteadyColors = lightColorScheme(
    primary = Sky,
    onPrimary = White,
    primaryContainer = SkySoft,
    onPrimaryContainer = Ink,
    secondary = Mint,
    onSecondary = White,
    secondaryContainer = MintSoft,
    tertiary = Coral,
    tertiaryContainer = CoralSoft,
    background = Canvas,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    outline = Color(0xFFDCE2F0),
)

@Composable
fun SteadySenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SteadyColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
