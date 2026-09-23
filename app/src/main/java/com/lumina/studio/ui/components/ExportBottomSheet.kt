package com.lumina.studio.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lumina.studio.data.model.ExportCodec
import com.lumina.studio.data.model.ProjectEntity
import com.lumina.studio.data.model.VideoResolution
import com.lumina.studio.engine.export.ShareExporter
import com.lumina.studio.ui.theme.CyberGold
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.NeonViolet
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianBorderActive
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceContainer
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary

/**
 * Format specification for video export
 */
enum class ExportVideoFormat(
  val id: String,
  val label: String,
  val extension: String,
  val codec: ExportCodec,
  val badge: String,
  val description: String,
  val sizeEfficiencyMultiplier: Float
) {
  MP4(
    id = "MP4",
    label = "MP4 (H.264 / AVC)",
    extension = ".mp4",
    codec = ExportCodec.AVC_H264,
    badge = "الأوسع توافقاً",
    description = "متوافق مع 100% من الهواتف والحواسيب وشبكات التواصل",
    sizeEfficiencyMultiplier = 1.0f
  ),
  HEVC(
    id = "HEVC",
    label = "HEVC (H.265)",
    extension = ".mp4",
    codec = ExportCodec.HEVC_H265,
    badge = "توفير 50% مساحة",
    description = "ترميز عالي الكفاءة ينتج نصف الحجم بنفس الجودة السينمائية",
    sizeEfficiencyMultiplier = 0.55f
  )
}

/**
 * Resolution items specifically required for export selection: 4K, 1080p, 720p
 */
enum class ExportResolutionOption(
  val resolution: VideoResolution,
  val title: String,
  val dimensionsText: String,
  val badge: String,
  val targetPlatform: String,
  val accentColor: Color,
  val testTag: String
) {
  RES_4K(
    resolution = VideoResolution.UHD_4K,
    title = "4K Ultra HD",
    dimensionsText = "3840 × 2160",
    badge = "4K UHD",
    targetPlatform = "شاشات العرض الكبيرة والإنتاج السينمائي",
    accentColor = CyberGold,
    testTag = "resolution_option_4k"
  ),
  RES_1080P(
    resolution = VideoResolution.FHD_1080P,
    title = "1080p Full HD",
    dimensionsText = "1920 × 1080",
    badge = "1080p FHD",
    targetPlatform = "المثالية لـ YouTube و Reels و TikTok",
    accentColor = ElectricCyan,
    testTag = "resolution_option_1080p"
  ),
  RES_720P(
    resolution = VideoResolution.HD_720P,
    title = "720p HD",
    dimensionsText = "1280 × 720",
    badge = "720p HD",
    targetPlatform = "حجم اقتصادي للمشاركة السريعة عبر الواتساب",
    accentColor = EmeraldGreen,
    testTag = "resolution_option_720p"
  )
}

