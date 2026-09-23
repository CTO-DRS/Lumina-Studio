package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.MediaType
import com.example.engine.ImageProcessingEngine
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun CameraCaptureDialog(
  onDismiss: () -> Unit,
  onMediaCaptured: (file: File, type: MediaType, durationMs: Long) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
    hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission || !hasAudioPermission) {
      permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(ObsidianBg)
        .testTag("camera_capture_dialog")
    ) {
      if (hasCameraPermission) {
        CameraXView(
          context = context,
          lifecycleOwner = lifecycleOwner,
          hasAudioPermission = hasAudioPermission,
          onClose = onDismiss,
          onMediaCaptured = onMediaCaptured
        )
      } else {
        // Permission Request Fallback UI
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = "طلب إذن الكاميرا",
            tint = ElectricCyan,
            modifier = Modifier.size(64.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "إذن الكاميرا مطلوب للالتقاط المباشر",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "يرجى منح صلاحية الوصول للكاميرا والميكروفون لتصوير الصور وفيديوهات 4K مع التركيز وضبط التعريض يدوياً في Lumina Studio.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = {
              permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
            modifier = Modifier.testTag("grant_camera_permission_button")
          ) {
            Text("منح إذن الكاميرا والصوت", fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated, contentColor = TextSecondary)
          ) {
            Text("إلغاء")
          }
        }
      }
    }
  }
}

