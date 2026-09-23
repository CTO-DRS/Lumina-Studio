package com.lumina.studio.ui.editor

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.lumina.studio.engine.audio.AudioGraphEngine
import com.lumina.studio.data.model.TrackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumina.studio.data.model.EditorMode
import com.lumina.studio.data.model.EditorStudioType
import com.lumina.studio.ui.components.CameraCaptureDialog
import com.lumina.studio.ui.components.ExportBottomSheet
import com.lumina.studio.ui.components.FloatingEditAction
import com.lumina.studio.ui.components.FloatingEditToolbar
import com.lumina.studio.ui.components.HistogramVuMeter
import com.lumina.studio.ui.components.ImportMediaDialog
import com.lumina.studio.ui.components.MediaCanvasView
import com.lumina.studio.ui.components.ProToolsPanel
import com.lumina.studio.ui.components.ProjectsSheet
import com.lumina.studio.ui.components.TimelineView
import com.lumina.studio.ui.components.TopStudioBar
import com.lumina.studio.ui.components.VideoTrimSlider
import com.lumina.studio.ui.components.tools.AudioMixerPanel
import com.lumina.studio.ui.components.tools.ColorGradingPanel
import com.lumina.studio.ui.components.tools.CropRatioPanel
import com.lumina.studio.ui.components.tools.FiltersGalleryPanel
import com.lumina.studio.ui.components.tools.InstantAiEnhancePanel
import com.lumina.studio.ui.components.tools.SmartAlgorithmsPanel
import com.lumina.studio.ui.components.tools.VisualFxPanel
import androidx.compose.material.icons.filled.PrecisionManufacturing
import com.lumina.studio.ui.theme.AudioTrackColor
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
import com.lumina.studio.ui.viewmodels.StudioViewModel

enum class StudioActiveTool(
  val labelAr: String,
  val labelEn: String,
  val icon: ImageVector,
  val accentColor: Color,
  val testTag: String
) {
  SMART_ALGORITHMS("خوارزميات ذكية", "Smart Algorithms", Icons.Default.PrecisionManufacturing, CyberGold, "tool_btn_smart_algo"),
  TRIM("قص وتشذيب", "Trim & Cut", Icons.Default.ContentCut, CyberGold, "tool_btn_trim"),
  AI_ENHANCE("تحسين فوري", "AI Enhance", Icons.Default.AutoAwesome, ElectricCyan, "tool_btn_ai_enhance"),
  FILTERS("فلاتر سينمائية", "LUT Filters", Icons.Default.Filter, NeonViolet, "tool_btn_filters"),
  COLOR_GRADING("تدريج الألوان", "Color Grading", Icons.Default.Tune, CyberGold, "tool_btn_color_grading"),
  CROP("أبعاد وقص", "Crop & Aspect", Icons.Default.Crop, EmeraldGreen, "tool_btn_crop"),
  TIMELINE("المخطط الزمني", "Timeline Tracks", Icons.Default.ViewTimeline, ElectricCyan, "tool_btn_timeline"),
  AUDIO("مكساج الصوت", "Audio & VU", Icons.Default.GraphicEq, AudioTrackColor, "tool_btn_audio"),
  FX("مؤثرات FX", "Visual FX", Icons.Default.AutoFixHigh, SunsetCoral, "tool_btn_fx"),
  PRO_SUITE("أدوات متقدمة", "Pro Suite", Icons.Default.Speed, SunsetCoral, "tool_btn_pro_suite")
}

