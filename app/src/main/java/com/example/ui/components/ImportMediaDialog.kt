package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ImportMediaDialog(
  onDismiss: () -> Unit,
  onMediaSelected: (uri: Uri, isVideo: Boolean) -> Unit
) {
  // Real Android Photo Picker for Videos
  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      onMediaSelected(uri, true)
      onDismiss()
    }
  }

  // Real Android Photo Picker for Photos
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      onMediaSelected(uri, false)
      onDismiss()
    }
  }

  // Fallback generic media picker (Files)
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      // Determine if video or photo from URI string or default to video
      val isVideo = uri.toString().contains("video", ignoreCase = true) ||
          uri.toString().endsWith(".mp4", ignoreCase = true) ||
          uri.toString().endsWith(".mov", ignoreCase = true) ||
          uri.toString().endsWith(".mkv", ignoreCase = true)
      onMediaSelected(uri, isVideo)
      onDismiss()
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(ObsidianSurface)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
        .padding(16.dp)
        .testTag("import_media_dialog")
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "استيراد وسائط حقيقية من الهاتف",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
          )
          Text(
            text = "Real Android Photo Picker & File Storage",
            color = ElectricCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(28.dp).testTag("close_import_dialog_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "إغلاق",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Option 1: Import Video from Gallery
      ImportOptionCard(
        title = "استيراد فيديو من المعرض",
        subtitle = "يدعم مقاطع 4K Ultra HD، 60fps، ومقاطع Reels/Shorts",
        badge = "محرر الفيديو 🎬",
        badgeColor = CyberGold,
        icon = Icons.Default.VideoLibrary,
        iconTint = CyberGold,
        testTag = "import_video_option_button",
        onClick = {
          videoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
          )
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Option 2: Import Photo from Gallery
      ImportOptionCard(
        title = "استيراد صورة من المعرض",
        subtitle = "يدعم صور الكاميرا الخام عالية الدقة وصور البورتريه",
        badge = "محرر الصور 📸",
        badgeColor = NeonViolet,
        icon = Icons.Default.PhotoLibrary,
        iconTint = NeonViolet,
        testTag = "import_photo_option_button",
        onClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Option 3: Browse phone internal storage
      ImportOptionCard(
        title = "تصفح ملفات وحدة التخزين",
        subtitle = "استيراد ملفات وسائط من مجلد التحميلات أو بطاقة الذاكرة",
        badge = "تخزين محلي 📁",
        badgeColor = EmeraldGreen,
        icon = Icons.Default.FolderOpen,
        iconTint = EmeraldGreen,
        testTag = "import_files_option_button",
        onClick = {
          filePickerLauncher.launch("*/*")
        }
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Footer note
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(ObsidianBg)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
          .padding(8.dp)
      ) {
        Text(
          text = "✓ استيراد حقيقي بنسبة 100% دون أي محاكاة. يتم قراءة معدل الإطارات والأبعاد والمدة تلقائياً وحفظ المشروع محلياً في قاعدة بيانات Room.",
          color = TextMuted,
          fontSize = 9.sp,
          lineHeight = 13.sp
        )
      }
    }
  }
}

@Composable
private fun ImportOptionCard(
  title: String,
  subtitle: String,
  badge: String,
  badgeColor: Color,
  icon: ImageVector,
  iconTint: Color,
  testTag: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(ObsidianSurfaceElevated)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(12.dp)
      .testTag(testTag)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(iconTint.copy(alpha = 0.15f))
          .border(1.dp, iconTint.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconTint,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = title,
            color = TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(badgeColor.copy(alpha = 0.2f))
              .padding(horizontal = 5.dp, vertical = 2.dp)
          ) {
            Text(
              text = badge,
              color = badgeColor,
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          color = TextMuted,
          fontSize = 9.5.sp,
          lineHeight = 13.sp
        )
      }
    }
  }
}