@Composable
private fun CameraXView(
  context: Context,
  lifecycleOwner: androidx.lifecycle.LifecycleOwner,
  hasAudioPermission: Boolean,
  onClose: () -> Unit,
  onMediaCaptured: (file: File, type: MediaType, durationMs: Long) -> Unit
) {
  var isVideoMode by remember { mutableStateOf(false) }
  var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
  var isTorchOn by remember { mutableStateOf(false) }

  // Tap-to-Focus & Exposure Control States
  var focusTarget by remember { mutableStateOf<Offset?>(null) }
  var isFocusVisible by remember { mutableStateOf(false) }
  var showExposurePanel by remember { mutableStateOf(false) }
  var exposureIndex by remember { mutableIntStateOf(0) }
  var minExposureIndex by remember { mutableIntStateOf(-4) }
  var maxExposureIndex by remember { mutableIntStateOf(4) }
  var exposureStep by remember { mutableFloatStateOf(0.1667f) }

  // Video Recording States
  var selectedVideoQuality by remember { mutableStateOf(Quality.UHD) }
  var isAudioEnabled by remember { mutableStateOf(true) }
  var isRecording by remember { mutableStateOf(false) }
  var isPaused by remember { mutableStateOf(false) }
  var recordedBytes by remember { mutableLongStateOf(0L) }
  var recordedDurationNanos by remember { mutableLongStateOf(0L) }
  var recordingSeconds by remember { mutableLongStateOf(0L) }

  // Image Processing States
  val coroutineScope = rememberCoroutineScope()
  var selectedFilter by remember { mutableStateOf("NORMAL") } // "NORMAL", "GRAYSCALE", "SEPIA"
  var brightnessLevel by remember { mutableFloatStateOf(0f) } // -50f to +50f

  var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
  var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
  var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
  var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
  var activeRecording by remember { mutableStateOf<Recording?>(null) }

  val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

  DisposableEffect(Unit) {
    onDispose {
      activeRecording?.stop()
      cameraExecutor.shutdown()
    }
  }

  // Timer for video recording display fallback
  LaunchedEffect(isRecording, isPaused) {
    if (isRecording && !isPaused) {
      while (isRecording && !isPaused) {
        delay(1000)
        recordingSeconds++
      }
    } else if (!isRecording) {
      recordingSeconds = 0L
    }
  }

  // Auto-hide focus reticle after 3 seconds
  LaunchedEffect(focusTarget) {
    if (focusTarget != null) {
      isFocusVisible = true
      delay(3000)
      isFocusVisible = false
    }
  }

  val previewView = remember {
    PreviewView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }

  // Bind Camera Provider when mode, lens, or video quality changes
  LaunchedEffect(lensFacing, isVideoMode, selectedVideoQuality) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
      try {
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().also {
          it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
          .requireLensFacing(lensFacing)
          .build()

        val boundCamera = if (isVideoMode) {
          val qualitySelector = QualitySelector.from(
            selectedVideoQuality,
            FallbackStrategy.lowerQualityOrHigherThan(selectedVideoQuality)
          )
          val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
          val vCapture = VideoCapture.withOutput(recorder)
          videoCapture = vCapture

          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            vCapture
          )
        } else {
          val imgCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
          imageCapture = imgCapture

          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imgCapture
          )
        }

        cameraControl = boundCamera.cameraControl
        cameraInfo = boundCamera.cameraInfo

        // Initialize Exposure parameters
        val expState = boundCamera.cameraInfo.exposureState
        if (expState.isExposureCompensationSupported) {
          minExposureIndex = expState.exposureCompensationRange.lower
          maxExposureIndex = expState.exposureCompensationRange.upper
          val stepRational = expState.exposureCompensationStep
          exposureStep = if (stepRational.denominator != 0) {
            stepRational.numerator.toFloat() / stepRational.denominator.toFloat()
          } else {
            0.1667f
          }
          exposureIndex = expState.exposureCompensationIndex
        }
      } catch (exc: Exception) {
        Log.e("CameraX", "Use case binding failed", exc)
      }
    }, ContextCompat.getMainExecutor(context))
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. Live Camera Preview Stream
    AndroidView(
      factory = { previewView },
      modifier = Modifier.fillMaxSize()
    )

    // Viewfinder Touch Layer for Tap-To-Focus & Metering
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(previewView) {
          detectTapGestures { offset ->
            try {
              val factory = previewView.meteringPointFactory
              val point = factory.createPoint(offset.x, offset.y)
              val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
              )
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

              cameraControl?.startFocusAndMetering(action)
              focusTarget = offset
              isFocusVisible = true
            } catch (e: Exception) {
              Log.e("CameraX", "Tap-to-focus gesture failed", e)
            }
          }
        }
        .testTag("camera_viewfinder_touch_surface")
    )

    // Animated Tap-to-Focus Reticle
    AnimatedVisibility(
      visible = isFocusVisible && focusTarget != null,
      enter = fadeIn(tween(150)),
      exit = fadeOut(tween(300))
    ) {
      focusTarget?.let { target ->
        val evCurrent = exposureIndex * exposureStep
        val evText = if (evCurrent >= 0) String.format(Locale.US, "+%.1f EV", evCurrent) else String.format(Locale.US, "%.1f EV", evCurrent)
        FocusReticle(
          offset = target,
          evText = evText
        )
      }
    }

    // 2. Top Controls Bar: Close, Live REC HUD / Mode Info, Exposure, Torch, Mic, Lens Switch
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
            )
          )
          .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            if (isRecording) activeRecording?.stop()
            onClose()
          },
          modifier = Modifier.testTag("camera_close_button")
        ) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
        }

        // Live 4K Recording Status HUD
        if (isRecording) {
          val totalSecs = if (recordedDurationNanos > 0) recordedDurationNanos / 1_000_000_000L else recordingSeconds
          val mins = totalSecs / 60L
          val secs = totalSecs % 60L
          val mbRecorded = recordedBytes.toFloat() / (1024f * 1024f)

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(if (isPaused) CyberGold.copy(alpha = 0.9f) else SunsetCoral.copy(alpha = 0.95f))
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("live_rec_hud")
          ) {
            Box(
              modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (isPaused) Color.Black else Color.White)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isPaused) {
                String.format("مؤقت %02d:%02d • 4K", mins, secs)
              } else {
                String.format("REC %02d:%02d • %.1fMB", mins, secs, mbRecorded)
              },
              color = if (isPaused) Color.Black else Color.White,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        } else {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black.copy(alpha = 0.6f))
              .border(1.dp, if (isVideoMode) NeonViolet.copy(alpha = 0.6f) else ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 5.dp)
          ) {
            val resLabel = when (selectedVideoQuality) {
              Quality.UHD -> "4K UHD (3840×2160)"
              Quality.FHD -> "1080p FHD"
              else -> "720p HD"
            }
            Text(
              text = if (isVideoMode) "كاميرا فيديو $resLabel" else "كاميرا صور سينمائية فائقة",
              color = if (isVideoMode) NeonViolet else ElectricCyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Exposure Toggle Button
          IconButton(
            onClick = { showExposurePanel = !showExposurePanel },
            modifier = Modifier.testTag("camera_exposure_toggle_button")
          ) {
            Icon(
              imageVector = Icons.Default.Exposure,
              contentDescription = "التحكم في التعريض الضوئي",
              tint = if (showExposurePanel || exposureIndex != 0) CyberGold else Color.White
            )
          }

          // Audio Toggle for Video
          if (isVideoMode) {
            IconButton(
              onClick = { isAudioEnabled = !isAudioEnabled },
              modifier = Modifier.testTag("video_audio_toggle")
            ) {
              Icon(
                imageVector = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (isAudioEnabled) "الميكروفون مفعّل" else "الميكروفون مكتوم",
                tint = if (isAudioEnabled) EmeraldGreen else SunsetCoral
              )
            }
          }

          // Torch / Flash Toggle
          IconButton(
            onClick = {
              val nextTorch = !isTorchOn
              isTorchOn = nextTorch
              cameraControl?.enableTorch(nextTorch)
            }
          ) {
            Icon(
              imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
              contentDescription = "الفلاش",
              tint = if (isTorchOn) CyberGold else Color.White
            )
          }

          // Lens Switch (Back / Front)
          IconButton(
            onClick = {
              lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
              } else {
                CameraSelector.LENS_FACING_BACK
              }
            },
            modifier = Modifier.testTag("camera_switch_lens_button")
          ) {
            Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "تبديل الكاميرا", tint = Color.White)
          }
        }
      }

      // Manual Exposure Slider Panel
      AnimatedVisibility(
        visible = showExposurePanel,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
      ) {
        val evValue = exposureIndex * exposureStep
        val formattedEv = if (evValue >= 0) String.format(Locale.US, "+%.1f EV", evValue) else String.format(Locale.US, "%.1f EV", evValue)

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("exposure_slider_panel"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = "التعريض",
            tint = CyberGold,
            modifier = Modifier.size(18.dp)
          )

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = formattedEv,
            color = CyberGold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(62.dp)
          )

          Slider(
            value = exposureIndex.toFloat(),
            onValueChange = { newIdx ->
              val intIdx = newIdx.toInt()
              exposureIndex = intIdx
              cameraControl?.setExposureCompensationIndex(intIdx)
            },
            valueRange = minExposureIndex.toFloat()..maxExposureIndex.toFloat(),
            modifier = Modifier
              .weight(1f)
              .height(28.dp)
              .testTag("exposure_compensation_slider"),
            colors = SliderDefaults.colors(
              thumbColor = CyberGold,
              activeTrackColor = CyberGold,
              inactiveTrackColor = ObsidianBorder
            )
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Quick Reset EV Button
          IconButton(
            onClick = {
              exposureIndex = 0
              cameraControl?.setExposureCompensationIndex(0)
            },
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(ObsidianSurfaceElevated)
              .testTag("reset_exposure_button")
          ) {
            Icon(
              imageVector = Icons.Default.RestartAlt,
              contentDescription = "إعادة ضبط التعريض",
              tint = TextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // 3. Bottom Controls Bar: Mode Switcher, Quality Selector & Shutter / Recording Controls
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
          )
        )
        .padding(bottom = 36.dp, top = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Photo / Video Mode Selector Pills
      if (!isRecording) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(if (!isVideoMode) ElectricCyan else Color.Transparent)
              .clickable { isVideoMode = false }
              .padding(horizontal = 14.dp, vertical = 6.dp)
              .testTag("photo_mode_tab")
          ) {
            Text(
              text = "صورة PHOTO",
              color = if (!isVideoMode) Color.Black else Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(if (isVideoMode) NeonViolet else Color.Transparent)
              .clickable { isVideoMode = true }
              .padding(horizontal = 14.dp, vertical = 6.dp)
              .testTag("video_mode_tab")
          ) {
            Text(
              text = "فيديو 4K VIDEO",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Video Quality Selector Pills (4K UHD, 1080p FHD, 720p HD)
        if (isVideoMode) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black.copy(alpha = 0.6f))
              .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              Quality.UHD to "4K UHD (2160p)",
              Quality.FHD to "1080p FHD",
              Quality.HD to "720p HD"
            ).forEach { (quality, label) ->
              val isSelected = selectedVideoQuality == quality
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) NeonViolet else ObsidianSurfaceElevated)
                  .clickable { selectedVideoQuality = quality }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
                  .testTag("quality_chip_${label.take(2)}")
              ) {
                Text(
                  text = label,
                  color = if (isSelected) Color.White else TextSecondary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // Processing Engine Controls for Photo Mode (Grayscale, Sepia, Brightness)
        if (!isVideoMode) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black.copy(alpha = 0.55f))
              .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(
              "NORMAL" to "أصلي Normal",
              "GRAYSCALE" to "رمادي Grayscale",
              "SEPIA" to "سيبيا Sepia"
            ).forEach { (mode, label) ->
              val isSelected = selectedFilter == mode
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) ElectricCyan else ObsidianSurfaceElevated)
                  .clickable { selectedFilter = mode }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("filter_chip_$mode")
              ) {
                Text(
                  text = label,
                  color = if (isSelected) Color.Black else TextSecondary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Brightness Control Slider
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "سطوع: ${brightnessLevel.toInt()}",
              color = TextSecondary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.width(68.dp)
            )
            Slider(
              value = brightnessLevel,
              onValueChange = { brightnessLevel = it },
              valueRange = -50f..50f,
              modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .testTag("camera_brightness_slider"),
              colors = SliderDefaults.colors(
                thumbColor = CyberGold,
                activeTrackColor = CyberGold,
                inactiveTrackColor = ObsidianBorder
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      // Shutter / Recording Controls
      if (isVideoMode) {
        if (isRecording) {
          // Active 4K Recording Controls: Pause/Resume + Stop
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
          ) {
            // Pause / Resume Button
            IconButton(
              onClick = {
                if (isPaused) {
                  activeRecording?.resume()
                  isPaused = false
                } else {
                  activeRecording?.pause()
                  isPaused = true
                }
              },
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ObsidianSurfaceElevated)
                .border(2.dp, if (isPaused) CyberGold else ObsidianBorder, CircleShape)
                .testTag("video_pause_resume_button")
            ) {
              Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "استئناف التسجيل" else "إيقاف مؤقت",
                tint = if (isPaused) CyberGold else Color.White,
                modifier = Modifier.size(28.dp)
              )
            }

            // Stop / Finish 4K Recording Button
            Box(
              modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .padding(6.dp)
                .clip(CircleShape)
                .background(SunsetCoral)
                .clickable {
                  activeRecording?.stop()
                  activeRecording = null
                }
                .testTag("video_stop_button"),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color.White)
              )
            }
          }
        } else {
          // Idle Video Record Trigger Button (Red 4K Record Trigger)
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .border(4.dp, Color.White, CircleShape)
              .padding(6.dp)
              .clip(CircleShape)
              .background(SunsetCoral)
              .clickable {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
                val prefix = if (selectedVideoQuality == Quality.UHD) "VID_4K_" else "VID_"
                val videoFile = File(context.cacheDir, "$prefix$timeStamp.mp4")
                val outputOptions = FileOutputOptions.Builder(videoFile).build()

                val pendingRecord = videoCapture?.output
                  ?.prepareRecording(context, outputOptions)

                val recording = pendingRecord?.apply {
                  if (isAudioEnabled && hasAudioPermission && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                  }
                }?.start(ContextCompat.getMainExecutor(context)) { event ->
                  when (event) {
                    is VideoRecordEvent.Start -> {
                      isRecording = true
                      isPaused = false
                      recordedBytes = 0L
                      recordedDurationNanos = 0L
                    }
                    is VideoRecordEvent.Pause -> {
                      isPaused = true
                    }
                    is VideoRecordEvent.Resume -> {
                      isPaused = false
                    }
                    is VideoRecordEvent.Status -> {
                      recordedBytes = event.recordingStats.numBytesRecorded
                      recordedDurationNanos = event.recordingStats.recordedDurationNanos
                    }
                    is VideoRecordEvent.Finalize -> {
                      isRecording = false
                      isPaused = false
                      if (!event.hasError()) {
                        val durationMs = (recordedDurationNanos / 1_000_000L).coerceAtLeast(1000L)
                        val resName = if (selectedVideoQuality == Quality.UHD) "4K UHD" else "عالية الدقة"
                        Toast.makeText(context, "تم تصوير وحفظ فيديو $resName بنجاح!", Toast.LENGTH_SHORT).show()
                        onMediaCaptured(videoFile, MediaType.VIDEO, durationMs)
                        onClose()
                      } else {
                        Log.e("CameraX", "Video recording error: ${event.error}")
                        Toast.makeText(context, "حدث خطأ أثناء حفظ الفيديو", Toast.LENGTH_SHORT).show()
                      }
                    }
                  }
                }
                activeRecording = recording
              }
              .testTag("video_shutter_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.FiberManualRecord,
              contentDescription = "بدء تسجيل فيديو 4K",
              tint = Color.White,
              modifier = Modifier.size(34.dp)
            )
          }
        }
      } else {
        // Photo Capture Button
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(ElectricCyan)
            .clickable {
              val imgCap = imageCapture ?: return@clickable
              val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
              val photoFile = File(context.cacheDir, "IMG_RAW_$timeStamp.jpg")
              val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

              imgCap.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                  override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val isGrayscale = selectedFilter == "GRAYSCALE"
                    val isSepia = selectedFilter == "SEPIA"
                    val brightness = brightnessLevel

                    if (isGrayscale || isSepia || brightness != 0f) {
                      coroutineScope.launch {
                        val processedFile = File(context.cacheDir, "IMG_PROC_$timeStamp.jpg")
                        val finalFile = ImageProcessingEngine.processCapturedImageFile(
                          inputFile = photoFile,
                          outputFile = processedFile,
                          isGrayscale = isGrayscale,
                          isSepia = isSepia,
                          brightness = brightness
                        )
                        Toast.makeText(context, "تم التقاط ومعالجة الصورة بنجاح!", Toast.LENGTH_SHORT).show()
                        onMediaCaptured(finalFile, MediaType.PHOTO, 0L)
                        onClose()
                      }
                    } else {
                      Toast.makeText(context, "تم التقاط الصورة بجودة فائقة!", Toast.LENGTH_SHORT).show()
                      onMediaCaptured(photoFile, MediaType.PHOTO, 0L)
                      onClose()
                    }
                  }

                  override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(context, "فشل التقاط الصورة", Toast.LENGTH_SHORT).show()
                  }
                }
              )
            }
            .testTag("photo_shutter_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = "التقاط صورة",
            tint = Color.Black,
            modifier = Modifier.size(32.dp)
          )
        }
      }
    }
  }
}

