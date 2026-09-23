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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FilterCategory
import com.example.data.model.FilterPreset
import com.example.data.model.TimelineClip
import com.example.data.model.TrackType
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
 * Dedicated Custom UI for Cinematic LUTs & Color Grading Filter Library.
 * Features 24 real-time color grading filters across 6 cinematic categories,
 * with real-time intensity blending and per-clip or master grading controls.
 */
@Composable
fun FiltersGalleryPanel(
  selectedPreset: FilterPreset,
  lutIntensity: Float = 100f,
  onSelectPreset: (FilterPreset) -> Unit,
  onIntensityChange: (Float) -> Unit = {},
  onResetPreset: () -> Unit,
  timelineClips: List<TimelineClip> = emptyList(),
  selectedClipId: String? = null,
  onSelectClip: (String?) -> Unit = {},
  onApplyToAllClips: (FilterPreset, Float) -> Unit = { _, _ -> },
  isCompareActive: Boolean = false,
  onToggleCompare: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedCategory by remember { mutableStateOf(FilterCategory.ALL) }
  val videoClips = timelineClips.filter { it.trackType == TrackType.VIDEO }

  val filteredPresets = remember(selectedCategory) {
    if (selectedCategory == FilterCategory.ALL) {
      FilterPreset.values().toList()
    } else {
      FilterPreset.values().filter { it.category == selectedCategory }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(ObsidianSurface)
      .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
      .padding(12.dp)
      .testTag("filters_gallery_panel")
  ) {
    // 1. Header Bar: Title, Count badge, A/B Compare and Reset
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(NeonViolet.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Filter,
            contentDescription = "مكتبة الفلاتر السينمائية",
            tint = NeonViolet,
            modifier = Modifier.size(17.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "مكتبة تدريج الألوان والفلاتر السينمائية",
              color = TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(NeonViolet.copy(alpha = 0.25f))
                .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
              Text(
                text = "${FilterPreset.values().size} فلتر 4K",
                color = ElectricCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Text(
            text = "Real-Time 3D LUT Cinematic Grading Library",
            color = TextSecondary,
            fontSize = 9.5.sp
          )
        }
      }

      // Quick Actions: Compare & Reset
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // A/B Compare Toggle Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCompareActive) ElectricCyan.copy(alpha = 0.25f) else ObsidianSurfaceElevated)
            .border(1.dp, if (isCompareActive) ElectricCyan else ObsidianBorder, RoundedCornerShape(6.dp))
            .clickable { onToggleCompare() }
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .testTag("filter_compare_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Compare,
              contentDescription = "مقارنة قبل وبعد",
              tint = if (isCompareActive) ElectricCyan else TextSecondary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "مقارنة A/B",
              color = if (isCompareActive) ElectricCyan else TextSecondary,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Reset to RAW / Original
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
            .clickable { onResetPreset() }
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .testTag("filters_reset_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.RestartAlt,
              contentDescription = "الألوان الأصلية",
              tint = TextSecondary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text("الأصلي", color = TextSecondary, fontSize = 9.5.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Target Video Clip Selector (Per-Clip Grading vs Master All Clips)
    if (videoClips.isNotEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
          .padding(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Movie,
              contentDescription = null,
              tint = ElectricCyan,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "المقطع المستهدف للتلوين:",
              color = TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Apply to all clips button
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(NeonViolet.copy(alpha = 0.2f))
              .clickable { onApplyToAllClips(selectedPreset, lutIntensity) }
              .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.DoneAll,
              contentDescription = null,
              tint = NeonViolet,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "تطبيق على كافة المقاطع",
              color = NeonViolet,
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Clip selection chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Master Grade chip
          val isMasterSelected = selectedClipId == null
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isMasterSelected) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurface)
              .border(
                1.dp,
                if (isMasterSelected) ElectricCyan else ObsidianBorder,
                RoundedCornerShape(6.dp)
              )
              .clickable { onSelectClip(null) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("clip_target_master"),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "🌐 كامل الفيديو (Master)",
                color = if (isMasterSelected) ElectricCyan else TextPrimary,
                fontSize = 9.5.sp,
                fontWeight = if (isMasterSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }

          // Individual Video Clips
          videoClips.forEachIndexed { index, clip ->
            val isClipSelected = selectedClipId == clip.id
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isClipSelected) NeonViolet.copy(alpha = 0.25f) else ObsidianSurface)
                .border(
                  1.dp,
                  if (isClipSelected) NeonViolet else ObsidianBorder,
                  RoundedCornerShape(6.dp)
                )
                .clickable { onSelectClip(clip.id) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("clip_target_${clip.id}"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "🎬 ${clip.title}",
                  color = if (isClipSelected) Color.White else TextPrimary,
                  fontSize = 9.5.sp,
                  fontWeight = if (isClipSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (clip.filterPreset != FilterPreset.ORIGINAL) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(Color(clip.filterPreset.primaryColorHex))
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
    }

    // 3. Category Filter Chips (All, Hollywood Cinema, Analog Film, Moody, Modern HDR, Noir, Sci-Fi)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FilterCategory.values().forEach { cat ->
        val isCatSelected = selectedCategory == cat
        val count = if (cat == FilterCategory.ALL) {
          FilterPreset.values().size
        } else {
          FilterPreset.values().count { it.category == cat }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCatSelected) NeonViolet else ObsidianSurfaceElevated)
            .border(
              1.dp,
              if (isCatSelected) NeonViolet else ObsidianBorder,
              RoundedCornerShape(16.dp)
            )
            .clickable { selectedCategory = cat }
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("filter_cat_${cat.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = cat.labelAr,
              color = if (isCatSelected) Color.White else TextSecondary,
              fontSize = 10.sp,
              fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "($count)",
              color = if (isCatSelected) Color.White.copy(alpha = 0.8f) else TextMuted,
              fontSize = 8.5.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. Real-time Filter Intensity Slider (0% - 100%)
    if (selectedPreset != FilterPreset.ORIGINAL) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
          .padding(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = NeonViolet,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "شدة الفلتر ومزج الـ LUT:",
              color = TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${lutIntensity.toInt()}%",
              color = NeonViolet,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Quick preset pills: 25%, 50%, 75%, 100%
            listOf(25f, 50f, 75f, 100f).forEach { qVal ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(3.dp))
                  .background(if (lutIntensity.toInt() == qVal.toInt()) NeonViolet else ObsidianSurface)
                  .clickable { onIntensityChange(qVal) }
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "${qVal.toInt()}%",
                  color = if (lutIntensity.toInt() == qVal.toInt()) Color.White else TextMuted,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.width(3.dp))
            }
          }
        }

        Slider(
          value = lutIntensity,
          onValueChange = { onIntensityChange(it) },
          valueRange = 0f..100f,
          colors = SliderDefaults.colors(
            thumbColor = NeonViolet,
            activeTrackColor = NeonViolet,
            inactiveTrackColor = ObsidianBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .testTag("filter_intensity_slider")
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
    }

    // 5. Filter Cards Carousel / Horizontal Grid
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      filteredPresets.forEach { preset ->
        val isSelected = selectedPreset == preset
        val swatchColors = listOf(Color(preset.primaryColorHex), Color(preset.secondaryColorHex))

        Box(
          modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonViolet.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
            .border(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) NeonViolet else ObsidianBorder,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelectPreset(preset) }
            .padding(8.dp)
            .testTag("filter_preset_${preset.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Visual Swatch Gradient Box with Category Tag
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(swatchColors)),
              contentAlignment = Alignment.TopEnd
            ) {
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .padding(3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "مفعل",
                    tint = ElectricCyan,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }

              // Category Mini Label
              if (preset != FilterPreset.ORIGINAL) {
                Box(
                  modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                  Text(
                    text = preset.category.labelAr,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = preset.labelAr,
              color = if (isSelected) Color.White else TextPrimary,
              fontSize = 10.5.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1
            )
            Text(
              text = preset.labelEn,
              color = if (isSelected) ElectricCyan else TextMuted,
              fontSize = 8.5.sp,
              maxLines = 1
            )
          }
        }
      }
    }

    // 6. Detailed Active Filter Inspector Card
    if (selectedPreset != FilterPreset.ORIGINAL) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(ObsidianSurfaceElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
              Brush.linearGradient(
                listOf(
                  Color(selectedPreset.primaryColorHex),
                  Color(selectedPreset.secondaryColorHex)
                )
              )
            )
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = selectedPreset.labelAr,
              color = TextPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "• ${selectedPreset.labelEn}",
              color = NeonViolet,
              fontSize = 9.sp
            )
          }
          Text(
            text = selectedPreset.descriptionAr,
            color = TextSecondary,
            fontSize = 9.5.sp,
            maxLines = 2
          )
        }
      }
    }
  }
}
