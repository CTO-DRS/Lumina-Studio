package com.example.ui.components.tools

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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CropAspect
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Dedicated Custom UI for Crop & Social Media Aspect Ratio Tool.
 */
@Composable
fun CropRatioPanel(
  selectedCrop: CropAspect,
  onCropSelect: (CropAspect) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(ObsidianSurface)
      .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
      .padding(12.dp)
      .testTag("crop_ratio_panel")
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(EmeraldGreen.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Crop,
          contentDescription = "قص وتأطير الأبعاد",
          tint = EmeraldGreen,
          modifier = Modifier.size(15.dp)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = "أبعاد وقص المنصات (Social Media Aspect Presets)",
          color = TextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Zero-loss Auto Framing for TikTok, Reels & YouTube",
          color = EmeraldGreen,
          fontSize = 9.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Aspect Cards
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      CropAspect.values().forEach { crop ->
        val isSelected = selectedCrop == crop
        val (desc, platform) = when (crop) {
          CropAspect.ORIGINAL -> Pair("بدون قص", "الكاميرا الأصلية")
          CropAspect.RATIO_16_9 -> Pair("عرض أفقي", "YouTube / TV")
          CropAspect.RATIO_9_16 -> Pair("طولي Reels", "TikTok / Shorts")
          CropAspect.RATIO_1_1 -> Pair("مربع 1:1", "Instagram Post")
          CropAspect.RATIO_4_5 -> Pair("بورتريه", "Instagram Feed")
          CropAspect.RATIO_21_9 -> Pair("سينمائي عريض", "Cinemascope")
        }

        Box(
          modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) EmeraldGreen.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
            .border(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) EmeraldGreen else ObsidianBorder,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onCropSelect(crop) }
            .padding(8.dp)
            .testTag("crop_aspect_${crop.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = crop.label,
              color = if (isSelected) EmeraldGreen else TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = desc,
              color = if (isSelected) Color.White else TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = platform,
              color = TextMuted,
              fontSize = 8.sp
            )
          }
        }
      }
    }
  }
}