/**
 * Modern 2027 Pro Viewfinder Focus & Exposure Reticle with scale pulse and EV indicator.
 */
@Composable
private fun FocusReticle(
  offset: Offset,
  evText: String
) {
  var animatedScale by remember { mutableFloatStateOf(1.35f) }
  val scale by animateFloatAsState(
    targetValue = animatedScale,
    animationSpec = tween(durationMillis = 220),
    label = "focus_scale"
  )

  LaunchedEffect(offset) {
    animatedScale = 1.0f
  }

  Box(
    modifier = Modifier
      .offset {
        IntOffset(
          (offset.x - 36.dp.toPx()).toInt(),
          (offset.y - 36.dp.toPx()).toInt()
        )
      }
      .size(72.dp)
      .testTag("focus_reticle")
  ) {
    // Outer focus box with animated scale
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .border(1.5.dp, CyberGold, RoundedCornerShape(8.dp))
    ) {
      // Center precision focal point
      Box(
        modifier = Modifier
          .size(4.dp)
          .align(Alignment.Center)
          .background(CyberGold, CircleShape)
      )
    }

    // Side Sun / EV indicator badge
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .offset(x = 42.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color.Black.copy(alpha = 0.75f))
        .border(0.5.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
      Icon(
        imageVector = Icons.Default.WbSunny,
        contentDescription = "مؤشر التعريض",
        tint = CyberGold,
        modifier = Modifier.size(12.dp)
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = evText,
        color = CyberGold,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}
