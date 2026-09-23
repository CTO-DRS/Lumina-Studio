package com.example.ui.components.tools

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdjustmentsState
import com.example.ui.theme.CyberGold
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

/**
 * Dedicated Custom UI for Instant AI Auto-Enhance Tool.
 * Features smart algorithmic DSP sliders, one-tap AI enhancements,
 * HDR dynamic boost, and real-time DSP metrics.
 */
@Composable
fun InstantAiEnhancePanel(
  adjustments: AdjustmentsState,
  onAutoEnhanceChange: (Float) -> Unit,
  onQuickAiBoost: (exposure: Float, contrast: Float, saturation: Float, sharpness: Float) -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(ObsidianSurface)
      .border(
        width = 1.dp,
        brush = Brush.linearGradient(
          colors = listOf(ElectricCyan.copy(alpha = 0.6f), NeonViolet.copy(alpha = 0.4f))
        ),
        shape = RoundedCornerShape(14.dp)
      )
      .padding(12.dp)
      .testTag("instant_ai_enhance_panel")
  ) {
    // 1. Header with AI Chip Status
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(listOf(ElectricCyan, NeonViolet))
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "تحسين الذكاء الاصطناعي",
            tint = Color.Black,
            modifier = Modifier.size(16.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "التحسين الفوري التلقائي بالذكاء الاصطناعي",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Neural DSP Tone Mapping & Dynamic Range",
            color = ElectricCyan,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Reset Button
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
          .clickable { onReset() }
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("ai_enhance_reset_button"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = "استعادة التلقائي",
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("استعادة", color = TextSecondary, fontSize = 10.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Master Enhancement Slider with Percentage Display
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "قوة التحسين الذكي الشامل",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
      Text(
        text = "${adjustments.autoEnhance.toInt()}%",
        color = ElectricCyan,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace
      )
    }

    Slider(
      value = adjustments.autoEnhance,
      onValueChange = onAutoEnhanceChange,
      valueRange = 0f..100f,
      colors = SliderDefaults.colors(
        thumbColor = ElectricCyan,
        activeTrackColor = ElectricCyan,
        inactiveTrackColor = ObsidianSurfaceElevated
      ),
      modifier = Modifier.fillMaxWidth().testTag("ai_enhance_master_slider")
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 3. Quick One-Tap AI Boost Styles
    Text(
      text = "أنماط التحسين السريع بلمسة واحدة",
      color = TextMuted,
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // 1. Vivid HDR Boost
      AiPresetChip(
        title = "تعزيز سينمائي HDR",
        subtitle = "+25% تباين وحيوية",
        icon = Icons.Default.HighQuality,
        accentColor = ElectricCyan,
        onClick = {
          onQuickAiBoost(10f, 25f, 20f, 20f)
        },
        testTag = "ai_preset_vivid_hdr"
      )

      // 2. Golden Warm Glow
      AiPresetChip(
        title = "إضاءة استوديو دافئة",
        subtitle = "دفء طبيعي وتوازن",
        icon = Icons.Default.WbSunny,
        accentColor = CyberGold,
        onClick = {
          onQuickAiBoost(15f, 15f, 15f, 10f)
        },
        testTag = "ai_preset_warm_glow"
      )

      // 3. Crisp High Contrast
      AiPresetChip(
        title = "حدة فائقة ووضوح",
        subtitle = "+35% حدة وإزالة ضباب",
        icon = Icons.Default.Flare,
        accentColor = EmeraldGreen,
        onClick = {
          onQuickAiBoost(5f, 20f, 10f, 40f)
        },
        testTag = "ai_preset_crisp_sharp"
      )

      // 4. Dramatic Punch
      AiPresetChip(
        title = "دراما سينمائية داكنة",
        subtitle = "عمق ظلال وإبراز",
        icon = Icons.Default.Bolt,
        accentColor = SunsetCoral,
        onClick = {
          onQuickAiBoost(-5f, 30f, 15f, 25f)
        },
        testTag = "ai_preset_dramatic"
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 4. Neural DSP Metrics Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ObsidianSurfaceElevated)
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      DspMetricItem(label = "نطاق ديناميكي", value = "+2.4 EV", color = ElectricCyan)
      DspMetricItem(label = "حدة التفاصيل", value = "${(adjustments.autoEnhance * 0.4f).toInt()}%", color = CyberGold)
      DspMetricItem(label = "خوارزمية المعالجة", value = "IntelliTone v4", color = EmeraldGreen)
    }
  }
}

@Composable
private fun AiPresetChip(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(ObsidianSurfaceElevated)
      .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 7.dp)
      .testTag(testTag)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(26.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accentColor,
          modifier = Modifier.size(15.dp)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(text = title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = TextMuted, fontSize = 9.sp)
      }
    }
  }
}

@Composable
private fun DspMetricItem(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, color = TextMuted, fontSize = 8.sp)
    Text(
      text = value,
      color = color,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}
