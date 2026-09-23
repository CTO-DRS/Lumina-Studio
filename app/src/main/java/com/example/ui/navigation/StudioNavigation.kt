package com.example.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class StudioScreenTab(
  val labelAr: String,
  val labelEn: String,
  val icon: ImageVector,
  val testTag: String
) {
  DASHBOARD("الرئيسية", "Dashboard", Icons.Default.Dashboard, "nav_tab_dashboard"),
  STUDIO("المحرر", "Studio", Icons.Default.MovieFilter, "nav_tab_studio"),
  AI_LAB("المؤثرات", "AI Lab", Icons.Default.AutoAwesome, "nav_tab_ai_lab"),
  GALLERY("المعرض", "Gallery", Icons.Default.PhotoLibrary, "nav_tab_gallery"),
  PROJECTS("المشاريع", "Projects", Icons.Default.VideoLibrary, "nav_tab_projects"),
  SETTINGS("الإعدادات", "Settings", Icons.Default.Settings, "nav_tab_settings")
}

@Composable
fun StudioBottomNavBar(
  currentTab: StudioScreenTab,
  onTabSelect: (StudioScreenTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(ObsidianSurface)
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(ObsidianBorder, Color.Black)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
      )
      .padding(horizontal = 4.dp, vertical = 6.dp)
      .testTag("studio_bottom_nav_bar")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      StudioScreenTab.values().forEach { tab ->
        val isSelected = currentTab == tab
        val scale by animateFloatAsState(targetValue = if (isSelected) 1.05f else 1.0f, label = "tab_scale")
        val iconColor by animateColorAsState(
          targetValue = when {
            isSelected && tab == StudioScreenTab.STUDIO -> CyberGold
            isSelected && tab == StudioScreenTab.AI_LAB -> NeonViolet
            isSelected -> ElectricCyan
            else -> TextMuted
          },
          label = "tab_icon_color"
        )

        Box(
          modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(
              if (isSelected) {
                Brush.verticalGradient(
                  colors = listOf(
                    when (tab) {
                      StudioScreenTab.STUDIO -> CyberGold.copy(alpha = 0.18f)
                      StudioScreenTab.AI_LAB -> NeonViolet.copy(alpha = 0.20f)
                      else -> ElectricCyan.copy(alpha = 0.15f)
                    },
                    Color.Transparent
                  )
                )
              } else {
                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
              }
            )
            .clickable { onTabSelect(tab) }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag(tab.testTag),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = tab.icon,
              contentDescription = tab.labelAr,
              tint = iconColor,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = tab.labelAr,
              color = if (isSelected) TextPrimary else TextMuted,
              fontSize = 9.5.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
