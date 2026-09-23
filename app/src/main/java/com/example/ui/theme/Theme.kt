package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioColorScheme = darkColorScheme(
  primary = ElectricCyan,
  onPrimary = ObsidianBg,
  primaryContainer = ObsidianSurfaceElevated,
  onPrimaryContainer = ElectricCyan,
  secondary = NeonViolet,
  onSecondary = ObsidianBg,
  secondaryContainer = ObsidianSurfaceElevated,
  onSecondaryContainer = NeonViolet,
  tertiary = SunsetCoral,
  onTertiary = ObsidianBg,
  background = ObsidianBg,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = ObsidianBorder,
  outlineVariant = ObsidianBorderActive
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Always preserve the high-end 2027 Studio aesthetic
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = StudioColorScheme,
    typography = Typography,
    content = content
  )
}
