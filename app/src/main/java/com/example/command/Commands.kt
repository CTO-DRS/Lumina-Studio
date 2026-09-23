package com.example.command

import com.example.data.model.AdjustmentsState
import com.example.data.model.CropAspect
import com.example.data.model.FilterPreset
import com.example.data.model.HistoryEntry
import com.example.data.model.TimelineClip
import java.util.UUID

/**
 * Command for color grading, tone curve, and lighting adjustments.
 * Supports debounced coalescing when a user drags a slider continuously.
 */
class AdjustmentsCommand(
  override val id: String = UUID.randomUUID().toString(),
  val previousState: AdjustmentsState,
  val newState: AdjustmentsState,
  val propertyKey: String?,
  override val descriptionAr: String,
  override val descriptionEn: String,
  override val timestampMs: Long = System.currentTimeMillis(),
  private val onApply: (AdjustmentsState) -> Unit
) : EditCommand {

  override val category: CommandCategory = CommandCategory.ADJUSTMENTS

  override fun execute() {
    onApply(newState)
  }

  override fun undo() {
    onApply(previousState)
  }

  override fun canMergeWith(other: EditCommand): Boolean {
    if (other !is AdjustmentsCommand) return false
    val sameProperty = propertyKey != null && propertyKey == other.propertyKey
    val withinTimeWindow = (other.timestampMs - timestampMs) < 650L
    return sameProperty && withinTimeWindow
  }

  override fun mergeWith(other: EditCommand): EditCommand {
    val incoming = other as AdjustmentsCommand
    return AdjustmentsCommand(
      id = this.id,
      previousState = this.previousState, // Keep the original starting state
      newState = incoming.newState,       // Adopt the newest target state
      propertyKey = incoming.propertyKey,
      descriptionAr = incoming.descriptionAr,
      descriptionEn = incoming.descriptionEn,
      timestampMs = incoming.timestampMs,
      onApply = this.onApply
    )
  }

  override fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry {
    return HistoryEntry(
      id = id,
      adjustments = previousState,
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      timestampMs = timestampMs
    )
  }
}

/**
 * Command for In/Out trimming bounds on video/audio timeline.
 */
class TrimRangeCommand(
  override val id: String = UUID.randomUUID().toString(),
  val oldStartMs: Long,
  val oldEndMs: Long,
  val newStartMs: Long,
  val newEndMs: Long,
  override val descriptionAr: String = "تعديل مدى القص In/Out",
  override val descriptionEn: String = "Trim In/Out Range",
  override val timestampMs: Long = System.currentTimeMillis(),
  private val snapshotAdjustments: AdjustmentsState,
  private val onApply: (startMs: Long, endMs: Long) -> Unit
) : EditCommand {

  override val category: CommandCategory = CommandCategory.TRIM

  override fun execute() {
    onApply(newStartMs, newEndMs)
  }

  override fun undo() {
    onApply(oldStartMs, oldEndMs)
  }

  override fun canMergeWith(other: EditCommand): Boolean {
    if (other !is TrimRangeCommand) return false
    return (other.timestampMs - timestampMs) < 500L
  }

  override fun mergeWith(other: EditCommand): EditCommand {
    val incoming = other as TrimRangeCommand
    return TrimRangeCommand(
      id = this.id,
      oldStartMs = this.oldStartMs,
      oldEndMs = this.oldEndMs,
      newStartMs = incoming.newStartMs,
      newEndMs = incoming.newEndMs,
      descriptionAr = incoming.descriptionAr,
      descriptionEn = incoming.descriptionEn,
      timestampMs = incoming.timestampMs,
      snapshotAdjustments = this.snapshotAdjustments,
      onApply = this.onApply
    )
  }

  override fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry {
    return HistoryEntry(
      id = id,
      adjustments = this.snapshotAdjustments,
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      timestampMs = timestampMs
    )
  }
}

/**
 * Command for splitting a timeline clip at the playhead position.
 */
class SplitClipCommand(
  override val id: String = UUID.randomUUID().toString(),
  val splitPlayheadMs: Long,
  val clipsBefore: List<TimelineClip>,
  val clipsAfter: List<TimelineClip>,
  override val descriptionAr: String = "قص المقطع عند ${splitPlayheadMs / 1000f} ثانية",
  override val descriptionEn: String = "Split Clip at ${splitPlayheadMs / 1000f}s",
  override val timestampMs: Long = System.currentTimeMillis(),
  private val snapshotAdjustments: AdjustmentsState,
  private val onApply: (List<TimelineClip>) -> Unit
) : EditCommand {

  override val category: CommandCategory = CommandCategory.SPLIT_CLIP

  override fun execute() {
    onApply(clipsAfter)
  }

  override fun undo() {
    onApply(clipsBefore)
  }

  override fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry {
    return HistoryEntry(
      id = id,
      adjustments = this.snapshotAdjustments,
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      timestampMs = timestampMs
    )
  }
}

/**
 * Command for ripple-deleting a segment across timeline tracks.
 */
