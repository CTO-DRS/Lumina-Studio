package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.EditorMode
import com.example.data.model.ProjectEntity
import com.example.ui.components.TopStudioBar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun studio_top_bar_screenshot() {
    val sampleProject = ProjectEntity(
      id = 1L,
      title = "Cyberpunk Neo-Tokyo 4K",
      mediaType = "VIDEO",
      assetDrawableName = "img_sample_video",
      durationMs = 45000L,
      resolution = "UHD_4K",
      fps = 60,
      isCloudSynced = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        TopStudioBar(
          currentProject = sampleProject,
          editorMode = EditorMode.PRO,
          isCompareActive = true,
          onOpenProjects = {},
          onToggleMode = {},
          onToggleCompare = {},
          onUndo = {},
          onRedo = {},
          onOpenCamera = {},
          onExportClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/studio_bar.png")
  }
}

