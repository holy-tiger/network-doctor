package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = BrandBlue,
  onPrimary = Color.White,
  primaryContainer = BrandLightBlueBg,
  onPrimaryContainer = BrandBlueDark,
  secondary = EmeraldGreen,
  onSecondary = Color.White,
  secondaryContainer = EmeraldGreenBg,
  onSecondaryContainer = EmeraldGreenDark,
  tertiary = BrandIndigo,
  onTertiary = Color.White,
  background = AppBgLight,
  onBackground = TextSlatePrimary,
  surface = SurfaceCardLight,
  onSurface = TextSlatePrimary,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = TextSlateSecondary,
  outline = SurfaceCardBorder,
  outlineVariant = Color(0xFFF1F5F9),
  error = CrimsonRed,
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = BrandBlue,
  onPrimary = Color.White,
  primaryContainer = Color(0xFF1E3A8A),
  onPrimaryContainer = Color(0xFFBFDBFE),
  secondary = EmeraldGreen,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF064E3B),
  onSecondaryContainer = Color(0xFFA7F3D0),
  tertiary = BrandIndigo,
  onTertiary = Color.White,
  background = Color(0xFF0F172A),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF1E293B),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF475569),
  outlineVariant = Color(0xFF334155),
  error = CrimsonRed,
  onError = Color.White
)

@Composable
fun NetDiagTheme(
  darkTheme: Boolean = false, // Default to light modern clean theme matching reference design
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
