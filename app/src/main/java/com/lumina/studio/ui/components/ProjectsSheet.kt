package com.lumina.studio.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lumina.studio.data.model.MediaType
import com.lumina.studio.data.model.ProjectEntity
import com.lumina.studio.data.model.VideoResolution
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.NeonViolet
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceContainer
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary

@Composable
fun ProjectsSheet(
  projects: List<ProjectEntity>,
  activeProjectId: Long?,
  onSelectProject: (ProjectEntity) -> Unit,
  onCreateNewProject: (String, MediaType, VideoResolution) -> Unit,
  onDismiss: () -> Unit,
  onOpenImport: () -> Unit = {}
) {
  var isCreatingNew by remember { mutableStateOf(false) }
  var newTitle by remember { mutableStateOf("") }
  var newType by remember { mutableStateOf(MediaType.VIDEO) }
  var newResolution by remember { mutableStateOf(VideoResolution.UHD_4K) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .testTag("projects_sheet_dialog"),
      colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isCreatingNew) "إنشاء مشروع استوديو جديد" else "مشاريع Lumina Studio (محلية)",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isCreatingNew) {
          // Form to create new project
          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("اسم المشروع") },
            placeholder = { Text("مثال: إعلان سينمائي 4K") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ElectricCyan,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedLabelColor = ElectricCyan
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Media Type selection
          Text(text = "نوع الوسائط:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            MediaType.values().forEach { type ->
              val isSel = newType == type
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSel) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated)
                  .border(1.dp, if (isSel) ElectricCyan else ObsidianBorder, RoundedCornerShape(8.dp))
                  .clickable { newType = type }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${type.labelAr} (${type.labelEn})",
                  color = if (isSel) ElectricCyan else TextSecondary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Resolution
          Text(text = "الدقة الافتراضية:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            VideoResolution.values().forEach { res ->
              val isSel = newResolution == res
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSel) NeonViolet.copy(alpha = 0.25f) else ObsidianSurfaceElevated)
                  .border(1.dp, if (isSel) NeonViolet else ObsidianBorder, RoundedCornerShape(8.dp))
                  .clickable { newResolution = res }
                  .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = res.badge,
                  color = if (isSel) Color.White else TextMuted,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { isCreatingNew = false },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated, contentColor = TextSecondary)
            ) {
              Text("إلغاء")
            }

            Button(
              onClick = {
                onCreateNewProject(newTitle, newType, newResolution)
                onDismiss()
              },
              modifier = Modifier.weight(1f).testTag("confirm_create_project_button"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
            ) {
              Text("إنشاء والبدء", fontWeight = FontWeight.Bold)
            }
          }
        } else if (projects.isEmpty()) {
          // Real empty state
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(ObsidianSurfaceElevated)
              .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "لا توجد مشاريع بعد — أنشئ مشروعك الأول أو استورد وسائط",
              color = TextMuted,
              fontSize = 12.sp
            )
          }
        } else {
          // List of projects
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(projects) { p ->
              val isAct = p.id == activeProjectId
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isAct) ElectricCyan.copy(alpha = 0.12f) else ObsidianSurfaceElevated)
                  .border(1.dp, if (isAct) ElectricCyan else ObsidianBorder, RoundedCornerShape(10.dp))
                  .clickable {
                    onSelectProject(p)
                    onDismiss()
                  }
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(34.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (p.mediaType == MediaType.VIDEO.name) Color(0xFF0284C7) else Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = if (p.mediaType == MediaType.VIDEO.name) Icons.Default.Movie else Icons.Default.Image,
                      contentDescription = p.mediaType,
                      tint = Color.White,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = p.title,
                      color = if (isAct) ElectricCyan else TextPrimary,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = "${p.resolution.replace("UHD_", "")} • ${if (p.fps > 0) "${p.fps}fps" else "صورة"}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

          // Import from Device Button
          OutlinedButton(
            onClick = {
              onDismiss()
              onOpenImport()
            },
            modifier = Modifier.fillMaxWidth().testTag("import_media_sheet_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
          ) {
            Icon(imageVector = Icons.Default.Movie, contentDescription = "استيراد من الهاتف", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("استيراد فيديو أو صورة من المعرض والهاتف", fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Create New Button
          Button(
            onClick = { isCreatingNew = true },
            modifier = Modifier.fillMaxWidth().testTag("new_project_modal_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "مشروع جديد", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("مشروع جديد 4K", fontWeight = FontWeight.Bold)
          }
      }
    }
  }
}