/**
 * Bottom-sheet UI component for selecting export resolution (4K, 1080p, 720p)
 * and format (MP4, HEVC) before saving the edited video.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
  project: ProjectEntity?,
  isExporting: Boolean,
  exportProgress: Float,
  exportComplete: Boolean,
  exportSpeedFps: Float,
  exportStage: String = "",
  exportResultPath: String? = null,
  exportError: String? = null,
  exportedFile: java.io.File? = null,
  onDismiss: () -> Unit,
  onStartExport: (VideoResolution, Int, ExportCodec, Int) -> Unit,
  onCancelExport: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedResolutionOption by remember { mutableStateOf(ExportResolutionOption.RES_4K) }
  var selectedFormat by remember { mutableStateOf(ExportVideoFormat.HEVC) }
  var selectedFps by remember { mutableIntStateOf(60) }

  val baseBitrate = selectedResolutionOption.resolution.defaultBitrateMbps
  val effectiveBitrate = remember(baseBitrate, selectedFormat) {
    if (selectedFormat == ExportVideoFormat.HEVC) {
      (baseBitrate * 0.65f).toInt().coerceAtLeast(6)
    } else {
      baseBitrate
    }
  }

  val totalDurationMs = project?.durationMs ?: 30000L
  val estimatedSizeMb = remember(selectedResolutionOption, effectiveBitrate, totalDurationMs, selectedFormat) {
    val rawMb = ShareExporter.calculateEstimatedFileSizeMb(
      selectedResolutionOption.resolution,
      totalDurationMs,
      effectiveBitrate
    )
    val finalMb = rawMb * selectedFormat.sizeEfficiencyMultiplier
    (Math.round(finalMb * 10f) / 10f).coerceAtLeast(1.2f)
  }

  ModalBottomSheet(
    onDismissRequest = {
      if (!isExporting) onDismiss()
    },
    sheetState = sheetState,
    containerColor = ObsidianSurface,
    scrimColor = Color.Black.copy(alpha = 0.7f),
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    dragHandle = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .width(44.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(ObsidianBorderActive)
        )
      }
    },
    modifier = Modifier.testTag("export_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp)
    ) {
      // Header: Title & Close Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(ElectricCyan, CyberGold))),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = "حفظ وتصدير الفيديو",
              tint = Color.Black,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "حفظ وتصدير الفيديو",
              color = TextPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "حدد دقة العرض وتنسيق الفيديو قبل الحفظ على الجهاز",
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        }

        if (!isExporting) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_export_sheet_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "إغلاق",
              tint = TextMuted
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Current Video Info Pill
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.VideoFile,
              contentDescription = null,
              tint = ElectricCyan,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = project?.title ?: "مشروع الفيديو المعدل",
              color = TextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Text(
            text = "المدة: ${totalDurationMs / 1000} ثانية",
            color = CyberGold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      if (isExporting || exportComplete || exportError != null) {
        // Active Rendering / Success Save View
        ExportProgressSection(
          isExporting = isExporting,
          exportComplete = exportComplete,
          exportProgress = exportProgress,
          exportSpeedFps = exportSpeedFps,
          exportStage = exportStage,
          exportResultPath = exportResultPath,
          exportError = exportError,
          exportedFile = exportedFile,
          resolution = selectedResolutionOption.resolution,
          fps = selectedFps,
          codec = selectedFormat.codec,
          projectTitle = project?.title ?: "فيديو معدل",
          context = context,
          onCancelExport = onCancelExport,
          onDismiss = onDismiss
        )
      } else {
        // SECTION 1: RESOLUTION SELECTION (4K, 1080p, 720p)
        Text(
          text = "1. دقة التصدير وجودة الفيديو:",
          color = TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "اختر الدقة المناسبة لشاشتك أو لمنصة النشر المستهدفة",
          color = TextMuted,
          fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ExportResolutionOption.values().forEach { option ->
            val isSelected = selectedResolutionOption == option
            ResolutionOptionCard(
              option = option,
              isSelected = isSelected,
              onClick = { selectedResolutionOption = option }
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 2: FORMAT SELECTION (MP4, HEVC)
        Text(
          text = "2. صيغة وتنسيق الفيديو (Format):",
          color = TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "اختر التنسيق الأمثل للتوافق أو لتوفير مساحة التخزين",
          color = TextMuted,
          fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ExportVideoFormat.values().forEach { format ->
            val isSelected = selectedFormat == format
            FormatOptionCard(
              format = format,
              isSelected = isSelected,
              modifier = Modifier.weight(1f),
              onClick = { selectedFormat = format }
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: FRAME RATE (FPS)
        Text(
          text = "3. معدل الإطارات (Frame Rate):",
          color = TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            Triple(60, "60 FPS", "سلاسة فائقة"),
            Triple(30, "30 FPS", "قياسي للويب"),
            Triple(24, "24 FPS", "سينمائي")
          ).forEach { (fps, label, desc) ->
            val isSelected = selectedFps == fps
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) NeonViolet.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
                .border(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) NeonViolet else ObsidianBorder,
                  shape = RoundedCornerShape(10.dp)
                )
                .clickable { selectedFps = fps }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = label,
                  color = if (isSelected) NeonViolet else TextPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = desc,
                  color = TextMuted,
                  fontSize = 9.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 4: PRE-SAVE METRICS & SPEC SUMMARY
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, selectedResolutionOption.accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Storage,
                  contentDescription = null,
                  tint = CyberGold,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "الحجم التقديري قبل الحفظ:",
                  color = TextSecondary,
                  fontSize = 11.sp
                )
              }
              Text(
                text = "~$estimatedSizeMb MB",
                color = CyberGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.HighQuality,
                  contentDescription = null,
                  tint = selectedResolutionOption.accentColor,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "المواصفات المختارة:",
                  color = TextSecondary,
                  fontSize = 11.sp
                )
              }
              Text(
                text = "${selectedResolutionOption.title} • ${selectedFormat.id} • $selectedFps FPS",
                color = TextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
              )
            }

            if (selectedFormat == ExportVideoFormat.HEVC) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(NeonViolet.copy(alpha = 0.12f))
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "✨ تم تطبيق ضغط HEVC عالي الكفاءة: تم توفير ~45% من مساحة الملف مع الحفاظ على الدقة السينمائية الكاملة.",
                  color = NeonViolet,
                  fontSize = 10.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons: Save Video & Cancel
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("cancel_export_button"),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = TextSecondary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
          ) {
            Text("إلغاء", fontSize = 13.sp)
          }

          Button(
            onClick = {
              onStartExport(
                selectedResolutionOption.resolution,
                selectedFps,
                selectedFormat.codec,
                effectiveBitrate
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(2f)
              .height(50.dp)
              .testTag("save_export_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = selectedResolutionOption.accentColor,
              contentColor = Color.Black
            )
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = "حفظ وتصدير الفيديو",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "حفظ وتصدير الفيديو",
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }
    }
  }
}

/**
 * Card for selecting resolution (4K, 1080p, 720p)
 */
