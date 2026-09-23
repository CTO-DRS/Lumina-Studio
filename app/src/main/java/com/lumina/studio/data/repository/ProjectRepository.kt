package com.lumina.studio.data.repository

import com.lumina.studio.data.db.ProjectDao
import com.lumina.studio.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Real project repository backed by the on-device Room database.
 *
 * NOTE: the previous fake `seedInitialProjectsIfNeeded()` that injected three
 * demo projects ("Cyberpunk Neo Tokyo 4K Master", ...) was removed on purpose.
 * A fresh install now honestly starts with ZERO projects and the UI shows a
 * real empty state until the user imports or captures actual media.
 */
class ProjectRepository(private val projectDao: ProjectDao) {

  val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

  suspend fun getById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

  suspend fun insert(project: ProjectEntity): Long = projectDao.insertProject(project)

  suspend fun update(project: ProjectEntity) = projectDao.updateProject(project)

  suspend fun deleteById(id: Long) = projectDao.deleteProjectById(id)
}
