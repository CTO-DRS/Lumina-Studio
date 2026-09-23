package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutoSaveStatus
import com.example.data.model.EditorMode
import com.example.data.model.ProjectEntity
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TopStudioBar(
  currentProject: ProjectEntity?,
  editorMode: EditorMode,
  isCompareActive: Boolean,
  onOpenProjects: () -> Unit,
  onToggleMode: () -> Unit,
  onToggleCompare: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onOpenCamera: () -> Unit,
  onExportClick: () -> Unit,
  onOpenImport: () -> Unit = {},
  canUndo: Boolean = false,
  canRedo: Boolean = false,
  undoCount: Int = 0,
  redoCount: Int = 0,
  autoSaveStatus: AutoSaveStatus = AutoSaveStatus.Idle,
  onSaveNow: () -> Unit = {}
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(ObsidianSurface)
      .drawBehind {
        drawLine(
          color = ObsidianBorder,
          start = Offset(0f, size.height),
          end = Offset(size.width, size.height),
          strokeWidth = 1.dp.toPx()
        )
      }
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    // Row 1: Title, Cloud Status, Mode Switcher, Export 4K
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Project Selector & Badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
          .clickable { onOpenProjects() }
          .padding(horizontal = 8.dp, vertical = 6.dp)
          .testTag("project_picker_button")
      ) {
        Icon(
          imageVector = Icons.Default.FolderOpen,
          contentDescription = "قائمة المشاريع",
          tint = ElectricCyan,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = currentProject?.title ?: "مشروع جديد",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(130.dp)
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF0284C7).copy(alpha = 0.35f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
              Text(
                text = currentProject?.resolution?.replace("UHD_", "") ?: "4K",
                color = ElectricCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
              )
            }
            if (currentProject?.fps ?: 0 > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${currentProject?.fps}fps",
                color = TextMuted,
                fontSize = 9.sp
              )
            }
          }
        }
      }

      // Mode Switcher: Beginner vs Pro
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
          .clickable { onToggleMode() }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("mode_toggle_button")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (editorMode == EditorMode.PRO) "⚡ محترف" else "🔰 سهل",
            color = if (editorMode == EditorMode.PRO) NeonViolet else ElectricCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Export 4K Action Button
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(
            Brush.horizontalGradient(
              colors = listOf(ElectricCyan, Color(0xFF0099FF))
            )
          )
          .clickable { onExportClick() }
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("export_4k_button")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "تصدير 4K",
            color = Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
          )
        }
      }
    }

    // Row 2: Secondary Quick Actions: Compare, Undo, Redo, Mode Label
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Compare Before / After Split
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCompareActive) ElectricCyan.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onToggleCompare() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("compare_slider_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Compare,
              contentDescription = "مقارنة قبل وبعد",
              tint = if (isCompareActive) ElectricCyan else TextSecondary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isCompareActive) "المقارنة مفعلة" else "مقارنة قبل/بعد",
              color = if (isCompareActive) ElectricCyan else TextSecondary,
              fontSize = 11.sp
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Import Media from Device Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(EmeraldGreen.copy(alpha = 0.2f))
            .border(1.dp, EmeraldGreen.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .clickable { onOpenImport() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("open_import_media_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.VideoLibrary,
              contentDescription = "استيراد وسائط من الهاتف",
              tint = EmeraldGreen,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "استيراد من الهاتف",
              color = EmeraldGreen,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Direct CameraX Capture Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NeonViolet.copy(alpha = 0.2f))
            .border(1.dp, NeonViolet.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable { onOpenCamera() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("open_camera_bar_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.PhotoCamera,
              contentDescription = "تصوير كاميرا 4K",
              tint = NeonViolet,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "تصوير كاميرا 4K",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.width(6.dp))

        // Room Database Auto-Save Status Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ObsidianSurfaceElevated)
            .border(
              1.dp,
              when (autoSaveStatus) {
                is AutoSaveStatus.Saving -> ElectricCyan.copy(alpha = 0.5f)
                is AutoSaveStatus.Saved -> EmeraldGreen.copy(alpha = 0.4f)
                is AutoSaveStatus.Error -> SunsetCoral.copy(alpha = 0.4f)
                is AutoSaveStatus.Idle -> ObsidianBorder
              },
              RoundedCornerShape(6.dp)
            )
            .clickable { onSaveNow() }
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .testTag("auto_save_status_badge")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            when (autoSaveStatus) {
              is AutoSaveStatus.Saving -> {
                CircularProgressIndicator(
                  modifier = Modifier.size(11.dp),
                  strokeWidth = 1.5.dp,
                  color = ElectricCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "حفظ Room...",
                  color = ElectricCyan,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium
                )
              }
              is AutoSaveStatus.Saved -> {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "تم الحفظ تلقائياً في Room",
                  tint = EmeraldGreen,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Room تلقائي ✓",
                  color = EmeraldGreen,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              is AutoSaveStatus.Error -> {
                Icon(
                  imageVector = Icons.Default.Save,
                  contentDescription = "خطأ بالحفظ",
                  tint = SunsetCoral,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "إعادة حفظ",
                  color = SunsetCoral,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              is AutoSaveStatus.Idle -> {
                Icon(
                  imageVector = Icons.Default.Save,
                  contentDescription = "حفظ الآن",
                  tint = TextSecondary,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Room",
                  color = TextSecondary,
                  fontSize = 10.sp
                )
              }
            }
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        IconButton(
          onClick = onUndo,
          enabled = canUndo,
          modifier = Modifier.size(32.dp).testTag("undo_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = if (canUndo) "تراجع ($undoCount)" else "تراجع",
            tint = if (canUndo) ElectricCyan else TextMuted.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp)
          )
        }
        IconButton(
          onClick = onRedo,
          enabled = canRedo,
          modifier = Modifier.size(32.dp).testTag("redo_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Redo,
            contentDescription = if (canRedo) "إعادة ($redoCount)" else "إعادة",
            tint = if (canRedo) NeonViolet else TextMuted.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
