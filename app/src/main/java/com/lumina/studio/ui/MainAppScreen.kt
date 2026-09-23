package com.lumina.studio.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumina.studio.data.model.EditorMode
import com.lumina.studio.ui.components.CameraCaptureDialog
import com.lumina.studio.ui.components.ProjectsSheet
import com.lumina.studio.ui.navigation.StudioBottomNavBar
import com.lumina.studio.ui.navigation.StudioScreenTab
import com.lumina.studio.ui.screens.AiCineLabScreen
import com.lumina.studio.ui.screens.DashboardScreen
import com.lumina.studio.ui.screens.GalleryScreen
import com.lumina.studio.ui.screens.ProjectsScreen
import com.lumina.studio.ui.screens.SettingsScreen
import com.lumina.studio.ui.theme.ObsidianBg

@Composable
fun MainAppScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  var currentTab by remember { mutableStateOf(StudioScreenTab.DASHBOARD) }

  val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
  val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
  val editorMode by viewModel.editorMode.collectAsStateWithLifecycle()

  var showCreateProjectSheet by remember { mutableStateOf(false) }
  var showCameraDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .statusBarsPadding()
      .navigationBarsPadding(),
    containerColor = ObsidianBg,
    bottomBar = {
      StudioBottomNavBar(
        currentTab = currentTab,
        onTabSelect = { currentTab = it }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("main_app_content_host")
    ) {
      Crossfade(
        targetState = currentTab,
        label = "screen_navigation_crossfade"
      ) { tab ->
        when (tab) {
          StudioScreenTab.DASHBOARD -> {
            DashboardScreen(
              projects = allProjects,
              activeProject = currentProject,
              onOpenProjectInStudio = { project ->
                viewModel.selectProject(project)
                currentTab = StudioScreenTab.STUDIO
              },
              onCreateNewProject = { showCreateProjectSheet = true },
              onLaunchCamera = { showCameraDialog = true },
              onGoToStudio = { currentTab = StudioScreenTab.STUDIO },
              onOpenAiLab = { currentTab = StudioScreenTab.AI_LAB }
            )
          }

          StudioScreenTab.STUDIO -> {
            StudioScreen(viewModel = viewModel)
          }

          StudioScreenTab.AI_LAB -> {
            AiCineLabScreen(
              viewModel = viewModel,
              onNavigateToStudio = { currentTab = StudioScreenTab.STUDIO }
            )
          }

          StudioScreenTab.GALLERY -> {
            GalleryScreen(
              onOpenInStudio = { file, type, durationMs ->
                viewModel.handleCapturedMedia(file, type, durationMs)
                currentTab = StudioScreenTab.STUDIO
              },
              onLaunchCamera = { showCameraDialog = true }
            )
          }

          StudioScreenTab.PROJECTS -> {
            ProjectsScreen(
              projects = allProjects,
              activeProjectId = currentProject?.id,
              onSelectProject = { project ->
                viewModel.selectProject(project)
                currentTab = StudioScreenTab.STUDIO
              },
              onCreateNewProject = { showCreateProjectSheet = true },
              onDeleteProject = { project ->
                viewModel.deleteProject(project)
              }
            )
          }

          StudioScreenTab.SETTINGS -> {
            SettingsScreen(
              editorMode = editorMode,
              onToggleEditorMode = {
                val next = if (editorMode == EditorMode.PRO) EditorMode.BEGINNER else EditorMode.PRO
                viewModel.setEditorMode(next)
              },
              viewModel = viewModel
            )
          }
        }
      }
    }

    // Common Dialogs triggered from navigation or dashboard
    if (showCreateProjectSheet) {
      ProjectsSheet(
        projects = allProjects,
        activeProjectId = currentProject?.id,
        onSelectProject = { proj ->
          viewModel.selectProject(proj)
          showCreateProjectSheet = false
          currentTab = StudioScreenTab.STUDIO
        },
        onCreateNewProject = { title, type, res ->
          viewModel.createNewProject(title, type, res)
          showCreateProjectSheet = false
          currentTab = StudioScreenTab.STUDIO
        },
        onDismiss = { showCreateProjectSheet = false }
      )
    }

    if (showCameraDialog) {
      CameraCaptureDialog(
        onDismiss = { showCameraDialog = false },
        onMediaCaptured = { file, type, durationMs ->
          viewModel.handleCapturedMedia(file, type, durationMs)
          showCameraDialog = false
          currentTab = StudioScreenTab.STUDIO
        }
      )
    }
  }
}
