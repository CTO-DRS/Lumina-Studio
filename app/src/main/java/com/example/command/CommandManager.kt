package com.example.command

import com.example.data.model.AdjustmentsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central Command Manager implementing the Command Pattern for undo/redo state management.
 * Provides reversible action tracking, command coalescing for high-frequency user interactions,
 * atomic batch transactions, and branch time-travel navigation.
 */
class CommandManager(
  private val maxHistoryCapacity: Int = 60
) {
  private val _undoStack = MutableStateFlow<List<EditCommand>>(emptyList())
  val undoStack: StateFlow<List<EditCommand>> = _undoStack.asStateFlow()

  private val _redoStack = MutableStateFlow<List<EditCommand>>(emptyList())
  val redoStack: StateFlow<List<EditCommand>> = _redoStack.asStateFlow()

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

  // Batch transaction state
  private var isBatching = false
  private val activeBatch = mutableListOf<EditCommand>()

  /**
   * Begins an atomic batch transaction. Subsequent commands will be accumulated
   * until [endBatch] is called, forming a single reversible composite command.
   */
  fun beginBatch() {
    isBatching = true
    activeBatch.clear()
  }

  /**
   * Concludes an atomic batch transaction, committing all accumulated commands.
   */
  fun endBatch(
    descriptionAr: String,
    descriptionEn: String,
    snapshotAdjustments: AdjustmentsState
  ): CompositeCommand? {
    isBatching = false
    if (activeBatch.isEmpty()) return null

    val composite = CompositeCommand(
      commands = activeBatch.toList(),
      descriptionAr = descriptionAr,
      descriptionEn = descriptionEn,
      snapshotAdjustments = snapshotAdjustments
    )
    activeBatch.clear()
    pushToUndoStack(composite)
    return composite
  }

  /**
   * Cancels any active batch transaction without recording to history.
   */
  fun cancelBatch() {
    isBatching = false
    activeBatch.clear()
  }

  /**
   * Dispatches and records an EditCommand.
   * If [autoExecute] is true, executes the command immediately.
   */
  fun executeCommand(command: EditCommand, autoExecute: Boolean = true) {
    if (autoExecute) {
      command.execute()
    }

    if (isBatching) {
      activeBatch.add(command)
      return
    }

    pushToUndoStack(command)
  }

  private fun pushToUndoStack(command: EditCommand) {
    val currentUndo = _undoStack.value.toMutableList()

    // Check for command coalescing (e.g. rapid slider dragging)
    if (currentUndo.isNotEmpty() && currentUndo.last().canMergeWith(command)) {
      val top = currentUndo.removeAt(currentUndo.lastIndex)
      val merged = top.mergeWith(command)
      currentUndo.add(merged)
    } else {
      if (currentUndo.size >= maxHistoryCapacity) {
        currentUndo.removeAt(0)
      }
      currentUndo.add(command)
      // New distinct action invalidates future redo branch
      _redoStack.value = emptyList()
    }

    _undoStack.value = currentUndo
    updateStateFlags()
  }

  /**
   * Undoes the most recent command, reversing its effect and pushing it to the redo stack.
   */
  fun undo(): Boolean {
    val undoList = _undoStack.value.toMutableList()
    if (undoList.isEmpty()) return false

    val commandToUndo = undoList.removeAt(undoList.lastIndex)
    commandToUndo.undo()

    val redoList = _redoStack.value.toMutableList()
    redoList.add(commandToUndo)

    _undoStack.value = undoList
    _redoStack.value = redoList
    updateStateFlags()
    return true
  }

  /**
   * Redoes the most recently undone command, re-applying its effect and pushing to the undo stack.
   */
  fun redo(): Boolean {
    val redoList = _redoStack.value.toMutableList()
    if (redoList.isEmpty()) return false

    val commandToRedo = redoList.removeAt(redoList.lastIndex)
    commandToRedo.redo()

    val undoList = _undoStack.value.toMutableList()
    undoList.add(commandToRedo)

    _undoStack.value = undoList
    _redoStack.value = redoList
    updateStateFlags()
    return true
  }

  /**
   * Time-travel jump to a specific command step in the undo or redo history.
   * If [targetCommandId] is in the undo stack, undoes back to and including that command.
   * If in the redo stack, redoes forward up to and including that command.
   */
  fun jumpToCommand(targetCommandId: String): Boolean {
    val undoList = _undoStack.value.toMutableList()
    val undoIndex = undoList.indexOfFirst { it.id == targetCommandId }

    if (undoIndex >= 0) {
      val redoList = _redoStack.value.toMutableList()
      // Undo all commands down to and including targetIndex
      while (undoList.size > undoIndex) {
        val cmd = undoList.removeAt(undoList.lastIndex)
        cmd.undo()
        redoList.add(cmd)
      }
      _undoStack.value = undoList
      _redoStack.value = redoList
      updateStateFlags()
      return true
    }

    val redoList = _redoStack.value.toMutableList()
    val redoIndex = redoList.indexOfFirst { it.id == targetCommandId }
    if (redoIndex >= 0) {
      // Redo all commands up to and including redoIndex
      while (redoList.isNotEmpty()) {
        val cmd = redoList.removeAt(redoList.lastIndex)
        cmd.redo()
        undoList.add(cmd)
        if (cmd.id == targetCommandId) break
      }
      _undoStack.value = undoList
      _redoStack.value = redoList
      updateStateFlags()
      return true
    }

    return false
  }

  /**
   * Resets both undo and redo stacks.
   */
  fun clear() {
    _undoStack.value = emptyList()
    _redoStack.value = emptyList()
    updateStateFlags()
  }

  private fun updateStateFlags() {
    val undoList = _undoStack.value
    val redoList = _redoStack.value

    _canUndo.value = undoList.isNotEmpty()
    _canRedo.value = redoList.isNotEmpty()
    _undoCount.value = undoList.size
    _redoCount.value = redoList.size
    _lastActionDescription.value = undoList.lastOrNull()?.descriptionAr ?: "الحالة الأصلية"
  }
}
