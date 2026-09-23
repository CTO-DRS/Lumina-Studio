package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExportCodec
import com.example.data.model.ProjectEntity
import com.example.data.model.VideoResolution
import com.example.engine.ShareExporter
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun Export4KDialog(
  project: ProjectEntity?,
  isExporting: Boolean,
  exportProgress: Float,
  exportComplete: Boolean,
  exportSpeedFps: Float,
  onDismiss: () -> Unit,
  onStartExport: (VideoResolution, Int, ExportCodec, Int) -> Unit,
  onCancelExport: () -> Unit
) {
  val context = LocalContext.current

  var selectedResolution by remember { mutableStateOf(VideoResolution.UHD_4K) }
  var selectedFps by remember { mutableIntStateOf(60) }
  var selectedCodec by remember { mutableStateOf(ExportCodec.HEVC_H265) }
  var bitrateMbps by remember { mutableIntStateOf(60) }

  val estimatedSizeMb = remember(selectedResolution, bitrateMbps, project?.durationMs) {
    ShareExporter.calculateEstimatedFileSizeMb(
      selectedResolution,
      project?.durationMs ?: 30000L,
      bitrateMbps
    )
  }

  Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .testTag("export_4k_dialog"),
      colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ElectricCyan, NeonViolet))),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "تصدير 4K",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "محرك التصدير السينمائي 4K",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "معالجة فائقة السرعة Vulkan (بدون AI)",
                color = ElectricCyan,
                fontSize = 10.sp
              )
            }
          }

          if (!isExporting) {
            IconButton(onClick = onDismiss) {
              Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isExporting || exportComplete) {
          // Live Rendering Progress Screen
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(ObsidianSurfaceElevated)
              .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
              .padding(14.dp)
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              if (exportComplete) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "تم التصدير بنجاح",
                  tint = EmeraldGreen,
                  modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "تم تصدير الفيديو بجودة 4K بنجاح!",
                  color = TextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${selectedResolution.width}x${selectedResolution.height} • $selectedFps FPS • ${selectedCodec.label}",
                  color = TextSecondary,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace
                )
              } else {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(text = "جاري معالجة الإطارات بدقة 4K...", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = "${exportProgress.toInt()}%",
                    color = ElectricCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                  progress = { exportProgress / 100f },
                  modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                  color = ElectricCyan,
                  trackColor = ObsidianSurfaceContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "سرعة المعالجة: ${exportSpeedFps.toInt()} FPS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = "استهلاك الموارد: منخفض 18% CPU",
                    color = EmeraldGreen,
                    fontSize = 10.sp
                  )
                }
              }
            }
          }

          if (exportComplete) {
            Spacer(modifier = Modifier.height(14.dp))

            // Direct Social Media Sharing Buttons
            Text(
              text = "المشاركة المباشرة على وسائل التواصل الاجتماعي:",
              color = TextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                SocialShareButton(
                  title = "Instagram Reels",
                  color = Color(0xFFE1306C),
                  modifier = Modifier.weight(1f),
                  onClick = {
                    ShareExporter.shareExportedMedia(
                      context,
                      project?.title ?: "4K Video",
                      ShareExporter.SocialPlatform.INSTAGRAM,
                      selectedResolution,
                      selectedFps,
                      selectedCodec
                    )
                  }
                )
                SocialShareButton(
                  title = "TikTok 4K",
                  color = Color(0xFF00F0FF),
                  modifier = Modifier.weight(1f),
                  onClick = {
                    ShareExporter.shareExportedMedia(
                      context,
                      project?.title ?: "4K Video",
                      ShareExporter.SocialPlatform.TIKTOK,
                      selectedResolution,
                      selectedFps,
                      selectedCodec
                    )
                  }
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                SocialShareButton(
                  title = "YouTube Shorts",
                  color = Color(0xFFFF0000),
                  modifier = Modifier.weight(1f),
                  onClick = {
                    ShareExporter.shareExportedMedia(
                      context,
                      project?.title ?: "4K Video",
                      ShareExporter.SocialPlatform.YOUTUBE,
                      selectedResolution,
                      selectedFps,
                      selectedCodec
                    )
                  }
                )
                SocialShareButton(
                  title = "WhatsApp HD",
                  color = Color(0xFF25D366),
                  modifier = Modifier.weight(1f),
                  onClick = {
                    ShareExporter.shareExportedMedia(
                      context,
                      project?.title ?: "4K Video",
                      ShareExporter.SocialPlatform.WHATSAPP,
                      selectedResolution,
                      selectedFps,
                      selectedCodec
                    )
                  }
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                SocialShareButton(
                  title = "منصة X (Twitter)",
                  color = Color(0xFF1DA1F2),
                  modifier = Modifier.weight(1f),
                  onClick = {
                    ShareExporter.shareExportedMedia(
                      context,
                      project?.title ?: "4K Video",
                      ShareExporter.SocialPlatform.X_TWITTER,
                      selectedResolution,
                      selectedFps,
                      selectedCodec
                    )
                  }
                )
                SocialShareButton(
                  title = "تيليجرام 4K",
                  color = Color(0xFF0088CC),
                  modifier = Modifier.weight(1f),
                  onClick = {
                    ShareExporter.shareExportedMedia(
                      context,
                      project?.title ?: "4K Video",
                      ShareExporter.SocialPlatform.TELEGRAM,
                      selectedResolution,
                      selectedFps,
                      selectedCodec
                    )
                  }
                )
              }

              Button(
                onClick = {
                  ShareExporter.shareExportedMedia(
                    context,
                    project?.title ?: "4K Video",
                    ShareExporter.SocialPlatform.UNIVERSAL_SHARE,
                    selectedResolution,
                    selectedFps,
                    selectedCodec
                  )
                },
                modifier = Modifier.fillMaxWidth().testTag("universal_share_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = ObsidianSurfaceElevated,
                  contentColor = ElectricCyan
                )
              ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة عامة", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة عبر كافة التطبيقات الأخرى", fontWeight = FontWeight.Bold)
              }
            }
          }
        } else {
          // Pre-Export Settings & Resolution Picker
          Text(text = "دقة العرض وجودة الفيديو:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            VideoResolution.values().forEach { res ->
              val isSel = selectedResolution == res
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSel) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
                  .border(1.dp, if (isSel) ElectricCyan else ObsidianBorder, RoundedCornerShape(8.dp))
                  .clickable {
                    selectedResolution = res
                    bitrateMbps = res.defaultBitrateMbps
                  }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = res.badge,
                    color = if (isSel) ElectricCyan else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(text = "${res.height}p", color = TextMuted, fontSize = 9.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Frame Rate FPS
          Text(text = "معدل الإطارات (Frame Rate):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(60, 30, 24).forEach { fps ->
              val isSel = selectedFps == fps
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSel) NeonViolet.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
                  .border(1.dp, if (isSel) NeonViolet else ObsidianBorder, RoundedCornerShape(8.dp))
                  .clickable { selectedFps = fps }
                  .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (fps == 24) "24 FPS (سينمائي)" else "$fps FPS",
                  color = if (isSel) NeonViolet else TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Codec Selection
          Text(text = "ترميز الفيديو (Codec):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(6.dp))

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ExportCodec.values().forEach { codec ->
              val isSel = selectedCodec == codec
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSel) ElectricCyan.copy(alpha = 0.1f) else ObsidianSurfaceElevated)
                  .border(1.dp, if (isSel) ElectricCyan else ObsidianBorder, RoundedCornerShape(8.dp))
                  .clickable { selectedCodec = codec }
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(text = codec.label, color = if (isSel) ElectricCyan else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  Text(text = codec.description, color = TextMuted, fontSize = 9.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Estimated File Size & Resource Metric Info
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(ObsidianSurfaceElevated)
              .padding(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = "الحجم التقديري: ~$estimatedSizeMb MB", color = CyberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text(text = "محرك التسريع: Vulkan 1.3", color = EmeraldGreen, fontSize = 10.sp)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Start Export Button
          Button(
            onClick = {
              onStartExport(selectedResolution, selectedFps, selectedCodec, bitrateMbps)
            },
            modifier = Modifier.fillMaxWidth().testTag("start_render_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ElectricCyan,
              contentColor = Color.Black
            )
          ) {
            Icon(imageVector = Icons.Default.PlayCircle, contentDescription = "بدء المعالجة والتصدير", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("بدء المعالجة والتصدير فائق الدقة 4K", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }
  }
}

@Composable
private fun SocialShareButton(
  title: String,
  color: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(color.copy(alpha = 0.15f))
      .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(vertical = 8.dp, horizontal = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
