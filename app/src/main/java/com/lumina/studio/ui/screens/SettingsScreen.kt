package com.lumina.studio.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.studio.data.model.EditorMode
import com.lumina.studio.data.model.ExportCodec
import com.lumina.studio.data.model.VideoResolution
import com.lumina.studio.ui.StudioPreferences
import com.lumina.studio.ui.StudioViewModel
import com.lumina.studio.ui.theme.CyberGold
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.ObsidianBg
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * REAL studio settings.
 *
 * Every control here has an actual effect:
 *  - default editor mode is persisted and applied on next launch
 *  - default export resolution/fps/codec feed the export sheet defaults
 *  - cache size is measured from the real cache directory and clearing it
 *    really deletes preview/export files
 *  - version info comes from PackageManager, not a hardcoded string
 *
 * The previous fake toggles (Vulkan/HDR switches wired to local remember
 * state, invented "142.8 MB" cache, fictional "IntelliTone DSP" engine name)
 * were removed.
 */
@Composable
fun SettingsScreen(
  editorMode: EditorMode,
  onToggleEditorMode: () -> Unit,
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var cacheClearedMessage by remember { mutableStateOf<String?>(null) }
  var currentCacheSize by remember { mutableStateOf("...") }
  var defaultResolution by remember { mutableStateOf(StudioPreferences.getDefaultExportResolution(context)) }
  var defaultFps by remember { mutableStateOf(StudioPreferences.getDefaultExportFps(context)) }
  var defaultCodec by remember { mutableStateOf(StudioPreferences.getDefaultExportCodec(context)) }
  var appVersion by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    currentCacheSize = withContext(Dispatchers.IO) { measureCacheSize(context) }
    appVersion = withContext(Dispatchers.IO) { readAppVersion(context) }
  }

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
          text = "إعدادات الاستوديو (Studio Preferences)",
          color = TextPrimary,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "الوضع الافتراضي • جودة التصدير • الذاكرة المؤقتة",
          color = TextMuted,
          fontSize = 10.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 1. Export defaults — REAL: they drive the export sheet
    SettingsSectionHeader(title = "افتراضيات التصدير (Export Defaults)", icon = Icons.Default.HighQuality, color = ElectricCyan)

    SettingsCard {
      SettingSelectionRow(
        title = "دقة التصدير الافتراضية",
        options = listOf(
          "UHD_4K" to "4K (3840×2160)",
          "FHD_1080P" to "1080p",
          "HD_720P" to "720p"
        ),
        selectedKey = defaultResolution.name,
        onSelect = { key ->
          val res = VideoResolution.valueOf(key)
          defaultResolution = res
          StudioPreferences.setDefaultExportResolution(context, res)
        },
        testTag = "setting_default_resolution"
      )

      SettingsDivider()

      SettingSelectionRow(
        title = "معدل الإطارات الافتراضي",
        options = listOf("24" to "24 FPS", "30" to "30 FPS", "60" to "60 FPS"),
        selectedKey = defaultFps.toString(),
        onSelect = { key ->
          val fps = key.toIntOrNull() ?: 30
          defaultFps = fps
          StudioPreferences.setDefaultExportFps(context, fps)
        },
        testTag = "setting_default_fps"
      )

      SettingsDivider()

      SettingSelectionRow(
        title = "الترميز الافتراضي",
        options = listOf(
          ExportCodec.AVC_H264.name to "H.264 / AVC (الأوسع توافقاً)",
          ExportCodec.HEVC_H265.name to "H.265 / HEVC (أصغر حجماً)"
        ),
        selectedKey = defaultCodec.name,
        onSelect = { key ->
          val codec = ExportCodec.valueOf(key)
          defaultCodec = codec
          StudioPreferences.setDefaultExportCodec(context, codec)
        },
        testTag = "setting_default_codec"
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 2. Storage & Cache — REAL measurement & deletion
    SettingsSectionHeader(title = "الذاكرة المؤقتة (Storage & Cache)", icon = Icons.Default.Storage, color = EmeraldGreen)

    SettingsCard {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("حجم الذاكرة المؤقتة", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Text("ملفات المعاينة والتصدير المؤقتة", color = TextMuted, fontSize = 10.sp)
        }
        Text(
          text = currentCacheSize,
          color = EmeraldGreen,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.testTag("setting_cache_size_value")
        )
      }

      SettingsDivider()

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            cacheClearedMessage = null
            clearCacheAsync(context) { freed ->
              currentCacheSize = formatBytes(freed)
              cacheClearedMessage = "تم تنظيف الذاكرة المؤقتة بنجاح!"
            }
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
            Text("حذف ملفات التصدير المؤقتة فعلياً من cacheDir", color = TextMuted, fontSize = 10.sp)
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

    // 3. Editor Interface Preferences — persisted & applied for real
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
          Text("الوضع الحالي للمحرر", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Text(
            text = if (editorMode == EditorMode.PRO) "وضع المحترفين (أدوات متقدمة ومقاييس)" else "الوضع البسيط (لمسة واحدة)",
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

    // 4. REAL About section
    SettingsSectionHeader(title = "حول التطبيق (About)", icon = Icons.Default.Info, color = TextMuted)

    SettingsCard {
      Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        AboutRow("إصدار التطبيق", appVersion.ifBlank { "..." })
        AboutRow("محرك التدريج اللوني", "Android ColorMatrix (GPU)", ElectricCyan)
        AboutRow("محرك التصدير", "MediaCodec + MediaMuxer", ElectricCyan)
        AboutRow("محرك التحليل", "قياس بيكسل حقيقي (Gray World / Laplacian)", CyberGold)
        AboutRow("محرك الصوت", "PCM فعلي عبر MediaCodec", CyberGold)
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable
private fun AboutRow(label: String, value: String, valueColor: Color = TextPrimary) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, color = TextSecondary, fontSize = 11.sp)
    Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }
}

@Composable
private fun SettingSelectionRow(
  title: String,
  options: List<Pair<String, String>>,
  selectedKey: String,
  onSelect: (String) -> Unit,
  testTag: String
) {
  Column(modifier = Modifier.fillMaxWidth().padding(12.dp).testTag(testTag)) {
    Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      options.forEach { (key, label) ->
        val isSelected = key == selectedKey
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ElectricCyan.copy(alpha = 0.18f) else ObsidianSurfaceElevated)
            .border(
              1.dp,
              if (isSelected) ElectricCyan else ObsidianBorder,
              RoundedCornerShape(6.dp)
            )
            .clickable { onSelect(key) }
            .padding(vertical = 7.dp, horizontal = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = label,
            color = if (isSelected) ElectricCyan else TextSecondary,
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
          )
        }
      }
    }
  }
}

