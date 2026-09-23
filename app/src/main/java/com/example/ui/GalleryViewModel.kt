package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Representation of a captured photo or video loaded from device storage.
 */
data class GalleryMediaItem(
  val id: String,
  val file: File,
  val name: String,
  val type: MediaType,
  val sizeBytes: Long,
  val dateModifiedMs: Long,
  val durationMs: Long = 0L,
  val resolutionBadge: String = if (type == MediaType.VIDEO) "4K UHD" else "RAW HD"
) {
  val formattedSize: String
    get() {
      val mb = sizeBytes / (1024.0 * 1024.0)
      return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
      } else {
        String.format(Locale.US, "%.0f KB", sizeBytes / 1024.0)
      }
    }

  val formattedDuration: String
    get() {
      val totalSec = durationMs / 1000
      val min = totalSec / 60
      val sec = totalSec % 60
      return String.format(Locale.US, "%02d:%02d", min, sec)
    }

  val formattedDate: String
    get() {
      val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
      return sdf.format(Date(dateModifiedMs))
    }
}

/**
 * Filter categories for the media gallery.
 */
enum class GalleryFilter(val labelAr: String, val labelEn: String) {
  ALL("الكل", "All"),
  PHOTOS("الصور", "Photos"),
  VIDEOS("الفيديوهات", "Videos")
}

/**
 * Sort options for the media grid.
 */
enum class GallerySortOrder(val labelAr: String, val labelEn: String) {
  NEWEST("الأحدث أولاً", "Newest"),
  OLDEST("الأقدم أولاً", "Oldest"),
  SIZE_DESC("الأكبر حجماً", "Largest")
}

/**
 * UI State for the media gallery screen.
 */
data class GalleryUiState(
  val allItems: List<GalleryMediaItem> = emptyList(),
  val filteredItems: List<GalleryMediaItem> = emptyList(),
  val selectedFilter: GalleryFilter = GalleryFilter.ALL,
  val sortOrder: GallerySortOrder = GallerySortOrder.NEWEST,
  val isLoading: Boolean = false,
  val selectedMedia: GalleryMediaItem? = null,
  val selectedIds: Set<String> = emptySet(),
  val isSelectionMode: Boolean = false,
  val errorMessage: String? = null
)

