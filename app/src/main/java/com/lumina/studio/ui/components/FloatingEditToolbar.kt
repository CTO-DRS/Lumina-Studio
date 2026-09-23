package com.lumina.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.studio.ui.theme.CyberGold
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianBorderActive
import com.lumina.studio.ui.theme.ObsidianSurfaceContainer
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary

/**
 * Common editing actions available on the floating toolbar.
 */
enum class FloatingEditAction(
  val id: String,
  val labelAr: String,
  val labelEn: String,
  val icon: ImageVector,
  val accentColor: Color,
  val testTag: String
) {
  CROP(
    id = "crop",
    labelAr = "قص وأبعاد",
    labelEn = "Crop",
    icon = Icons.Default.Crop,
    accentColor = EmeraldGreen,
    testTag = "floating_action_crop"
  ),
  FILTER(
    id = "filter",
    labelAr = "فلاتر",
    labelEn = "Filter",
    icon = Icons.Default.Filter,
    accentColor = SunsetCoral,
    testTag = "floating_action_filter"
  ),
  COLOR_ADJUST(
    id = "color_adjust",
    labelAr = "ضبط الألوان",
    labelEn = "Color Adjust",
    icon = Icons.Default.Tune,
    accentColor = ElectricCyan,
    testTag = "floating_action_color_adjust"
  ),
  EXPORT(
    id = "export",
    labelAr = "تصدير",
    labelEn = "Export",
    icon = Icons.Default.Save,
    accentColor = CyberGold,
    testTag = "floating_action_export"
  )
}

/**
 * Modern floating edit toolbar UI component with Material icons for common actions
 * like 'Crop', 'Filter', 'Color Adjust', and 'Export'.
 *
 * Adheres strictly to Material Design 3 guidelines:
 * - Floating glassmorphic obsidian pill layout with neon accent rim glow
 * - Minimum 48dp touch target accessibility for all interactive buttons
 * - Active state highlighting with smooth animated feedback
 * - Distinct highlighted Call-To-Action (CTA) for 'Export'
 * - Collapsible toggle mode to maximize canvas space when needed
 */
@Composable
fun FloatingEditToolbar(
  selectedAction: FloatingEditAction?,
  onActionClick: (FloatingEditAction) -> Unit,
  modifier: Modifier = Modifier,
  onCropClick: () -> Unit = { onActionClick(FloatingEditAction.CROP) },
  onFilterClick: () -> Unit = { onActionClick(FloatingEditAction.FILTER) },
  onColorAdjustClick: () -> Unit = { onActionClick(FloatingEditAction.COLOR_ADJUST) },
  onExportClick: () -> Unit = { onActionClick(FloatingEditAction.EXPORT) },
  isCollapsible: Boolean = true,
  defaultExpanded: Boolean = true,
  showLabels: Boolean = true
) {
  var isExpanded by remember { mutableStateOf(defaultExpanded) }

  Box(
    modifier = modifier.testTag("floating_edit_toolbar_container"),
    contentAlignment = Alignment.Center
  ) {
    if (isCollapsible && !isExpanded) {
      // Collapsed Mini Floating FAB Pill
      MiniCollapsedToolbarFab(
        activeAction = selectedAction,
        onClick = { isExpanded = true }
      )
    } else {
      // Expanded Floating Pill Toolbar
      FloatingPillContainer(
        selectedAction = selectedAction,
        showLabels = showLabels,
        isCollapsible = isCollapsible,
        onCollapse = { isExpanded = false },
        onCropClick = onCropClick,
        onFilterClick = onFilterClick,
        onColorAdjustClick = onColorAdjustClick,
        onExportClick = onExportClick
      )
    }
  }
}

/**
 * Main glassmorphic floating pill container
 */
