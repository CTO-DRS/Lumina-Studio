package com.lumina.studio.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lumina.studio.data.model.MediaType
import com.lumina.studio.ui.viewmodels.GalleryFilter
import com.lumina.studio.ui.viewmodels.GalleryMediaItem
import com.lumina.studio.ui.viewmodels.GallerySortOrder
import com.lumina.studio.ui.viewmodels.GalleryViewModel
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
import java.io.File

/**
 * Media Gallery Screen that uses Coil to load and display captured photos
 * and videos from device storage in a responsive, scrollable grid layout.
 */
@Composable
fun GalleryScreen(
  onOpenInStudio: (File, MediaType, Long) -> Unit,
  onLaunchCamera: () -> Unit,
  modifier: Modifier = Modifier,
  galleryViewModel: GalleryViewModel = viewModel()
) {
  val uiState by galleryViewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var showSortMenu by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .testTag("gallery_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // 1. Top Bar / Selection Mode Bar
      if (uiState.isSelectionMode) {
        GallerySelectionTopBar(
          selectedCount = uiState.selectedIds.size,
          onDeleteSelected = { galleryViewModel.deleteSelected() },
          onClearSelection = { galleryViewModel.clearSelection() }
        )
      } else {
        GalleryStandardTopBar(
          itemCount = uiState.filteredItems.size,
          onLaunchCamera = onLaunchCamera,
          onOpenSortMenu = { showSortMenu = true },
          sortOrder = uiState.sortOrder,
          showSortMenu = showSortMenu,
          onDismissSortMenu = { showSortMenu = false },
          onSelectSortOrder = { order ->
            galleryViewModel.setSortOrder(order)
            showSortMenu = false
          }
        )
      }

      // 2. Filter Category Chips (All, Photos, Videos)
      GalleryFilterRow(
        selectedFilter = uiState.selectedFilter,
        totalCount = uiState.allItems.size,
        photoCount = uiState.allItems.count { it.type == MediaType.PHOTO },
        videoCount = uiState.allItems.count { it.type == MediaType.VIDEO },
        onSelectFilter = { galleryViewModel.setFilter(it) }
      )

      // 3. Main Media Content Grid
      if (uiState.isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .testTag("gallery_loading_spinner"),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(
            color = ElectricCyan,
            modifier = Modifier.size(42.dp)
          )
        }
      } else if (uiState.filteredItems.isEmpty()) {
        GalleryEmptyState(
          filter = uiState.selectedFilter,
          onLaunchCamera = onLaunchCamera
        )
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 112.dp),
          contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxSize()
            .testTag("gallery_media_grid")
        ) {
          items(
            items = uiState.filteredItems,
            key = { it.id }
          ) { item ->
            val isSelected = uiState.selectedIds.contains(item.id)

            MediaGridItemCard(
              item = item,
              isSelectionMode = uiState.isSelectionMode,
              isSelected = isSelected,
              onClick = {
                if (uiState.isSelectionMode) {
                  galleryViewModel.toggleSelection(item.id)
                } else {
                  galleryViewModel.selectMedia(item)
                }
              },
              onLongClick = {
                galleryViewModel.toggleSelection(item.id)
              }
            )
          }
        }
      }
    }

    // 4. Floating Camera Shutter FAB
    FloatingActionButton(
      onClick = onLaunchCamera,
      containerColor = CyberGold,
      contentColor = Color.Black,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 24.dp)
        .testTag("gallery_floating_camera_button")
    ) {
      Icon(
        imageVector = Icons.Default.PhotoCamera,
        contentDescription = "التقاط بالكاميرا",
        modifier = Modifier.size(26.dp)
      )
    }

    // 5. Fullscreen Media Detail & Action Dialog
    uiState.selectedMedia?.let { selected ->
      MediaDetailPreviewDialog(
        mediaItem = selected,
        onDismiss = { galleryViewModel.selectMedia(null) },
        onOpenInStudio = {
          galleryViewModel.selectMedia(null)
          onOpenInStudio(selected.file, selected.type, selected.durationMs)
        },
        onShare = {
          shareMediaFile(context, selected.file, selected.type)
        },
        onDelete = {
          galleryViewModel.deleteMedia(selected)
        }
      )
    }
  }
}