@Composable
fun StudioScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
  val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
  val editorMode by viewModel.editorMode.collectAsStateWithLifecycle()
  val adjustments by viewModel.adjustments.collectAsStateWithLifecycle()
  val autoSaveStatus by viewModel.autoSaveStatus.collectAsStateWithLifecycle()

  val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
  val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
  val undoCount by viewModel.undoCount.collectAsStateWithLifecycle()
  val redoCount by viewModel.redoCount.collectAsStateWithLifecycle()
  val historyStack by viewModel.historyStack.collectAsStateWithLifecycle()
  val redoHistoryStack by viewModel.redoHistoryStack.collectAsStateWithLifecycle()
  val lastActionDescription by viewModel.lastActionDescription.collectAsStateWithLifecycle()

  val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
  val playheadMs by viewModel.playheadMs.collectAsStateWithLifecycle()
  val timelineClips by viewModel.timelineClips.collectAsStateWithLifecycle()
  val timelineZoom by viewModel.timelineZoom.collectAsStateWithLifecycle()
  val selectedClipIdForGrading by viewModel.selectedClipIdForGrading.collectAsStateWithLifecycle()

  val peakLeft by viewModel.audioPeakLeft.collectAsStateWithLifecycle()
  val peakRight by viewModel.audioPeakRight.collectAsStateWithLifecycle()

  val isCompareActive by viewModel.isCompareActive.collectAsStateWithLifecycle()
  val compareSliderPosition by viewModel.compareSliderPosition.collectAsStateWithLifecycle()
  val isLetterboxActive by viewModel.isLetterboxActive.collectAsStateWithLifecycle()
  val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()

  val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
  val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
  val exportComplete by viewModel.exportComplete.collectAsStateWithLifecycle()
  val exportSpeedFps by viewModel.exportSpeedFps.collectAsStateWithLifecycle()
  val exportStage by viewModel.exportStage.collectAsStateWithLifecycle()
  val exportResultPath by viewModel.exportResultPath.collectAsStateWithLifecycle()
  val exportError by viewModel.exportError.collectAsStateWithLifecycle()
  val exportedShareFile by viewModel.exportedShareFile.collectAsStateWithLifecycle()

  // REAL analysis data (pixel histogram + edge/zebra masks + PCM envelope)
  val histogramData by viewModel.histogramData.collectAsStateWithLifecycle()
  val edgeOverlay by viewModel.edgeOverlayBitmap.collectAsStateWithLifecycle()
  val zebraOverlay by viewModel.zebraOverlayBitmap.collectAsStateWithLifecycle()
  val audioEnvelope by viewModel.audioEnvelope.collectAsStateWithLifecycle()

  // REAL mixer state (applied to the actual audio pipeline)
  val masterAudioVolume by viewModel.masterAudioVolume.collectAsStateWithLifecycle()
  val isAudioMuted by viewModel.isAudioMuted.collectAsStateWithLifecycle()

  // Smart Computational Photography & Video Algorithms State
  val smartBeatMarkers by viewModel.smartBeatMarkers.collectAsStateWithLifecycle()
  val smartAlgorithmNotice by viewModel.smartAlgorithmNotice.collectAsStateWithLifecycle()

  // Video Trimming State
  val trimStartMs by viewModel.trimStartMs.collectAsStateWithLifecycle()
  val trimEndMs by viewModel.trimEndMs.collectAsStateWithLifecycle()
  val isTrimmingActive by viewModel.isTrimmingActive.collectAsStateWithLifecycle()
  val trimFps by viewModel.trimFps.collectAsStateWithLifecycle()
  val trimSnapToFrames by viewModel.trimSnapToFrames.collectAsStateWithLifecycle()

  // Video vs Photo Studio Mode
  val editorStudioType by viewModel.editorStudioType.collectAsStateWithLifecycle()
  val context = LocalContext.current

  // Active Tool Selection State (Defaults to AI Enhance or Trim)
  var activeTool by remember {
    mutableStateOf(if (currentProject?.mediaType == "VIDEO") StudioActiveTool.TRIM else StudioActiveTool.AI_ENHANCE)
  }

  // Modals state
  var showProjectsSheet by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var showCameraDialog by remember { mutableStateOf(false) }
  var showImportDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 76.dp)
        .testTag("studio_main_screen")
    ) {
    // 1. Top Studio Bar
    TopStudioBar(
      currentProject = currentProject,
      editorMode = editorMode,
      isCompareActive = isCompareActive,
      onOpenProjects = { showProjectsSheet = true },
      onToggleMode = {
        val nextMode = if (editorMode == EditorMode.BEGINNER) EditorMode.PRO else EditorMode.BEGINNER
        viewModel.setEditorMode(nextMode)
      },
      onToggleCompare = { viewModel.toggleCompare() },
      onUndo = { viewModel.undo() },
      onRedo = { viewModel.redo() },
      onOpenCamera = { showCameraDialog = true },
      onExportClick = { showExportDialog = true },
      onOpenImport = { showImportDialog = true },
      canUndo = canUndo,
      canRedo = canRedo,
      undoCount = undoCount,
      redoCount = redoCount,
      autoSaveStatus = autoSaveStatus,
      onSaveNow = { viewModel.saveSessionNow() }
    )

    // 2. Interactive Media Preview Canvas (REAL frame at playhead + live ColorMatrix)
    MediaCanvasView(
      project = currentProject,
      adjustments = adjustments,
      playheadMs = playheadMs,
      isCompareActive = isCompareActive,
      compareSliderPosition = compareSliderPosition,
      onCompareSliderChange = { viewModel.setCompareSlider(it) },
      isLetterboxActive = isLetterboxActive,
      onToggleLetterbox = { viewModel.toggleLetterbox() },
      isPlaying = isPlaying,
      edgeOverlay = edgeOverlay,
      zebraOverlay = zebraOverlay,
      modifier = Modifier.fillMaxWidth()
    )

    // 2.5. Mini Playback Transport Strip (for Video workflows)
    if (editorStudioType == EditorStudioType.VIDEO) {
      MiniTransportControls(
        isPlaying = isPlaying,
        playheadMs = playheadMs,
        totalDurationMs = currentProject?.durationMs ?: 30000L,
        onTogglePlay = { viewModel.togglePlayback() },
        onStepFrame = { viewModel.stepFrame(it) },
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
      )
    }

    // 3. Pro Scopes (REAL pixel histogram + REAL PCM amplitude VU meter)
    HistogramVuMeter(
      histogramData = histogramData,
      peakLeft = peakLeft,
      peakRight = peakRight,
      peakLeftDb = AudioGraphEngine.formatDb(peakLeft),
      peakRightDb = AudioGraphEngine.formatDb(peakRight),
      modifier = Modifier.padding(horizontal = 10.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 3.5. Studio Mode Switcher (Video Editor vs Photo Editor) + Real Phone Actions
    EditorStudioModeSwitcher(
      selectedType = editorStudioType,
      onSelectType = { type ->
        viewModel.setEditorStudioType(type)
        if (type == EditorStudioType.PHOTO && (activeTool == StudioActiveTool.TRIM || activeTool == StudioActiveTool.TIMELINE || activeTool == StudioActiveTool.AUDIO)) {
          activeTool = StudioActiveTool.COLOR_GRADING
        } else if (type == EditorStudioType.VIDEO && activeTool == StudioActiveTool.COLOR_GRADING) {
          activeTool = StudioActiveTool.TRIM
        }
      },
      onImportClick = { showImportDialog = true },
      onSaveToGalleryClick = {
        viewModel.saveCurrentMediaToGallery(context) { result ->
          if (result.isSuccess) {
            Toast.makeText(
              context,
              "✓ تم الحفظ بنجاح في المعرض:\n${result.displayPath}",
              Toast.LENGTH_LONG
            ).show()
          } else {
            Toast.makeText(
              context,
              "حدث خطأ أثناء الحفظ: ${result.errorMessage}",
              Toast.LENGTH_SHORT
            ).show()
          }
        }
      },
      onShareClick = {
        viewModel.shareCurrentMedia(context) { success ->
          if (!success) {
            Toast.makeText(context, "لم يتم العثور على وسائط للمشاركة", Toast.LENGTH_SHORT).show()
          }
        }
      },
      modifier = Modifier.padding(horizontal = 10.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 4. Interactive Dedicated Tools Selector Bar (Filtered by Video vs Photo Studio)
    StudioToolSelectorBar(
      activeTool = activeTool,
      editorStudioType = editorStudioType,
      onSelectTool = { activeTool = it },
      modifier = Modifier.padding(horizontal = 10.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 5. Active Tool Panel with its own bespoke custom UI design
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp)
        .testTag("active_tool_container")
    ) {
      AnimatedContent(
        targetState = activeTool,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tool_content_animation"
      ) { targetTool ->
        when (targetTool) {
          StudioActiveTool.SMART_ALGORITHMS -> {
            SmartAlgorithmsPanel(
              editorStudioType = editorStudioType,
              adjustments = adjustments,
              beatMarkers = smartBeatMarkers,
              smartNotice = smartAlgorithmNotice,
              onDismissNotice = { viewModel.dismissSmartNotice() },
              onApplyGrayWorldAwb = { viewModel.applyGrayWorldAwb() },
              onApplySmartAutoTone = { viewModel.applySmartAutoTone() },
              onApplySmartSharpness = { viewModel.applySmartSharpness() },
              onToggleFocusPeaking = { viewModel.toggleFocusPeaking() },
              onToggleZebraStripes = { viewModel.toggleZebraStripes() },
              onDetectAudioBeats = { bpm -> viewModel.detectAudioBeats(bpm) },
              onClearBeatMarkers = { viewModel.clearBeatMarkers() },
              onAutoSplitOnBeats = { viewModel.autoSplitOnBeats() },
              onApplyGyroStabilization = { level -> viewModel.applyGyroStabilization(level) },
              onApplyAutoHorizonLeveling = { viewModel.applyAutoHorizonLeveling() },
              onResetHorizonLeveling = { viewModel.resetHorizonLeveling() },
              onApplySpeedRamp = { preset -> viewModel.applySpeedRamp(preset) }
            )
          }

          StudioActiveTool.TRIM -> {
            VideoTrimSlider(
              trimStartMs = trimStartMs,
              trimEndMs = trimEndMs,
              totalDurationMs = currentProject?.durationMs ?: 30000L,
              playheadMs = playheadMs,
              isPlaying = isPlaying,
              mediaUri = currentProject?.mediaUri,
              audioWaveform = audioEnvelope?.waveformBars(96),
              onTrimChange = { start, end -> viewModel.updateTrim(start, end) },
              onSeekTo = { viewModel.seekTo(it) },
              onTogglePlay = { viewModel.togglePlayback() },
              onNudgeStart = { viewModel.nudgeTrimStart(it) },
              onNudgeEnd = { viewModel.nudgeTrimEnd(it) },
              onSetStartToPlayhead = { viewModel.setTrimStartToPlayhead() },
              onSetEndToPlayhead = { viewModel.setTrimEndToPlayhead() },
              onApplyTrim = { viewModel.applyTrimToClip() },
              onResetTrim = { viewModel.resetTrim() }
            )
          }

          StudioActiveTool.AI_ENHANCE -> {
            InstantAiEnhancePanel(
              adjustments = adjustments,
              onAutoEnhanceChange = { value ->
                viewModel.updateAdjustment { it.copy(autoEnhance = value) }
              },
              onQuickAiBoost = { exp, cont, sat, sharp ->
                viewModel.updateAdjustment {
                  it.copy(
                    autoEnhance = 85f,
                    exposure = exp,
                    contrast = cont,
                    saturation = sat,
                    sharpness = sharp
                  )
                }
              },
              onReset = {
                viewModel.updateAdjustment { it.copy(autoEnhance = 0f) }
              }
            )
          }

          StudioActiveTool.FILTERS -> {
            FiltersGalleryPanel(
              selectedPreset = adjustments.selectedPreset,
              lutIntensity = adjustments.lutIntensity,
              onSelectPreset = { preset -> viewModel.selectFilterPreset(preset) },
              onIntensityChange = { intensity -> viewModel.setFilterIntensity(intensity) },
              onResetPreset = { viewModel.selectFilterPreset(com.lumina.studio.data.model.FilterPreset.ORIGINAL) },
              timelineClips = timelineClips,
              selectedClipId = selectedClipIdForGrading,
              onSelectClip = { clipId -> viewModel.selectClipForGrading(clipId) },
              onApplyToAllClips = { preset, intensity -> viewModel.applyFilterToAllVideoClips(preset, intensity) },
              isCompareActive = isCompareActive,
              onToggleCompare = { viewModel.toggleCompare() }
            )
          }

          StudioActiveTool.COLOR_GRADING -> {
            ColorGradingPanel(
              adjustments = adjustments,
              onAdjustmentChange = { transform -> viewModel.updateAdjustment(transform) },
              onReset = { viewModel.resetAllAdjustments() },
              onSelectPreset = { preset -> viewModel.selectFilterPreset(preset) },
              isCompareActive = isCompareActive,
              onToggleCompare = { viewModel.toggleCompare() },
              mediaType = currentProject?.mediaType ?: "VIDEO",
              canUndo = canUndo,
              canRedo = canRedo,
              onUndo = { viewModel.undo() },
              onRedo = { viewModel.redo() },
              undoCount = undoCount,
              redoCount = redoCount,
              historyStack = historyStack,
              redoHistoryStack = redoHistoryStack,
              lastActionDescription = lastActionDescription,
              onJumpToHistoryStep = { entry -> viewModel.jumpToHistoryStep(entry) },
              onClearHistory = { viewModel.clearHistory() },
              autoSaveStatus = autoSaveStatus,
              onSaveNow = { viewModel.saveSessionNow() }
            )
          }

          StudioActiveTool.CROP -> {
            CropRatioPanel(
              selectedCrop = adjustments.selectedCrop,
              onCropSelect = { crop -> viewModel.selectCropAspect(crop) }
            )
          }

          StudioActiveTool.TIMELINE -> {
            TimelineView(
              isPlaying = isPlaying,
              playheadMs = playheadMs,
              durationMs = currentProject?.durationMs ?: 30000L,
              timelineClips = timelineClips,
              isTrimmingActive = isTrimmingActive,
              trimStartMs = trimStartMs,
              trimEndMs = trimEndMs,
              timelineZoom = timelineZoom,
              fps = trimFps,
              snapToFrames = trimSnapToFrames,
              onZoomChange = { viewModel.setTimelineZoom(it) },
              beatMarkers = smartBeatMarkers,
              audioWaveform = audioEnvelope?.waveformBars(160),
              audioClipCount = timelineClips.count { it.trackType == TrackType.AUDIO },
              onTogglePlay = { viewModel.togglePlayback() },
              onSeekTo = { viewModel.seekTo(it) },
              onStepFrame = { viewModel.stepFrame(it) },
              onSplitClip = { viewModel.splitClipAtPlayhead() },
              onToggleTrimMode = { viewModel.toggleTrimMode() },
              onTrimChange = { start, end -> viewModel.updateTrim(start, end) },
              onNudgeStartFrame = { delta -> viewModel.nudgeTrimStartFrame(delta) },
              onNudgeEndFrame = { delta -> viewModel.nudgeTrimEndFrame(delta) },
              onSetStartToPlayhead = { viewModel.setTrimStartToPlayhead() },
              onSetEndToPlayhead = { viewModel.setTrimEndToPlayhead() },
              onPreviewStartFrame = { viewModel.previewTrimInPoint() },
              onPreviewEndFrame = { viewModel.previewTrimOutPoint() },
              onApplyTrimExtract = { viewModel.applyTrimExtract() },
              onApplyRippleDelete = { viewModel.applyRippleDelete() },
              onResetTrim = { viewModel.resetTrim() },
              onToggleSnapToFrames = { viewModel.toggleTrimSnapToFrames() },
              onFpsChange = { viewModel.setTrimFps(it) }
            )
          }

          StudioActiveTool.AUDIO -> {
            AudioMixerPanel(
              volume = masterAudioVolume,
              isMuted = isAudioMuted,
              peakLeft = peakLeft,
              peakRight = peakRight,
              audioClips = timelineClips.filter { it.trackType == TrackType.AUDIO },
              onVolumeChange = { viewModel.setMasterAudioVolume(it) },
              onToggleMute = { viewModel.toggleAudioMute() },
              onClipVolumeChange = { clipId, vol -> viewModel.setClipVolume(clipId, vol) },
              onClipToggleMute = { clipId -> viewModel.toggleClipMute(clipId) }
            )
          }

          StudioActiveTool.FX -> {
            VisualFxPanel(
              adjustments = adjustments,
              onAdjustmentChange = { transform -> viewModel.updateAdjustment(transform) },
              onReset = {
                viewModel.updateAdjustment {
                  it.copy(
                    vignette = 0f,
                    filmGrain = 0f,
                    bloomGlow = 0f,
                    glitchRgb = 0f,
                    blur = 0f
                  )
                }
              }
            )
          }

          StudioActiveTool.PRO_SUITE -> {
            ProToolsPanel(
              adjustments = adjustments,
              onAdjustmentChange = { transform -> viewModel.updateAdjustment(transform) },
              onReset = { viewModel.resetAllAdjustments() },
              playbackSpeed = playbackSpeed,
              onSpeedChange = { viewModel.setPlaybackSpeed(it) },
              isLetterboxActive = isLetterboxActive,
              onToggleLetterbox = { viewModel.toggleLetterbox() }
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
    }

    // 6. Floating Edit Toolbar (Crop, Filter, Color Adjust, Export)
    val currentFloatingAction = when (activeTool) {
      StudioActiveTool.CROP -> FloatingEditAction.CROP
      StudioActiveTool.FILTERS -> FloatingEditAction.FILTER
      StudioActiveTool.COLOR_GRADING -> FloatingEditAction.COLOR_ADJUST
      else -> null
    }

    FloatingEditToolbar(
      selectedAction = currentFloatingAction,
      onActionClick = { action ->
        when (action) {
          FloatingEditAction.CROP -> activeTool = StudioActiveTool.CROP
          FloatingEditAction.FILTER -> activeTool = StudioActiveTool.FILTERS
          FloatingEditAction.COLOR_ADJUST -> activeTool = StudioActiveTool.COLOR_GRADING
          FloatingEditAction.EXPORT -> showExportDialog = true
        }
      },
      onCropClick = { activeTool = StudioActiveTool.CROP },
      onFilterClick = { activeTool = StudioActiveTool.FILTERS },
      onColorAdjustClick = { activeTool = StudioActiveTool.COLOR_GRADING },
      onExportClick = { showExportDialog = true },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 12.dp)
    )
  }

  // Dialogs
  if (showProjectsSheet) {
    ProjectsSheet(
      projects = allProjects,
      activeProjectId = currentProject?.id,
      onSelectProject = { proj -> viewModel.selectProject(proj) },
      onCreateNewProject = { title, type, res ->
        viewModel.createNewProject(title, type, res)
      },
      onDismiss = { showProjectsSheet = false },
      onOpenImport = {
        showProjectsSheet = false
        showImportDialog = true
      }
    )
  }

  if (showImportDialog) {
    ImportMediaDialog(
      onDismiss = { showImportDialog = false },
      onMediaSelected = { uri, isVideo ->
        viewModel.importMediaFromUri(
          context = context,
          uri = uri,
          isVideo = isVideo,
          onSuccess = { proj ->
            Toast.makeText(
              context,
              "✓ تم استيراد الوسائط بنجاح: ${proj.title}",
              Toast.LENGTH_LONG
            ).show()
          }
        )
      }
    )
  }

  if (showExportDialog) {
    ExportBottomSheet(
      project = currentProject,
      isExporting = isExporting,
      exportProgress = exportProgress,
      exportComplete = exportComplete,
      exportSpeedFps = exportSpeedFps,
      exportStage = exportStage,
      exportResultPath = exportResultPath,
      exportError = exportError,
      exportedFile = exportedShareFile,
      onDismiss = {
        if (!isExporting) showExportDialog = false
      },
      onStartExport = { res, fps, codec, bitrate ->
        viewModel.startExport(res, fps, codec, bitrate)
      },
      onCancelExport = { viewModel.cancelExport() }
    )
  }

  if (showCameraDialog) {
    CameraCaptureDialog(
      onDismiss = { showCameraDialog = false },
      onMediaCaptured = { file, type, durationMs ->
        viewModel.handleCapturedMedia(file, type, durationMs)
        showCameraDialog = false
      }
    )
  }
}

/**
 * Scrollable Bar of Tool Feature Buttons with active badges and styling.
 */
@Composable
private fun StudioToolSelectorBar(
  activeTool: StudioActiveTool,
  editorStudioType: EditorStudioType,
  onSelectTool: (StudioActiveTool) -> Unit,
  modifier: Modifier = Modifier
) {
  val availableTools = remember(editorStudioType) {
    if (editorStudioType == EditorStudioType.PHOTO) {
      listOf(
        StudioActiveTool.SMART_ALGORITHMS,
        StudioActiveTool.COLOR_GRADING,
        StudioActiveTool.FILTERS,
        StudioActiveTool.CROP,
        StudioActiveTool.AI_ENHANCE,
        StudioActiveTool.FX,
        StudioActiveTool.PRO_SUITE
      )
    } else {
      listOf(
        StudioActiveTool.SMART_ALGORITHMS,
        StudioActiveTool.TRIM,
        StudioActiveTool.TIMELINE,
        StudioActiveTool.AUDIO,
        StudioActiveTool.COLOR_GRADING,
        StudioActiveTool.FILTERS,
        StudioActiveTool.CROP,
        StudioActiveTool.AI_ENHANCE,
        StudioActiveTool.FX,
        StudioActiveTool.PRO_SUITE
      )
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (editorStudioType == EditorStudioType.PHOTO)
          "أدوات محرر الصور الفوتوغرافية (Photo Tools)"
        else
          "أدوات محرر الفيديو والمونتاج (Video Tools)",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = activeTool.labelAr,
        color = activeTool.accentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      availableTools.forEach { tool ->
        val isSelected = activeTool == tool

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
              if (isSelected) {
                Brush.verticalGradient(
                  listOf(tool.accentColor.copy(alpha = 0.25f), ObsidianSurfaceElevated)
                )
              } else {
                Brush.linearGradient(listOf(ObsidianSurface, ObsidianSurface))
              }
            )
            .border(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) tool.accentColor else ObsidianBorder,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelectTool(tool) }
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag(tool.testTag)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) tool.accentColor else tool.accentColor.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = tool.icon,
                contentDescription = tool.labelAr,
                tint = if (isSelected) Color.Black else tool.accentColor,
                modifier = Modifier.size(13.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = tool.labelAr,
                color = if (isSelected) Color.White else TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
              Text(
                text = tool.labelEn,
                color = if (isSelected) tool.accentColor else TextMuted,
                fontSize = 8.sp
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Switcher between Video Editor and Photo Editor, with real device actions: Import, Save to Gallery, Share.
 */
@Composable
private fun EditorStudioModeSwitcher(
  selectedType: EditorStudioType,
  onSelectType: (EditorStudioType) -> Unit,
  onImportClick: () -> Unit,
  onSaveToGalleryClick: () -> Unit,
  onShareClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(ObsidianSurface)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
      .padding(8.dp)
      .testTag("editor_studio_mode_switcher")
  ) {
    // Mode Switcher Tabs: Video Editor vs Photo Editor
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(ObsidianBg)
        .padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      val isVideo = selectedType == EditorStudioType.VIDEO
      val isPhoto = selectedType == EditorStudioType.PHOTO

      // Video Editor Tab
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .then(
            if (isVideo) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF0284C7), ElectricCyan)))
            else Modifier.background(Color.Transparent)
          )
          .clickable { onSelectType(EditorStudioType.VIDEO) }
          .padding(vertical = 8.dp)
          .testTag("tab_video_editor"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = "محرر الفيديو",
            tint = if (isVideo) Color.Black else TextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "🎬 محرر الفيديو (Video)",
            color = if (isVideo) Color.Black else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isVideo) FontWeight.ExtraBold else FontWeight.Medium
          )
        }
      }

      // Photo Editor Tab
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .then(
            if (isPhoto) Modifier.background(Brush.horizontalGradient(listOf(NeonViolet, Color(0xFFE1306C))))
            else Modifier.background(Color.Transparent)
          )
          .clickable { onSelectType(EditorStudioType.PHOTO) }
          .padding(vertical = 8.dp)
          .testTag("tab_photo_editor"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = "محرر الصور",
            tint = if (isPhoto) Color.White else TextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "📸 محرر الصور (Photo)",
            color = if (isPhoto) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isPhoto) FontWeight.ExtraBold else FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Real Device Quick Operations Bar: Import from Gallery, Save to Phone, Share to Apps
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      DeviceActionChip(
        label = "استيراد من الهاتف",
        icon = Icons.Default.FileDownload,
        tint = EmeraldGreen,
        testTag = "quick_action_import",
        modifier = Modifier.weight(1f),
        onClick = onImportClick
      )

      DeviceActionChip(
        label = "حفظ في المعرض",
        icon = Icons.Default.Save,
        tint = CyberGold,
        testTag = "quick_action_save_gallery",
        modifier = Modifier.weight(1f),
        onClick = onSaveToGalleryClick
      )

      DeviceActionChip(
        label = "مشاركة بالهاتف",
        icon = Icons.Default.Share,
        tint = ElectricCyan,
        testTag = "quick_action_share_phone",
        modifier = Modifier.weight(1f),
        onClick = onShareClick
      )
    }
  }
}

