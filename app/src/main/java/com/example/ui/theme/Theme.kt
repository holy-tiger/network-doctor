package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = CyanNeon,
  onPrimary = Color(0xFF00373D),
  primaryContainer = Color(0xFF004F56),
  onPrimaryContainer = Color(0xFF70F5FF),
  secondary = MintNeon,
  onSecondary = Color(0xFF00391A),
  secondaryContainer = Color(0xFF005328),
  onSecondaryContainer = Color(0xFF7CFFAC),
  tertiary = ElectricBlue,
  onTertiary = Color(0xFF002B73),
  background = TechNavyBg,
  onBackground = TextPrimary,
  surface = TechNavySurface,
  onSurface = TextPrimary,
  surfaceVariant = TechNavySurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = TechNavyBorder,
  outlineVariant = Color(0xFF1E2A47),
  error = StatusError,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF006874),
  onPrimary = Color.White,
  primaryContainer = Color(0xFF9EEFFD),
  onPrimaryContainer = Color(0xFF001F24),
  secondary = Color(0xFF006D38),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF9AF6B2),
  onSecondaryContainer = Color(0xFF00210C),
  tertiary = Color(0xFF0056D2),
  onTertiary = Color.White,
  background = Color(0xFFF8FAFC),
  onBackground = Color(0xFF0F172A),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFE2E8F0),
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
  outlineVariant = Color(0xFFE2E8F0),
  error = Color(0xFFBA1A1A),
  onError = Color.White
)

@Composable
fun NetDiagTheme(
  darkTheme: Boolean = true, // Network diagnostics looks best in focused dark cyber theme by default
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
