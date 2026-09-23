package com.example.data.repository

import com.example.data.db.ProjectDao
import com.example.data.model.MediaType
import com.example.data.model.ProjectEntity
import com.example.data.model.VideoResolution
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
  val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

  suspend fun insert(project: ProjectEntity): Long = projectDao.insertProject(project)

  suspend fun update(project: ProjectEntity) = projectDao.updateProject(project)

  suspend fun deleteById(id: Long) = projectDao.deleteProjectById(id)

  suspend fun markSynced(id: Long) = projectDao.markProjectSynced(id, System.currentTimeMillis())

  suspend fun seedInitialProjectsIfNeeded() {
    if (projectDao.getCount() == 0) {
      val defaultProjects = listOf(
        ProjectEntity(
          title = "Cyberpunk Neo Tokyo 4K Master",
          mediaType = MediaType.VIDEO.name,
          assetDrawableName = "img_sample_cinematic",
          durationMs = 45000L,
          resolution = VideoResolution.UHD_4K.name,
          fps = 60,
          isCloudSynced = true,
          cloudSyncTimestamp = System.currentTimeMillis() - 1200000L,
          cloudStorageSizeBytes = 432590000L // ~432 MB
        ),
        ProjectEntity(
          title = "Bioluminescent Ocean Glow 4K",
          mediaType = MediaType.VIDEO.name,
          assetDrawableName = "img_sample_video",
          durationMs = 30000L,
          resolution = VideoResolution.UHD_4K.name,
          fps = 60,
          isCloudSynced = true,
          cloudSyncTimestamp = System.currentTimeMillis() - 7200000L,
          cloudStorageSizeBytes = 312000000L // ~312 MB
        ),
        ProjectEntity(
          title = "Golden Hour Cinematic Portrait",
          mediaType = MediaType.PHOTO.name,
          assetDrawableName = "img_sample_cinematic",
          durationMs = 0L,
          resolution = VideoResolution.UHD_4K.name,
          fps = 0,
          isCloudSynced = false,
          cloudSyncTimestamp = 0L,
          cloudStorageSizeBytes = 48200000L // ~48 MB
        )
      )
      for (p in defaultProjects) {
        projectDao.insertProject(p)
      }
    }
  }
}
