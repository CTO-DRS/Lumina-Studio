package com.lumina.studio.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumina.studio.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Representation of a captured photo or video loaded from REAL device storage.
 * Duration, resolution and size all come from the actual files — the previous
 * fake fields (name-based duration guessing, "4K UHD" badge on a JPEG, sizes
 * inflated with coerceAtLeast) were removed.
 */
data class GalleryMediaItem(
  val id: String,
  val file: File,
  val name: String,
  val type: MediaType,
  val sizeBytes: Long,
  val dateModifiedMs: Long,
  val durationMs: Long = 0L,
  val width: Int = 0,
  val height: Int = 0
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

  /** REAL resolution badge derived from the measured pixel dimensions. */
  val resolutionBadge: String
    get() = when {
      width <= 0 || height <= 0 -> "غير معروف"
      maxOf(width, height) >= 3840 -> "4K UHD"
      maxOf(width, height) >= 2560 -> "2K QHD"
      maxOf(width, height) >= 1920 -> "1080p FHD"
      maxOf(width, height) >= 1280 -> "720p HD"
      else -> "${width}×${height}"
    }
}

enum class GalleryFilter(val labelAr: String, val labelEn: String) {
  ALL("الكل", "All"),
  PHOTOS("الصور", "Photos"),
  VIDEOS("الفيديوهات", "Videos")
}

enum class GallerySortOrder(val labelAr: String, val labelEn: String) {
  NEWEST("الأحدث أولاً", "Newest"),
  OLDEST("الأقدم أولاً", "Oldest"),
  SIZE_DESC("الأكبر حجماً", "Largest")
}

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
 * ViewModel scanning REAL app media directories. An empty gallery now stays
 * empty and the UI shows an honest empty state — the fake "seed 5 sample
 * files" behaviour (solid-color JPEGs presented as 4K MP4 videos) is gone.
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(GalleryUiState())
  val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

  init {
    loadMediaFromDeviceStorage()
  }

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
                  val dims = measureImageDimensions(file)
                  scanned.add(
                    GalleryMediaItem(
                      id = file.absolutePath,
                      file = file,
                      name = file.name,
                      type = MediaType.PHOTO,
                      sizeBytes = file.length(),
                      dateModifiedMs = file.lastModified(),
                      width = dims.first,
                      height = dims.second
                    )
                  )
                } else if (ext in videoExtensions) {
                  val meta = measureVideoMetadata(file)
                  scanned.add(
                    GalleryMediaItem(
                      id = file.absolutePath,
                      file = file,
                      name = file.name,
                      type = MediaType.VIDEO,
                      sizeBytes = file.length(),
                      dateModifiedMs = file.lastModified(),
                      durationMs = meta.first,
                      width = meta.second,
                      height = meta.third
                    )
                  )
                }
              }
            }
          }
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

  private fun measureImageDimensions(file: File): Pair<Int, Int> {
    return try {
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(file.absolutePath, options)
      Pair(options.outWidth.coerceAtLeast(0), options.outHeight.coerceAtLeast(0))
    } catch (e: Exception) {
      Pair(0, 0)
    }
  }

  /** Real duration + dimensions via the platform media decoder. */
  private fun measureVideoMetadata(file: File): Triple<Long, Int, Int> {
    var retriever: MediaMetadataRetriever? = null
    return try {
      retriever = MediaMetadataRetriever()
      retriever.setDataSource(file.absolutePath)
      val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
      val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
      Triple(duration.coerceAtLeast(0L), width, height)
    } catch (e: Exception) {
      Triple(0L, 0, 0)
    } finally {
      try {
        retriever?.release()
      } catch (_: Exception) {}
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

  fun setFilter(filter: GalleryFilter) {
    _uiState.update { state ->
      val updated = applyFilterAndSort(state.allItems, filter, state.sortOrder)
      state.copy(selectedFilter = filter, filteredItems = updated)
    }
  }

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

  fun selectMedia(item: GalleryMediaItem?) {
    _uiState.update { it.copy(selectedMedia = item) }
  }

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

  fun clearSelection() {
    _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
  }

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