/**
 * Standard Header Bar for Gallery with title, count, camera launcher, and sort menu.
 */
@Composable
private fun GalleryStandardTopBar(
  itemCount: Int,
  onLaunchCamera: () -> Unit,
  onOpenSortMenu: () -> Unit,
  sortOrder: GallerySortOrder,
  showSortMenu: Boolean,
  onDismissSortMenu: () -> Unit,
  onSelectSortOrder: (GallerySortOrder) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(ObsidianSurface)
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(listOf(ObsidianBorder, Color.Black)),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
      )
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            Brush.linearGradient(listOf(ElectricCyan.copy(alpha = 0.2f), NeonViolet.copy(alpha = 0.2f)))
          )
          .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.PhotoLibrary,
          contentDescription = "معرض الصور والفيديوهات",
          tint = ElectricCyan,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        Text(
          text = "معرض الوسائط",
          color = TextPrimary,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "$itemCount عناصر ملتقطة على الجهاز",
          color = TextMuted,
          fontSize = 11.sp
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Box {
        IconButton(
          onClick = onOpenSortMenu,
          modifier = Modifier.testTag("gallery_sort_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = "ترتيب الوسائط",
            tint = ElectricCyan
          )
        }

        DropdownMenu(
          expanded = showSortMenu,
          onDismissRequest = onDismissSortMenu,
          modifier = Modifier.background(ObsidianSurfaceElevated)
        ) {
          GallerySortOrder.values().forEach { order ->
            DropdownMenuItem(
              text = {
                Text(
                  text = "${order.labelAr} (${order.labelEn})",
                  color = if (sortOrder == order) CyberGold else TextPrimary,
                  fontSize = 13.sp,
                  fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                )
              },
              onClick = { onSelectSortOrder(order) }
            )
          }
        }
      }

      IconButton(
        onClick = onLaunchCamera,
        modifier = Modifier.testTag("gallery_top_camera_button")
      ) {
        Icon(
          imageVector = Icons.Default.PhotoCamera,
          contentDescription = "فتح الكاميرا",
          tint = CyberGold
        )
      }
    }
  }
}

/**
 * Top Bar active during multi-select mode with batch actions.
 */
@Composable
private fun GallerySelectionTopBar(
  selectedCount: Int,
  onDeleteSelected: () -> Unit,
  onClearSelection: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(ObsidianSurfaceElevated)
      .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
      .padding(horizontal = 16.dp, vertical = 10.dp)
      .testTag("gallery_selection_bar"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onClearSelection) {
        Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد", tint = TextPrimary)
      }
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "تم تحديد $selectedCount",
        color = CyberGold,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onDeleteSelected,
        modifier = Modifier.testTag("gallery_batch_delete_button")
      ) {
        Icon(Icons.Default.Delete, contentDescription = "حذف المحدد", tint = SunsetCoral)
      }
    }
  }
}

/**
 * Category Filter Row with All, Photos, and Videos chips.
 */
@Composable
private fun GalleryFilterRow(
  selectedFilter: GalleryFilter,
  totalCount: Int,
  photoCount: Int,
  videoCount: Int,
  onSelectFilter: (GalleryFilter) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    GalleryFilterChip(
      label = "الكل",
      count = totalCount,
      isSelected = selectedFilter == GalleryFilter.ALL,
      testTag = "gallery_filter_all",
      onClick = { onSelectFilter(GalleryFilter.ALL) }
    )

    GalleryFilterChip(
      label = "صور",
      count = photoCount,
      isSelected = selectedFilter == GalleryFilter.PHOTOS,
      testTag = "gallery_filter_photos",
      onClick = { onSelectFilter(GalleryFilter.PHOTOS) }
    )

    GalleryFilterChip(
      label = "فيديوهات 4K",
      count = videoCount,
      isSelected = selectedFilter == GalleryFilter.VIDEOS,
      testTag = "gallery_filter_videos",
      onClick = { onSelectFilter(GalleryFilter.VIDEOS) }
    )
  }
}