/** Recursively measures the real size of the app cache directory. */
private fun measureCacheSize(context: Context): String {
  val bytes = folderSize(context.cacheDir)
  return formatBytes(bytes)
}

private fun folderSize(dir: File?): Long {
  if (dir == null || !dir.exists()) return 0L
  return dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
}

private fun formatBytes(bytes: Long): String {
  val mb = bytes / (1024.0 * 1024.0)
  return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb)
  else String.format(Locale.US, "%.0f KB", bytes / 1024.0)
}

private fun clearCacheAsync(context: Context, onDone: (Long) -> Unit) {
  kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
    val freed = withContext(Dispatchers.IO) {
      val target = context.cacheDir
      val before = folderSize(target)
      val ok = try {
        target.listFiles()?.forEach { child -> child.deleteRecursively() }
        true
      } catch (e: Exception) {
        false
      }
      if (ok) before else -1L
    }
    onDone(freed)
  }
}

private fun readAppVersion(context: Context): String {
  return try {
    val pm = context.packageManager
    val pkg = context.packageName
    val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      pm.getPackageInfo(pkg, 0)
    } else {
      @Suppress("DEPRECATION")
      pm.getPackageInfo(pkg, 0)
    }
    "Lumina Studio ${info.versionName}"
  } catch (e: Exception) {
    "Lumina Studio"
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
private fun SettingsDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(ObsidianBorder)
  )
}
