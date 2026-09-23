package com.example.ui.components.tools

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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdjustmentsState
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Dedicated Custom UI for Visual FX (Film Grain, Vignette, Glitch, Bloom, Blur).
 */
@Composable
fun VisualFxPanel(
  adjustments: AdjustmentsState,
  onAdjustmentChange: ((AdjustmentsState) -> AdjustmentsState) -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(ObsidianSurface)
      .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
      .padding(12.dp)
      .testTag("visual_fx_panel")
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
            .size(28.dp)
            .clip(CircleShape)
            .background(NeonViolet.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoFixHigh,
            contentDescription = "مؤثرات بصرية سينمائية",
            tint = NeonViolet,
            modifier = Modifier.size(15.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "مؤثرات بصرية سينمائية (Visual FX)",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Cinematic Grain, Glow & Optical Dispersion",
            color = NeonViolet,
            fontSize = 9.sp
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
          .clickable { onReset() }
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("fx_reset_button"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = "إلغاء المؤثرات",
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("إلغاء", color = TextSecondary, fontSize = 10.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    FxSliderItem(
      label = "تظليل الحواف السينمائي Vignette",
      value = adjustments.vignette,
      color = SunsetCoral,
      onValueChange = { v -> onAdjustmentChange { it.copy(vignette = v) } },
      testTag = "fx_slider_vignette"
    )

    FxSliderItem(
      label = "حبيبات شريط الفيلم 35mm Film Grain",
      value = adjustments.filmGrain,
      color = CyberGold,
      onValueChange = { v -> onAdjustmentChange { it.copy(filmGrain = v) } },
      testTag = "fx_slider_grain"
    )

    FxSliderItem(
      label = "توهج الإضاءة السينمائي Bloom Glow",
      value = adjustments.bloomGlow,
      color = NeonViolet,
      onValueChange = { v -> onAdjustmentChange { it.copy(bloomGlow = v) } },
      testTag = "fx_slider_bloom"
    )

    FxSliderItem(
      label = "تشويش لوني رقمي RGB Glitch",
      value = adjustments.glitchRgb,
      color = ElectricCyan,
      onValueChange = { v -> onAdjustmentChange { it.copy(glitchRgb = v) } },
      testTag = "fx_slider_glitch"
    )

    FxSliderItem(
      label = "ضبابية العدسة Gaussian Blur",
      value = adjustments.blur,
      color = Color(0xFFA78BFA),
      onValueChange = { v -> onAdjustmentChange { it.copy(blur = v) } },
      testTag = "fx_slider_blur"
    )
  }
}

@Composable
private fun FxSliderItem(
  label: String,
  value: Float,
  color: Color,
  onValueChange: (Float) -> Unit,
  testTag: String
) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = label, color = TextSecondary, fontSize = 11.sp)
      Text(
        text = "${value.toInt()}%",
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = 0f..100f,
      colors = SliderDefaults.colors(
        thumbColor = color,
        activeTrackColor = color,
        inactiveTrackColor = ObsidianSurfaceElevated
      ),
      modifier = Modifier.fillMaxWidth().testTag(testTag)
    )
  }
}
