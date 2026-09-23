package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EditorMode
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

@Composable
fun SettingsScreen(
  editorMode: EditorMode,
  onToggleEditorMode: () -> Unit,
  modifier: Modifier = Modifier
) {
  var hardwareAcceleration by remember { mutableStateOf(true) }
  var hdrColorGrading by remember { mutableStateOf(true) }
  var export4kDefault by remember { mutableStateOf(true) }
  var highBitratePreview by remember { mutableStateOf(true) }
  var cacheClearedMessage by remember { mutableStateOf<String?>(null) }
  var currentCacheSize by remember { mutableStateOf("142.8 MB") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
      .testTag("settings_screen")
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(CyberGold.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "الإعدادات",
          tint = CyberGold,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "إعدادات الاستوديو والمحرك (Studio Preferences)",
          color = TextPrimary,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Hardware DSP, Export Quality & Cloud Engine",
          color = TextMuted,
          fontSize = 10.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 1. Export & Rendering Engine Settings
    SettingsSectionHeader(title = "محرك الرندر والتصدير (Export Engine)", icon = Icons.Default.HighQuality, color = ElectricCyan)

    SettingsCard {
      SettingsSwitchItem(
        title = "دقة التصدير الافتراضية 4K UHD",
        subtitle = "3840x2160 @ 60 FPS بأقصى معدل بيانات 60 Mbps",
        checked = export4kDefault,
        onCheckedChange = { export4kDefault = it },
        testTag = "setting_switch_4k"
      )

      SettingsDivider()

      SettingsSwitchItem(
        title = "تسريع العتاد بواسطة GPU (Vulkan / OpenGL)",
        subtitle = "معالجة فورية وتوليد الإطارات بدون تباطؤ",
        checked = hardwareAcceleration,
        onCheckedChange = { hardwareAcceleration = it },
        testTag = "setting_switch_gpu"
      )

      SettingsDivider()

      SettingsSwitchItem(
        title = "تدرج ألوان HDR 10-bit Rec.2020",
        subtitle = "عمق لوني فائق للمشاهد السينمائية والظلال",
        checked = hdrColorGrading,
        onCheckedChange = { hdrColorGrading = it },
        testTag = "setting_switch_hdr"
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 2. Storage & Cache Management
    SettingsSectionHeader(title = "الذاكرة المؤقتة والتخزين (Storage & Cache)", icon = Icons.Default.Storage, color = EmeraldGreen)

    SettingsCard {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("حجم الذاكرة المؤقتة للرندر", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Text("إطارات المعاينة والتأثيرات المحفوظة", color = TextMuted, fontSize = 10.sp)
        }
        Text(
          text = currentCacheSize,
          color = EmeraldGreen,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      SettingsDivider()

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            currentCacheSize = "0.0 MB"
            cacheClearedMessage = "تم تنظيف الذاكرة المؤقتة بنجاح!"
          }
          .padding(12.dp)
          .testTag("setting_clear_cache_button"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = "مسح الذاكرة",
            tint = SunsetCoral,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("مسح الذاكرة المؤقتة (Clear Cache)", color = SunsetCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("تحرير المساحة وإعادة بناء مصغرات المقاطع", color = TextMuted, fontSize = 10.sp)
          }
        }
      }

      if (cacheClearedMessage != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(EmeraldGreen.copy(alpha = 0.15f))
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(text = cacheClearedMessage!!, color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 3. Editor Interface Preferences
    SettingsSectionHeader(title = "تفضيلات واجهة التحرير (Editor Preferences)", icon = Icons.Default.Tune, color = CyberGold)

    SettingsCard {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggleEditorMode() }
          .padding(12.dp)
          .testTag("setting_toggle_editor_mode"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("الوضع الافتراضي للمحرر", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Text(
            text = if (editorMode == EditorMode.PRO) "وضع المحترفين (Pro Scopes & 32-bit Float)" else "الوضع البسيط (Beginner One-Tap)",
            color = TextMuted,
            fontSize = 10.sp
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (editorMode == EditorMode.PRO) CyberGold else ElectricCyan)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = if (editorMode == EditorMode.PRO) "PRO" else "BEGINNER",
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 5. App Info & Hardware DSP
    SettingsSectionHeader(title = "حول التطبيق والمعالج (About & Engine)", icon = Icons.Default.Info, color = TextMuted)

    SettingsCard {
      Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("إصدار الاستوديو", color = TextSecondary, fontSize = 11.sp)
          Text("Lumina Studio Pro v2.5.0", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("محرك الرندر", color = TextSecondary, fontSize = 11.sp)
          Text("OpenGLES 3.2 + Jetpack Compose", color = ElectricCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("خوارزمية الذكاء الاصطناعي", color = TextSecondary, fontSize = 11.sp)
          Text("IntelliTone DSP Neural Suite", color = CyberGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(bottom = 8.dp)
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(text = title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(ObsidianSurface)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
  ) {
    content()
  }
}

@Composable
private fun SettingsSwitchItem(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.Black,
        checkedTrackColor = CyberGold,
        uncheckedThumbColor = TextMuted,
        uncheckedTrackColor = ObsidianSurfaceElevated
      ),
      modifier = Modifier.testTag(testTag)
    )
  }
}

@Composable
private fun SettingsDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(ObsidianBorder)
  )
}
