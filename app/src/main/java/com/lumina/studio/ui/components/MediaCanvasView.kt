package com.lumina.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.studio.data.model.AdjustmentsState
import com.lumina.studio.data.model.CropAspect
import com.lumina.studio.data.model.ProjectEntity
import com.lumina.studio.engine.color.ColorMatrixEngine
import com.lumina.studio.engine.media.RealMediaManager
import com.lumina.studio.engine.color.VisualEffectsEngine
import com.lumina.studio.ui.theme.CyberGold
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.NeonViolet
import com.lumina.studio.ui.theme.ObsidianBg
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun MediaCanvasView(
  project: ProjectEntity?,
  adjustments: AdjustmentsState,
  playheadMs: Long,
  isCompareActive: Boolean,
  compareSliderPosition: Float,
  onCompareSliderChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  isLetterboxActive: Boolean = false,
  onToggleLetterbox: () -> Unit = {},
  isPlaying: Boolean = false,
  edgeOverlay: android.graphics.Bitmap? = null,
  zebraOverlay: android.graphics.Bitmap? = null
) {
  val context = LocalContext.current
  val isVideoProject = project?.mediaType == "VIDEO" && !project.mediaUri.isNullOrBlank()

  // REAL frame-accurate video preview: samples the actual frame at the
  // playhead position with the platform decoder (throttled during playback).
  val frameSampleKey = if (isPlaying) playheadMs / 400L else playheadMs
  var videoFrame by remember(project?.mediaUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
  if (isVideoProject) {
    LaunchedEffect(project?.mediaUri, frameSampleKey) {
      val uri = project?.mediaUri ?: return@LaunchedEffect
      val bitmap = withContext(Dispatchers.IO) {
        RealMediaManager.loadVideoFrameAt(context, uri, frameSampleKey * (if (isPlaying) 400L else 1L))
      }
      if (bitmap != null) videoFrame = bitmap
    }
  }

  // Calculate Unified ColorMatrix
  val colorMatrix = remember(adjustments) {
    ColorMatrixEngine.createUnifiedMatrix(adjustments)
  }

  val aspectModifier = when (adjustments.selectedCrop) {
    CropAspect.RATIO_16_9 -> Modifier.aspectRatio(16f / 9f)
    CropAspect.RATIO_9_16 -> Modifier.aspectRatio(9f / 16f)
    CropAspect.RATIO_1_1 -> Modifier.aspectRatio(1f)
    CropAspect.RATIO_4_5 -> Modifier.aspectRatio(4f / 5f)
    CropAspect.RATIO_21_9 -> Modifier.aspectRatio(21f / 9f)
    CropAspect.ORIGINAL -> Modifier.aspectRatio(16f / 9f)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(ObsidianBg)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
        .then(aspectModifier)
        .testTag("media_canvas_view")
    ) {
      val widthPx = constraints.maxWidth.toFloat()
      val heightPx = constraints.maxHeight.toFloat()

      // 4K Video Detail Zoom and Pan Inspection State
      var scale by remember { mutableFloatStateOf(1f) }
      var panOffset by remember { mutableStateOf(Offset.Zero) }

      // Maximum pan boundaries based on current scale
      val maxPanX = if (scale > 1f) (widthPx * (scale - 1f)) / 2f else 0f
      val maxPanY = if (scale > 1f) (heightPx * (scale - 1f)) / 2f else 0f

      // 1. Zoomable & Pannable Media Layer Container
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clipToBounds()
          .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
              val newScale = (scale * zoom).coerceIn(1f, 6f)
              val currentMaxPanX = (widthPx * (newScale - 1f)) / 2f
              val currentMaxPanY = (heightPx * (newScale - 1f)) / 2f

              panOffset = if (newScale <= 1.01f) {
                Offset.Zero
              } else {
                Offset(
                  x = (panOffset.x + pan.x).coerceIn(-currentMaxPanX, currentMaxPanX),
                  y = (panOffset.y + pan.y).coerceIn(-currentMaxPanY, currentMaxPanY)
                )
              }
              scale = newScale
            }
          }
          .pointerInput(Unit) {
            detectTapGestures(
              onDoubleTap = { tapOffset ->
                if (scale > 1.2f) {
                  scale = 1f
                  panOffset = Offset.Zero
                } else {
                  scale = 2.5f
                  val targetPanX = (widthPx / 2f - tapOffset.x) * 1.5f
                  val targetPanY = (heightPx / 2f - tapOffset.y) * 1.5f
                  val currentMaxPanX = (widthPx * (2.5f - 1f)) / 2f
                  val currentMaxPanY = (heightPx * (2.5f - 1f)) / 2f
                  panOffset = Offset(
                    x = targetPanX.coerceIn(-currentMaxPanX, currentMaxPanX),
                    y = targetPanY.coerceIn(-currentMaxPanY, currentMaxPanY)
                  )
                }
              }
            )
          }
          .graphicsLayer {
            val autoCropScale = if (adjustments.horizonTiltDeg != 0f) {
              val rad = Math.toRadians(kotlin.math.abs(adjustments.horizonTiltDeg).toDouble())
              (1.0 / kotlin.math.cos(rad)).toFloat().coerceIn(1.0f, 1.15f)
            } else 1.0f
            scaleX = scale * autoCropScale
            scaleY = scale * autoCropScale
            rotationZ = adjustments.horizonTiltDeg
            translationX = panOffset.x
            translationY = panOffset.y
          }
          .testTag("zoomable_media_container")
      ) {
        if (!isCompareActive) {
          // Standard View: REAL media content with Real-Time ColorMatrix Filter
          RenderMediaContent(
            mediaUri = project?.mediaUri,
            videoFrame = videoFrame,
            contentDescription = "معاينة الوسائط",
            colorFilter = ColorFilter.colorMatrix(colorMatrix),
            modifier = Modifier.fillMaxSize()
          )

          // Overlay Canvas Effects: Vignette, Glitch, Grain, Bloom + REAL masks
          Canvas(modifier = Modifier.fillMaxSize()) {
            VisualEffectsEngine.renderCanvasEffects(
              scope = this,
              state = adjustments,
              seedOffset = playheadMs,
              edgeOverlay = edgeOverlay,
              zebraOverlay = zebraOverlay
            )
          }
        } else {
          // Split Screen Compare View:
          // Left side = RAW Unedited frame; Right side = Graded & Effected Preview
          val splitX = (widthPx * compareSliderPosition).coerceIn(0f, widthPx)

          // Raw original layer (clipped to left half)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .drawWithContent {
                clipRect(left = 0f, top = 0f, right = splitX, bottom = heightPx) {
                  this@drawWithContent.drawContent()
                }
              }
          ) {
            RenderMediaContent(
              mediaUri = project?.mediaUri,
              videoFrame = videoFrame,
              contentDescription = "الصورة الأصلية الخام",
              colorFilter = null,
              modifier = Modifier.fillMaxSize()
            )
          }

          // Graded layer (clipped to right half)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .drawWithContent {
                clipRect(left = splitX, top = 0f, right = widthPx, bottom = heightPx) {
                  this@drawWithContent.drawContent()
                }
              }
          ) {
            RenderMediaContent(
              mediaUri = project?.mediaUri,
              videoFrame = videoFrame,
              contentDescription = "المعاينة بعد المعالجة",
              colorFilter = ColorFilter.colorMatrix(colorMatrix),
              modifier = Modifier.fillMaxSize()
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
              VisualEffectsEngine.renderCanvasEffects(
                scope = this,
                state = adjustments,
                seedOffset = playheadMs,
                edgeOverlay = edgeOverlay,
                zebraOverlay = zebraOverlay
              )
            }
          }
        }
      }

      // 2. Comparison Slider and Labels (drawn on top of viewport coordinates)
      if (isCompareActive) {
        val splitX = (widthPx * compareSliderPosition).coerceIn(0f, widthPx)

        // Split Divider Line
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawLine(
            color = ElectricCyan,
            start = Offset(splitX, 0f),
            end = Offset(splitX, heightPx),
            strokeWidth = 2.dp.toPx()
          )
        }

        // Draggable Handle
        Box(
          modifier = Modifier
            .offset { IntOffset(splitX.roundToInt() - 16.dp.roundToPx(), (heightPx / 2f).roundToInt() - 16.dp.roundToPx()) }
            .size(32.dp)
            .clip(CircleShape)
            .background(ElectricCyan)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                val newPos = (splitX + dragAmount.x) / widthPx
                onCompareSliderChange(newPos)
              }
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "مقبض المقارنة",
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }

        // Floating Labels: "قبل" & "بعد"
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(text = "الأصل (قبل)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(NeonViolet.copy(alpha = 0.75f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(text = "المعالجة (بعد)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }

      // 3. Cinematic Letterbox black bars (2.39:1 CinemaScope anamorphic aspect)
      if (isLetterboxActive) {
        val barHeight = heightPx * 0.12f
        Canvas(modifier = Modifier.fillMaxSize()) {
          // Top black bar
          drawRect(
            color = Color.Black,
            topLeft = Offset(0f, 0f),
            size = Size(widthPx, barHeight)
          )
          // Bottom black bar
          drawRect(
            color = Color.Black,
            topLeft = Offset(0f, heightPx - barHeight),
            size = Size(widthPx, barHeight)
          )
        }
      }

      // 4. 4K Zoom Level & Reset HUD Badge (Appears when zoomed in)
      AnimatedVisibility(
        visible = scale > 1.05f,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(if (isCompareActive) Alignment.TopCenter else Alignment.TopEnd)
          .padding(8.dp)
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, ElectricCyan.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ZoomIn,
            contentDescription = "تكبير 4K",
            tint = ElectricCyan,
            modifier = Modifier.size(15.dp)
          )
          Text(
            text = "4K Zoom ${(scale * 100).toInt()}%",
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(ElectricCyan)
              .clickable {
                scale = 1f
                panOffset = Offset.Zero
              }
              .padding(horizontal = 6.dp, vertical = 2.dp)
              .testTag("reset_zoom_button")
          ) {
            Text(
              text = "إعادة 1x",
              color = Color.Black,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Smart Non-AI Algorithmic Overlays HUD (Focus Peaking, Zebra, Horizon)
      val hasSmartOverlay = adjustments.focusPeaking || adjustments.zebraStripes || adjustments.horizonTiltDeg != 0f
      AnimatedVisibility(
        visible = hasSmartOverlay,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(8.dp)
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (adjustments.focusPeaking) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF00FF66).copy(alpha = 0.9f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("smart_hud_focus_peaking")
            ) {
              Text(
                text = "PEAK FOCUS",
                color = Color.Black,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          if (adjustments.zebraStripes) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFFD700).copy(alpha = 0.9f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("smart_hud_zebra")
            ) {
              Text(
                text = "ZEBRA 95%",
                color = Color.Black,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          if (adjustments.horizonTiltDeg != 0f) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ElectricCyan.copy(alpha = 0.9f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("smart_hud_horizon")
            ) {
              Text(
                text = "HORIZON ${adjustments.horizonTiltDeg}°",
                color = Color.Black,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      // 5. 4K Viewport Pan Radar / Minimap (when zoomed in)
      AnimatedVisibility(
        visible = scale > 1.3f,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(bottom = 38.dp, end = 8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(4.dp))
            .padding(2.dp)
        ) {
          val minimapW = 42f
          val minimapH = 22f
          val rectW = (minimapW / scale).coerceIn(6f, minimapW)
          val rectH = (minimapH / scale).coerceIn(4f, minimapH)
          val normX = if (maxPanX > 0f) (-panOffset.x / maxPanX) else 0f
          val normY = if (maxPanY > 0f) (-panOffset.y / maxPanY) else 0f
          val rectLeft = ((minimapW - rectW) / 2f + normX * ((minimapW - rectW) / 2f)).coerceIn(0f, minimapW - rectW)
          val rectTop = ((minimapH - rectH) / 2f + normY * ((minimapH - rectH) / 2f)).coerceIn(0f, minimapH - rectH)

          Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
              color = ElectricCyan.copy(alpha = 0.35f),
              topLeft = Offset(rectLeft, rectTop),
              size = Size(rectW, rectH)
            )
            drawRect(
              color = ElectricCyan,
              topLeft = Offset(rectLeft, rectTop),
              size = Size(rectW, rectH),
              style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
          }
        }
      }

      // 6. Studio Overlay Watermark / HUD Badge (Bottom Bar of Canvas)
      Row(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          val fps = if ((project?.fps ?: 0) > 0) project!!.fps else 30
          val seconds = (playheadMs / 1000L) % 60L
          val minutes = (playheadMs / 60000L) % 60L
          val frames = ((playheadMs % 1000L) * fps / 1000L).toInt()
          val timecode = String.format("%02d:%02d:%02d", minutes, seconds, frames)

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isPlaying) Color.Red else ElectricCyan)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "TC $timecode • ${fps}FPS",
              color = ElectricCyan,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // CinemaScope Toggle Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isLetterboxActive) CyberGold.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.65f))
            .border(1.dp, if (isLetterboxActive) CyberGold else ObsidianBorder, RoundedCornerShape(6.dp))
            .clickable { onToggleLetterbox() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("toggle_letterbox_button")
        ) {
          Text(
            text = if (isLetterboxActive) "2.39:1 شريط سينمائي" else "شريط سينمائي",
            color = if (isLetterboxActive) CyberGold else TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // 4K Zoom Quick Preset Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (scale > 1.05f) ElectricCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.65f))
            .border(1.dp, if (scale > 1.05f) ElectricCyan else ObsidianBorder, RoundedCornerShape(6.dp))
            .clickable {
              scale = when {
                scale < 1.8f -> 2.0f
                scale < 3.5f -> 4.0f
                else -> 1.0f
              }
              if (scale == 1.0f) panOffset = Offset.Zero
            }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("canvas_zoom_pill")
        ) {
          Text(
            text = if (scale > 1.05f) "تكبير ${(scale * 100).toInt()}%" else "تكبير 4K",
            color = if (scale > 1.05f) ElectricCyan else TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun RenderMediaContent(
  mediaUri: String?,
  videoFrame: android.graphics.Bitmap?,
  contentDescription: String,
  colorFilter: ColorFilter?,
  modifier: Modifier = Modifier
) {
  when {
    mediaUri.isNullOrBlank() -> {
      // HONEST empty state: no stock photo pretending to be user media
      Column(
        modifier = modifier.background(Color(0xFF101318)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.AddAPhoto,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(42.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "لا توجد وسائط في المشروع",
          color = TextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "استخدم زر الاستيراد أو الكاميرا لإضافة ملف حقيقي",
          color = TextMuted,
          fontSize = 11.sp,
          textAlign = TextAlign.Center
        )
      }
    }
    videoFrame != null -> {
      // REAL decoded frame at the current playhead position
      Image(
        bitmap = videoFrame.asImageBitmap(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        colorFilter = colorFilter,
        modifier = modifier
      )
    }
    else -> {
      AsyncImage(
        model = mediaUri,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        colorFilter = colorFilter,
        modifier = modifier
      )
    }
  }
}
