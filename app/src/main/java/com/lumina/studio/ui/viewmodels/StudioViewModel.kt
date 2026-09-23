package com.lumina.studio.ui.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumina.studio.data.db.AppDatabase
import com.lumina.studio.data.model.AdjustmentsState
import com.lumina.studio.data.model.AutoSaveStatus
import com.lumina.studio.data.model.CropAspect
import com.lumina.studio.data.model.EditingSessionEntity
import com.lumina.studio.data.model.EditorMode
import com.lumina.studio.data.model.EditorStudioType
import com.lumina.studio.data.model.ExportCodec
import com.lumina.studio.data.model.FilterPreset
import com.lumina.studio.data.model.HistoryEntry
import com.lumina.studio.data.model.MediaType
import com.lumina.studio.data.model.ProjectEntity
import com.lumina.studio.data.model.TimelineClip
import com.lumina.studio.data.model.TrackType
import com.lumina.studio.data.model.VideoResolution
import com.lumina.studio.data.model.toAdjustmentsState
import com.lumina.studio.data.model.toSessionEntity
import com.lumina.studio.data.repository.ProjectRepository
import com.lumina.studio.data.repository.SessionRepository
import com.lumina.studio.engine.audio.AudioGraphEngine
import com.lumina.studio.engine.export.CinematicVideoExporter
import com.lumina.studio.engine.analysis.MediaAnalysisEngine
import com.lumina.studio.engine.audio.PreviewAudioController
import com.lumina.studio.engine.media.RealMediaManager
import com.lumina.studio.engine.media.SaveResult
import com.lumina.studio.engine.audio.SfxSynthesizer
import com.lumina.studio.engine.analysis.SmartComputationalEngine
import com.lumina.studio.engine.analysis.FrameAccurateTimecodeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import com.lumina.studio.ui.editor.StudioPreferences

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

  // REAL export telemetry (replaces the old fake random speed)
  private val _exportStage = MutableStateFlow("")
  val exportStage: StateFlow<String> = _exportStage.asStateFlow()

  /** REAL measured encode throughput (frames/second), not a random number. */
  private val _exportSpeed = MutableStateFlow(0f)
  val exportSpeedFps: StateFlow<Float> = _exportSpeed.asStateFlow()

  private val _exportFrames = MutableStateFlow(0)
  val exportFrames: StateFlow<Int> = _exportFrames.asStateFlow()

  private val _exportActualCodec = MutableStateFlow("")
  val exportActualCodec: StateFlow<String> = _exportActualCodec.asStateFlow()

  private val _exportResultPath = MutableStateFlow<String?>(null)
  val exportResultPath: StateFlow<String?> = _exportResultPath.asStateFlow()

  private val _exportError = MutableStateFlow<String?>(null)
  val exportError: StateFlow<String?> = _exportError.asStateFlow()

  // REAL analysis state (computed from actual pixels / decoded PCM)
  private val _histogramData = MutableStateFlow<MediaAnalysisEngine.HistogramData?>(null)
  val histogramData: StateFlow<MediaAnalysisEngine.HistogramData?> = _histogramData.asStateFlow()

  private val _edgeOverlayBitmap = MutableStateFlow<Bitmap?>(null)
  val edgeOverlayBitmap: StateFlow<Bitmap?> = _edgeOverlayBitmap.asStateFlow()

  private val _zebraOverlayBitmap = MutableStateFlow<Bitmap?>(null)
  val zebraOverlayBitmap: StateFlow<Bitmap?> = _zebraOverlayBitmap.asStateFlow()

  private val _audioEnvelope = MutableStateFlow<AudioGraphEngine.AudioEnvelope?>(null)
  val audioEnvelope: StateFlow<AudioGraphEngine.AudioEnvelope?> = _audioEnvelope.asStateFlow()

  // Real mixer state applied to the PreviewAudioController
  private val _masterAudioVolume = MutableStateFlow(0.85f)
  val masterAudioVolume: StateFlow<Float> = _masterAudioVolume.asStateFlow()

  private val _isAudioMuted = MutableStateFlow(false)
  val isAudioMuted: StateFlow<Boolean> = _isAudioMuted.asStateFlow()

  private var playbackJob: Job? = null
  private var exportJob: Job? = null
  private var analysisJob: Job? = null
  private var envelopeJob: Job? = null
  private var analysisBitmap: Bitmap? = null
  private var analysisBitmapProjectId: Long = -1

  private val audioController = PreviewAudioController(application.applicationContext)

  init {
    val db = AppDatabase.getDatabase(application)
    repository = ProjectRepository(db.projectDao())
    sessionRepository = SessionRepository(db.sessionDao())
    // REAL persisted preference: editor mode from the Settings screen
    _editorMode.value = StudioPreferences.getDefaultEditorMode(application)
    allProjects = repository.allProjects.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    // A fresh install honestly starts with ZERO projects (the old fake
    // seeding of 3 demo projects was removed). The UI shows a real empty state.
    viewModelScope.launch {
      repository.allProjects.collect { projects ->
        if (_currentProject.value == null && projects.isNotEmpty()) {
          selectProject(projects.first())
        }
      }
    }

    startPeriodicAutoSave()
  }

  override fun onCleared() {
    audioController.releaseAll()
    super.onCleared()
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
    // Persist the choice for the next app launch (real preference)
    StudioPreferences.setDefaultEditorMode(getApplication(), mode)
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
    buildClipsFromProject(project)
    resetAnalysisState()
    kickoffMediaAnalysis(project)
    kickoffAudioEnvelope(project)

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

  /**
   * Builds the timeline from the project's REAL media only. One video clip
   * with the true duration and ORIGINAL grading — the previous five fake
   * clips ("مقطع 4K رئيسي", "موسيقى سينمائية ستيريو 48kHz", "شعار استوديو 2027",
   * "تأثير تيل وبرتقالي HDR") were pure fabrication.
   */
  private fun buildClipsFromProject(project: ProjectEntity) {
    if (project.mediaUri.isNullOrBlank()) {
      _timelineClips.value = emptyList()
      return
    }
    val dur = if (project.durationMs > 0) project.durationMs else 30000L
    _timelineClips.value = listOf(
      TimelineClip(
        id = "clip_main_${project.id}",
        trackType = TrackType.VIDEO,
        title = project.title,
        startMs = 0L,
        durationMs = dur
      )
    )
  }

  /**
   * REAL media analysis kickoff: loads the actual frame/bitmap for the
   * project, computes the true histogram, and computes the real edge/zebra
   * masks used by the scopes and analysis overlays.
   */
  private fun kickoffMediaAnalysis(project: ProjectEntity) {
    analysisJob?.cancel()
    if (project.mediaUri.isNullOrBlank()) {
      analysisBitmap = null
      analysisBitmapProjectId = -1
      _histogramData.value = null
      _edgeOverlayBitmap.value = null
      _zebraOverlayBitmap.value = null
      return
    }
    analysisJob = viewModelScope.launch(Dispatchers.Default) {
      val bitmap = withContext(Dispatchers.IO) {
        val base = if (project.mediaType == "PHOTO") {
          RealMediaManager.loadProjectBitmapOrNull(getApplication(), project)
        } else {
          RealMediaManager.loadVideoFrameAt(getApplication(), project.mediaUri!!, 0L)
        }
        base?.let { MediaAnalysisEngine.downscaleForAnalysis(it, 256) }
      }
      if (bitmap == null) {
        _histogramData.value = null
        return@launch
      }
      analysisBitmap = bitmap
      analysisBitmapProjectId = project.id
      _histogramData.value = MediaAnalysisEngine.computeHistogram(bitmap)
      refreshAnalysisMasksLocked()
    }
  }

  /** Recomputes the REAL edge/zebra overlays from the cached analysis bitmap. */
  private fun refreshAnalysisMasksLocked() {
    val bitmap = analysisBitmap ?: run {
      _edgeOverlayBitmap.value = null
      _zebraOverlayBitmap.value = null
      return
    }
    val masks = MediaAnalysisEngine.computeFrameMasks(bitmap)
    _edgeOverlayBitmap.value = MediaAnalysisEngine.maskToOverlayBitmap(
      masks.edgeMask, masks.maskWidth, masks.maskHeight, 0xFF00FF66.toInt()
    )
    _zebraOverlayBitmap.value = MediaAnalysisEngine.maskToOverlayBitmap(
      masks.overexposureMask, masks.maskWidth, masks.maskHeight, 0xFFFFD700.toInt()
    )
  }

  /**
   * REAL audio envelope: decodes the project's actual audio track (or falls
   * back to the first real SFX clip) so VU meters and waveforms show truth.
   */
  private fun kickoffAudioEnvelope(project: ProjectEntity) {
    envelopeJob?.cancel()
    _audioEnvelope.value = null
    envelopeJob = viewModelScope.launch(Dispatchers.Default) {
      val envelope = when {
        !project.mediaUri.isNullOrBlank() -> {
          withContext(Dispatchers.IO) {
            AudioGraphEngine.decodeEnvelope(
              context = getApplication(),
              uri = Uri.parse(project.mediaUri),
              maxDurationMs = 10 * 60 * 1000L
            )
          }
        }
        else -> null
      }
      _audioEnvelope.value = envelope
    }
  }

  private fun resetAnalysisState() {
    _smartBeatMarkers.value = emptyList()
    _smartAlgorithmNotice.value = null
    _audioPeakLeft.value = 0f
    _audioPeakRight.value = 0f
  }

  fun createNewProject(title: String, type: MediaType, resolution: VideoResolution) {
    viewModelScope.launch {
      val newProj = ProjectEntity(
        title = title.ifBlank { if (type == MediaType.VIDEO) "مشروع فيديو جديد" else "مشروع صورة جديد" },
        mediaType = type.name,
        durationMs = 0L, // REAL duration; updated when media is attached
        resolution = resolution.name
      )
      val id = repository.insert(newProj)
      val created = repository.getById(id) ?: newProj.copy(id = id)
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

  /**
   * Handles REAL captured media from the CameraX dialog.
   * The file is copied from the transient cache into permanent app storage,
   * its real duration/size/dimensions are measured, and the project points at
   * the actual file — the old fake path (stock drawable + invented duration +
   * invented 15 MB size) is gone.
   */
  fun handleCapturedMedia(file: java.io.File, type: MediaType, durationMs: Long = 0L) {
    viewModelScope.launch {
      val app = getApplication<Application>()
      val persisted = withContext(Dispatchers.IO) { persistCapturedFile(app, file) }
        ?: run {
          _smartAlgorithmNotice.value = "فشل حفظ الملف الملتقط محلياً"
          return@launch
        }

      var duration = if (type == MediaType.VIDEO) durationMs.coerceAtLeast(0L) else 0L
      var width = 0
      var height = 0
      var fps = 0
      var resolutionLabel = ""

      withContext(Dispatchers.IO) {
        if (type == MediaType.VIDEO) {
          val meta = RealMediaManager.extractVideoMetadata(app, Uri.fromFile(persisted))
          if (duration <= 0L) duration = meta.durationMs
          width = meta.width
          height = meta.height
          fps = meta.fps
          resolutionLabel = when {
            meta.width >= 3840 -> "UHD_4K"
            meta.width >= 2560 -> "QHD_2K"
            meta.width >= 1920 -> "FHD_1080P"
            else -> "HD_720P"
          }
        } else {
          val meta = RealMediaManager.extractPhotoMetadata(app, Uri.fromFile(persisted))
          width = meta.width
          height = meta.height
          resolutionLabel = "${meta.width}x${meta.height}"
        }
      }

      val newProj = ProjectEntity(
        title = persisted.nameWithoutExtension,
        mediaType = type.name,
        durationMs = duration,
        width = width,
        height = height,
        fps = fps,
        fileSizeBytes = persisted.length(),
        mediaUri = Uri.fromFile(persisted).toString(),
        resolution = resolutionLabel
      )
      val id = repository.insert(newProj)
      val created = repository.getById(id) ?: newProj.copy(id = id)
      selectProject(created)
    }
  }

  /** Copies a captured file from cache into filesDir/media for permanence. */
  private fun persistCapturedFile(app: Application, source: File): File? {
    return try {
      val mediaDir = File(app.filesDir, "media").apply { mkdirs() }
      val target = File(mediaDir, source.name)
      if (source.absolutePath != target.absolutePath) {
        source.inputStream().use { input ->
          target.outputStream().use { output -> input.copyTo(output) }
        }
      }
      target
    } catch (e: Exception) {
      null
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
    val project = _currentProject.value
    val mediaUri = project?.mediaUri

    // REAL audio: start the project's actual audio track from the playhead
    if (mediaUri != null) {
      audioController.startMain(mediaUri, _playheadMs.value, _playbackSpeed.value)
      if (_isAudioMuted.value) audioController.setMuted(true)
    }

    playbackJob?.cancel()
    val firedSfx = mutableSetOf<String>()
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
          firedSfx.clear()
          // REAL audio loop: re-seek the player to the trim start
          audioController.seekMainTo(minBound)
        } else {
          _playheadMs.value = next
        }
        val position = _playheadMs.value

        // REAL VU meter: amplitude measured from the decoded PCM envelope
        val envelope = _audioEnvelope.value
        if (envelope != null) {
          val amp = envelope.amplitudeAt(position)
          _audioPeakLeft.value = amp
          _audioPeakRight.value = amp
        } else {
          _audioPeakLeft.value = 0f
          _audioPeakRight.value = 0f
        }

        // REAL one-shot SFX scheduling from their timeline positions
        _timelineClips.value.forEach { clip ->
          if (clip.trackType == TrackType.AUDIO && !clip.isMuted && clip.sourceId != null) {
            val key = "${clip.id}:${position / maxBound}"
            if (position >= clip.startMs && position < clip.startMs + clip.durationMs &&
              position < clip.startMs + 300L && firedSfx.add(key)
            ) {
              val sfxId = clip.sourceId?.removePrefix("sfx:")
              if (clip.sourceId?.startsWith("sfx:") == true && sfxId != null) {
                val file = SfxSynthesizer.getFile(getApplication(), sfxId)
                audioController.playOneShot(file, clip.volume)
              }
            }
          }
        }
      }
    }
  }

  fun pausePlayback() {
    _isPlaying.value = false
    playbackJob?.cancel()
    audioController.pauseMain()
  }

  fun seekTo(ms: Long) {
    val duration = _currentProject.value?.durationMs ?: 30000L
    _playheadMs.value = ms.coerceIn(0L, duration)
    // Keep the REAL audio stream in sync with the scrub position
    audioController.seekMainTo(_playheadMs.value)
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
  // Smart Computational Photography — REAL analysis on ACTUAL pixels
  // =========================================================================

  private fun currentAnalysisBitmapOrNull(): Bitmap? {
    val project = _currentProject.value ?: return null
    if (analysisBitmap != null && analysisBitmapProjectId == project.id) return analysisBitmap
    // Compute synchronously as a fallback (callers are already off the main thread)
    val base = if (project.mediaType == "PHOTO") {
      RealMediaManager.loadProjectBitmapOrNull(getApplication(), project)
    } else {
      RealMediaManager.loadVideoFrameAt(getApplication(), project.mediaUri ?: return null, 0L)
    } ?: return null
    val downscaled = MediaAnalysisEngine.downscaleForAnalysis(base, 256)
    analysisBitmap = downscaled
    analysisBitmapProjectId = project.id
    return downscaled
  }

  fun applyGrayWorldAwb() {
    viewModelScope.launch(Dispatchers.Default) {
      val bitmap = currentAnalysisBitmapOrNull()
      if (bitmap == null) {
        _smartAlgorithmNotice.value = "لا توجد وسائط لتحليلها. قم باستيراد أو التقاط ملف أولاً."
        return@launch
      }
      val stats = MediaAnalysisEngine.analyzeColorBalance(bitmap)
      val result = SmartComputationalEngine.computeGrayWorldAwb(stats)
      updateAdjustment(
        actionNameAr = "توازن بياض ذكي (Gray World على البكسلات)",
        actionNameEn = "Smart AWB (Gray World on pixels)"
      ) {
        it.copy(
          temperature = result.recommendedTemp,
          tint = result.recommendedTint
        )
      }
      _smartAlgorithmNotice.value = result.descriptionAr
    }
  }

  fun applySmartAutoTone() {
    viewModelScope.launch(Dispatchers.Default) {
      val bitmap = currentAnalysisBitmapOrNull()
      if (bitmap == null) {
        _smartAlgorithmNotice.value = "لا توجد وسائط لتحليلها. قم باستيراد أو التقاط ملف أولاً."
        return@launch
      }
      val stats = MediaAnalysisEngine.analyzeTone(bitmap)
      val result = SmartComputationalEngine.computeSmartAutoTone(stats)
      updateAdjustment(
        actionNameAr = "موازنة ديناميكية ذكية (Auto-Levels على المدرج الحقيقي)",
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
  }

  fun applySmartSharpness() {
    viewModelScope.launch(Dispatchers.Default) {
      val bitmap = currentAnalysisBitmapOrNull()
      if (bitmap == null) {
        _smartAlgorithmNotice.value = "لا توجد وسائط لتحليلها. قم باستيراد أو التقاط ملف أولاً."
        return@launch
      }
      val stats = MediaAnalysisEngine.analyzeSharpness(bitmap)
      val sharpness = SmartComputationalEngine.computeSmartSharpness(stats)
      updateAdjustment(
        actionNameAr = "قناع حدة التفاصيل (تباين لابلاسي حقيقي)",
        actionNameEn = "Smart Micro-Contrast (Laplacian)"
      ) {
        it.copy(
          sharpness = sharpness,
          contrast = (it.contrast + 6f).coerceIn(-100f, 100f)
        )
      }
      _smartAlgorithmNotice.value =
        "قياس لابلاسي حقيقي (تباين ‎%.0f‎؛ وضوح ‎%.0f%%‎): قناع حدة $sharpness%%".format(
          stats.laplacianVariance,
          stats.normalizedAcuity * 100f
        )
    }
  }

  fun toggleFocusPeaking() {
    val next = !_adjustments.value.focusPeaking
    updateAdjustment(
      actionNameAr = if (next) "تفعيل المحدد البؤري" else "تعطيل المحدد البؤري",
      actionNameEn = if (next) "Enable Focus Peaking" else "Disable Focus Peaking"
    ) {
      it.copy(focusPeaking = next)
    }
    _smartAlgorithmNotice.value = if (next) {
      val maskReady = _edgeOverlayBitmap.value != null
      if (maskReady) "المحدد البؤري: يعرض حواف سوبل الحقيقية المقاسة من الإطار"
      else "المحدد البؤري: مفعل (بانتظار اكتمال تحليل الإطار)"
    } else "المحدد البؤري: معطل"
  }

  fun toggleZebraStripes() {
    val next = !_adjustments.value.zebraStripes
    updateAdjustment(
      actionNameAr = if (next) "تفعيل تحذير التعريض" else "تعطيل تحذير التعريض",
      actionNameEn = if (next) "Enable Zebra Warning" else "Disable Zebra Warning"
    ) {
      it.copy(zebraStripes = next)
    }
    _smartAlgorithmNotice.value = if (next) {
      val maskReady = _zebraOverlayBitmap.value != null
      if (maskReady) "تحذير التعريض: يعرض البكسلات المحروقة فعلياً (≥ 95 IRE)"
      else "تحذير التعريض: مفعل (بانتظار اكتمال تحليل الإطار)"
    } else "تحذير التعريض: معطل"
  }

  /**
   * REAL beat detection: decodes the project's actual audio via MediaCodec
   * and runs adaptive-threshold onset detection on the measured envelope.
   */
  fun detectAudioBeats(bpm: Int = 124) {
    viewModelScope.launch(Dispatchers.Default) {
      val envelope = _audioEnvelope.value ?: run {
        val project = _currentProject.value
        if (project?.mediaUri.isNullOrBlank()) {
          _smartAlgorithmNotice.value = "لا يوجد مسار صوتي لتحليله في هذا المشروع"
          return@launch
        }
        _smartAlgorithmNotice.value = "جارٍ فك ترميز الصوت الفعلي..."
        withContext(Dispatchers.IO) {
          AudioGraphEngine.decodeEnvelope(
            context = getApplication(),
            uri = Uri.parse(project!!.mediaUri),
            maxDurationMs = 10 * 60 * 1000L
          )
        }.also { _audioEnvelope.value = it }
      }

      if (envelope == null) {
        _smartAlgorithmNotice.value = "تعذر فك ترميز الصوت (قد يكون الملف بلا مسار صوتي)"
        return@launch
      }

      val result = AudioGraphEngine.detectBeats(envelope)
      _smartBeatMarkers.value = result.beatMarkersMs
      _smartAlgorithmNotice.value = result.descriptionAr
    }
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
    updateAdjustment(
      actionNameAr = "تثبيت حركي (معامل EMA)",
      actionNameEn = "Motion Stabilization (EMA)"
    ) {
      it.copy(gyroSmoothing = smoothingPercent)
    }
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

  // =========================================================================
  // REAL cinematic export: MediaCodec decode → GL color-grade → encode → MP4
  // =========================================================================
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
    _exportResultPath.value = null
    _exportError.value = null
    _exportStage.value = "تهيئة محرك التصدير"

    exportJob = viewModelScope.launch {
      val context = getApplication<Application>()
      val project = _currentProject.value
      if (project == null) {
        _isExporting.value = false
        _exportError.value = "لا يوجد مشروع مفتوح"
        onComplete()
        return@launch
      }

      // Photo projects export a real 5-second slideshow from the graded frame
      val isPhoto = project.mediaType == "PHOTO"
      val trimStart = _trimStartMs.value.coerceAtLeast(0L)
      val trimEnd = if (isPhoto || project.durationMs <= 0L) {
        maxOf(5000L, trimStart + 5000L)
      } else {
        _trimEndMs.value.coerceAtLeast(trimStart + 500L)
      }

      val adjustments = _adjustments.value
      val sourceBitmap = if (!project.mediaUri.isNullOrBlank() && isPhoto) {
        withContext(Dispatchers.IO) {
          RealMediaManager.loadProjectBitmapOrNull(context, project)
        }
      } else null

      val params = CinematicVideoExporter.ExportParams(
        sourceUri = if (isPhoto) null else project.mediaUri?.let { Uri.parse(it) },
        sourceBitmap = sourceBitmap,
        adjustments = adjustments,
        trimStartMs = trimStart,
        trimEndMs = trimEnd,
        targetWidth = resolution.width,
        targetHeight = resolution.height,
        fps = fps.coerceIn(24, 60),
        codec = codec,
        bitrateMbps = bitrateMbps.coerceIn(4, 120),
        outputDir = File(context.cacheDir, "exports")
      )

      val result = CinematicVideoExporter.export(context, params) { progress ->
        _exportProgress.value = progress.ratio * 100f
        _exportStage.value = progress.stageAr
        _exportSpeed.value = progress.speedFps
      }

      if (result.success && result.outputFile != null) {
        _exportStage.value = "حفظ الملف في معرض الصور"
        val saveResult = RealMediaManager.saveExportedVideoFileToGallery(context, result.outputFile)
        if (saveResult.isSuccess) {
          _exportProgress.value = 100f
          _exportComplete.value = true
          _exportResultPath.value = saveResult.displayPath
          _exportFrames.value = result.framesWritten
          _exportActualCodec.value = result.actualCodecLabel
          _exportedShareFile.value = result.outputFile
        } else {
          _exportError.value = saveResult.errorMessage ?: "فشل حفظ الملف المُصدّر"
        }
      } else {
        _exportError.value = result.errorMessageAr ?: "فشل التصدير"
      }
      _isExporting.value = false
      onComplete()
    }
  }

  private val _exportedShareFile = MutableStateFlow<File?>(null)
  val exportedShareFile: StateFlow<File?> = _exportedShareFile.asStateFlow()

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
    val clamped = speed.coerceIn(0.25f, 4.0f)
    _playbackSpeed.value = clamped
    // Apply REAL speed to the active audio stream
    audioController.setMainSpeed(clamped)
    // Update active video clip speed
    val updatedClips = _timelineClips.value.map { clip ->
      if (clip.trackType == TrackType.VIDEO) {
        clip.copy(speed = clamped)
      } else {
        clip
      }
    }
    _timelineClips.value = updatedClips
  }

  /** Real mixer controls, applied to the PreviewAudioController instantly. */
  fun setMasterAudioVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    _masterAudioVolume.value = clamped
    audioController.setMasterVolume(clamped)
  }

  fun toggleAudioMute() {
    _isAudioMuted.value = !_isAudioMuted.value
    audioController.setMuted(_isAudioMuted.value)
  }

  fun addAudioTrackClip(title: String, durationMs: Long, sourceId: String? = null) {
    val newId = "audio_${System.currentTimeMillis()}"
    val newClip = TimelineClip(
      id = newId,
      trackType = TrackType.AUDIO,
      title = title,
      startMs = _playheadMs.value,
      durationMs = durationMs.coerceAtLeast(1500L),
      volume = 0.85f,
      sourceId = sourceId
    )
    _timelineClips.value = _timelineClips.value + newClip
  }

  /** Sets the REAL volume of a timeline clip (used by the mixer panel). */
  fun setClipVolume(clipId: String, volume: Float) {
    _timelineClips.value = _timelineClips.value.map { clip ->
      if (clip.id == clipId) clip.copy(volume = volume.coerceIn(0f, 1f)) else clip
    }
  }

  /** Toggles the REAL mute flag of a timeline clip. */
  fun toggleClipMute(clipId: String) {
    _timelineClips.value = _timelineClips.value.map { clip ->
      if (clip.id == clipId) clip.copy(isMuted = !clip.isMuted) else clip
    }
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
    _exportStage.value = ""
  }

  fun setEditorStudioType(type: EditorStudioType) {
    _editorStudioType.value = type
  }

  /**
   * REAL media import: the picked content URI is COPIED into permanent app
   * storage (picker URIs are transient and can expire), real metadata is
   * extracted, and no fake drawable fallback is attached.
   */
  fun importMediaFromUri(
    context: Context,
    uri: Uri,
    isVideo: Boolean,
    onSuccess: (ProjectEntity) -> Unit = {}
  ) {
    viewModelScope.launch {
      val app = getApplication<Application>()

      // Persist the media so the project survives picker permission expiry
      val persistedFile = withContext(Dispatchers.IO) {
        try {
          val ext = when {
            isVideo -> "mp4"
            else -> "jpg"
          }
          val mediaDir = File(app.filesDir, "media").apply { mkdirs() }
          val target = File(mediaDir, "LUMINA_IMPORT_${System.currentTimeMillis()}.$ext")
          context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
          }
          if (target.exists() && target.length() > 0) target else null
        } catch (e: Exception) {
          null
        }
      }

      val mediaUriString = persistedFile?.let { Uri.fromFile(it).toString() } ?: uri.toString()
      val mediaUri = Uri.parse(mediaUriString)

      val project = if (isVideo) {
        val meta = RealMediaManager.extractVideoMetadata(context, mediaUri)
        ProjectEntity(
          title = meta.title,
          mediaType = "VIDEO",
          durationMs = meta.durationMs,
          width = meta.width,
          height = meta.height,
          fps = meta.fps,
          fileSizeBytes = meta.sizeBytes,
          mediaUri = mediaUriString,
          resolution = when {
            meta.width >= 3840 -> "UHD_4K"
            meta.width >= 2560 -> "QHD_2K"
            meta.width >= 1920 -> "FHD_1080P"
            else -> "HD_720P"
          }
        )
      } else {
        val meta = RealMediaManager.extractPhotoMetadata(context, mediaUri)
        ProjectEntity(
          title = meta.title,
          mediaType = "PHOTO",
          durationMs = 0L,
          width = meta.width,
          height = meta.height,
          fps = 0,
          fileSizeBytes = meta.sizeBytes,
          mediaUri = mediaUriString,
          resolution = "${meta.width}x${meta.height}"
        )
      }
      val id = repository.insert(project)
      val createdProject = repository.getById(id) ?: project.copy(id = id)
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
