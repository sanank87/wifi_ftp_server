package com.wififtp.server.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Color Palette ─────────────────────────────────────────────────────────
val Primary       = Color(0xFF3B82F6)
val PrimaryDark   = Color(0xFF2563EB)
val Success       = Color(0xFF22C55E)
val Warning       = Color(0xFFF59E0B)
val Error         = Color(0xFFEF4444)

val BgDark        = Color(0xFF0A0E1A)
val BgCard        = Color(0xFF111827)
val BgCard2       = Color(0xFF1A2235)
val BorderColor   = Color(0x12FFFFFF)
val BorderPrimary = Color(0x33FFFFFF)

val TextPrimary   = Color(0xFFF1F5F9)
val TextMuted     = Color(0xFF64748B)
val TextDim       = Color(0xFF94A3B8)

// ─── Dark Color Scheme ──────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Primary,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF1E3A5F),
    secondary        = Success,
    onSecondary      = Color.White,
    tertiary         = Warning,
    background       = BgDark,
    surface          = BgCard,
    surfaceVariant   = BgCard2,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextMuted,
    error            = Error,
    outline          = BorderColor,
)

@Composable
fun WifiFTPServerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content,
    )
}
