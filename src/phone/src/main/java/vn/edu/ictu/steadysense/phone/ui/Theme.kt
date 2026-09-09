/*
 * Copyright 2026 SteadySense AI Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vn.edu.ictu.steadysense.phone.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Wecare Medical Dashboard Inspired Palette
val Teal = Color(0xFF0FB7A4)
val TealDark = Color(0xFF0A8A7C)
val TealSoft = Color(0xFFE8F8F5)
val TealContainer = Color(0xFFD0F2EB)

val Coral = Color(0xFFEF4444)
val CoralSoft = Color(0xFFFEE2E2)

val Amber = Color(0xFFF59E0B)
val AmberSoft = Color(0xFFFEF3C7)

val Sky = Color(0xFF0EA5E9)
val SkySoft = Color(0xFFE0F2FE)

val Mint = Teal
val MintSoft = TealSoft
val Sun = Amber

val Ink = Color(0xFF0F172A)
val InkSecondary = Color(0xFF334155)
val Muted = Color(0xFF64748B)
val Canvas = Color(0xFFF8FAFC)
val SurfaceBorder = Color(0xFFE2E8F0)
val White = Color(0xFFFFFFFF)

private val SteadyColors = lightColorScheme(
    primary = Teal,
    onPrimary = White,
    primaryContainer = TealSoft,
    onPrimaryContainer = Ink,
    secondary = Sky,
    onSecondary = White,
    secondaryContainer = SkySoft,
    tertiary = Amber,
    tertiaryContainer = AmberSoft,
    background = Canvas,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    outline = SurfaceBorder,
)

@Composable
fun SteadySenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SteadyColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
