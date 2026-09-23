package com.lumina.studio.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.studio.data.model.ProjectEntity
import com.lumina.studio.ui.theme.CyberGold
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.NeonViolet
import com.lumina.studio.ui.theme.ObsidianBg
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary

@Composable
fun ProjectsScreen(
  projects: List<ProjectEntity>,
  activeProjectId: Long?,
  onSelectProject: (ProjectEntity) -> Unit,
  onCreateNewProject: () -> Unit,
  onDeleteProject: (ProjectEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: الكل, 1: فيديو 4K, 2: صور

  val filteredProjects = projects.filter { proj ->
    val matchesSearch = proj.title.contains(searchQuery, ignoreCase = true)
    val matchesType = when (selectedFilterTab) {
      1 -> proj.mediaType == "VIDEO"
      2 -> proj.mediaType == "PHOTO"
      else -> true
    }
    matchesSearch && matchesType
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .padding(16.dp)
      .testTag("projects_screen")
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
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ElectricCyan.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = "مكتبة المشاريع",
            tint = ElectricCyan,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "مكتبة مشاريع الاستوديو (Projects Library)",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${projects.size} مشاريع محفوظة محلياً على الجهاز",
            color = TextMuted,
            fontSize = 10.sp
          )
        }
      }

      // New Project Button
      Button(
        onClick = onCreateNewProject,
        colors = ButtonDefaults.buttonColors(
          containerColor = CyberGold,
          contentColor = Color.Black
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("projects_new_button")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("مشروع جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("بحث في أسماء المشاريع...", color = TextMuted, fontSize = 12.sp) },
      leadingIcon = {
        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = ObsidianSurface,
        unfocusedContainerColor = ObsidianSurface,
        focusedBorderColor = ElectricCyan,
        unfocusedBorderColor = ObsidianBorder
      ),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().testTag("projects_search_field")
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        label = "الكل (${projects.size})",
        isSelected = selectedFilterTab == 0,
        onClick = { selectedFilterTab = 0 }
      )
      FilterChip(
        label = "فيديو 4K (${projects.count { it.mediaType == "VIDEO" }})",
        isSelected = selectedFilterTab == 1,
        onClick = { selectedFilterTab = 1 }
      )
      FilterChip(
        label = "صور RAW (${projects.count { it.mediaType == "PHOTO" }})",
        isSelected = selectedFilterTab == 2,
        onClick = { selectedFilterTab = 2 }
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Projects List — with a real empty state
    if (filteredProjects.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianSurface)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
          .padding(20.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "لا توجد مشاريع مطابقة — أنشئ مشروعاً جديداً أو استورد وسائط",
          color = TextMuted,
          fontSize = 12.sp
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredProjects, key = { it.id }) { proj ->
          val isActive = activeProjectId == proj.id
          ProjectCardItem(
            project = proj,
            isActive = isActive,
            onOpen = { onSelectProject(proj) },
            onDelete = { onDeleteProject(proj) }
          )
        }
      }
    }
  }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurface)
      .border(
        width = 1.dp,
        color = if (isSelected) ElectricCyan else ObsidianBorder,
        shape = RoundedCornerShape(8.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      color = if (isSelected) ElectricCyan else TextSecondary,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    )
  }
}

@Composable
private fun ProjectCardItem(
  project: ProjectEntity,
  isActive: Boolean,
  onOpen: () -> Unit,
  onDelete: () -> Unit
) {
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
      .padding(12.dp)
      .testTag("project_card_${project.id}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
              if (project.mediaType == "VIDEO") ElectricCyan.copy(alpha = 0.15f) else NeonViolet.copy(alpha = 0.15f)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (project.mediaType == "VIDEO") Icons.Default.HighQuality else Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = if (project.mediaType == "VIDEO") ElectricCyan else NeonViolet,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = project.title,
              color = TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (isActive) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(CyberGold.copy(alpha = 0.2f))
                  .padding(horizontal = 5.dp, vertical = 1.dp)
              ) {
                Text("نشط", color = CyberGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "${project.resolution.replace("_", " ")} • ${project.durationMs / 1000}s • ${project.fps} FPS",
            color = TextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        // Open Button
        Button(
          onClick = onOpen,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) CyberGold else ObsidianSurfaceElevated,
            contentColor = if (isActive) Color.Black else ElectricCyan
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("open_project_button_${project.id}")
        ) {
          Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = if (isActive) "مفتوح" else "فتح", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Delete Button
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(32.dp).testTag("delete_project_button_${project.id}")
        ) {
          Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = SunsetCoral.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}
