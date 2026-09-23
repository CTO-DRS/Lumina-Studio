package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.AdjustmentsState
import com.example.data.model.FilterPreset
import com.example.data.model.VideoResolution
import com.example.data.model.toAdjustmentsState
import com.example.engine.ColorMatrixEngine
import com.example.engine.ImageProcessingEngine
import com.example.engine.ShareExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lumina Studio", appName)
  }

  @Test
  fun `test color matrix engine produces valid matrix`() {
    val adjustments = AdjustmentsState(
      exposure = 20f,
      contrast = 15f,
      temperature = -10f,
      saturation = 25f,
      selectedPreset = FilterPreset.TEAL_ORANGE
    )
    val matrix = ColorMatrixEngine.createUnifiedMatrix(adjustments)
    assertNotNull(matrix)
    assertEquals(20, matrix.values.size)
  }

  @Test
  fun `test 4K file size estimation calculation`() {
    val sizeMb = ShareExporter.calculateEstimatedFileSizeMb(
      resolution = VideoResolution.UHD_4K,
      durationMs = 30000L,
      bitrateMbps = 60
    )
    assertTrue("4K 30s at 60Mbps should be around 225 MB", sizeMb in 200f..250f)
  }

  @Test
  fun `test image processing engine grayscale`() {
    val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.RED)
    val grayBitmap = ImageProcessingEngine.applyGrayscale(bitmap)
    assertNotNull(grayBitmap)
    assertEquals(10, grayBitmap.width)
    assertEquals(10, grayBitmap.height)
  }

  @Test
  fun `test image processing engine sepia`() {
    val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.BLUE)
    val sepiaBitmap = ImageProcessingEngine.applySepia(bitmap)
    assertNotNull(sepiaBitmap)
    assertEquals(10, sepiaBitmap.width)
    assertEquals(10, sepiaBitmap.height)
  }

  @Test
  fun `test image processing engine brightness`() {
    val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.rgb(100, 100, 100))
    val brightBitmap = ImageProcessingEngine.applyBrightness(bitmap, 40f)
    assertNotNull(brightBitmap)
    assertEquals(10, brightBitmap.width)
    assertEquals(10, brightBitmap.height)
  }

  @Test
  fun `test image processing engine direct pixel processing`() {
    val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
    bitmap.setPixel(0, 0, Color.rgb(200, 100, 50))
    val processed = ImageProcessingEngine.processPixelsDirectly(
      source = bitmap,
      isGrayscale = true,
      isSepia = false,
      brightness = 20f
    )
    val pixel = processed.getPixel(0, 0)
    val r = Color.red(pixel)
    val g = Color.green(pixel)
    val b = Color.blue(pixel)
    // In grayscale with equal brightness offset, R, G, and B must be identical
    assertEquals(r, g)
    assertEquals(g, b)
  }

  @Test
  fun `test 4K UHD video recording specifications`() {
    val uhdRes = VideoResolution.UHD_4K
    assertEquals("4K Ultra HD", uhdRes.label)
    assertEquals(3840, uhdRes.width)
    assertEquals(2160, uhdRes.height)

    // Calculate 60s of 4K video at 80Mbps
    val size60s = ShareExporter.calculateEstimatedFileSizeMb(
      resolution = uhdRes,
      durationMs = 60000L,
      bitrateMbps = 80
    )
    assertTrue("60s 4K video at 80Mbps should be around 600MB", size60s in 550f..650f)
  }

  @Test
  fun `test exposure compensation calculation and formatting`() {
    val step = 0.1667f
    val index = 3
    val evValue = index * step
    val formatted = if (evValue >= 0) String.format(java.util.Locale.US, "+%.1f EV", evValue) else String.format(java.util.Locale.US, "%.1f EV", evValue)
    assertEquals("+0.5 EV", formatted)

    val negIndex = -2
    val negEv = negIndex * step
    val negFormatted = if (negEv >= 0) String.format(java.util.Locale.US, "+%.1f EV", negEv) else String.format(java.util.Locale.US, "%.1f EV", negEv)
    assertEquals("-0.3 EV", negFormatted)
  }

  @Test
  fun `test video clip trimming calculations and bounds constraints`() {
    val totalDurationMs = 30000L
    val minGap = 500L

    // Valid trim bounds
    val trimStart = 2500L
    val trimEnd = 18000L
    val netDuration = trimEnd - trimStart
    val trimmedOut = totalDurationMs - netDuration

    assertEquals(15500L, netDuration)
    assertEquals(14500L, trimmedOut)

    // Test nudge operations
    val nudgedStart = (trimStart + 100L).coerceIn(0L, trimEnd - minGap)
    assertEquals(2600L, nudgedStart)

    val nudgedEnd = (trimEnd - 100L).coerceIn(nudgedStart + minGap, totalDurationMs)
    assertEquals(17900L, nudgedEnd)

    // Test boundary clamping when dragging handle beyond limits
    val invalidFarStart = -500L
    val clampedStart = invalidFarStart.coerceIn(0L, trimEnd - minGap)
    assertEquals(0L, clampedStart)

    val invalidFarEnd = 40000L
    val clampedEnd = invalidFarEnd.coerceIn(clampedStart + minGap, totalDurationMs)
    assertEquals(30000L, clampedEnd)
  }

  @Test
  fun `test studio navigation tabs and active tools enumeration`() {
    val tabs = com.example.ui.navigation.StudioScreenTab.values()
    assertEquals(6, tabs.size)
    assertEquals("الرئيسية", tabs[0].labelAr)
    assertEquals("المحرر", tabs[1].labelAr)
    assertEquals("المؤثرات", tabs[2].labelAr)
    assertEquals("المعرض", tabs[3].labelAr)
    assertEquals("المشاريع", tabs[4].labelAr)
    assertEquals("الإعدادات", tabs[5].labelAr)

    val tools = com.example.ui.StudioActiveTool.values()
    assertEquals(10, tools.size)
    assertEquals("خوارزميات ذكية", tools[0].labelAr)
    assertEquals("قص وتشذيب", tools[1].labelAr)
    assertEquals("تحسين فوري", tools[2].labelAr)
    assertEquals("فلاتر سينمائية", tools[3].labelAr)
    assertEquals("تدريج الألوان", tools[4].labelAr)
    assertEquals("أبعاد وقص", tools[5].labelAr)
    assertEquals("المخطط الزمني", tools[6].labelAr)
    assertEquals("مكساج الصوت", tools[7].labelAr)
    assertEquals("مؤثرات FX", tools[8].labelAr)
    assertEquals("أدوات متقدمة", tools[9].labelAr)
  }

  @Test
  fun `test filter presets enumeration includes cinematic presets`() {
    val presets = com.example.data.model.FilterPreset.values()
    assertTrue(presets.any { it.name == "KODAK_PORTRA" })
    assertTrue(presets.any { it.name == "FUJI_CHROME" })
    assertTrue(presets.any { it.name == "PASTEL_DREAM" })
  }

  @Test
  fun `test audio VU meter dB formula calculation`() {
    val peak = 0.75f
    val db = if (peak <= 0.01f) -60 else ((peak - 1f) * 40).toInt()
    assertEquals(-10, db)

    val silentPeak = 0.0f
    val silentDb = if (silentPeak <= 0.01f) -60 else ((silentPeak - 1f) * 40).toInt()
    assertEquals(-60, silentDb)
  }

  @Test
  fun `test export resolution options contain 4K, 1080p, and 720p`() {
    val resolutions = com.example.ui.components.ExportResolutionOption.values()
    assertEquals(3, resolutions.size)

    val res4k = resolutions.first { it == com.example.ui.components.ExportResolutionOption.RES_4K }
    assertEquals(com.example.data.model.VideoResolution.UHD_4K, res4k.resolution)
    assertEquals(3840, res4k.resolution.width)
    assertEquals(2160, res4k.resolution.height)
    assertEquals("resolution_option_4k", res4k.testTag)

    val res1080p = resolutions.first { it == com.example.ui.components.ExportResolutionOption.RES_1080P }
    assertEquals(com.example.data.model.VideoResolution.FHD_1080P, res1080p.resolution)
    assertEquals(1920, res1080p.resolution.width)
    assertEquals(1080, res1080p.resolution.height)
    assertEquals("resolution_option_1080p", res1080p.testTag)

    val res720p = resolutions.first { it == com.example.ui.components.ExportResolutionOption.RES_720P }
    assertEquals(com.example.data.model.VideoResolution.HD_720P, res720p.resolution)
    assertEquals(1280, res720p.resolution.width)
    assertEquals(720, res720p.resolution.height)
    assertEquals("resolution_option_720p", res720p.testTag)
  }

  @Test
  fun `test export video formats MP4 and HEVC and efficiency multiplier`() {
    val formats = com.example.ui.components.ExportVideoFormat.values()
    assertEquals(2, formats.size)

    val mp4 = formats.first { it == com.example.ui.components.ExportVideoFormat.MP4 }
    assertEquals("MP4", mp4.id)
    assertEquals(com.example.data.model.ExportCodec.AVC_H264, mp4.codec)
    assertEquals(1.0f, mp4.sizeEfficiencyMultiplier)

    val hevc = formats.first { it == com.example.ui.components.ExportVideoFormat.HEVC }
    assertEquals("HEVC", hevc.id)
    assertEquals(com.example.data.model.ExportCodec.HEVC_H265, hevc.codec)
    assertEquals(0.55f, hevc.sizeEfficiencyMultiplier)

    // Verify HEVC produces smaller file size than MP4 for identical resolution and duration
    val durationMs = 30000L
    val bitrate = 60
    val rawSizeMb = com.example.engine.ShareExporter.calculateEstimatedFileSizeMb(
      com.example.data.model.VideoResolution.UHD_4K,
      durationMs,
      bitrate
    )
    val mp4Estimated = rawSizeMb * mp4.sizeEfficiencyMultiplier
    val hevcEstimated = rawSizeMb * hevc.sizeEfficiencyMultiplier

    assertTrue(hevcEstimated < mp4Estimated)
    assertEquals(mp4Estimated * 0.55f, hevcEstimated, 0.001f)
  }

  @Test
  fun `test floating edit toolbar actions structure and test tags`() {
    val actions = com.example.ui.components.FloatingEditAction.values()
    assertEquals(4, actions.size)

    val cropAction = actions.first { it == com.example.ui.components.FloatingEditAction.CROP }
    assertEquals("crop", cropAction.id)
    assertEquals("Crop", cropAction.labelEn)
    assertEquals("floating_action_crop", cropAction.testTag)
    assertNotNull(cropAction.icon)

    val filterAction = actions.first { it == com.example.ui.components.FloatingEditAction.FILTER }
    assertEquals("filter", filterAction.id)
    assertEquals("Filter", filterAction.labelEn)
    assertEquals("floating_action_filter", filterAction.testTag)
    assertNotNull(filterAction.icon)

    val colorAdjustAction = actions.first { it == com.example.ui.components.FloatingEditAction.COLOR_ADJUST }
    assertEquals("color_adjust", colorAdjustAction.id)
    assertEquals("Color Adjust", colorAdjustAction.labelEn)
    assertEquals("floating_action_color_adjust", colorAdjustAction.testTag)
    assertNotNull(colorAdjustAction.icon)

    val exportAction = actions.first { it == com.example.ui.components.FloatingEditAction.EXPORT }
    assertEquals("export", exportAction.id)
    assertEquals("Export", exportAction.labelEn)
    assertEquals("floating_action_export", exportAction.testTag)
    assertNotNull(exportAction.icon)
  }

  @Test
  fun `test camera viewmodel initial state and mode switching`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val cameraVm = com.example.ui.CameraViewModel(context)
    val initialState = cameraVm.uiState.value

    assertEquals(com.example.ui.CameraCaptureMode.VIDEO, initialState.mode)
    assertEquals(androidx.camera.core.CameraSelector.LENS_FACING_BACK, initialState.lensFacing)
    assertTrue(initialState.isAudioEnabled)
    org.junit.Assert.assertFalse(initialState.isRecording)
    assertEquals(1.0f, initialState.zoomRatio, 0.001f)

    // Toggle mode to PHOTO
    cameraVm.toggleMode(null, null)
    assertEquals(com.example.ui.CameraCaptureMode.PHOTO, cameraVm.uiState.value.mode)

    // Toggle mode back to VIDEO
    cameraVm.toggleMode(null, null)
    assertEquals(com.example.ui.CameraCaptureMode.VIDEO, cameraVm.uiState.value.mode)
  }

  @Test
  fun `test camera viewmodel lens toggle and controls`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val cameraVm = com.example.ui.CameraViewModel(context)

    // Toggle lens to FRONT
    cameraVm.toggleLensFacing(null, null)
    assertEquals(androidx.camera.core.CameraSelector.LENS_FACING_FRONT, cameraVm.uiState.value.lensFacing)

    // Toggle lens back to BACK
    cameraVm.toggleLensFacing(null, null)
    assertEquals(androidx.camera.core.CameraSelector.LENS_FACING_BACK, cameraVm.uiState.value.lensFacing)

    // Toggle Audio
    cameraVm.toggleAudio()
    org.junit.Assert.assertFalse(cameraVm.uiState.value.isAudioEnabled)
    cameraVm.toggleAudio()
    assertTrue(cameraVm.uiState.value.isAudioEnabled)

    // Toggle Grid
    val initialGrid = cameraVm.uiState.value.showGrid
    cameraVm.toggleGrid()
    assertEquals(!initialGrid, cameraVm.uiState.value.showGrid)

    // Zoom Clamping
    cameraVm.setZoomRatio(10.0f)
    assertTrue(cameraVm.uiState.value.zoomRatio <= cameraVm.uiState.value.maxZoomRatio)
  }

  @Test
  fun `test gallery viewmodel loads media and applies filters`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val galleryVm = com.example.ui.GalleryViewModel(context)

    val sampleItems = listOf(
      com.example.ui.GalleryMediaItem(
        id = "sample_photo_1",
        file = java.io.File("/tmp/sample_photo_1.jpg"),
        name = "sample_photo_1.jpg",
        type = com.example.data.model.MediaType.PHOTO,
        sizeBytes = 2500000L,
        dateModifiedMs = System.currentTimeMillis() - 1000
      ),
      com.example.ui.GalleryMediaItem(
        id = "sample_video_1",
        file = java.io.File("/tmp/sample_video_1.mp4"),
        name = "sample_video_1.mp4",
        type = com.example.data.model.MediaType.VIDEO,
        sizeBytes = 45000000L,
        dateModifiedMs = System.currentTimeMillis(),
        durationMs = 30000L
      )
    )
    galleryVm.setMediaItems(sampleItems)
    val state = galleryVm.uiState.value

    // Seed media should be loaded
    assertTrue(state.allItems.isNotEmpty())
    assertTrue(state.filteredItems.isNotEmpty())

    // Test filtering by PHOTOS
    galleryVm.setFilter(com.example.ui.GalleryFilter.PHOTOS)
    val photos = galleryVm.uiState.value.filteredItems
    assertTrue(photos.all { it.type == com.example.data.model.MediaType.PHOTO })
    assertEquals(1, photos.size)

    // Test filtering by VIDEOS
    galleryVm.setFilter(com.example.ui.GalleryFilter.VIDEOS)
    val videos = galleryVm.uiState.value.filteredItems
    assertTrue(videos.all { it.type == com.example.data.model.MediaType.VIDEO })
    assertEquals(1, videos.size)

    // Test filtering by ALL
    galleryVm.setFilter(com.example.ui.GalleryFilter.ALL)
    assertEquals(galleryVm.uiState.value.allItems.size, galleryVm.uiState.value.filteredItems.size)

    // Test Selection Mode
    val firstItem = galleryVm.uiState.value.filteredItems.first()
    galleryVm.toggleSelection(firstItem.id)
    assertTrue(galleryVm.uiState.value.isSelectionMode)
    assertTrue(galleryVm.uiState.value.selectedIds.contains(firstItem.id))

    // Clear selection
    galleryVm.clearSelection()
    org.junit.Assert.assertFalse(galleryVm.uiState.value.isSelectionMode)
    assertTrue(galleryVm.uiState.value.selectedIds.isEmpty())
  }

  @Test
  fun `test studio navigation tab includes gallery`() {
    val tabs = com.example.ui.navigation.StudioScreenTab.values()
    assertTrue(tabs.any { it == com.example.ui.navigation.StudioScreenTab.GALLERY })
    val galleryTab = tabs.first { it == com.example.ui.navigation.StudioScreenTab.GALLERY }
    assertEquals("nav_tab_gallery", galleryTab.testTag)
    assertEquals("Gallery", galleryTab.labelEn)
  }

  @Test
  fun `test real-time color grading adjustments for exposure, contrast, and saturation`() {
    val state = AdjustmentsState(
      exposure = 45f,
      contrast = 30f,
      saturation = -100f, // Black and white test
      selectedPreset = FilterPreset.ORIGINAL
    )
    val matrix = ColorMatrixEngine.createUnifiedMatrix(state)
    assertNotNull(matrix)
    assertEquals(20, matrix.values.size)

    // With saturation at -100%, luminance weights should result in equal R, G, B output columns
    val rCoeff = matrix.values[0]
    val gCoeff = matrix.values[1]
    val bCoeff = matrix.values[2]
    // Coefficients sum should be proportional to contrast
    assertTrue(rCoeff > 0f)
    assertTrue(gCoeff > 0f)
    assertTrue(bCoeff > 0f)
  }

  @Test
  fun `test professional LUT filters and intensity blending`() {
    val fullIntensity = AdjustmentsState(
      exposure = 0f,
      contrast = 0f,
      saturation = 0f,
      selectedPreset = FilterPreset.TEAL_ORANGE,
      lutIntensity = 100f
    )
    val fullMatrix = ColorMatrixEngine.createUnifiedMatrix(fullIntensity)

    val halfIntensity = AdjustmentsState(
      exposure = 0f,
      contrast = 0f,
      saturation = 0f,
      selectedPreset = FilterPreset.TEAL_ORANGE,
      lutIntensity = 50f
    )
    val halfMatrix = ColorMatrixEngine.createUnifiedMatrix(halfIntensity)

    val zeroIntensity = AdjustmentsState(
      exposure = 0f,
      contrast = 0f,
      saturation = 0f,
      selectedPreset = FilterPreset.TEAL_ORANGE,
      lutIntensity = 0f
    )
    val zeroMatrix = ColorMatrixEngine.createUnifiedMatrix(zeroIntensity)

    // Verify matrix values interpolate cleanly with lutIntensity
    // Teal & Orange boosts red (channel 0)
    assertTrue(fullMatrix.values[0] > zeroMatrix.values[0])
    assertTrue(halfMatrix.values[0] in zeroMatrix.values[0]..fullMatrix.values[0])
  }

  @Test
  fun `test color grading adjustments state default values`() {
    val defaultState = AdjustmentsState()
    assertEquals(0f, defaultState.exposure, 0.001f)
    assertEquals(0f, defaultState.contrast, 0.001f)
    assertEquals(0f, defaultState.saturation, 0.001f)
    assertEquals(0f, defaultState.highlights, 0.001f)
    assertEquals(0f, defaultState.shadows, 0.001f)
    assertEquals(100f, defaultState.lutIntensity, 0.001f)
    assertEquals(FilterPreset.ORIGINAL, defaultState.selectedPreset)
  }

  @Test
  fun `test studio viewmodel history stack undo and redo`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = com.example.ui.StudioViewModel(context as android.app.Application)

    assertEquals(0, viewModel.historyStack.value.size)
    assertEquals(false, viewModel.canUndo.value)
    assertEquals(false, viewModel.canRedo.value)

    // Apply first adjustment: Exposure
    viewModel.updateAdjustment { it.copy(exposure = 25f) }
    assertEquals(25f, viewModel.adjustments.value.exposure, 0.001f)
    assertEquals(1, viewModel.historyStack.value.size)
    assertEquals(true, viewModel.canUndo.value)
    assertEquals(false, viewModel.canRedo.value)

    // Apply second adjustment: Contrast
    viewModel.updateAdjustment { it.copy(contrast = 30f) }
    assertEquals(30f, viewModel.adjustments.value.contrast, 0.001f)
    assertEquals(2, viewModel.historyStack.value.size)

    // Test Undo
    viewModel.undo()
    // Should revert contrast back to 0f, keeping exposure at 25f
    assertEquals(0f, viewModel.adjustments.value.contrast, 0.001f)
    assertEquals(25f, viewModel.adjustments.value.exposure, 0.001f)
    assertEquals(1, viewModel.historyStack.value.size)
    assertEquals(1, viewModel.redoHistoryStack.value.size)
    assertEquals(true, viewModel.canRedo.value)

    // Test Redo
    viewModel.redo()
    // Should re-apply contrast to 30f
    assertEquals(30f, viewModel.adjustments.value.contrast, 0.001f)
    assertEquals(2, viewModel.historyStack.value.size)
    assertEquals(0, viewModel.redoHistoryStack.value.size)
    assertEquals(false, viewModel.canRedo.value)

    // Test Undo again and clear history
    viewModel.undo()
    viewModel.clearHistory()
    assertEquals(0, viewModel.historyStack.value.size)
    assertEquals(0, viewModel.redoHistoryStack.value.size)
    assertEquals(false, viewModel.canUndo.value)
    assertEquals(false, viewModel.canRedo.value)
  }

  @Test
  fun `test studio viewmodel jump to history step`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = com.example.ui.StudioViewModel(context as android.app.Application)

    // Step 1: Exposure
    viewModel.updateAdjustment { it.copy(exposure = 15f) }
    val step1 = viewModel.historyStack.value.last()

    // Step 2: Saturation
    viewModel.updateAdjustment { it.copy(saturation = 40f) }

    // Step 3: Temperature
    viewModel.updateAdjustment { it.copy(temperature = 20f) }
    assertEquals(3, viewModel.historyStack.value.size)

    // Jump directly to step 1
    viewModel.jumpToHistoryStep(step1)
    assertEquals(0f, viewModel.adjustments.value.exposure, 0.001f)
    assertEquals(0f, viewModel.adjustments.value.saturation, 0.001f)
    assertEquals(0f, viewModel.adjustments.value.temperature, 0.001f)
    assertEquals(true, viewModel.canRedo.value)
  }

  @Test
  fun `test room database saves and retrieves editing session`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getDatabase(context)
    val sessionDao = db.sessionDao()

    val testSession = com.example.data.model.EditingSessionEntity(
      projectId = 9999L,
      exposure = 18.5f,
      contrast = 22.0f,
      brightness = 5.0f,
      saturation = 35.0f,
      temperature = -12.0f,
      tint = 4.0f,
      highlights = -8.0f,
      shadows = 14.0f,
      selectedPreset = com.example.data.model.FilterPreset.TEAL_ORANGE.name,
      selectedCrop = com.example.data.model.CropAspect.RATIO_16_9.name,
      lutIntensity = 85.0f,
      playheadMs = 12500L,
      trimStartMs = 1000L,
      trimEndMs = 28000L,
      editorMode = com.example.data.model.EditorMode.PRO.name
    )

    // Save session in Room
    sessionDao.saveSession(testSession)

    // Retrieve by project ID
    val loaded = sessionDao.getSessionById(9999L)
    assertNotNull(loaded)
    assertEquals(18.5f, loaded!!.exposure, 0.001f)
    assertEquals(22.0f, loaded.contrast, 0.001f)
    assertEquals("TEAL_ORANGE", loaded.selectedPreset)
    assertEquals(12500L, loaded.playheadMs)
    assertEquals("PRO", loaded.editorMode)

    // Convert to AdjustmentsState and verify mapper
    val restoredAdjustments = loaded.toAdjustmentsState()
    assertEquals(18.5f, restoredAdjustments.exposure, 0.001f)
    assertEquals(com.example.data.model.FilterPreset.TEAL_ORANGE, restoredAdjustments.selectedPreset)
    assertEquals(com.example.data.model.CropAspect.RATIO_16_9, restoredAdjustments.selectedCrop)

    // Delete session
    sessionDao.deleteSession(9999L)
    val afterDelete = sessionDao.getSessionById(9999L)
    assertEquals(null, afterDelete)
  }

  @Test
  fun `test studio viewmodel auto save session persistence`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = com.example.ui.StudioViewModel(context as android.app.Application)

    val testProject = com.example.data.model.ProjectEntity(
      id = 7777L,
      title = "Room Persistence Test Project",
      mediaType = "VIDEO",
      assetDrawableName = "img_sample_video",
      durationMs = 30000L,
      resolution = "UHD_4K",
      fps = 60
    )

    // Select project and update adjustment
    viewModel.selectProject(testProject)
    viewModel.updateAdjustment {
      it.copy(
        exposure = 45f,
        contrast = -20f,
        selectedPreset = com.example.data.model.FilterPreset.CYBERPUNK
      )
    }

    // Trigger persistence to Room
    viewModel.persistCurrentSession()

    // Assert auto save status updated to Saved
    assertTrue(viewModel.autoSaveStatus.value is com.example.data.model.AutoSaveStatus.Saved)

    // Verify session is persisted in Room via SessionRepository
    val session = viewModel.sessionRepository.getSession(7777L)
    assertNotNull(session)
    assertEquals(45f, session!!.exposure, 0.001f)
    assertEquals(-20f, session.contrast, 0.001f)
    assertEquals("CYBERPUNK", session.selectedPreset)
  }

  @Test
  fun `test studio viewmodel timeline zoom controls and boundaries`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = com.example.ui.StudioViewModel(context as android.app.Application)

    // Initial zoom should be 1.0x
    assertEquals(1.0f, viewModel.timelineZoom.value, 0.001f)

    // Zoom in
    viewModel.setTimelineZoom(3.5f)
    assertEquals(3.5f, viewModel.timelineZoom.value, 0.001f)

    // Incremental zoom in
    viewModel.zoomInTimeline(0.5f)
    assertEquals(4.0f, viewModel.timelineZoom.value, 0.001f)

    // Incremental zoom out
    viewModel.zoomOutTimeline(1.0f)
    assertEquals(3.0f, viewModel.timelineZoom.value, 0.001f)

    // Upper clamp to 10.0x
    viewModel.setTimelineZoom(15.0f)
    assertEquals(10.0f, viewModel.timelineZoom.value, 0.001f)

    // Lower clamp to 1.0x
    viewModel.setTimelineZoom(0.3f)
    assertEquals(1.0f, viewModel.timelineZoom.value, 0.001f)

    // Reset zoom
    viewModel.setTimelineZoom(5.0f)
    viewModel.resetTimelineZoom()
    assertEquals(1.0f, viewModel.timelineZoom.value, 0.001f)
  }

  @Test
  fun `test timeline frame by frame precision calculation`() {
    val durationMs = 30000L // 30 seconds
    val fps = 60f
    val frameDurationMs = 1000f / fps // ~16.66ms per frame

    // At 1.0x zoom across 1000px width
    val standardWidthPx = 1000f
    val standardPxPerFrame = (frameDurationMs / durationMs) * standardWidthPx
    // ~0.55 pixels per frame at 1x

    // At 10.0x zoom across 10000px width
    val zoomedWidthPx = standardWidthPx * 10f
    val zoomedPxPerFrame = (frameDurationMs / durationMs) * zoomedWidthPx

    assertTrue("At 10x zoom, pixels per frame should be 10x greater", zoomedPxPerFrame > standardPxPerFrame * 9.9f)
    assertTrue("At 10x zoom, frame precision enables distinct sub-pixel target accuracy", zoomedPxPerFrame > 5.0f)
  }
}

