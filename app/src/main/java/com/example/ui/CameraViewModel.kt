package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera capture modes: Stills (Photo) or Video Recording.
 */
enum class CameraCaptureMode {
  PHOTO,
  VIDEO
}

/**
 * Complete UI state for CameraX operations, covering exposure, zoom, focus,
 * flash/torch, and video recording progress.
 */
data class CameraUiState(
  val mode: CameraCaptureMode = CameraCaptureMode.VIDEO,
  val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
  val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
  val isTorchOn: Boolean = false,
  val isAudioEnabled: Boolean = true,
  val isRecording: Boolean = false,
  val isRecordingPaused: Boolean = false,
  val recordingDurationMs: Long = 0L,
  val recordingSeconds: Long = 0L,
  val recordedBytes: Long = 0L,
  val isCapturingPhoto: Boolean = false,
  val zoomRatio: Float = 1.0f,
  val minZoomRatio: Float = 1.0f,
  val maxZoomRatio: Float = 5.0f,
  val exposureIndex: Int = 0,
  val minExposureIndex: Int = -4,
  val maxExposureIndex: Int = 4,
  val exposureStep: Float = 0.1667f,
  val selectedVideoQuality: Quality = Quality.UHD,
  val hasFlashUnit: Boolean = false,
  val isCameraReady: Boolean = false,
  val errorMessage: String? = null,
  val showGrid: Boolean = true,
  val selectedFilter: String = "NORMAL",
  val focusPointX: Float? = null,
  val focusPointY: Float? = null,
  val isFocusVisible: Boolean = false
)