@Composable
private fun ResolutionOptionCard(
  option: ExportResolutionOption,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) option.accentColor.copy(alpha = 0.12f) else ObsidianSurfaceElevated)
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = if (isSelected) option.accentColor else ObsidianBorder,
        shape = RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag(option.testTag)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Radio / Selection Indicator
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isSelected) option.accentColor else ObsidianSurfaceContainer)
            .border(1.dp, if (isSelected) option.accentColor else ObsidianBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (isSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = option.title,
              color = if (isSelected) option.accentColor else TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(option.accentColor.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = option.dimensionsText,
                color = option.accentColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = option.targetPlatform,
            color = TextMuted,
            fontSize = 10.sp
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(ObsidianSurfaceContainer)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "${option.resolution.defaultBitrateMbps} Mbps",
          color = TextSecondary,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

/**
 * Card for selecting video format (MP4 vs HEVC)
 */
@Composable
private fun FormatOptionCard(
  format: ExportVideoFormat,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val accentColor = if (format == ExportVideoFormat.HEVC) NeonViolet else ElectricCyan
  val testTag = if (format == ExportVideoFormat.HEVC) "format_option_hevc" else "format_option_mp4"

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) accentColor.copy(alpha = 0.12f) else ObsidianSurfaceElevated)
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = if (isSelected) accentColor else ObsidianBorder,
        shape = RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .padding(12.dp)
      .testTag(testTag)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = format.id,
          color = if (isSelected) accentColor else TextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.ExtraBold
        )

        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(if (isSelected) accentColor else ObsidianSurfaceContainer)
            .border(1.dp, if (isSelected) accentColor else ObsidianBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (isSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(accentColor.copy(alpha = 0.18f))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = format.badge,
          color = accentColor,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = format.description,
        color = TextMuted,
        fontSize = 10.sp,
        lineHeight = 14.sp
      )
    }
  }
}

/**
 * Progress and completion view shown inside the bottom sheet
 */
