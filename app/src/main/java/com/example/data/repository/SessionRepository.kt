package com.example.data.repository

import com.example.data.db.SessionDao
import com.example.data.model.EditingSessionEntity
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {

  fun getSessionFlow(projectId: Long): Flow<EditingSessionEntity?> = sessionDao.getSessionFlow(projectId)

  suspend fun getSession(projectId: Long): EditingSessionEntity? = sessionDao.getSessionById(projectId)

  suspend fun saveSession(session: EditingSessionEntity) = sessionDao.saveSession(session)

  suspend fun updateSession(session: EditingSessionEntity) = sessionDao.updateSession(session)

  suspend fun deleteSession(projectId: Long) = sessionDao.deleteSession(projectId)

  val latestSessionFlow: Flow<EditingSessionEntity?> = sessionDao.getLatestSessionFlow()

  suspend fun getLatestSession(): EditingSessionEntity? = sessionDao.getLatestSession()

  suspend fun getCount(): Int = sessionDao.getCount()
}