class RippleDeleteCommand(
  override val id: String = UUID.randomUUID().toString(),
  val startMs: Long,
  val endMs: Long,
  val clipsBefore: List<TimelineClip>,
  val clipsAfter: List<TimelineClip>,
  val durationBefore: Long,
  val durationAfter: Long,
  override val descriptionAr: String = "حذف وحزم المدى (${(endMs - startMs) / 1000f} ثانية)",
  override val descriptionEn: String = "Ripple Delete Range (${(endMs - startMs) / 1000f}s)",
  override val timestampMs: Long = System.currentTimeMillis(),
  private val snapshotAdjustments: AdjustmentsState,
  private val onApply: (List<TimelineClip>, Long) -> Unit
) : EditCommand {

  override val category: CommandCategory = CommandCategory.RIPPLE_DELETE

  override fun execute() {
    onApply(clipsAfter, durationAfter)
  }

  override fun undo() {
    onApply(clipsBefore, durationBefore)
  }

  override fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry {
    return HistoryEntry(
      id = id,
      adjustments = this.snapshotAdjustments,
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      timestampMs = timestampMs
    )
  }
}

/**
 * Command for extracting a selected range across all tracks.
 */
class ExtractSelectionCommand(
  override val id: String = UUID.randomUUID().toString(),
  val startMs: Long,
  val endMs: Long,
  val clipsBefore: List<TimelineClip>,
  val clipsAfter: List<TimelineClip>,
  val durationBefore: Long,
  val durationAfter: Long,
  override val descriptionAr: String = "استخلاص المدى المحدد (${(endMs - startMs) / 1000f} ثانية)",
  override val descriptionEn: String = "Extract Selected Range (${(endMs - startMs) / 1000f}s)",
  override val timestampMs: Long = System.currentTimeMillis(),
  private val snapshotAdjustments: AdjustmentsState,
  private val onApply: (List<TimelineClip>, Long) -> Unit
) : EditCommand {

  override val category: CommandCategory = CommandCategory.EXTRACT_SELECTION

  override fun execute() {
    onApply(clipsAfter, durationAfter)
  }

  override fun undo() {
    onApply(clipsBefore, durationBefore)
  }

  override fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry {
    return HistoryEntry(
      id = id,
      adjustments = this.snapshotAdjustments,
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      timestampMs = timestampMs
    )
  }
}

/**
 * Command for applying cinematic color LUT filter presets.
 */
class FilterPresetCommand(
  override val id: String = UUID.randomUUID().toString(),
  val oldPreset: FilterPreset,
  val oldIntensity: Float,
  val newPreset: FilterPreset,
  val newIntensity: Float,
  override val descriptionAr: String = "تطبيق فلتر ${newPreset.labelAr}",
  override val descriptionEn: String = "Apply Filter ${newPreset.labelEn}",
  override val timestampMs: Long = System.currentTimeMillis(),
  private val snapshotAdjustments: AdjustmentsState,
  private val onApply: (FilterPreset, Float) -> Unit
) : EditCommand {

  override val category: CommandCategory = CommandCategory.FILTER

  override fun execute() {
    onApply(newPreset, newIntensity)
  }

  override fun undo() {
    onApply(oldPreset, oldIntensity)
  }

  override fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry {
    return HistoryEntry(
      id = id,
      adjustments = this.snapshotAdjustments,
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      timestampMs = timestampMs
    )
  }
}

/**
 * Command for changing aspect ratio and crop boundaries.
 */
class CropAspectCommand(
  override val id: String = UUID.randomUUID().toString(),
  val oldCrop: CropAspect,
  val newCrop: CropAspect,
  override val descriptionAr: String = "تغيير الأبعاد إلى ${newCrop.labelAr}",
  override val descriptionEn: String = "Change Crop to ${newCrop.labelEn}",
  override val timestampMs: Long = System.currentTimeMillis(),
  private val snapshotAdjustments: AdjustmentsState,
  private val onApply: (CropAspect) -> Unit
) : EditCommand {

  override val category: CommandCategory = CommandCategory.CROP

  override fun execute() {
    onApply(newCrop)
  }

  override fun undo() {
    onApply(oldCrop)
  }

  override fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry {
    return HistoryEntry(
      id = id,
      adjustments = this.snapshotAdjustments,
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      timestampMs = timestampMs
    )
  }
}

/**
 * Composite Command grouping multiple sub-commands into an atomic undo/redo transaction.
 */
class CompositeCommand(
  override val id: String = UUID.randomUUID().toString(),
  val commands: List<EditCommand>,
  override val descriptionAr: String,
  override val descriptionEn: String,
  override val category: CommandCategory = CommandCategory.COMPOSITE,
  override val timestampMs: Long = System.currentTimeMillis(),
  private val snapshotAdjustments: AdjustmentsState
) : EditCommand {

  override fun execute() {
    for (cmd in commands) {
      cmd.execute()
    }
  }

  override fun undo() {
    for (cmd in commands.asReversed()) {
      cmd.undo()
    }
  }

  override fun redo() {
    for (cmd in commands) {
      cmd.redo()
    }
  }

  override fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry {
    return HistoryEntry(
      id = id,
      adjustments = this.snapshotAdjustments,
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      timestampMs = timestampMs
    )
  }
}
