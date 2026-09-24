package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {

    fun observeProjects(): Flow<List<Project>>

    fun observeProject(projectId: Int): Flow<Project?>

    fun observeProjectsManagedBy(managerId: Int): Flow<List<Project>>
}
