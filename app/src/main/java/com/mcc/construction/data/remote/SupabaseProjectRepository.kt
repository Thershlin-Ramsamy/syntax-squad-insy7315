package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.remote.dto.ProjectRow
import com.mcc.construction.data.repository.ProjectRepository
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupabaseProjectRepository(
    private val reads: BackendReadStore
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> {
        return reads.observeRows<ProjectRow>(
            table = "projects",
            primaryKey = "project_id"
        ).map { rows ->
            rows.map { row ->
                mapProject(row)
            }.sortedBy {
                it.projectId
            }
        }
    }

    override fun observeProject(
        projectId: Int
    ): Flow<Project?> {
        return observeProjects().map { projects ->
            projects.firstOrNull {
                it.projectId == projectId
            }
        }
    }

    override fun observeProjectsManagedBy(
        managerId: Int
    ): Flow<List<Project>> {
        return observeProjects().map { projects ->
            projects.filter {
                it.managerId == managerId
            }
        }
    }

    private fun mapProject(
        row: ProjectRow
    ): Project {
        return Project(
            projectId = row.projectId,
            managerId = row.managerId,
            projectName = row.projectName,
            location = row.location,
            budget = BigDecimal(row.budget.content),
            startDate = LocalDate.parse(row.startDate),
            endDate = LocalDate.parse(row.endDate),
            status = ProjectStatus.valueOf(row.status),
            progressPercent = row.progressPercent.content.toDouble()
        )
    }
}