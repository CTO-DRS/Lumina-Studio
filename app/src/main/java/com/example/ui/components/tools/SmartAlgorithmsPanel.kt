package com.example.ui.components.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdjustmentsState
import com.example.data.model.EditorStudioType
import com.example.engine.SmartComputationalEngine
import com.example.ui.theme.AudioTrackColor
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Smart Computational Algorithms Panel (Photo & Video Studio).
 * 100% Deterministic Mathematical & DSP Operations — ZERO Artificial Intelligence / Machine Learning.
 */
@Composable
fun SmartAlgorithmsPanel(
  editorStudioType: EditorStudioType,
  adjustments: AdjustmentsState,
  beatMarkers: List<Long>,
  smartNotice: String?,
  onDismissNotice: () -> Unit,
  onApplyGrayWorldAwb: () -> Unit,
  onApplySmartAutoTone: () -> Unit,
  onApplySmartSharpness: () -> Unit,
  onToggleFocusPeaking: () -> Unit,
  onToggleZebraStripes: () -> Unit,
  onDetectAudioBeats: (Int) -> Unit,
  onClearBeatMarkers: () -> Unit,
  onAutoSplitOnBeats: () -> Unit,
  onApplyGyroStabilization: (Float) -> Unit,
  onApplyAutoHorizonLeveling: () -> Unit,
  onResetHorizonLeveling: () -> Unit,
  onApplySpeedRamp: (SmartComputationalEngine.SpeedRampPreset) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedBpm by remember { mutableIntStateOf(124) }
  var gyroSmoothingSlider by remember(adjustments.gyroSmoothing) {
    mutableStateOf(if (adjustments.gyroSmoothing > 0f) adjustments.gyroSmoothing else 65f)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(ObsidianSurface)
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .verticalScroll(rememberScrollState())
      .testTag("smart_algorithms_panel")
  ) {
    // 1. Algorithmic Pure-Math Guarantee Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(
          Brush.horizontalGradient(
            listOf(CyberGold.copy(alpha = 0.15f), NeonViolet.copy(alpha = 0.12f), ObsidianSurfaceElevated)
          )
        )
        .border(1.dp, CyberGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(CyberGold.copy(alpha = 0.2f))
          .border(1.dp, CyberGold, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.PrecisionManufacturing,
          contentDescription = "خوارزميات حسابية نقية",
          tint = CyberGold,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "خوارزميات حسابية ذكية (بدون ذكاء اصطناعي)",
            color = CyberGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = "معالجة إشارة رقمية رياضية حتمية (Deterministic DSP & Math) - أداء فائق وسرعة فورية",
          color = TextSecondary,
          fontSize = 10.sp
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
        .background(EmeraldGreen.copy(alpha = 0.18f))
        .border(1.dp, EmeraldGreen.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
        .padding(horizontal = 6.dp, vertical = 3.dp)
      ) {
        Text(
          text = "Zero AI • 100% Math",
          color = EmeraldGreen,
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // 2. Live Feedback Notification Banner (Dismissible)
    AnimatedVisibility(
      visible = smartNotice != null,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      if (smartNotice != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ElectricCyan.copy(alpha = 0.12f))
            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.ElectricBolt,
            contentDescription = "إشعار العملية الحسابية",
            tint = ElectricCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = smartNotice,
            color = Color.White,
            fontSize = 10.5.sp,
            modifier = Modifier.weight(1f)
          )
          IconButton(
            onClick = onDismissNotice,
            modifier = Modifier.size(22.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "إغلاق الإشعار",
              tint = TextMuted,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // =========================================================================
    // SECTION A: Photo Computational Algorithms (محرر الصور الفوتوغرافية)
    // =========================================================================
    SmartSectionHeader(
      title = "خوارزميات معالجة الصور الفوتوغرافية (Photo Processing)",
      icon = Icons.Default.CenterFocusStrong,
      color = ElectricCyan
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Grid of Photo Algorithms
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // 1. Gray World Auto White Balance (AWB)
      SmartActionCard(
        title = "توازن البياض الذكي",
        subtitle = "Gray World AWB",
        badge = "D56 5600K",
        accentColor = CyberGold,
        onClick = onApplyGrayWorldAwb,
        testTag = "btn_smart_awb",
        modifier = Modifier.weight(1f)
      )

      // 2. Dynamic Range & Auto-Levels Tone Mapping
      SmartActionCard(
        title = "توسيع النطاق الديناميكي",
        subtitle = "Histogram Equalizer",
        badge = "+2.3 EV",
        accentColor = ElectricCyan,
        onClick = onApplySmartAutoTone,
        testTag = "btn_smart_tone_mapping",
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // 3. Laplacian Unsharp Mask (Micro-Contrast)
      SmartActionCard(
        title = "قناع حدة التفاصيل",
        subtitle = "Laplacian Micro-Contrast",
        badge = "S-Mask 45%",
        accentColor = NeonViolet,
        onClick = onApplySmartSharpness,
        testTag = "btn_smart_unsharp_mask",
        modifier = Modifier.weight(1f)
      )

      // 4. Virtual Inclinometer Auto Horizon
      SmartActionCard(
        title = "تسوية خط الأفق",
        subtitle = "Inclinometer Leveler",
        badge = if (adjustments.horizonTiltDeg != 0f) "${adjustments.horizonTiltDeg}°" else "±0.0°",
        accentColor = EmeraldGreen,
        onClick = {
          if (adjustments.horizonTiltDeg == 0f) onApplyAutoHorizonLeveling() else onResetHorizonLeveling()
        },
        testTag = "btn_smart_horizon",
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Studio Sensor Diagnostics Toggles (Focus Peaking & Zebra 95% IRE)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Focus Peaking Toggle
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("toggle_focus_peaking_row")
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (adjustments.focusPeaking) Color(0xFF00FF66) else TextMuted)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "المحدد البؤري (Focus Peaking)",
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "كشف الحواف الحادة بلون نيون",
            color = TextSecondary,
            fontSize = 9.sp
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
          checked = adjustments.focusPeaking,
          onCheckedChange = { onToggleFocusPeaking() },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFF00FF66),
            uncheckedTrackColor = ObsidianBg
          ),
          modifier = Modifier.testTag("switch_focus_peaking")
        )
      }

      // Zebra 95% IRE Toggle
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("toggle_zebra_row")
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (adjustments.zebraStripes) Color(0xFFFFD700) else TextMuted)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "تحذير التعريض (Zebra 95%)",
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "خطوط تحذير للمناطق المحروقة",
            color = TextSecondary,
            fontSize = 9.sp
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
          checked = adjustments.zebraStripes,
          onCheckedChange = { onToggleZebraStripes() },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFFFFD700),
            uncheckedTrackColor = ObsidianBg
          ),
          modifier = Modifier.testTag("switch_zebra_stripes")
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // =========================================================================
    // SECTION B: Video & Motion Computational Algorithms (محرر الفيديو والمونتاج)
    // =========================================================================
    SmartSectionHeader(
      title = "خوارزميات الفيديو والمونتاج الحركي (Video & Motion DSP)",
      icon = Icons.Default.GraphicEq,
      color = AudioTrackColor
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 1. Audio Beat & Rhythm Transient Detector (تقطيع تلقائي على الإيقاع)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        .padding(10.dp)
        .testTag("smart_beat_detector_panel")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "رصد نبضات الإيقاع الموسيقي (Energy Peaks Transient)",
            color = NeonViolet,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "تحليل مظروف الطاقة الصوتية RMS للتقطيع والمزامنة مع الموسيقى",
            color = TextSecondary,
            fontSize = 9.5.sp
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(NeonViolet.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = if (beatMarkers.isNotEmpty()) "${beatMarkers.size} ضربة مرصودة" else "غير مفعل",
            color = if (beatMarkers.isNotEmpty()) NeonViolet else TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // BPM Selector Pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "إيقاع BPM:",
          color = TextSecondary,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )

        listOf(110, 120, 124, 128, 140).forEach { bpm ->
          val isSelected = selectedBpm == bpm
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (isSelected) NeonViolet else ObsidianBg)
              .border(1.dp, if (isSelected) NeonViolet else ObsidianBorder, RoundedCornerShape(4.dp))
              .clickable { selectedBpm = bpm }
              .padding(horizontal = 8.dp, vertical = 3.dp)
              .testTag("bpm_preset_$bpm")
          ) {
            Text(
              text = "$bpm",
              color = if (isSelected) Color.White else TextMuted,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Action Buttons: Detect Beats, Auto-Split on Beats, Clear
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Detect Beats Button
        ElevatedButton(
          onClick = { onDetectAudioBeats(selectedBpm) },
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = NeonViolet.copy(alpha = 0.25f),
            contentColor = NeonViolet
          ),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_detect_beats")
        ) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("رصد الإيقاع", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        // Auto-Split on Beats Button
        ElevatedButton(
          onClick = onAutoSplitOnBeats,
          colors = ButtonDefaults.elevatedButtonColors(
            containerColor = CyberGold.copy(alpha = 0.25f),
            contentColor = CyberGold
          ),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .weight(1.3f)
            .testTag("btn_auto_split_beats")
        ) {
          Icon(
            imageVector = Icons.Default.ElectricBolt,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("تقطيع تلقائي على الإيقاع", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        if (beatMarkers.isNotEmpty()) {
          ElevatedButton(
            onClick = onClearBeatMarkers,
            colors = ButtonDefaults.elevatedButtonColors(
              containerColor = ObsidianBg,
              contentColor = SunsetCoral
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.testTag("btn_clear_beats")
          ) {
            Text("مسح", fontSize = 10.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Gyroscope Camera Motion Stabilizer (EMA Smoothing)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        .padding(10.dp)
        .testTag("smart_gyro_stabilizer_panel")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "تثبيت الاهتزاز الجيروسكوبي (Exponential Moving Average)",
            color = SunsetCoral,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "فلترة متجهات الحركة لتقليل اهتزازات اليد والكاميرا",
            color = TextSecondary,
            fontSize = 9.5.sp
          )
        }

        val jitterReduced = (45f + (gyroSmoothingSlider / 100f) * 42f).roundToInt()
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SunsetCoral.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "كبح الاهتزاز $jitterReduced%",
            color = SunsetCoral,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Vibration,
          contentDescription = null,
          tint = SunsetCoral,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Slider(
          value = gyroSmoothingSlider,
          onValueChange = {
            gyroSmoothingSlider = it
            onApplyGyroStabilization(it)
          },
          valueRange = 0f..100f,
          modifier = Modifier
            .weight(1f)
            .height(28.dp)
            .testTag("slider_gyro_stabilization"),
          colors = SliderDefaults.colors(
            thumbColor = SunsetCoral,
            activeTrackColor = SunsetCoral,
            inactiveTrackColor = ObsidianBorder
          )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${gyroSmoothingSlider.toInt()}%",
          color = Color.White,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.width(32.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Bézier Speed Ramping Curves (منحنيات تسريع بيزييه)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        .padding(10.dp)
        .testTag("smart_speed_ramping_panel")
    ) {
      Text(
        text = "منحنيات تسريع بيزييه (Cubic Bézier Time-Remapping)",
        color = CyberGold,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "إعادة تشكيل السرعة بسلاسة رياضية دون تقطيع في الإطارات",
        color = TextSecondary,
        fontSize = 9.5.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        SmartComputationalEngine.availableSpeedRamps.forEach { preset ->
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(ObsidianBg)
              .border(1.dp, CyberGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
              .clickable { onApplySpeedRamp(preset) }
              .padding(8.dp)
              .testTag("speed_ramp_${preset.id}")
          ) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = preset.titleAr.split("(").firstOrNull()?.trim() ?: preset.titleAr,
                  color = CyberGold,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
                Icon(
                  imageVector = Icons.Default.Speed,
                  contentDescription = null,
                  tint = CyberGold,
                  modifier = Modifier.size(12.dp)
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = preset.descriptionAr,
                color = TextSecondary,
                fontSize = 8.5.sp,
                lineHeight = 11.sp,
                maxLines = 2
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SmartSectionHeader(
  title: String,
  icon: ImageVector,
  color: Color
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 2.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = color,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = title,
      color = color,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun SmartActionCard(
  title: String,
  subtitle: String,
  badge: String,
  accentColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(ObsidianSurfaceElevated)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(10.dp)
      .testTag(testTag)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          color = TextPrimary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(accentColor.copy(alpha = 0.2f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Text(
            text = badge,
            color = accentColor,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = subtitle,
        color = TextSecondary,
        fontSize = 9.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Memory,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(12.dp)
        )
        Text(
          text = "تطبيق فوري (100% DSP)",
          color = accentColor,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
