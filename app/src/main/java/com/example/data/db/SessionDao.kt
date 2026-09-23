package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EditingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

  @Query("SELECT * FROM editing_sessions WHERE projectId = :projectId")
  fun getSessionFlow(projectId: Long): Flow<EditingSessionEntity?>

  @Query("SELECT * FROM editing_sessions WHERE projectId = :projectId")
  suspend fun getSessionById(projectId: Long): EditingSessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveSession(session: EditingSessionEntity)

  @Update
  suspend fun updateSession(session: EditingSessionEntity)

  @Query("DELETE FROM editing_sessions WHERE projectId = :projectId")
  suspend fun deleteSession(projectId: Long)

  @Query("SELECT * FROM editing_sessions ORDER BY lastSavedMs DESC LIMIT 1")
  fun getLatestSessionFlow(): Flow<EditingSessionEntity?>

  @Query("SELECT * FROM editing_sessions ORDER BY lastSavedMs DESC LIMIT 1")
  suspend fun getLatestSession(): EditingSessionEntity?

  @Query("SELECT COUNT(*) FROM editing_sessions")
  suspend fun getCount(): Int
}
