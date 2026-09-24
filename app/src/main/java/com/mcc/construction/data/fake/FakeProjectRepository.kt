package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Project
import com.mcc.construction.data.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeProjectRepository(seed: FakeSeedData) : ProjectRepository {

    private val projects = MutableStateFlow(seed.projects)

    override fun observeProjects(): Flow<List<Project>> = projects

    override fun observeProject(projectId: Int): Flow<Project?> =
        projects.map { list -> list.firstOrNull { it.projectId == projectId } }

    override fun observeProjectsManagedBy(managerId: Int): Flow<List<Project>> =
        projects.map { list -> list.filter { it.managerId == managerId } }

    internal fun findProject(projectId: Int): Project? =
        projects.value.firstOrNull { it.projectId == projectId }
}