/**
 * CameraViewModel responsible for integrating with CameraX:
 * - Robust lifecycle-aware camera binding and unbinding
 * - Video recording with pause/resume/stop lifecycle management
 * - High-resolution still photo capture
 * - Hardware camera controls (Zoom, Exposure compensation, Flash, Torch, Tap-to-Focus)
 * - Clean state exposure via StateFlow<CameraUiState>
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

  // CameraX components
  private var cameraProvider: ProcessCameraProvider? = null
  private var camera: Camera? = null
  private var cameraControl: CameraControl? = null
  private var cameraInfo: CameraInfo? = null
  private var imageCapture: ImageCapture? = null
  private var videoCapture: VideoCapture<Recorder>? = null
  private var activeRecording: Recording? = null

  // Background executor for photo capture callbacks
  private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

  // Job for recording timer
  private var timerJob: Job? = null
  private var focusHideJob: Job? = null

  /**
   * Binds CameraX use cases to the provided LifecycleOwner and PreviewView.
   */
  fun bindCameraToLifecycle(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
  ) {
    val context = getApplication<Application>().applicationContext
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
      try {
        val provider = cameraProviderFuture.get()
        cameraProvider = provider
        rebindUseCases(lifecycleOwner, previewView)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to get ProcessCameraProvider", e)
        _uiState.update { it.copy(errorMessage = "فشل في تشغيل الكاميرا: ${e.localizedMessage}") }
      }
    }, ContextCompat.getMainExecutor(context))
  }

  /**
   * Internal helper to rebind Preview, ImageCapture, and VideoCapture to the current lifecycle.
   */
  fun rebindUseCases(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
  ) {
    val provider = cameraProvider ?: return
    try {
      provider.unbindAll()

      val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
      }

      val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(_uiState.value.lensFacing)
        .build()

      val currentState = _uiState.value

      val boundCamera = if (currentState.mode == CameraCaptureMode.VIDEO) {
        val qualitySelector = QualitySelector.from(
          currentState.selectedVideoQuality,
          FallbackStrategy.lowerQualityOrHigherThan(currentState.selectedVideoQuality)
        )
        val recorder = Recorder.Builder()
          .setQualitySelector(qualitySelector)
          .build()
        val vCapture = VideoCapture.withOutput(recorder)
        videoCapture = vCapture

        provider.bindToLifecycle(
          lifecycleOwner,
          cameraSelector,
          preview,
          vCapture
        )
      } else {
        val iCapture = ImageCapture.Builder()
          .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
          .setFlashMode(currentState.flashMode)
          .build()
        imageCapture = iCapture

        provider.bindToLifecycle(
          lifecycleOwner,
          cameraSelector,
          preview,
          iCapture
        )
      }

      camera = boundCamera
      cameraControl = boundCamera.cameraControl
      cameraInfo = boundCamera.cameraInfo

      // Query hardware capabilities
      val zoomState = boundCamera.cameraInfo.zoomState.value
      val expState = boundCamera.cameraInfo.exposureState
      val flashUnit = boundCamera.cameraInfo.hasFlashUnit()

      _uiState.update {
        it.copy(
          isCameraReady = true,
          hasFlashUnit = flashUnit,
          minZoomRatio = zoomState?.minZoomRatio ?: 1.0f,
          maxZoomRatio = zoomState?.maxZoomRatio ?: 5.0f,
          zoomRatio = zoomState?.zoomRatio ?: 1.0f,
          minExposureIndex = expState.exposureCompensationRange.lower,
          maxExposureIndex = expState.exposureCompensationRange.upper,
          exposureIndex = expState.exposureCompensationIndex,
          exposureStep = expState.exposureCompensationStep.toFloat(),
          errorMessage = null
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Use case binding failed", e)
      _uiState.update { it.copy(errorMessage = "خطأ في تهيئة الكاميرا: ${e.localizedMessage}") }
    }
  }

  /**
   * Sets the capture mode (PHOTO or VIDEO) and rebinds if necessary.
   */
  fun setCaptureMode(mode: CameraCaptureMode, lifecycleOwner: LifecycleOwner?, previewView: PreviewView?) {
    if (_uiState.value.mode == mode) return
    if (_uiState.value.isRecording) {
      stopRecording()
    }
    _uiState.update { it.copy(mode = mode) }
    if (lifecycleOwner != null && previewView != null) {
      rebindUseCases(lifecycleOwner, previewView)
    }
  }

  /**
   * Toggles between Photo and Video capture modes.
   */
  fun toggleMode(lifecycleOwner: LifecycleOwner?, previewView: PreviewView?) {
    val nextMode = if (_uiState.value.mode == CameraCaptureMode.PHOTO) CameraCaptureMode.VIDEO else CameraCaptureMode.PHOTO
    setCaptureMode(nextMode, lifecycleOwner, previewView)
  }

  /**
   * Switches camera lens between front and back.
   */
  fun toggleLensFacing(lifecycleOwner: LifecycleOwner?, previewView: PreviewView?) {
    if (_uiState.value.isRecording) return // prevent switching during active recording
    val nextFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
      CameraSelector.LENS_FACING_FRONT
    } else {
      CameraSelector.LENS_FACING_BACK
    }
    _uiState.update { it.copy(lensFacing = nextFacing, isTorchOn = false) }
    if (lifecycleOwner != null && previewView != null) {
      rebindUseCases(lifecycleOwner, previewView)
    }
  }

  /**
   * Toggles torch (flashlight) state.
   */
  fun toggleTorch() {
    val currentTorch = _uiState.value.isTorchOn
    val nextTorch = !currentTorch
    cameraControl?.enableTorch(nextTorch)?.addListener({
      _uiState.update { it.copy(isTorchOn = nextTorch) }
    }, ContextCompat.getMainExecutor(getApplication()))
  }

  /**
   * Sets the flash mode for photo capture.
   */
  fun setFlashMode(flashMode: Int) {
    imageCapture?.flashMode = flashMode
    _uiState.update { it.copy(flashMode = flashMode) }
  }

  /**
   * Sets the zoom ratio (between min and max).
   */
  fun setZoomRatio(ratio: Float) {
    val clamped = ratio.coerceIn(_uiState.value.minZoomRatio, _uiState.value.maxZoomRatio)
    cameraControl?.setZoomRatio(clamped)
    _uiState.update { it.copy(zoomRatio = clamped) }
  }

  /**
   * Sets the exposure compensation index.
   */
  fun setExposureIndex(index: Int) {
    val clamped = index.coerceIn(_uiState.value.minExposureIndex, _uiState.value.maxExposureIndex)
    cameraControl?.setExposureCompensationIndex(clamped)
    _uiState.update { it.copy(exposureIndex = clamped) }
  }

  /**
   * Performs tap-to-focus and metering at the given preview coordinates.
   */
  fun focusAndMeter(x: Float, y: Float, previewWidth: Float, previewHeight: Float) {
    if (previewWidth <= 0f || previewHeight <= 0f) return
    val control = cameraControl ?: return

    try {
      val factory = SurfaceOrientedMeteringPointFactory(previewWidth, previewHeight)
      val point = factory.createPoint(x, y)
      val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
        .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
        .build()

      control.startFocusAndMetering(action)

      _uiState.update {
        it.copy(
          focusPointX = x,
          focusPointY = y,
          isFocusVisible = true
        )
      }

      focusHideJob?.cancel()
      focusHideJob = viewModelScope.launch {
        delay(3000)
        _uiState.update { it.copy(isFocusVisible = false) }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed tap to focus", e)
    }
  }

  /**
   * Toggles rule-of-thirds composition grid.
   */
  fun toggleGrid() {
    _uiState.update { it.copy(showGrid = !it.showGrid) }
  }

  /**
   * Toggles audio recording enablement.
   */
  fun toggleAudio() {
    _uiState.update { it.copy(isAudioEnabled = !it.isAudioEnabled) }
  }

  /**
   * Sets video quality (UHD 4K, FHD 1080p, HD 720p, etc.).
   */
  fun setVideoQuality(quality: Quality, lifecycleOwner: LifecycleOwner?, previewView: PreviewView?) {
    _uiState.update { it.copy(selectedVideoQuality = quality) }
    if (_uiState.value.mode == CameraCaptureMode.VIDEO && lifecycleOwner != null && previewView != null) {
      rebindUseCases(lifecycleOwner, previewView)
    }
  }

  /**
   * Sets live viewfinder color filter preview ("NORMAL", "GRAYSCALE", "SEPIA", etc.).
   */
  fun setFilter(filter: String) {
    _uiState.update { it.copy(selectedFilter = filter) }
  }

  /**
   * Captures a high-resolution photo and saves it to a persistent app file.
   */
  fun capturePhoto(
    context: Context,
    onPhotoCaptured: (File) -> Unit,
    onError: (Exception) -> Unit
  ) {
    val capture = imageCapture ?: run {
      onError(IllegalStateException("ImageCapture is not initialized"))
      return
    }

    _uiState.update { it.copy(isCapturingPhoto = true) }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
    val photoFile = File(outputDir, "LUMINA_IMG_$timeStamp.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
      outputOptions,
      cameraExecutor,
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          viewModelScope.launch {
            _uiState.update { it.copy(isCapturingPhoto = false) }
            onPhotoCaptured(photoFile)
          }
        }

        override fun onError(exception: ImageCaptureException) {
          Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
          viewModelScope.launch {
            _uiState.update {
              it.copy(
                isCapturingPhoto = false,
                errorMessage = "فشل التقاط الصورة: ${exception.message}"
              )
            }
            onError(exception)
          }
        }
      }
    )
  }

  /**
   * Starts video recording with proper lifecycle listeners and byte/duration telemetry.
   */
  fun startRecording(
    context: Context,
    hasAudioPermission: Boolean,
    onVideoRecorded: (File, Long) -> Unit,
    onError: (Exception) -> Unit
  ) {
    val vCapture = videoCapture ?: run {
      onError(IllegalStateException("VideoCapture is not initialized"))
      return
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
    val videoFile = File(outputDir, "LUMINA_VID_$timeStamp.mp4")

    val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

    var pendingRecording = vCapture.output.prepareRecording(context, fileOutputOptions)

    if (hasAudioPermission && _uiState.value.isAudioEnabled) {
      try {
        pendingRecording = pendingRecording.withAudioEnabled()
      } catch (e: SecurityException) {
        Log.w(TAG, "Audio permission was not granted for recording", e)
      }
    }

    val recordingStartTime = System.currentTimeMillis()

    activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
      when (recordEvent) {
        is VideoRecordEvent.Start -> {
          _uiState.update {
            it.copy(
              isRecording = true,
              isRecordingPaused = false,
              recordingDurationMs = 0L,
              recordingSeconds = 0L,
              recordedBytes = 0L
            )
          }
          startTimerJob(recordingStartTime)
        }

        is VideoRecordEvent.Status -> {
          val durationMs = recordEvent.recordingStats.recordedDurationNanos / 1_000_000L
          val bytes = recordEvent.recordingStats.numBytesRecorded
          _uiState.update {
            it.copy(
              recordingDurationMs = durationMs,
              recordedBytes = bytes
            )
          }
        }

        is VideoRecordEvent.Pause -> {
          _uiState.update { it.copy(isRecordingPaused = true) }
        }

        is VideoRecordEvent.Resume -> {
          _uiState.update { it.copy(isRecordingPaused = false) }
        }

        is VideoRecordEvent.Finalize -> {
          stopTimerJob()
          val finalDurationMs = recordEvent.recordingStats.recordedDurationNanos / 1_000_000L
          _uiState.update {
            it.copy(
              isRecording = false,
              isRecordingPaused = false,
              recordingDurationMs = finalDurationMs
            )
          }
          activeRecording = null

          if (!recordEvent.hasError()) {
            onVideoRecorded(videoFile, finalDurationMs)
          } else {
            Log.e(TAG, "Video recording error: ${recordEvent.error}")
            _uiState.update {
              it.copy(errorMessage = "خطأ أثناء تسجيل الفيديو: code ${recordEvent.error}")
            }
            onError(Exception("Recording finalized with error: ${recordEvent.error}"))
          }
        }
      }
    }
  }

  /**
   * Pauses active video recording.
   */
  fun pauseRecording() {
    activeRecording?.pause()
  }

  /**
   * Resumes paused video recording.
   */
  fun resumeRecording() {
    activeRecording?.resume()
  }

  /**
   * Stops video recording and releases active recording resources safely.
   */
  fun stopRecording() {
    activeRecording?.stop()
    activeRecording = null
    stopTimerJob()
  }

  private fun startTimerJob(startTime: Long) {
    stopTimerJob()
    timerJob = viewModelScope.launch {
      while (_uiState.value.isRecording) {
        delay(500)
        if (!_uiState.value.isRecordingPaused) {
          val elapsedSec = (System.currentTimeMillis() - startTime) / 1000L
          _uiState.update { it.copy(recordingSeconds = elapsedSec) }
        }
      }
    }
  }

  private fun stopTimerJob() {
    timerJob?.cancel()
    timerJob = null
  }

  /**
   * Unbinds CameraX cleanly when screen is paused or dismissed.
   */
  fun unbindCamera() {
    if (_uiState.value.isRecording) {
      stopRecording()
    }
    cameraProvider?.unbindAll()
    _uiState.update { it.copy(isCameraReady = false) }
  }

  override fun onCleared() {
    super.onCleared()
    stopRecording()
    stopTimerJob()
    focusHideJob?.cancel()
    cameraProvider?.unbindAll()
    cameraExecutor.shutdown()
  }

  companion object {
    private const val TAG = "CameraViewModel"
  }
}