@Composable
private fun GalleryFilterChip(
  label: String,
  count: Int,
  isSelected: Boolean,
  testTag: String,
  onClick: () -> Unit
) {
  val backgroundColor by animateColorAsState(
    targetValue = if (isSelected) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurface,
    label = "chip_bg"
  )
  val borderColor by animateColorAsState(
    targetValue = if (isSelected) ElectricCyan else ObsidianBorder,
    label = "chip_border"
  )
  val textColor by animateColorAsState(
    targetValue = if (isSelected) ElectricCyan else TextSecondary,
    label = "chip_text"
  )

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(backgroundColor)
      .border(1.dp, borderColor, RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = label,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      )
      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(if (isSelected) ElectricCyan else ObsidianBorder)
          .padding(horizontal = 6.dp, vertical = 1.dp)
      ) {
        Text(
          text = count.toString(),
          color = if (isSelected) Color.Black else TextMuted,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

/**
 * Media Item Card in the Grid displaying Coil loaded thumbnail, badges, and selection.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItemCard(
  item: GalleryMediaItem,
  isSelectionMode: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) {
  val context = LocalContext.current
  val scale by animateFloatAsState(targetValue = if (isSelected) 0.94f else 1f, label = "card_scale")
  val borderColor by animateColorAsState(
    targetValue = when {
      isSelected -> CyberGold
      item.type == MediaType.VIDEO -> ElectricCyan.copy(alpha = 0.4f)
      else -> ObsidianBorder
    },
    label = "border_color"
  )

  Box(
    modifier = Modifier
      .scale(scale)
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(ObsidianSurface)
      .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .testTag("gallery_item_${item.id.hashCode()}")
  ) {
    // 1. Coil SubcomposeAsyncImage loading file from disk
    SubcomposeAsyncImage(
      model = ImageRequest.Builder(context)
        .data(item.file)
        .crossfade(true)
        .build(),
      contentDescription = item.name,
      contentScale = ContentScale.Crop,
      loading = {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(ObsidianSurfaceElevated),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(
            color = ElectricCyan,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp)
          )
        }
      },
      error = {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.linearGradient(listOf(ObsidianSurfaceElevated, Color.Black))
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (item.type == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.PhotoLibrary,
            contentDescription = "تعذر تحميل الوسائط",
            tint = TextMuted,
            modifier = Modifier.size(28.dp)
          )
        }
      },
      modifier = Modifier.fillMaxSize()
    )

    // 2. Gradient scrim for video / bottom meta
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(38.dp)
        .align(Alignment.BottomCenter)
        .background(
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
          )
        )
    )

    // 3. Top-Right Badge: Video tag or Resolution tag
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(6.dp)
    ) {
      if (item.type == MediaType.VIDEO) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(0.5.dp, ElectricCyan.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Videocam,
              contentDescription = "فيديو",
              tint = ElectricCyan,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "4K",
              color = Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Text(
            text = "HDR",
            color = CyberGold,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // 4. Bottom Info: Duration (if video) and File Size
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(horizontal = 6.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = item.formattedSize,
        color = TextSecondary,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium
      )

      if (item.type == MediaType.VIDEO) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = item.formattedDuration,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // 5. Selection Mode Overlay Checkmark
    if (isSelectionMode) {
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(6.dp)
      ) {
        if (isSelected) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "محدد",
            tint = CyberGold,
            modifier = Modifier.size(22.dp)
          )
        } else {
          Icon(
            imageVector = Icons.Default.RadioButtonUnchecked,
            contentDescription = "غير محدد",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }
  }
}

/**
 * Empty State displayed when no media matches the current filter.
 */
@Composable
private fun GalleryEmptyState(
  filter: GalleryFilter,
  onLaunchCamera: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(ObsidianSurfaceElevated)
        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.PhotoLibrary,
        contentDescription = null,
        tint = ElectricCyan,
        modifier = Modifier.size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = when (filter) {
        GalleryFilter.PHOTOS -> "لا توجد صور ملتقطة بعد"
        GalleryFilter.VIDEOS -> "لا توجد مقاطع فيديو 4K بعد"
        GalleryFilter.ALL -> "معرض الوسائط فارغ"
      },
      color = TextPrimary,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "التقط صوراً عالية الدقة وفيديوهات سينمائية بدقة 4K عبر كاميرا Lumina لتظهر هنا مباشرة في المعرض.",
      color = TextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = onLaunchCamera,
      colors = ButtonDefaults.buttonColors(
        containerColor = CyberGold,
        contentColor = Color.Black
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.testTag("gallery_empty_capture_button")
    ) {
      Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("التقاط أول صورة أو فيديو", fontWeight = FontWeight.Bold)
    }
  }
}

/**
 * Fullscreen Interactive Media Detail Dialog offering high-res Coil preview
 * and options to Edit in Studio, Share, or Delete.
 */
@Composable
private fun MediaDetailPreviewDialog(
  mediaItem: GalleryMediaItem,
  onDismiss: () -> Unit,
  onOpenInStudio: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit
) {
  val context = LocalContext.current

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.95f))
        .testTag("gallery_media_detail_dialog")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Toolbar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق المعاينة", tint = Color.White)
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = mediaItem.name,
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${mediaItem.formattedDate} • ${mediaItem.formattedSize}",
              color = TextMuted,
              fontSize = 11.sp
            )
          }

          IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = SunsetCoral)
          }
        }

        // Center: Full Preview with Coil
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
              .data(mediaItem.file)
              .crossfade(true)
              .build(),
            contentDescription = mediaItem.name,
            contentScale = ContentScale.Fit,
            loading = {
              CircularProgressIndicator(color = ElectricCyan)
            },
            error = {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = if (mediaItem.type == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.PhotoLibrary,
                  contentDescription = null,
                  tint = TextMuted,
                  modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("معاينة الوسائط", color = TextSecondary)
              }
            },
            modifier = Modifier.fillMaxSize()
          )

          // Play icon overlay if video
          if (mediaItem.type == MediaType.VIDEO) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.5.dp, ElectricCyan, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "تشغيل الفيديو",
                tint = ElectricCyan,
                modifier = Modifier.size(36.dp)
              )
            }
          }
        }

        // Bottom Action Bar
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurface)
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Edit in Studio Button (Featured CTA)
            Button(
              onClick = onOpenInStudio,
              colors = ButtonDefaults.buttonColors(
                containerColor = CyberGold,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("gallery_edit_in_studio_button")
            ) {
              Icon(Icons.Default.MovieFilter, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("تعديل في الاستوديو", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Share Button
            Button(
              onClick = onShare,
              colors = ButtonDefaults.buttonColors(
                containerColor = ObsidianSurfaceElevated,
                contentColor = ElectricCyan
              ),
              border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("gallery_share_button")
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("مشاركة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
          }
        }
      }
    }
  }
}

/**
 * Standard Android Share helper for media files.
 */
private fun shareMediaFile(context: Context, file: File, type: MediaType) {
  try {
    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    val mimeType = if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg"
    val intent = Intent(Intent.ACTION_SEND).apply {
      this.type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة الوسائط عبر"))
  } catch (e: Exception) {
    // Fallback share with intent
    val intent = Intent(Intent.ACTION_SEND).apply {
      this.type = if (type == MediaType.VIDEO) "video/*" else "image/*"
      putExtra(Intent.EXTRA_TEXT, "تم التعديل والتصوير عبر Lumina Studio: ${file.name}")
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة الوسائط"))
  }
}
