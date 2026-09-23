package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
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
fun DashboardScreen(
  projects: List<ProjectEntity>,
  activeProject: ProjectEntity?,
  onOpenProjectInStudio: (ProjectEntity) -> Unit,
  onCreateNewProject: () -> Unit,
  onLaunchCamera: () -> Unit,
  onGoToStudio: () -> Unit,
  onOpenAiLab: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
      .testTag("dashboard_screen")
  ) {
    // 1. Studio Header & Branding
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              Brush.linearGradient(listOf(CyberGold, NeonViolet))
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.MovieFilter,
            contentDescription = "Lumina Studio",
            tint = Color.Black,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "لوحة التحكم الرئيسية",
              color = TextPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(CyberGold.copy(alpha = 0.2f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              Text(
                text = "PRO",
                color = CyberGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          Text(
            text = "Lumina Studio Cinematic Production Hub",
            color = TextMuted,
            fontSize = 10.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Active Project Hero Banner
    if (activeProject != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(
                ObsidianSurfaceElevated,
                ObsidianSurface
              )
            )
          )
          .border(
            width = 1.dp,
            brush = Brush.horizontalGradient(
              listOf(CyberGold.copy(alpha = 0.6f), ElectricCyan.copy(alpha = 0.3f))
            ),
            shape = RoundedCornerShape(16.dp)
          )
          .padding(14.dp)
          .testTag("dashboard_hero_project_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(CyberGold)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "المشروع النشط حالياً في المحرر",
                color = CyberGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = activeProject.resolution.replace("_", " "),
              color = ElectricCyan,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = activeProject.title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "المدة: ${activeProject.durationMs / 1000} ثانية • ${activeProject.fps} FPS • ${if (activeProject.mediaType == "VIDEO") "فيديو سينمائي 4K" else "صورة فوتوغرافية RAW"}",
            color = TextSecondary,
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { onGoToStudio() },
              colors = ButtonDefaults.buttonColors(
                containerColor = CyberGold,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("dashboard_continue_editing_button")
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("متابعة التحرير في الاستوديو", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Studio System Metrics Grid
    Text(
      text = "مؤشرات الإنتاج والأداء (Studio Metrics)",
      color = TextSecondary,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      MetricCard(
        title = "إجمالي المشاريع",
        value = "${projects.size}",
        subtitle = "مشاريع نشطة",
        icon = Icons.Default.VideoLibrary,
        accentColor = ElectricCyan,
        modifier = Modifier.weight(1f)
      )
      MetricCard(
        title = "فيديوهات 4K",
        value = "${projects.count { it.mediaType == "VIDEO" }}",
        subtitle = "جاهزة للمونتاج",
        icon = Icons.Default.HighQuality,
        accentColor = CyberGold,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      MetricCard(
        title = "المعالجة الفورية",
        value = "60 FPS",
        subtitle = "OpenGL ES 3.2",
        icon = Icons.Default.FlashOn,
        accentColor = EmeraldGreen,
        modifier = Modifier.weight(1f)
      )
      MetricCard(
        title = "الذاكرة السحابية",
        value = "184 MB",
        subtitle = "سحابي مشفر",
        icon = Icons.Default.Storage,
        accentColor = NeonViolet,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 4. Quick Action Buttons Bar
    Text(
      text = "إجراءات سريعة فورية",
      color = TextSecondary,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      QuickActionButton(
        label = "مشروع فيديو جديد",
        icon = Icons.Default.Add,
        color = CyberGold,
        onClick = onCreateNewProject,
        testTag = "dashboard_action_new_project"
      )
      QuickActionButton(
        label = "كاميرا سينمائية 4K",
        icon = Icons.Default.PhotoCamera,
        color = ElectricCyan,
        onClick = onLaunchCamera,
        testTag = "dashboard_action_camera"
      )
      QuickActionButton(
        label = "شاشة التعديل الفورية",
        icon = Icons.Default.MovieCreation,
        color = NeonViolet,
        onClick = onGoToStudio,
        testTag = "dashboard_action_studio"
      )
      QuickActionButton(
        label = "مختبر المؤثرات الذكية",
        icon = Icons.Default.AutoAwesome,
        color = SunsetCoral,
        onClick = onOpenAiLab,
        testTag = "dashboard_action_ai_lab"
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 5. Recent Projects Gallery
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "المشاريع الحديثة (Recent Projects)",
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "${projects.size} مشاريع",
        color = TextMuted,
        fontSize = 11.sp
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      projects.take(5).forEach { proj ->
        val isActive = activeProject?.id == proj.id
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(
              width = if (isActive) 1.5.dp else 1.dp,
              color = if (isActive) CyberGold else ObsidianBorder,
              shape = RoundedCornerShape(12.dp)
            )
            .clickable { onOpenProjectInStudio(proj) }
            .padding(12.dp)
            .testTag("dashboard_project_item_${proj.id}")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    if (proj.mediaType == "VIDEO") ElectricCyan.copy(alpha = 0.15f) else NeonViolet.copy(alpha = 0.15f)
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (proj.mediaType == "VIDEO") Icons.Default.HighQuality else Icons.Default.PhotoCamera,
                  contentDescription = null,
                  tint = if (proj.mediaType == "VIDEO") ElectricCyan else NeonViolet,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = proj.title,
                  color = TextPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${proj.resolution} • ${proj.durationMs / 1000}s • ${proj.fps}fps",
                  color = TextMuted,
                  fontSize = 10.sp
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isActive) CyberGold else ObsidianSurfaceElevated)
                .clickable { onOpenProjectInStudio(proj) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = if (isActive) "مفتوح" else "فتح في المحرر",
                color = if (isActive) Color.Black else ElectricCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(ObsidianSurface)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, color = TextMuted, fontSize = 10.sp)
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(13.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace
      )
      Text(text = subtitle, color = TextSecondary, fontSize = 9.sp)
    }
  }
}

@Composable
private fun QuickActionButton(
  label: String,
  icon: ImageVector,
  color: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(ObsidianSurface)
      .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag(testTag)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = color,
          modifier = Modifier.size(14.dp)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        color = TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
