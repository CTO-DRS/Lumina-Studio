package com.example.command

import com.example.data.model.AdjustmentsState
import com.example.data.model.HistoryEntry
import java.util.UUID

/**
 * Categorization for edit operations recorded in the Command Pattern.
 */
enum class CommandCategory(
  val labelAr: String,
  val labelEn: String,
  val iconName: String,
  val colorHex: Long
) {
  ADJUSTMENTS("تعديل ألوان", "Color Adjustments", "tune", 0xFFFFD166),
  TRIM("قص وتشذيب", "Trim & Cut", "content_cut", 0xFFFFD166),
  SPLIT_CLIP("تقسيم مقطع", "Split Clip", "content_cut", 0xFF00F0FF),
  RIPPLE_DELETE("حذف وحزم", "Ripple Delete", "delete_sweep", 0xFFFF4D6D),
  EXTRACT_SELECTION("استخلاص مدى", "Extract Selection", "crop", 0xFF00E676),
  FILTER("فلتر سينمائي", "Cinematic Filter", "filter", 0xFFA855F7),
  CROP("أبعاد وقص", "Crop Aspect", "crop", 0xFF00E676),
  AUDIO("مكساج صوت", "Audio Mix", "graphic_eq", 0xFF10B981),
  SPEED_RAMP("منحنى السرعة", "Speed Ramp", "speed", 0xFFFF7A00),
  RESET("إعادة ضبط", "Reset All", "restart_alt", 0xFF94A3B8),
  COMPOSITE("عملية مركبة", "Composite Action", "layers", 0xFF00F0FF)
}

/**
 * Encapsulates an edit operation according to the GoF Command Pattern.
 * Provides reversible execution, coalescing/merging for high-frequency input (sliders),
 * and state serialization to HistoryEntry.
 */
interface EditCommand {
  val id: String
  val descriptionAr: String
  val descriptionEn: String
  val category: CommandCategory
  val timestampMs: Long

  /**
   * Executes the edit operation and updates application state.
   */
  fun execute()

  /**
   * Reverses the edit operation, restoring previous state.
   */
  fun undo()

  /**
   * Re-applies the edit operation after an undo.
   */
  fun redo() {
    execute()
  }

  /**
   * Determines if this command can be merged with an incoming rapid command
   * (e.g. dragging an exposure slider produces 60 events/sec; merging groups them).
   */
  fun canMergeWith(other: EditCommand): Boolean = false

  /**
   * Merges an incoming command into this command, preserving the earliest
   * undo baseline while adopting the latest redo target.
   */
  fun mergeWith(other: EditCommand): EditCommand = this

  /**
   * Bridges to the existing HistoryEntry data structure for legacy UI compatibility.
   */
  fun toHistoryEntry(snapshotAdjustments: AdjustmentsState): HistoryEntry
}
