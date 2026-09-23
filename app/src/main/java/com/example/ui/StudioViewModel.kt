package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AdjustmentsState
import com.example.data.model.AutoSaveStatus
import com.example.data.model.CropAspect
import com.example.data.model.EditingSessionEntity
import com.example.data.model.EditorMode
import com.example.data.model.EditorStudioType
import com.example.data.model.ExportCodec
import com.example.data.model.FilterPreset
import com.example.data.model.HistoryEntry
import com.example.data.model.MediaType
import com.example.data.model.ProjectEntity
import com.example.data.model.TimelineClip
import com.example.data.model.TrackType
import com.example.data.model.VideoResolution
import com.example.data.model.toAdjustmentsState
import com.example.data.model.toSessionEntity
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SessionRepository
import com.example.engine.RealMediaManager
import com.example.engine.SaveResult
import com.example.engine.SmartComputationalEngine
import com.example.engine.FrameAccurateTimecodeEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

class StudioViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: ProjectRepository
  val sessionRepository: SessionRepository

  val allProjects: StateFlow<List<ProjectEntity>>

  private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
  val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

  private val _autoSaveStatus = MutableStateFlow<AutoSaveStatus>(AutoSaveStatus.Saved(System.currentTimeMillis()))
  val autoSaveStatus: StateFlow<AutoSaveStatus> = _autoSaveStatus.asStateFlow()

  private val _lastSavedTimestamp = MutableStateFlow<Long>(System.currentTimeMillis())
  val lastSavedTimestamp: StateFlow<Long> = _lastSavedTimestamp.asStateFlow()

  private var autoSaveJob: Job? = null

  private val _editorMode = MutableStateFlow(EditorMode.BEGINNER)
  val editorMode: StateFlow<EditorMode> = _editorMode.asStateFlow()

  // Dedicated Video Editor vs Photo Editor Studio Mode
  private val _editorStudioType = MutableStateFlow(EditorStudioType.VIDEO)
  val editorStudioType: StateFlow<EditorStudioType> = _editorStudioType.asStateFlow()

  private val _adjustments = MutableStateFlow(AdjustmentsState())
  val adjustments: StateFlow<AdjustmentsState> = _adjustments.asStateFlow()

  // State Management History Stack
  private val _historyStack = MutableStateFlow<List<HistoryEntry>>(emptyList())
  val historyStack: StateFlow<List<HistoryEntry>> = _historyStack.asStateFlow()

  private val _redoHistoryStack = MutableStateFlow<List<HistoryEntry>>(emptyList())
  val redoHistoryStack: StateFlow<List<HistoryEntry>> = _redoHistoryStack.asStateFlow()

  private val _canUndo = MutableStateFlow(false)
  val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

  private val _canRedo = MutableStateFlow(false)
  val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

  private val _undoCount = MutableStateFlow(0)
  val undoCount: StateFlow<Int> = _undoCount.asStateFlow()

  private val _redoCount = MutableStateFlow(0)
  val redoCount: StateFlow<Int> = _redoCount.asStateFlow()

  private val _lastActionDescription = MutableStateFlow<String?>("الحالة الأصلية")
  val lastActionDescription: StateFlow<String?> = _lastActionDescription.asStateFlow()

  // Playback & Timeline
  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _playheadMs = MutableStateFlow(0L)
  val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

  private val _timelineClips = MutableStateFlow<List<TimelineClip>>(emptyList())
  val timelineClips: StateFlow<List<TimelineClip>> = _timelineClips.asStateFlow()

  // Selected Clip for targeted color grading (null = Master / All Video Clips)
  private val _selectedClipIdForGrading = MutableStateFlow<String?>(null)
  val selectedClipIdForGrading: StateFlow<String?> = _selectedClipIdForGrading.asStateFlow()

  // Multi-track Timeline Zoom (1.0x to 10.0x for frame-by-frame precision)
  private val _timelineZoom = MutableStateFlow(1.0f)
  val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

  // Audio VU Meter Peaks
  private val _audioPeakLeft = MutableStateFlow(0.65f)
  val audioPeakLeft: StateFlow<Float> = _audioPeakLeft.asStateFlow()

  private val _audioPeakRight = MutableStateFlow(0.72f)
  val audioPeakRight: StateFlow<Float> = _audioPeakRight.asStateFlow()

  // Split-Screen Compare (Before/After)
  private val _isCompareActive = MutableStateFlow(false)
  val isCompareActive: StateFlow<Boolean> = _isCompareActive.asStateFlow()

  private val _compareSliderPosition = MutableStateFlow(0.5f)
  val compareSliderPosition: StateFlow<Float> = _compareSliderPosition.asStateFlow()

  // Video Clip Trimming State
  private val _trimStartMs = MutableStateFlow(0L)
  val trimStartMs: StateFlow<Long> = _trimStartMs.asStateFlow()

  private val _trimEndMs = MutableStateFlow(30000L)
  val trimEndMs: StateFlow<Long> = _trimEndMs.asStateFlow()

  private val _isTrimmingActive = MutableStateFlow(false)
  val isTrimmingActive: StateFlow<Boolean> = _isTrimmingActive.asStateFlow()

  private val _trimFps = MutableStateFlow(30)
  val trimFps: StateFlow<Int> = _trimFps.asStateFlow()

  private val _trimSnapToFrames = MutableStateFlow(true)
  val trimSnapToFrames: StateFlow<Boolean> = _trimSnapToFrames.asStateFlow()

  private val _selectedClipIdForTrimming = MutableStateFlow<String?>(null)
  val selectedClipIdForTrimming: StateFlow<String?> = _selectedClipIdForTrimming.asStateFlow()

  // Smart Computational Photography & Video Processing State (Deterministic / Non-AI)
  private val _smartBeatMarkers = MutableStateFlow<List<Long>>(emptyList())
  val smartBeatMarkers: StateFlow<List<Long>> = _smartBeatMarkers.asStateFlow()

  private val _smartAlgorithmNotice = MutableStateFlow<String?>(null)
  val smartAlgorithmNotice: StateFlow<String?> = _smartAlgorithmNotice.asStateFlow()

  // 4K Export Engine State
  private val _isExporting = MutableStateFlow(false)
  val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

  private val _exportProgress = MutableStateFlow(0f)
  val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

  private val _exportComplete = MutableStateFlow(false)
  val exportComplete: StateFlow<Boolean> = _exportComplete.asStateFlow()

  private val _exportSpeedFps = MutableStateFlow(74.2f)
  val exportSpeedFps: StateFlow<Float> = _exportSpeedFps.asStateFlow()

  private var playbackJob: Job? = null
  private var exportJob: Job? = null

  init {
    val db = AppDatabase.getDatabase(application)
    repository = ProjectRepository(db.projectDao())
    sessionRepository = SessionRepository(db.sessionDao())
    allProjects = repository.allProjects.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    viewModelScope.launch {
      repository.seedInitialProjectsIfNeeded()
      repository.allProjects.collect { projects ->
        if (_currentProject.value == null && projects.isNotEmpty()) {
          selectProject(projects.first())
        }
      }
    }

    startPeriodicAutoSave()
  }

  private fun startPeriodicAutoSave() {
    autoSaveJob?.cancel()
    autoSaveJob = viewModelScope.launch {
      while (isActive) {
        delay(3000L) // Automatically persist editing session every 3 seconds to prevent data loss
        persistCurrentSession()
      }
    }
  }

  suspend fun persistCurrentSession() {
    val project = _currentProject.value ?: return
    try {
      _autoSaveStatus.value = AutoSaveStatus.Saving
      val session = _adjustments.value.toSessionEntity(
        projectId = project.id,
        playheadMs = _playheadMs.value,
        trimStartMs = _trimStartMs.value,
        trimEndMs = _trimEndMs.value,
        editorMode = _editorMode.value
      )
      sessionRepository.saveSession(session)
      val now = System.currentTimeMillis()
      _lastSavedTimestamp.value = now
      _autoSaveStatus.value = AutoSaveStatus.Saved(now)
    } catch (e: Exception) {
      _autoSaveStatus.value = AutoSaveStatus.Error(e.message ?: "فشل الحفظ التلقائي في Room")
    }
  }

  fun saveSessionNow() {
    viewModelScope.launch {
      persistCurrentSession()
    }
  }

  fun setEditorMode(mode: EditorMode) {
    _editorMode.value = mode
  }

  fun selectProject(project: ProjectEntity) {
    val previousProject = _currentProject.value
    // If switching from an existing project, immediately flush its session to Room
    if (previousProject != null && previousProject.id != project.id) {
      viewModelScope.launch {
        val prevSession = _adjustments.value.toSessionEntity(
          projectId = previousProject.id,
          playheadMs = _playheadMs.value,
          trimStartMs = _trimStartMs.value,
          trimEndMs = _trimEndMs.value,
          editorMode = _editorMode.value
        )
        sessionRepository.saveSession(prevSession)
      }
    }

    _currentProject.value = project
    _editorStudioType.value = if (project.mediaType == "PHOTO") EditorStudioType.PHOTO else EditorStudioType.VIDEO
    val dur = if (project.durationMs > 0) project.durationMs else 30000L
    _trimFps.value = if (project.fps > 0) project.fps else 30
    _trimStartMs.value = 0L
    _trimEndMs.value = dur
    _playheadMs.value = 0L
    clearHistory()
    pausePlayback()
    setupDefaultClips(project)

    // Automatically retrieve and restore persisted editing session from Room DB to prevent data loss
    viewModelScope.launch {
      val saved = sessionRepository.getSession(project.id)
      if (saved != null) {
        _adjustments.value = saved.toAdjustmentsState()
        _playheadMs.value = saved.playheadMs
        _trimStartMs.value = saved.trimStartMs
        _trimEndMs.value = if (saved.trimEndMs > 0) saved.trimEndMs else dur
        _editorMode.value = try { EditorMode.valueOf(saved.editorMode) } catch (e: Exception) { EditorMode.BEGINNER }
        _lastSavedTimestamp.value = saved.lastSavedMs
        _autoSaveStatus.value = AutoSaveStatus.Saved(saved.lastSavedMs)
      } else {
        _adjustments.value = AdjustmentsState()
        val now = System.currentTimeMillis()
        _lastSavedTimestamp.value = now
        _autoSaveStatus.value = AutoSaveStatus.Saved(now)
      }
    }
  }

  private fun setupDefaultClips(project: ProjectEntity) {
    val dur = if (project.durationMs > 0) project.durationMs else 30000L
    val halfDur = dur / 2
    _timelineClips.value = listOf(
      TimelineClip("c1", TrackType.VIDEO, "مقطع 4K رئيسي (المشهد 1)", 0L, halfDur, filterPreset = FilterPreset.TEAL_ORANGE, filterIntensity = 95f),
      TimelineClip("c1_b", TrackType.VIDEO, "مقطع 4K مقرب (المشهد 2)", halfDur, dur - halfDur, filterPreset = FilterPreset.GOLDEN_HOUR, filterIntensity = 85f),
      TimelineClip("c2", TrackType.AUDIO, "موسيقى سينمائية ستيريو 48kHz", 0L, dur, volume = 0.85f),
      TimelineClip("c3", TrackType.OVERLAY, "شعار استوديو 2027", 0L, 10000L),
      TimelineClip("c4", TrackType.FX, "تأثير تيل وبرتقالي HDR", 0L, dur)
    )
  }

  fun createNewProject(title: String, type: MediaType, resolution: VideoResolution) {
    viewModelScope.launch {
      val newProj = ProjectEntity(
        title = title.ifBlank { "مشروع سينمائي جديد" },
        mediaType = type.name,
        assetDrawableName = if (type == MediaType.VIDEO) "img_sample_video" else "img_sample_cinematic",
        durationMs = if (type == MediaType.VIDEO) 30000L else 0L,
        resolution = resolution.name,
        fps = if (type == MediaType.VIDEO) 60 else 0,
        isCloudSynced = false,
        cloudStorageSizeBytes = if (type == MediaType.VIDEO) 240000000L else 32000000L
      )
      val id = repository.insert(newProj)
      val created = newProj.copy(id = id)
      selectProject(created)
    }
  }

  fun deleteProject(project: ProjectEntity) {
    viewModelScope.launch {
      repository.deleteById(project.id)
      if (_currentProject.value?.id == project.id) {
        val remaining = allProjects.value.filter { it.id != project.id }
        if (remaining.isNotEmpty()) {
          selectProject(remaining.first())
        } else {
          _currentProject.value = null
        }
      }
    }
  }

  fun handleCapturedMedia(file: java.io.File, type: MediaType, durationMs: Long = 0L) {
    viewModelScope.launch {
      val actualDuration = if (type == MediaType.VIDEO) {
        if (durationMs > 0L) durationMs else 15000L
      } else {
        0L
      }
      val title = if (type == MediaType.VIDEO) "فيديو 4K UHD ملتقط بالكاميرا" else "صورة ملتقطة بالكاميرا"
      val newProj = ProjectEntity(
        title = title,
        mediaType = type.name,
        assetDrawableName = if (type == MediaType.VIDEO) "img_sample_video" else "img_sample_cinematic",
        durationMs = actualDuration,
        resolution = VideoResolution.UHD_4K.name,
        fps = if (type == MediaType.VIDEO) 60 else 0,
        isCloudSynced = false,
        cloudStorageSizeBytes = file.length().coerceAtLeast(15000000L)
      )
      val id = repository.insert(newProj)
      val created = newProj.copy(id = id)
      selectProject(created)
    }
  }

  private var lastAdjustmentProperty: String? = null
  private var lastAdjustmentTimestamp: Long = 0L

  fun updateAdjustment(
    actionNameAr: String? = null,
    actionNameEn: String? = null,
    transform: (AdjustmentsState) -> AdjustmentsState
  ) {
    val previousState = _adjustments.value
    val newState = transform(previousState)
    if (previousState == newState) return

    val (descAr, descEn) = if (actionNameAr != null && actionNameEn != null) {
      Pair(actionNameAr, actionNameEn)
    } else {
      describeAdjustmentDifference(previousState, newState)
    }

    val now = System.currentTimeMillis()
    val propertyKey = descEn.substringBefore(":")

    val historyList = _historyStack.value.toMutableList()
    // Group continuous slider movements on the same parameter within 650ms
    if (propertyKey == lastAdjustmentProperty && now - lastAdjustmentTimestamp < 650L && historyList.isNotEmpty()) {
      val top = historyList.removeAt(historyList.lastIndex)
      historyList.add(top.copy(
        descriptionAr = descAr,
        descriptionEn = descEn,
        timestampMs = now
      ))
      _historyStack.value = historyList
    } else {
      val entry = HistoryEntry(
        adjustments = previousState,
        descriptionAr = descAr,
        descriptionEn = descEn,
        timestampMs = now
      )
      if (historyList.size >= 60) {
        historyList.removeAt(0)
      }
      historyList.add(entry)
      _historyStack.value = historyList
      _redoHistoryStack.value = emptyList()
    }

    lastAdjustmentProperty = propertyKey
    lastAdjustmentTimestamp = now

    _adjustments.value = newState
    _canUndo.value = _historyStack.value.isNotEmpty()
    _canRedo.value = _redoHistoryStack.value.isNotEmpty()
    _undoCount.value = _historyStack.value.size
    _redoCount.value = _redoHistoryStack.value.size
    _lastActionDescription.value = descAr
  }

  fun updateAdjustment(transform: (AdjustmentsState) -> AdjustmentsState) {
    updateAdjustment(null, null, transform)
  }

  fun undo() {
    val history = _historyStack.value
    if (history.isNotEmpty()) {
      val historyList = history.toMutableList()
      val lastEntry = historyList.removeAt(historyList.lastIndex)

      val redoList = _redoHistoryStack.value.toMutableList()
      redoList.add(
        HistoryEntry(
          adjustments = _adjustments.value,
          descriptionAr = lastEntry.descriptionAr,
          descriptionEn = lastEntry.descriptionEn,
          timestampMs = System.currentTimeMillis()
        )
      )
      _redoHistoryStack.value = redoList
      _historyStack.value = historyList

      _adjustments.value = lastEntry.adjustments
      _canUndo.value = historyList.isNotEmpty()
      _canRedo.value = redoList.isNotEmpty()
      _undoCount.value = historyList.size
      _redoCount.value = redoList.size
      _lastActionDescription.value = historyList.lastOrNull()?.descriptionAr ?: "الحالة الأصلية"
      lastAdjustmentProperty = null
    }
  }

  fun redo() {
    val redos = _redoHistoryStack.value
    if (redos.isNotEmpty()) {
      val redoList = redos.toMutableList()
      val redoEntry = redoList.removeAt(redoList.lastIndex)

      val historyList = _historyStack.value.toMutableList()
      historyList.add(
        HistoryEntry(
          adjustments = _adjustments.value,
          descriptionAr = redoEntry.descriptionAr,
          descriptionEn = redoEntry.descriptionEn,
          timestampMs = System.currentTimeMillis()
        )
      )
      _historyStack.value = historyList
      _redoHistoryStack.value = redoList

      _adjustments.value = redoEntry.adjustments
      _canUndo.value = historyList.isNotEmpty()
      _canRedo.value = redoList.isNotEmpty()
      _undoCount.value = historyList.size
      _redoCount.value = redoList.size
      _lastActionDescription.value = redoEntry.descriptionAr
      lastAdjustmentProperty = null
    }
  }

  fun jumpToHistoryStep(targetEntry: HistoryEntry) {
    val history = _historyStack.value
    val targetIndex = history.indexOfFirst { it.id == targetEntry.id }
    if (targetIndex >= 0) {
      val historyList = history.toMutableList()
      val redoList = _redoHistoryStack.value.toMutableList()

      // Push current state to redo
      redoList.add(
        HistoryEntry(
          adjustments = _adjustments.value,
          descriptionAr = "الحالة السابقة",
          descriptionEn = "Previous State",
          timestampMs = System.currentTimeMillis()
        )
      )

      // Pop everything down to and including targetIndex
      while (historyList.size > targetIndex + 1) {
        val popped = historyList.removeAt(historyList.lastIndex)
        redoList.add(popped)
      }
      val target = historyList.removeAt(historyList.lastIndex)

      _redoHistoryStack.value = redoList
      _historyStack.value = historyList
      _adjustments.value = target.adjustments
      _canUndo.value = historyList.isNotEmpty()
      _canRedo.value = redoList.isNotEmpty()
      _undoCount.value = historyList.size
      _redoCount.value = redoList.size
      _lastActionDescription.value = target.descriptionAr
      lastAdjustmentProperty = null
    }
  }

  fun resetAllAdjustments() {
    updateAdjustment(
      actionNameAr = "تصفير جميع المعايرات",
      actionNameEn = "Reset All Adjustments"
    ) { AdjustmentsState() }
  }

  fun clearHistory() {
    _historyStack.value = emptyList()
    _redoHistoryStack.value = emptyList()
    _canUndo.value = false
    _canRedo.value = false
    _undoCount.value = 0
    _redoCount.value = 0
    _lastActionDescription.value = "الحالة الأصلية"
    lastAdjustmentProperty = null
  }

  private fun describeAdjustmentDifference(old: AdjustmentsState, new: AdjustmentsState): Pair<String, String> {
    return when {
      old.exposure != new.exposure -> {
        val ev = new.exposure / 50f
        val sign = if (ev > 0) "+" else ""
        val formatted = String.format(java.util.Locale.US, "%.2f EV", ev)
        Pair("تعريض ضوئي: $sign$formatted", "Exposure: $sign$formatted")
      }
      old.contrast != new.contrast -> {
        val sign = if (new.contrast > 0) "+" else ""
        Pair("تباين: $sign${new.contrast.toInt()}%", "Contrast: $sign${new.contrast.toInt()}%")
      }
      old.saturation != new.saturation -> {
        val sign = if (new.saturation > 0) "+" else ""
        Pair("تشبع ألوان: $sign${new.saturation.toInt()}%", "Saturation: $sign${new.saturation.toInt()}%")
      }
      old.selectedPreset != new.selectedPreset -> {
        Pair("فلتر سينمائي: ${new.selectedPreset.labelAr}", "LUT Filter: ${new.selectedPreset.labelEn}")
      }
      old.lutIntensity != new.lutIntensity -> {
        Pair("شدة الـ LUT: ${new.lutIntensity.toInt()}%", "LUT Intensity: ${new.lutIntensity.toInt()}%")
      }
      old.highlights != new.highlights -> {
        val sign = if (new.highlights > 0) "+" else ""
        Pair("مناطق مضيئة: $sign${new.highlights.toInt()}%", "Highlights: $sign${new.highlights.toInt()}%")
      }
      old.shadows != new.shadows -> {
        val sign = if (new.shadows > 0) "+" else ""
        Pair("ظلال عميقة: $sign${new.shadows.toInt()}%", "Shadows: $sign${new.shadows.toInt()}%")
      }
      old.temperature != new.temperature -> {
        val sign = if (new.temperature > 0) "+" else ""
        Pair("حرارة اللون: $sign${new.temperature.toInt()}K", "Temperature: $sign${new.temperature.toInt()}K")
      }
      old.tint != new.tint -> {
        val sign = if (new.tint > 0) "+" else ""
        Pair("صبغة لونية: $sign${new.tint.toInt()}", "Tint: $sign${new.tint.toInt()}")
      }
      old.brightness != new.brightness -> {
        val sign = if (new.brightness > 0) "+" else ""
        Pair("سطوع كلي: $sign${new.brightness.toInt()}%", "Brightness: $sign${new.brightness.toInt()}%")
      }
      old.selectedCrop != new.selectedCrop -> {
        Pair("أبعاد وقص: ${new.selectedCrop.label}", "Crop: ${new.selectedCrop.label}")
      }
      old.vignette != new.vignette -> {
        Pair("تعتيم الأطراف: ${new.vignette.toInt()}%", "Vignette: ${new.vignette.toInt()}%")
      }
      old.sharpness != new.sharpness -> {
        Pair("حدة وتفاصيل: ${new.sharpness.toInt()}%", "Sharpness: ${new.sharpness.toInt()}%")
      }
      old.filmGrain != new.filmGrain -> {
        Pair("حبيبات الفيلم: ${new.filmGrain.toInt()}%", "Film Grain: ${new.filmGrain.toInt()}%")
      }
      old.glitchRgb != new.glitchRgb -> {
        Pair("تأثير جليتش: ${new.glitchRgb.toInt()}%", "RGB Glitch: ${new.glitchRgb.toInt()}%")
      }
      old.bloomGlow != new.bloomGlow -> {
        Pair("توهج سينمائي: ${new.bloomGlow.toInt()}%", "Bloom Glow: ${new.bloomGlow.toInt()}%")
      }
      else -> Pair("تعديل تدريج الألوان", "Color Grading Adjustment")
    }
  }

  fun selectClipForGrading(clipId: String?) {
    _selectedClipIdForGrading.value = clipId
    if (clipId != null) {
      val clip = _timelineClips.value.find { it.id == clipId }
      if (clip != null) {
        updateAdjustment(
          actionNameAr = "تحديد المقطع: ${clip.title}",
          actionNameEn = "Select Clip: ${clip.title}"
        ) {
          it.copy(
            selectedPreset = clip.filterPreset,
            lutIntensity = clip.filterIntensity
          )
        }
      }
    }
  }

  fun selectFilterPreset(preset: FilterPreset) {
    val targetClipId = _selectedClipIdForGrading.value
    updateAdjustment(
      actionNameAr = "فلتر سينمائي: ${preset.labelAr}",
      actionNameEn = "LUT Filter: ${preset.labelEn}"
    ) { it.copy(selectedPreset = preset) }

    if (targetClipId != null) {
      _timelineClips.value = _timelineClips.value.map { clip ->
        if (clip.id == targetClipId) {
          clip.copy(filterPreset = preset)
        } else clip
      }
    } else {
      _timelineClips.value = _timelineClips.value.map { clip ->
        if (clip.trackType == TrackType.VIDEO) {
          clip.copy(filterPreset = preset)
        } else clip
      }
    }
  }

  fun setFilterIntensity(intensity: Float) {
    val clamped = intensity.coerceIn(0f, 100f)
    val targetClipId = _selectedClipIdForGrading.value
    updateAdjustment(
      actionNameAr = "شدة الفلتر: ${clamped.toInt()}%",
      actionNameEn = "Filter Intensity: ${clamped.toInt()}%"
    ) { it.copy(lutIntensity = clamped) }

    if (targetClipId != null) {
      _timelineClips.value = _timelineClips.value.map { clip ->
        if (clip.id == targetClipId) {
          clip.copy(filterIntensity = clamped)
        } else clip
      }
    } else {
      _timelineClips.value = _timelineClips.value.map { clip ->
        if (clip.trackType == TrackType.VIDEO) {
          clip.copy(filterIntensity = clamped)
        } else clip
      }
    }
  }

  fun applyFilterToAllVideoClips(preset: FilterPreset, intensity: Float = 100f) {
    _selectedClipIdForGrading.value = null
    val clamped = intensity.coerceIn(0f, 100f)
    updateAdjustment(
      actionNameAr = "تطبيق الفلتر على جميع المقاطع: ${preset.labelAr}",
      actionNameEn = "Apply Filter To All Clips: ${preset.labelEn}"
    ) { it.copy(selectedPreset = preset, lutIntensity = clamped) }

    _timelineClips.value = _timelineClips.value.map { clip ->
      if (clip.trackType == TrackType.VIDEO) {
        clip.copy(filterPreset = preset, filterIntensity = clamped)
      } else clip
    }
  }

  fun selectCropAspect(crop: CropAspect) {
    updateAdjustment { it.copy(selectedCrop = crop) }
  }

  fun toggleCompare() {
    _isCompareActive.value = !_isCompareActive.value
  }

  fun setCompareSlider(position: Float) {
    _compareSliderPosition.value = position.coerceIn(0.05f, 0.95f)
  }

  // Playback Control
  fun togglePlayback() {
    if (_isPlaying.value) {
      pausePlayback()
    } else {
      startPlayback()
    }
  }

  private fun startPlayback() {
    _isPlaying.value = true
    playbackJob?.cancel()
    playbackJob = viewModelScope.launch {
      val minBound = _trimStartMs.value
      val maxBound = _trimEndMs.value.coerceAtLeast(minBound + 200L)

      if (_playheadMs.value < minBound || _playheadMs.value >= maxBound) {
        _playheadMs.value = minBound
      }

      while (isActive && _isPlaying.value) {
        delay(33) // ~30 fps tick for timeline
        val next = _playheadMs.value + 33L
        if (next >= maxBound) {
          _playheadMs.value = minBound // loop within trimmed interval
        } else {
          _playheadMs.value = next
        }
        // Random dynamic audio peak bounce
        _audioPeakLeft.value = (0.4f + Random.nextFloat() * 0.55f).coerceIn(0f, 1f)
        _audioPeakRight.value = (0.42f + Random.nextFloat() * 0.53f).coerceIn(0f, 1f)
      }
    }
  }

  fun pausePlayback() {
    _isPlaying.value = false
    playbackJob?.cancel()
  }

  fun seekTo(ms: Long) {
    val duration = _currentProject.value?.durationMs ?: 30000L
    _playheadMs.value = ms.coerceIn(0L, duration)
  }

  fun stepFrame(forward: Boolean) {
    val frameDuration = 1000L / 60L // 60 fps step = 16ms
    val duration = _currentProject.value?.durationMs ?: 30000L
    val target = if (forward) _playheadMs.value + frameDuration else _playheadMs.value - frameDuration
    _playheadMs.value = target.coerceIn(0L, duration)
  }

  // Interactive Video Trimming Functions
  fun toggleTrimMode() {
    _isTrimmingActive.value = !_isTrimmingActive.value
  }

  fun setTrimMode(active: Boolean) {
    _isTrimmingActive.value = active
  }

  fun setTrimFps(fps: Int) {
    _trimFps.value = fps.coerceIn(1, 120)
  }

  fun toggleTrimSnapToFrames() {
    _trimSnapToFrames.value = !_trimSnapToFrames.value
  }

  fun selectClipForTrimming(clipId: String?) {
    _selectedClipIdForTrimming.value = clipId
  }

  fun updateTrim(startMs: Long, endMs: Long) {
    val totalDur = _currentProject.value?.durationMs?.coerceAtLeast(1000L) ?: 30000L
    val fps = _trimFps.value
    val frameDurMs = FrameAccurateTimecodeEngine.frameToMs(1, fps).coerceAtLeast(16L)
    val minGap = frameDurMs * 2 // minimum 2 frames
    val rawStart = startMs.coerceIn(0L, (totalDur - minGap).coerceAtLeast(0L))
    val rawEnd = endMs.coerceIn(rawStart + minGap, totalDur)

    val validStart = if (_trimSnapToFrames.value) {
      FrameAccurateTimecodeEngine.quantizeToFrame(rawStart, fps, totalDur)
    } else {
      rawStart
    }
    val validEnd = if (_trimSnapToFrames.value) {
      FrameAccurateTimecodeEngine.quantizeToFrame(rawEnd, fps, totalDur).coerceAtLeast(validStart + minGap)
    } else {
      rawEnd
    }

    _trimStartMs.value = validStart
    _trimEndMs.value = validEnd

    // Keep playhead within trim interval
    if (_playheadMs.value < validStart) {
      _playheadMs.value = validStart
    } else if (_playheadMs.value > validEnd) {
      _playheadMs.value = validEnd
    }
  }

  fun nudgeTrimStart(deltaMs: Long) {
    updateTrim(_trimStartMs.value + deltaMs, _trimEndMs.value)
  }

  fun nudgeTrimEnd(deltaMs: Long) {
    updateTrim(_trimStartMs.value, _trimEndMs.value + deltaMs)
  }

  fun nudgeTrimStartFrame(deltaFrames: Int) {
    val totalDur = _currentProject.value?.durationMs?.coerceAtLeast(1000L) ?: 30000L
    val fps = _trimFps.value
    val minGap = FrameAccurateTimecodeEngine.frameToMs(2, fps)
    val newStart = FrameAccurateTimecodeEngine.nudgeByFrames(
      _trimStartMs.value,
      deltaFrames,
      fps,
      (_trimEndMs.value - minGap).coerceAtLeast(0L)
    )
    updateTrim(newStart, _trimEndMs.value)
    seekTo(newStart)
  }

  fun nudgeTrimEndFrame(deltaFrames: Int) {
    val totalDur = _currentProject.value?.durationMs?.coerceAtLeast(1000L) ?: 30000L
    val fps = _trimFps.value
    val minGap = FrameAccurateTimecodeEngine.frameToMs(2, fps)
    val minStart = _trimStartMs.value + minGap
    val newEnd = FrameAccurateTimecodeEngine.nudgeByFrames(
      _trimEndMs.value,
      deltaFrames,
      fps,
      totalDur
    ).coerceAtLeast(minStart)
    updateTrim(_trimStartMs.value, newEnd)
    seekTo(newEnd)
  }

  fun previewTrimInPoint() {
    seekTo(_trimStartMs.value)
  }

  fun previewTrimOutPoint() {
    seekTo(_trimEndMs.value)
  }

  fun setTrimStartToPlayhead() {
    val fps = _trimFps.value
    val snappedPlayhead = if (_trimSnapToFrames.value) {
      FrameAccurateTimecodeEngine.quantizeToFrame(_playheadMs.value, fps)
    } else {
      _playheadMs.value
    }
    updateTrim(snappedPlayhead, _trimEndMs.value)
  }

  fun setTrimEndToPlayhead() {
    val fps = _trimFps.value
    val snappedPlayhead = if (_trimSnapToFrames.value) {
      FrameAccurateTimecodeEngine.quantizeToFrame(_playheadMs.value, fps)
    } else {
      _playheadMs.value
    }
    updateTrim(_trimStartMs.value, snappedPlayhead)
  }

  fun resetTrim() {
    val totalDur = _currentProject.value?.durationMs?.coerceAtLeast(1000L) ?: 30000L
    _trimStartMs.value = 0L
    _trimEndMs.value = totalDur
    _playheadMs.value = 0L
  }

  fun applyTrimExtract() {
    val fps = _trimFps.value
    val result = FrameAccurateTimecodeEngine.extractRangeAcrossTracks(
      clips = _timelineClips.value,
      startMs = _trimStartMs.value,
      endMs = _trimEndMs.value,
      fps = fps
    )

    _currentProject.value?.let { proj ->
      val updated = proj.copy(durationMs = result.newTotalDurationMs)
      _currentProject.value = updated
      viewModelScope.launch {
        repository.update(updated)
      }
    }

    _timelineClips.value = result.updatedClips
    _trimStartMs.value = result.newTrimStartMs
    _trimEndMs.value = result.newTrimEndMs
    _playheadMs.value = result.newPlayheadMs
    _smartAlgorithmNotice.value = result.summaryAr
    _isTrimmingActive.value = false
  }

  fun applyRippleDelete() {
    val totalDur = _currentProject.value?.durationMs ?: 30000L
    val fps = _trimFps.value
    val result = FrameAccurateTimecodeEngine.rippleDeleteRangeAcrossTracks(
      clips = _timelineClips.value,
      startMs = _trimStartMs.value,
      endMs = _trimEndMs.value,
      totalDurationMs = totalDur,
      fps = fps
    )

    _currentProject.value?.let { proj ->
      val updated = proj.copy(durationMs = result.newTotalDurationMs)
      _currentProject.value = updated
      viewModelScope.launch {
        repository.update(updated)
      }
    }

    _timelineClips.value = result.updatedClips
    _trimStartMs.value = result.newTrimStartMs
    _trimEndMs.value = result.newTrimEndMs
    _playheadMs.value = result.newPlayheadMs
    _smartAlgorithmNotice.value = result.summaryAr
  }

  fun applyTrimToClip() {
    applyTrimExtract()
  }

  fun splitClipAtPlayhead() {
    val currentMs = _playheadMs.value
    val clips = _timelineClips.value.toMutableList()
    val videoClipIndex = clips.indexOfFirst { it.trackType == TrackType.VIDEO && currentMs > it.startMs && currentMs < it.startMs + it.durationMs }
    if (videoClipIndex != -1) {
      val original = clips[videoClipIndex]
      val firstDuration = currentMs - original.startMs
      val secondDuration = original.durationMs - firstDuration
      val clip1 = original.copy(durationMs = firstDuration)
      val clip2 = original.copy(
        id = "c_${System.currentTimeMillis()}",
        title = "${original.title} (جزء 2)",
        startMs = currentMs,
        durationMs = secondDuration
      )
      clips[videoClipIndex] = clip1
      clips.add(videoClipIndex + 1, clip2)
      _timelineClips.value = clips
    }
  }

  // Multi-track Timeline Zoom Controls (1.0x - 10.0x) for frame-by-frame precision
  fun setTimelineZoom(zoom: Float) {
    val rounded = ((zoom * 10f).roundToInt() / 10f).coerceIn(1.0f, 10.0f)
    _timelineZoom.value = rounded
  }

  fun zoomInTimeline(delta: Float = 0.5f) {
    setTimelineZoom(_timelineZoom.value + delta)
  }

  fun zoomOutTimeline(delta: Float = 0.5f) {
    setTimelineZoom(_timelineZoom.value - delta)
  }

  fun resetTimelineZoom() {
    _timelineZoom.value = 1.0f
  }

  // =========================================================================
  // Smart Computational Photography & Video Algorithms (Pure Math / Deterministic)
  // =========================================================================

  fun applyGrayWorldAwb() {
    val result = SmartComputationalEngine.computeGrayWorldAwb(_adjustments.value)
    updateAdjustment(
      actionNameAr = "توازن بياض ذكي (Gray World)",
      actionNameEn = "Smart AWB (Gray World)"
    ) {
      it.copy(
        temperature = result.recommendedTemp,
        tint = result.recommendedTint
      )
    }
    _smartAlgorithmNotice.value = result.descriptionAr
  }

  fun applySmartAutoTone() {
    val result = SmartComputationalEngine.computeSmartAutoTone(_adjustments.value)
    updateAdjustment(
      actionNameAr = "موازنة ديناميكية ذكية (Auto-Levels)",
      actionNameEn = "Smart Dynamic Range Tone"
    ) {
      it.copy(
        exposure = result.recommendedExposure,
        contrast = result.recommendedContrast,
        highlights = result.recommendedHighlights,
        shadows = result.recommendedShadows,
        brightness = result.recommendedBrightness
      )
    }
    _smartAlgorithmNotice.value = result.descriptionAr
  }

  fun applySmartSharpness() {
    val sharpness = SmartComputationalEngine.computeSmartSharpness()
    updateAdjustment(
      actionNameAr = "قناع حدة التفاصيل (Unsharp Mask)",
      actionNameEn = "Smart Micro-Contrast"
    ) {
      it.copy(
        sharpness = sharpness,
        contrast = (it.contrast + 6f).coerceIn(-100f, 100f)
      )
    }
    _smartAlgorithmNotice.value = "خوارزمية Unsharp Mask: تعزيز التباين المجهري للحواف بدقة 4K"
  }

  fun toggleFocusPeaking() {
    val next = !_adjustments.value.focusPeaking
    _adjustments.value = _adjustments.value.copy(focusPeaking = next)
    _smartAlgorithmNotice.value = if (next) "المحدد البؤري: مفعل (كشف حواف التباين بلون نيون)" else "المحدد البؤري: معطل"
  }

  fun toggleZebraStripes() {
    val next = !_adjustments.value.zebraStripes
    _adjustments.value = _adjustments.value.copy(zebraStripes = next)
    _smartAlgorithmNotice.value = if (next) "تحذير التعريض: مفعل (تحديد المناطق المفرطة 95% IRE)" else "تحذير التعريض: معطل"
  }

  fun detectAudioBeats(bpm: Int = 124) {
    val duration = _currentProject.value?.durationMs ?: 30000L
    val result = SmartComputationalEngine.detectAudioBeats(duration, bpm)
    _smartBeatMarkers.value = result.beatMarkersMs
    _smartAlgorithmNotice.value = result.descriptionAr
  }

  fun clearBeatMarkers() {
    _smartBeatMarkers.value = emptyList()
    _smartAlgorithmNotice.value = "تم مسح علامات الإيقاع"
  }

  fun autoSplitOnBeats() {
    if (_smartBeatMarkers.value.isEmpty()) {
      detectAudioBeats()
    }
    val updated = SmartComputationalEngine.autoSplitClipsOnBeats(
      clips = _timelineClips.value,
      beatMarkers = _smartBeatMarkers.value
    )
    _timelineClips.value = updated
    _smartAlgorithmNotice.value = "تم تقطيع مقاطع الفيديو تلقائياً بالتزامن مع ضربات الإيقاع"
  }

  fun applyGyroStabilization(smoothingPercent: Float) {
    val result = SmartComputationalEngine.computeMotionStabilization(smoothingPercent)
    _adjustments.value = _adjustments.value.copy(gyroSmoothing = smoothingPercent)
    _smartAlgorithmNotice.value = result.descriptionAr
  }

  fun applyAutoHorizonLeveling() {
    val result = SmartComputationalEngine.computeAutoHorizonLeveling(_adjustments.value.horizonTiltDeg)
    updateAdjustment(
      actionNameAr = "تسوية الأفق التلقائية",
      actionNameEn = "Auto Horizon Leveling"
    ) {
      it.copy(horizonTiltDeg = result.counterCorrectionDeg)
    }
    _smartAlgorithmNotice.value = result.descriptionAr
  }

  fun resetHorizonLeveling() {
    updateAdjustment(
      actionNameAr = "إعادة ضبط الأفق",
      actionNameEn = "Reset Horizon"
    ) {
      it.copy(horizonTiltDeg = 0f)
    }
    _smartAlgorithmNotice.value = "تمت إعادة ضبط زاوية الأفق إلى 0°"
  }

  fun applySpeedRamp(preset: SmartComputationalEngine.SpeedRampPreset) {
    val clips = _timelineClips.value.map { clip ->
      if (clip.trackType == TrackType.VIDEO) {
        val avgSpeed = preset.curvePoints.average().toFloat()
        clip.copy(speed = avgSpeed)
      } else clip
    }
    _timelineClips.value = clips
    _smartAlgorithmNotice.value = "تم تطبيق منحنى تسريع بيزييه: ${preset.titleAr}"
  }

  fun dismissSmartNotice() {
    _smartAlgorithmNotice.value = null
  }

  // 4K Video Export Simulation
  fun startExport(
    resolution: VideoResolution,
    fps: Int,
    codec: ExportCodec,
    bitrateMbps: Int,
    onComplete: () -> Unit = {}
  ) {
    exportJob?.cancel()
    _isExporting.value = true
    _exportProgress.value = 0f
    _exportComplete.value = false

    exportJob = viewModelScope.launch {
      // 4K rendering engine progress simulation with realistic Vulkan compute speed
      var progress = 0f
      while (progress < 100f) {
        delay(120)
        progress += 4f + (Random.nextFloat() * 2f)
        _exportProgress.value = progress.coerceAtMost(100f)
        _exportSpeedFps.value = 65f + (Random.nextFloat() * 20f)
      }
      _exportProgress.value = 100f
      _exportComplete.value = true
      _isExporting.value = false
      onComplete()
    }
  }

  // Letterbox CinemaScope 2.39:1 overlay
  private val _isLetterboxActive = MutableStateFlow(false)
  val isLetterboxActive: StateFlow<Boolean> = _isLetterboxActive.asStateFlow()

  // Playback Speed multiplier (0.25x to 4x)
  private val _playbackSpeed = MutableStateFlow(1.0f)
  val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

  fun toggleLetterbox() {
    _isLetterboxActive.value = !_isLetterboxActive.value
  }

  fun setPlaybackSpeed(speed: Float) {
    _playbackSpeed.value = speed.coerceIn(0.25f, 4.0f)
    // Update active video clip speed
    val updatedClips = _timelineClips.value.map { clip ->
      if (clip.trackType == TrackType.VIDEO) {
        clip.copy(speed = speed)
      } else {
        clip
      }
    }
    _timelineClips.value = updatedClips
  }

  fun addAudioTrackClip(title: String, durationMs: Long) {
    val newId = "audio_${System.currentTimeMillis()}"
    val newClip = TimelineClip(
      id = newId,
      trackType = TrackType.AUDIO,
      title = title,
      startMs = _playheadMs.value,
      durationMs = durationMs.coerceAtLeast(1500L),
      volume = 0.85f
    )
    _timelineClips.value = _timelineClips.value + newClip
  }

  fun applyAiEnhancementPreset(presetName: String, newAdjustments: AdjustmentsState) {
    updateAdjustment(
      actionNameAr = "تحسين الذكاء الاصطناعي: $presetName",
      actionNameEn = "AI Enhance: $presetName"
    ) { newAdjustments }
  }

  fun cancelExport() {
    exportJob?.cancel()
    _isExporting.value = false
    _exportProgress.value = 0f
  }

  fun setEditorStudioType(type: EditorStudioType) {
    _editorStudioType.value = type
  }

  fun importMediaFromUri(
    context: Context,
    uri: Uri,
    isVideo: Boolean,
    onSuccess: (ProjectEntity) -> Unit = {}
  ) {
    viewModelScope.launch {
      val project = if (isVideo) {
        val meta = RealMediaManager.extractVideoMetadata(context, uri)
        ProjectEntity(
          title = meta.title,
          mediaType = "VIDEO",
          assetDrawableName = "img_sample_video",
          durationMs = meta.durationMs,
          resolution = if (meta.width >= 3840) "UHD_4K" else if (meta.width >= 1920) "FHD_1080P" else "HD_720P",
          fps = meta.fps,
          mediaUri = uri.toString(),
          cloudStorageSizeBytes = meta.sizeBytes.coerceAtLeast(1024L)
        )
      } else {
        val meta = RealMediaManager.extractPhotoMetadata(context, uri)
        ProjectEntity(
          title = meta.title,
          mediaType = "PHOTO",
          assetDrawableName = "img_sample_cinematic",
          durationMs = 0L,
          resolution = "${meta.width}x${meta.height}",
          fps = 0,
          mediaUri = uri.toString(),
          cloudStorageSizeBytes = meta.sizeBytes.coerceAtLeast(1024L)
        )
      }
      val id = repository.insert(project)
      val createdProject = project.copy(id = id)
      selectProject(createdProject)
      _editorStudioType.value = if (isVideo) EditorStudioType.VIDEO else EditorStudioType.PHOTO
      onSuccess(createdProject)
    }
  }

  fun saveCurrentMediaToGallery(
    context: Context,
    onComplete: (SaveResult) -> Unit
  ) {
    viewModelScope.launch {
      val project = _currentProject.value
      val adjustments = _adjustments.value
      val isPhoto = project?.mediaType == "PHOTO" || _editorStudioType.value == EditorStudioType.PHOTO
      val result = if (isPhoto) {
        RealMediaManager.saveImageToDeviceGallery(context, project, adjustments)
      } else {
        RealMediaManager.saveVideoToDeviceGallery(context, project, adjustments)
      }
      onComplete(result)
    }
  }

  fun shareCurrentMedia(
    context: Context,
    onComplete: (Boolean) -> Unit = {}
  ) {
    viewModelScope.launch {
      val project = _currentProject.value
      val adjustments = _adjustments.value
      val isPhoto = project?.mediaType == "PHOTO" || _editorStudioType.value == EditorStudioType.PHOTO
      val saveResult = if (isPhoto) {
        RealMediaManager.saveImageToDeviceGallery(context, project, adjustments)
      } else {
        RealMediaManager.saveVideoToDeviceGallery(context, project, adjustments)
      }

      val shareUri = saveResult.shareableUri ?: saveResult.mediaStoreUri
      if (shareUri != null) {
        RealMediaManager.shareMediaFile(
          context = context,
          fileUri = shareUri,
          isVideo = !isPhoto,
          title = project?.title ?: "مشروع Lumina"
        )
        onComplete(true)
      } else {
        onComplete(false)
      }
    }
  }
}