@Composable
private fun ExportProgressSection(
  isExporting: Boolean,
  exportComplete: Boolean,
  exportProgress: Float,
  exportSpeedFps: Float,
  exportStage: String,
  exportResultPath: String?,
  exportError: String?,
  exportedFile: java.io.File?,
  resolution: VideoResolution,
  fps: Int,
  codec: ExportCodec,
  projectTitle: String,
  context: Context,
  onCancelExport: () -> Unit,
  onDismiss: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(ObsidianSurfaceElevated)
      .border(1.dp, if (exportComplete) EmeraldGreen else ElectricCyan, RoundedCornerShape(16.dp))
      .padding(16.dp)
      .testTag("export_progress_section")
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      if (exportError != null && !exportComplete) {
        // REAL export failure surface
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "فشل التصدير",
          tint = SunsetCoral,
          modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "فشل التصدير",
          color = TextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = exportError,
          color = SunsetCoral,
          fontSize = 11.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceContainer, contentColor = TextPrimary),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("العودة للمحرر", fontWeight = FontWeight.Bold)
        }
      } else if (exportComplete) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "تم الحفظ بنجاح",
          tint = EmeraldGreen,
          modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "تم إنتاج ملف MP4 حقيقي وحفظه في المعرض!",
          color = TextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${resolution.label} • $fps FPS • ${codec.label}",
          color = TextSecondary,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = exportResultPath ?: "المسار: Movies/LuminaStudio/",
          color = EmeraldGreen,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Direct Social Sharing — shares the REAL exported file
        Text(
          text = "مشاركة الملف المُصدّر فوراً:",
          color = TextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ShareMiniButton(
            title = "Instagram Reels",
            color = Color(0xFFE1306C),
            modifier = Modifier.weight(1f),
            onClick = {
              exportedFile?.let {
                ShareExporter.shareExportedFile(
                  context, it, true, projectTitle,
                  ShareExporter.SocialPlatform.INSTAGRAM
                )
              }
            }
          )

          ShareMiniButton(
            title = "TikTok 4K",
            color = Color(0xFF00F0FF),
            modifier = Modifier.weight(1f),
            onClick = {
              exportedFile?.let {
                ShareExporter.shareExportedFile(
                  context, it, true, projectTitle,
                  ShareExporter.SocialPlatform.TIKTOK
                )
              }
            }
          )

          ShareMiniButton(
            title = "YouTube Shorts",
            color = Color(0xFFFF0000),
            modifier = Modifier.weight(1f),
            onClick = {
              exportedFile?.let {
                ShareExporter.shareExportedFile(
                  context, it, true, projectTitle,
                  ShareExporter.SocialPlatform.YOUTUBE
                )
              }
            }
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldGreen,
            contentColor = Color.Black
          ),
          modifier = Modifier.fillMaxWidth().testTag("export_done_button")
        ) {
          Text("تم الانتهاء والعودة للمحرر", fontWeight = FontWeight.Bold)
        }
      } else {
        // Export in progress (REAL MediaCodec pipeline)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = exportStage.ifBlank { "جارٍ معالجة وتشفير الفيديو..." },
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${exportProgress.toInt()}%",
            color = ElectricCyan,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
          progress = { exportProgress / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .testTag("export_progress_bar"),
          color = ElectricCyan,
          trackColor = ObsidianSurfaceContainer
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = ElectricCyan,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "سرعة الترميز: ${exportSpeedFps.toInt()} إطار/ث",
              color = TextSecondary,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          Text(
            text = "المحرك: MediaCodec الأجهزة",
            color = EmeraldGreen,
            fontSize = 10.sp
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
          onClick = onCancelExport,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cancel_in_progress_export_button"),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SunsetCoral
          ),
          border = androidx.compose.foundation.BorderStroke(1.dp, SunsetCoral.copy(alpha = 0.5f))
        ) {
          Text("إلغاء المعالجة والتصدير", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun ShareMiniButton(
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
      .padding(vertical = 8.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      color = Color.White,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