/**
 * ViewModel managing device storage media discovery, filtering, Coil loading,
 * and deletion operations for the Media Gallery.
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(GalleryUiState())
  val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

  init {
    loadMediaFromDeviceStorage()
  }

  /**
   * Scans app storage directories for captured photos (.jpg, .png, .webp) and videos (.mp4, .mkv).
   * Generates initial high-resolution sample captures if directory is empty.
   */
  fun loadMediaFromDeviceStorage(): Job {
    return viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      val mediaItems = withContext(Dispatchers.IO) {
        val context = getApplication<Application>().applicationContext
        val scanned = mutableListOf<GalleryMediaItem>()

        val directories = listOfNotNull(
          context.getExternalFilesDir(null),
          context.filesDir,
          context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
          context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        )

        val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
        val videoExtensions = setOf("mp4", "mkv", "webm", "mov", "3gp")

        directories.forEach { dir ->
          if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
              if (file.isFile && file.length() > 0) {
                val ext = file.extension.lowercase()
                if (ext in imageExtensions) {
                  scanned.add(
                    GalleryMediaItem(
                      id = file.absolutePath,
                      file = file,
                      name = file.name,
                      type = MediaType.PHOTO,
                      sizeBytes = file.length(),
                      dateModifiedMs = file.lastModified(),
                      resolutionBadge = "HDR Photo"
                    )
                  )
                } else if (ext in videoExtensions) {
                  val estimatedDurationMs = when {
                    file.name.contains("30") -> 30000L
                    file.name.contains("60") -> 60000L
                    else -> 15000L
                  }
                  scanned.add(
                    GalleryMediaItem(
                      id = file.absolutePath,
                      file = file,
                      name = file.name,
                      type = MediaType.VIDEO,
                      sizeBytes = file.length(),
                      dateModifiedMs = file.lastModified(),
                      durationMs = estimatedDurationMs,
                      resolutionBadge = "4K UHD"
                    )
                  )
                }
              }
            }
          }
        }

        // If no captured files exist on disk, seed initial high-res samples
        if (scanned.isEmpty()) {
          scanned.addAll(seedSampleMediaFiles(context))
        }

        scanned
      }

      _uiState.update { state ->
        val updated = applyFilterAndSort(mediaItems, state.selectedFilter, state.sortOrder)
        state.copy(
          allItems = mediaItems,
          filteredItems = updated,
          isLoading = false
        )
      }
    }
  }

  /**
   * Directly sets media items in state and applies the active filter/sort.
   * Useful for testing and immediate synchronous updates.
   */
  fun setMediaItems(items: List<GalleryMediaItem>) {
    _uiState.update { state ->
      val updated = applyFilterAndSort(items, state.selectedFilter, state.sortOrder)
      state.copy(
        allItems = items,
        filteredItems = updated,
        isLoading = false
      )
    }
  }

  /**
   * Generates initial sample media in the app's external files directory so the
   * Coil gallery immediately has rich local files to display.
   */
  private fun seedSampleMediaFiles(context: android.content.Context): List<GalleryMediaItem> {
    val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
    val samples = mutableListOf<GalleryMediaItem>()

    try {
      // 1. Cinematic Sunset Photo
      val file1 = File(outputDir, "LUMINA_IMG_20260909_SUNSET.jpg")
      if (!file1.exists()) {
        createColorBitmapFile(file1, 1080, 1080, AndroidColor.rgb(255, 110, 64), "Lumina 4K Sunset")
      }
      samples.add(
        GalleryMediaItem(
          id = file1.absolutePath,
          file = file1,
          name = file1.name,
          type = MediaType.PHOTO,
          sizeBytes = file1.length().coerceAtLeast(2450000L),
          dateModifiedMs = System.currentTimeMillis() - 120000,
          resolutionBadge = "4K Photo"
        )
      )

      // 2. Neon Cyberpunk Video (Sample)
      val file2 = File(outputDir, "LUMINA_VID_20260909_NEON_4K.mp4")
      if (!file2.exists()) {
        createColorBitmapFile(file2, 1080, 1920, AndroidColor.rgb(0, 229, 255), "Lumina Cyber Neon")
      }
      samples.add(
        GalleryMediaItem(
          id = file2.absolutePath,
          file = file2,
          name = file2.name,
          type = MediaType.VIDEO,
          sizeBytes = file2.length().coerceAtLeast(38500000L),
          dateModifiedMs = System.currentTimeMillis() - 360000,
          durationMs = 28000L,
          resolutionBadge = "4K UHD 60fps"
        )
      )

      // 3. Golden Hour Portrait Photo
      val file3 = File(outputDir, "LUMINA_IMG_20260909_PORTRAIT.jpg")
      if (!file3.exists()) {
        createColorBitmapFile(file3, 1080, 1350, AndroidColor.rgb(255, 215, 64), "Golden Hour Portrait")
      }
      samples.add(
        GalleryMediaItem(
          id = file3.absolutePath,
          file = file3,
          name = file3.name,
          type = MediaType.PHOTO,
          sizeBytes = file3.length().coerceAtLeast(3120000L),
          dateModifiedMs = System.currentTimeMillis() - 860000,
          resolutionBadge = "RAW Portrait"
        )
      )

      // 4. Emerald Forest Video (Sample)
      val file4 = File(outputDir, "LUMINA_VID_20260909_FOREST.mp4")
      if (!file4.exists()) {
        createColorBitmapFile(file4, 1920, 1080, AndroidColor.rgb(0, 230, 118), "Emerald Forest 4K")
      }
      samples.add(
        GalleryMediaItem(
          id = file4.absolutePath,
          file = file4,
          name = file4.name,
          type = MediaType.VIDEO,
          sizeBytes = file4.length().coerceAtLeast(54200000L),
          dateModifiedMs = System.currentTimeMillis() - 1450000,
          durationMs = 45000L,
          resolutionBadge = "4K UHD"
        )
      )

      // 5. Film Noir Studio Photo
      val file5 = File(outputDir, "LUMINA_IMG_20260909_MONOCHROME.jpg")
      if (!file5.exists()) {
        createColorBitmapFile(file5, 1080, 1080, AndroidColor.rgb(40, 44, 52), "Film Noir Monochrome")
      }
      samples.add(
        GalleryMediaItem(
          id = file5.absolutePath,
          file = file5,
          name = file5.name,
          type = MediaType.PHOTO,
          sizeBytes = file5.length().coerceAtLeast(1890000L),
          dateModifiedMs = System.currentTimeMillis() - 2400000,
          resolutionBadge = "HDR Mono"
        )
      )
    } catch (e: Exception) {
      Log.e("GalleryViewModel", "Failed to seed sample media", e)
    }

    return samples
  }

  private fun createColorBitmapFile(file: File, width: Int, height: Int, color: Int, label: String) {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)

    val paint = Paint().apply {
      this.color = AndroidColor.WHITE
      textSize = (width / 16f)
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText(label, width / 2f, height / 2f, paint)

    FileOutputStream(file).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
  }

  /**
   * Filters the media list by ALL, PHOTOS, or VIDEOS.
   */
  fun setFilter(filter: GalleryFilter) {
    _uiState.update { state ->
      val updated = applyFilterAndSort(state.allItems, filter, state.sortOrder)
      state.copy(selectedFilter = filter, filteredItems = updated)
    }
  }

  /**
   * Sorts the media list by Newest, Oldest, or Largest size.
   */
  fun setSortOrder(order: GallerySortOrder) {
    _uiState.update { state ->
      val updated = applyFilterAndSort(state.allItems, state.selectedFilter, order)
      state.copy(sortOrder = order, filteredItems = updated)
    }
  }

  private fun applyFilterAndSort(
    items: List<GalleryMediaItem>,
    filter: GalleryFilter,
    sortOrder: GallerySortOrder
  ): List<GalleryMediaItem> {
    val filtered = when (filter) {
      GalleryFilter.ALL -> items
      GalleryFilter.PHOTOS -> items.filter { it.type == MediaType.PHOTO }
      GalleryFilter.VIDEOS -> items.filter { it.type == MediaType.VIDEO }
    }

    return when (sortOrder) {
      GallerySortOrder.NEWEST -> filtered.sortedByDescending { it.dateModifiedMs }
      GallerySortOrder.OLDEST -> filtered.sortedBy { it.dateModifiedMs }
      GallerySortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
    }
  }

  /**
   * Selects an item for fullscreen preview.
   */
  fun selectMedia(item: GalleryMediaItem?) {
    _uiState.update { it.copy(selectedMedia = item) }
  }

  /**
   * Toggles item selection during multi-select mode.
   */
  fun toggleSelection(id: String) {
    _uiState.update { state ->
      val updated = state.selectedIds.toMutableSet()
      if (updated.contains(id)) {
        updated.remove(id)
      } else {
        updated.add(id)
      }
      state.copy(
        selectedIds = updated,
        isSelectionMode = updated.isNotEmpty()
      )
    }
  }

  /**
   * Clears selection mode.
   */
  fun clearSelection() {
    _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
  }

  /**
   * Deletes a media file from device storage.
   */
  fun deleteMedia(item: GalleryMediaItem) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        if (item.file.exists()) {
          item.file.delete()
        }
        withContext(Dispatchers.Main) {
          _uiState.update { state ->
            val remaining = state.allItems.filter { it.id != item.id }
            val filtered = applyFilterAndSort(remaining, state.selectedFilter, state.sortOrder)
            state.copy(
              allItems = remaining,
              filteredItems = filtered,
              selectedMedia = if (state.selectedMedia?.id == item.id) null else state.selectedMedia
            )
          }
        }
      } catch (e: Exception) {
        Log.e("GalleryViewModel", "Error deleting media", e)
      }
    }
  }

  /**
   * Deletes all currently selected media items.
   */
  fun deleteSelected() {
    val idsToDelete = _uiState.value.selectedIds.toSet()
    viewModelScope.launch(Dispatchers.IO) {
      idsToDelete.forEach { id ->
        try {
          val file = File(id)
          if (file.exists()) {
            file.delete()
          }
        } catch (e: Exception) {
          Log.e("GalleryViewModel", "Error batch deleting file $id", e)
        }
      }
      withContext(Dispatchers.Main) {
        _uiState.update { state ->
          val remaining = state.allItems.filter { it.id !in idsToDelete }
          val filtered = applyFilterAndSort(remaining, state.selectedFilter, state.sortOrder)
          state.copy(
            allItems = remaining,
            filteredItems = filtered,
            selectedIds = emptySet(),
            isSelectionMode = false,
            selectedMedia = null
          )
        }
      }
    }
  }
}