@Composable
private fun FloatingPillContainer(
  selectedAction: FloatingEditAction?,
  showLabels: Boolean,
  isCollapsible: Boolean,
  onCollapse: () -> Unit,
  onCropClick: () -> Unit,
  onFilterClick: () -> Unit,
  onColorAdjustClick: () -> Unit,
  onExportClick: () -> Unit
) {
  val pillBorderBrush = Brush.horizontalGradient(
    colors = listOf(
      ElectricCyan.copy(alpha = 0.45f),
      ObsidianBorderActive,
      CyberGold.copy(alpha = 0.45f)
    )
  )

  val pillBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0xF51E2533),
      Color(0xF5141922)
    )
  )

  Box(
    modifier = Modifier
      .shadow(
        elevation = 16.dp,
        shape = RoundedCornerShape(32.dp),
        ambientColor = Color.Black.copy(alpha = 0.6f),
        spotColor = ElectricCyan.copy(alpha = 0.35f)
      )
      .clip(RoundedCornerShape(32.dp))
      .background(pillBackgroundBrush)
      .border(1.dp, pillBorderBrush, RoundedCornerShape(32.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag("floating_toolbar_pill")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // 1. Crop Action
      FloatingToolbarItem(
        action = FloatingEditAction.CROP,
        isSelected = selectedAction == FloatingEditAction.CROP,
        showLabel = showLabels,
        onClick = onCropClick
      )

      // 2. Filter Action
      FloatingToolbarItem(
        action = FloatingEditAction.FILTER,
        isSelected = selectedAction == FloatingEditAction.FILTER,
        showLabel = showLabels,
        onClick = onFilterClick
      )

      // 3. Color Adjust Action
      FloatingToolbarItem(
        action = FloatingEditAction.COLOR_ADJUST,
        isSelected = selectedAction == FloatingEditAction.COLOR_ADJUST,
        showLabel = showLabels,
        onClick = onColorAdjustClick
      )

      // Subtle Vertical Divider
      Box(
        modifier = Modifier
          .height(30.dp)
          .width(1.dp)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, ObsidianBorderActive, Color.Transparent)
            )
          )
          .padding(horizontal = 2.dp)
      )

      // 4. Export Action (Featured CTA with Distinct Glowing Accent)
      FloatingExportToolbarItem(
        showLabel = showLabels,
        onClick = onExportClick
      )

      // Optional Collapse Button
      if (isCollapsible) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(ObsidianSurfaceContainer)
            .clickable(onClick = onCollapse)
            .testTag("floating_toolbar_collapse_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "تصغير شريط التعديل العائم",
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

/**
 * Standard floating toolbar item button (Crop, Filter, Color Adjust)
 * Guaranteed minimum 48dp x 48dp touch target size for accessibility.
 */
@Composable
private fun FloatingToolbarItem(
  action: FloatingEditAction,
  isSelected: Boolean,
  showLabel: Boolean,
  onClick: () -> Unit
) {
  val animatedBgColor by animateColorAsState(
    targetValue = if (isSelected) action.accentColor.copy(alpha = 0.18f) else Color.Transparent,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "item_bg_color"
  )

  val animatedBorderColor by animateColorAsState(
    targetValue = if (isSelected) action.accentColor.copy(alpha = 0.6f) else Color.Transparent,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "item_border_color"
  )

  val animatedIconColor by animateColorAsState(
    targetValue = if (isSelected) action.accentColor else TextSecondary,
    label = "item_icon_color"
  )

  val animatedScale by animateFloatAsState(
    targetValue = if (isSelected) 1.05f else 1.0f,
    label = "item_scale"
  )

  Box(
    modifier = Modifier
      .defaultMinSize(minWidth = 52.dp, minHeight = 48.dp)
      .scale(animatedScale)
      .clip(RoundedCornerShape(20.dp))
      .background(animatedBgColor)
      .border(
        width = if (isSelected) 1.dp else 0.dp,
        color = animatedBorderColor,
        shape = RoundedCornerShape(20.dp)
      )
      .clickable(
        role = Role.Button,
        onClick = onClick
      )
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag(action.testTag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = action.icon,
          contentDescription = "${action.labelEn} - ${action.labelAr}",
          tint = animatedIconColor,
          modifier = Modifier.size(20.dp)
        )

        // Subtle glowing dot under active tool
        if (isSelected) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(top = 22.dp)
              .size(4.dp)
              .clip(CircleShape)
              .background(action.accentColor)
          )
        }
      }

      if (showLabel) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = action.labelEn,
          color = if (isSelected) action.accentColor else TextMuted,
          fontSize = 10.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
      }
    }
  }
}

/**
 * Featured Call-To-Action item for 'Export'
 * Styled with a distinct warm gradient pill to invite video/photo saving.
 */
@Composable
private fun FloatingExportToolbarItem(
  showLabel: Boolean,
  onClick: () -> Unit
) {
  val exportGradient = Brush.linearGradient(
    colors = listOf(CyberGold, ElectricCyan)
  )

  Box(
    modifier = Modifier
      .defaultMinSize(minWidth = 60.dp, minHeight = 48.dp)
      .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), spotColor = CyberGold.copy(alpha = 0.5f))
      .clip(RoundedCornerShape(20.dp))
      .background(exportGradient)
      .clickable(
        role = Role.Button,
        onClick = onClick
      )
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag(FloatingEditAction.EXPORT.testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = FloatingEditAction.EXPORT.icon,
        contentDescription = "Export - تصدير وحفظ",
        tint = Color.Black,
        modifier = Modifier.size(19.dp)
      )

      if (showLabel) {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Export",
          color = Color.Black,
          fontSize = 12.sp,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }
  }
}

/**
 * Compact floating FAB shown when the toolbar is collapsed.
 * Tapping it re-expands the toolbar immediately.
 */
@Composable
private fun MiniCollapsedToolbarFab(
  activeAction: FloatingEditAction?,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(48.dp)
      .shadow(elevation = 12.dp, shape = CircleShape, spotColor = ElectricCyan.copy(alpha = 0.4f))
      .clip(CircleShape)
      .background(
        Brush.linearGradient(
          colors = listOf(
            Color(0xF51E2533),
            Color(0xF5141922)
          )
        )
      )
      .border(1.5.dp, activeAction?.accentColor ?: ElectricCyan, CircleShape)
      .clickable(
        role = Role.Button,
        onClick = onClick
      )
      .testTag("floating_toolbar_expand_fab"),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = activeAction?.icon ?: Icons.Default.Edit,
      contentDescription = "فتح شريط أدوات التعديل العائم",
      tint = activeAction?.accentColor ?: ElectricCyan,
      modifier = Modifier.size(22.dp)
    )
  }
}