@Composable
private fun DeviceActionChip(
  label: String,
  icon: ImageVector,
  tint: Color,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(tint.copy(alpha = 0.12f))
      .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 4.dp, vertical = 6.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = label,
        color = tint,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1
      )
    }
  }
}

/**
 * Compact Playback Transport Controls
 */
@Composable
private fun MiniTransportControls(
  isPlaying: Boolean,
  playheadMs: Long,
  totalDurationMs: Long,
  onTogglePlay: () -> Unit,
  onStepFrame: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(ObsidianSurface)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Play/Pause & Step Buttons
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = { onStepFrame(false) },
        modifier = Modifier.size(32.dp).testTag("mini_step_back")
      ) {
        Icon(imageVector = Icons.Default.Replay10, contentDescription = "تأخير إطار", tint = TextSecondary, modifier = Modifier.size(16.dp))
      }

      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(CyberGold)
          .clickable { onTogglePlay() }
          .testTag("mini_play_pause_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
          tint = Color.Black,
          modifier = Modifier.size(18.dp)
        )
      }

      IconButton(
        onClick = { onStepFrame(true) },
        modifier = Modifier.size(32.dp).testTag("mini_step_forward")
      ) {
        Icon(imageVector = Icons.Default.Forward10, contentDescription = "تقديم إطار", tint = TextSecondary, modifier = Modifier.size(16.dp))
      }
    }

    // Live Timecode
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = formatMs(playheadMs),
        color = CyberGold,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = " / ${formatMs(totalDurationMs)}",
        color = TextMuted,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

private fun formatMs(ms: Long): String {
  val totalSec = ms / 1000
  val minutes = totalSec / 60
  val seconds = totalSec % 60
  val millis = (ms % 1000) / 10
  return String.format("%02d:%02d.%02d", minutes, seconds, millis)
}
