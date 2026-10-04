package com.mcc.construction.data.remote.dto

import com.mcc.construction.data.model.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.serialization.json.JsonPrimitive

object BackendDates {
    val southAfrica: ZoneId =
        ZoneId.of("Africa/Johannesburg")

    fun date(value: String): LocalDate {
        return LocalDate.parse(value)
    }

    fun localDateTime(value: String): LocalDateTime {
        return OffsetDateTime.parse(value)
            .atZoneSameInstant(southAfrica)
            .toLocalDateTime()
    }
}

private fun JsonPrimitive.money(): BigDecimal {
    return BigDecimal(content)
}

fun UserRow.toModel(): User {
    val resolvedRole = requireNotNull(
        UserRole.fromRoleId(roleId)
    ) {
        "Unsupported backend role ID."
    }

    return User(
        userId = userId,
        role = resolvedRole,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        jobTitle = jobTitle,
        employeeNumber = employeeNumber,
        isActive = isActive
    )
}

fun RoleRow.toModel(): Role {
    return Role(
        roleId = roleId,
        roleName = roleName,
        description = description
    )
}

fun ProjectRow.toModel(): Project {
    return Project(
        projectId = projectId,
        managerId = managerId,
        projectName = projectName,
        location = location,
        budget = budget.money(),
        startDate = BackendDates.date(startDate),
        endDate = BackendDates.date(endDate),
        status = ProjectStatus.valueOf(status),
        progressPercent = progressPercent.content.toDouble()
    )
}

fun TaskRow.toModel(): Task {
    return Task(
        taskId = taskId,
        projectId = projectId,
        title = title,
        description = description,
        deadline = BackendDates.date(deadline),
        priority = TaskPriority.valueOf(priority),
        status = TaskStatus.valueOf(status)
    )
}

fun TaskAssignmentRow.toModel(): TaskAssignment {
    return TaskAssignment(
        assignmentId = assignmentId,
        userId = userId,
        taskId = taskId,
        assignedDate =
            BackendDates.localDateTime(assignedDate)
    )
}

/**
 * A signed URL must be generated from storagePath before mapping a photo
 * for display. The permanent storage path is retained in the DTO/cache.
 */
fun TaskPhotoRow.toModel(displayUrl: String): TaskPhoto {
    return TaskPhoto(
        photoId = photoId,
        taskId = taskId,
        userId = userId,
        uri = displayUrl,
        addedAt = BackendDates.localDateTime(addedAt)
    )
}

fun AttendanceRow.toModel(): Attendance {
    return Attendance(
        attendanceId = attendanceId,
        userId = userId,
        date = BackendDates.date(date),
        clockIn = BackendDates.localDateTime(clockIn),
        clockOut = clockOut?.let {
            BackendDates.localDateTime(it)
        },
        totalHours = totalHours?.content?.toDouble()
    )
}

/**
 * URLs are resolved by the Storage repository.
 * Do not pass permanent Storage paths directly to an image component.
 */
fun DailyReportRow.toModel(
    displayPhotoUrls: List<String>
): DailyReport {
    require(displayPhotoUrls.size == photoPaths.size) {
        "Each report photograph requires a display URL."
    }

    return DailyReport(
        reportId = reportId,
        projectId = projectId,
        userId = userId,
        date = BackendDates.date(date),
        weather = weather,
        workCompleted = workCompleted,
        problemsEncountered = problemsEncountered,
        status = ReportStatus.valueOf(status),
        photoUris = displayPhotoUrls
    )
}

fun SupplierRow.toModel(): Supplier {
    return Supplier(
        supplierId = supplierId,
        companyName = companyName,
        contactPerson = contactPerson,
        phone = phone,
        email = email
    )
}

fun InventoryRow.toModel(): Inventory {
    return Inventory(
        materialId = materialId,
        projectId = projectId,
        supplierId = supplierId,
        materialName = materialName,
        quantityAvailable = quantityAvailable,
        minimumQuantity = minimumQuantity,
        unitType = unitType,
        unitCost = unitCost.money()
    )
}

fun DocumentRow.toModel(): Document {
    return Document(
        documentId = documentId,
        projectId = projectId,
        fileName = fileName,
        version = version,
        uploadDate =
            BackendDates.localDateTime(uploadDate),
        category = DocumentCategory.valueOf(category)
    )
}

fun SafetyChecklistRow.toModel(): SafetyChecklistItem {
    return SafetyChecklistItem(
        itemId = itemId,
        title = title,
        detail = detail
    )
}

fun SafetyInspectionRow.toModel(): SafetyInspection {
    return SafetyInspection(
        inspectionId = inspectionId,
        projectId = projectId,
        inspectorId = inspectorId,
        date = BackendDates.date(date),
        comments = comments,
        passed = passed
    )
}

fun IncidentRow.toModel(
    displayPhotoUrls: List<String>
): IncidentReport {
    require(displayPhotoUrls.size == photoPaths.size) {
        "Each incident photograph requires a display URL."
    }

    return IncidentReport(
        incidentId = incidentId,
        inspectionId = inspectionId,
        severityLevel = severityLevel,
        description = description,
        dateReported =
            BackendDates.localDateTime(dateReported),
        photoUris = displayPhotoUrls
    )
}

fun NotificationRow.toModel(): Notification {
    return Notification(
        notificationId = notificationId,
        userId = userId,
        type = NotificationType.valueOf(type),
        messageContent = messageContent,
        sentDate = BackendDates.localDateTime(sentDate),
        isRead = isRead
    )
}

fun MessageRow.toModel(): Message {
    return Message(
        messageId = messageId,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        timestamp =
            BackendDates.localDateTime(timestamp),
        isRead = isRead
    )
}

fun AuditLogRow.toModel(): AuditLog {
    return AuditLog(
        logId = logId,
        userId = userId,
        action = action,
        timestamp =
            BackendDates.localDateTime(timestamp)
    )
}

fun SystemSettingsRow.toModel(): SystemSettings {
    return SystemSettings(
        sessionTimeoutMinutes = sessionTimeoutMinutes,
        requireDailyChecklist = requireDailyChecklist,
        allowOfflineMode = allowOfflineMode,
        lowStockAlerts = lowStockAlerts,
        syncIntervalMinutes = syncIntervalMinutes
    )
}

fun OfflineSyncRow.toModel(): OfflineSync {
    return OfflineSync(
        syncId = syncId,
        userId = userId,
        deviceId = deviceId,
        syncStatus = SyncOutcome.valueOf(syncStatus),
        timestamp =
            BackendDates.localDateTime(timestamp)
    )
}