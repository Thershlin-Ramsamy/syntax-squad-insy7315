package com.mcc.construction.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class UserRow(
    @SerialName("user_id")
    val userId: Int,

    @SerialName("auth_user_id")
    val authUserId: String,

    @SerialName("role_id")
    val roleId: Int,

    @SerialName("first_name")
    val firstName: String,

    @SerialName("last_name")
    val lastName: String,

    val email: String,
    val phone: String,

    @SerialName("job_title")
    val jobTitle: String,

    @SerialName("employee_number")
    val employeeNumber: String,

    @SerialName("is_active")
    val isActive: Boolean
)

@Serializable
data class RoleRow(
    @SerialName("role_id")
    val roleId: Int,

    @SerialName("role_name")
    val roleName: String,

    val description: String
)

@Serializable
data class ProjectRow(
    @SerialName("project_id")
    val projectId: Int,

    @SerialName("manager_id")
    val managerId: Int,

    @SerialName("project_name")
    val projectName: String,

    val location: String,
    val budget: JsonPrimitive,

    @SerialName("start_date")
    val startDate: String,

    @SerialName("end_date")
    val endDate: String,

    val status: String,

    @SerialName("progress_percent")
    val progressPercent: JsonPrimitive
)

@Serializable
data class TaskRow(
    @SerialName("task_id")
    val taskId: Int,

    @SerialName("project_id")
    val projectId: Int,

    val title: String,
    val description: String,
    val deadline: String,
    val priority: String,
    val status: String,
    val revision: Int
)

@Serializable
data class TaskAssignmentRow(
    @SerialName("assignment_id")
    val assignmentId: Int,

    @SerialName("user_id")
    val userId: Int,

    @SerialName("task_id")
    val taskId: Int,

    @SerialName("assigned_date")
    val assignedDate: String
)

@Serializable
data class TaskPhotoRow(
    @SerialName("photo_id")
    val photoId: Int,

    @SerialName("task_id")
    val taskId: Int,

    @SerialName("user_id")
    val userId: Int,

    @SerialName("storage_path")
    val storagePath: String,

    @SerialName("added_at")
    val addedAt: String,

    @SerialName("is_removed")
    val isRemoved: Boolean
)

@Serializable
data class AttendanceRow(
    @SerialName("attendance_id")
    val attendanceId: Int,

    @SerialName("user_id")
    val userId: Int,

    val date: String,

    @SerialName("clock_in")
    val clockIn: String,

    @SerialName("clock_out")
    val clockOut: String?,

    @SerialName("total_hours")
    val totalHours: JsonPrimitive?
)

@Serializable
data class DailyReportRow(
    @SerialName("report_id")
    val reportId: Int,

    @SerialName("project_id")
    val projectId: Int,

    @SerialName("user_id")
    val userId: Int,

    val date: String,
    val weather: String,

    @SerialName("work_completed")
    val workCompleted: String,

    @SerialName("problems_encountered")
    val problemsEncountered: String,

    val status: String,

    @SerialName("photo_paths")
    val photoPaths: List<String>
)

@Serializable
data class SupplierRow(
    @SerialName("supplier_id")
    val supplierId: Int,

    @SerialName("company_name")
    val companyName: String,

    @SerialName("contact_person")
    val contactPerson: String,

    val phone: String,
    val email: String
)

@Serializable
data class InventoryRow(
    @SerialName("material_id")
    val materialId: Int,

    @SerialName("project_id")
    val projectId: Int,

    @SerialName("supplier_id")
    val supplierId: Int,

    @SerialName("material_name")
    val materialName: String,

    @SerialName("quantity_available")
    val quantityAvailable: Int,

    @SerialName("minimum_quantity")
    val minimumQuantity: Int,

    @SerialName("unit_type")
    val unitType: String,

    @SerialName("unit_cost")
    val unitCost: JsonPrimitive
)

@Serializable
data class DocumentRow(
    @SerialName("document_id")
    val documentId: Int,

    @SerialName("project_id")
    val projectId: Int,

    @SerialName("file_name")
    val fileName: String,

    val version: Int,

    @SerialName("upload_date")
    val uploadDate: String,

    val category: String,

    @SerialName("storage_path")
    val storagePath: String
)

@Serializable
data class SafetyChecklistRow(
    @SerialName("item_id")
    val itemId: Int,

    val title: String,
    val detail: String,

    @SerialName("sort_order")
    val sortOrder: Int,

    @SerialName("is_active")
    val isActive: Boolean
)

@Serializable
data class SafetyInspectionRow(
    @SerialName("inspection_id")
    val inspectionId: Int,

    @SerialName("project_id")
    val projectId: Int,

    @SerialName("inspector_id")
    val inspectorId: Int,

    val date: String,
    val comments: String,
    val passed: Boolean
)

@Serializable
data class IncidentRow(
    @SerialName("incident_id")
    val incidentId: Int,

    @SerialName("inspection_id")
    val inspectionId: Int,

    @SerialName("severity_level")
    val severityLevel: Int,

    val description: String,

    @SerialName("date_reported")
    val dateReported: String,

    @SerialName("photo_paths")
    val photoPaths: List<String>
)

@Serializable
data class NotificationRow(
    @SerialName("notification_id")
    val notificationId: Int,

    @SerialName("user_id")
    val userId: Int,

    val type: String,

    @SerialName("message_content")
    val messageContent: String,

    @SerialName("sent_date")
    val sentDate: String,

    @SerialName("is_read")
    val isRead: Boolean
)

@Serializable
data class MessageRow(
    @SerialName("message_id")
    val messageId: Int,

    @SerialName("sender_id")
    val senderId: Int,

    @SerialName("receiver_id")
    val receiverId: Int,

    val content: String,
    val timestamp: String,

    @SerialName("is_read")
    val isRead: Boolean
)

@Serializable
data class AuditLogRow(
    @SerialName("log_id")
    val logId: Int,

    @SerialName("user_id")
    val userId: Int,

    val action: String,
    val timestamp: String
)

@Serializable
data class SystemSettingsRow(
    @SerialName("session_timeout_minutes")
    val sessionTimeoutMinutes: Int,

    @SerialName("require_daily_checklist")
    val requireDailyChecklist: Boolean,

    @SerialName("allow_offline_mode")
    val allowOfflineMode: Boolean,

    @SerialName("low_stock_alerts")
    val lowStockAlerts: Boolean,

    @SerialName("sync_interval_minutes")
    val syncIntervalMinutes: Int
)

@Serializable
data class OfflineSyncRow(
    @SerialName("sync_id")
    val syncId: Int,

    @SerialName("user_id")
    val userId: Int,

    @SerialName("device_id")
    val deviceId: String,

    @SerialName("sync_status")
    val syncStatus: String,

    val timestamp: String
)

@Serializable
data class FileUploadRow(
    @SerialName("upload_id")
    val uploadId: String,

    @SerialName("project_id")
    val projectId: Int,

    @SerialName("user_id")
    val userId: Int,

    @SerialName("bucket_id")
    val bucketId: String,

    @SerialName("storage_path")
    val storagePath: String,

    @SerialName("file_name")
    val fileName: String,

    @SerialName("mime_type")
    val mimeType: String,

    @SerialName("size_bytes")
    val sizeBytes: Long,

    val state: String
)