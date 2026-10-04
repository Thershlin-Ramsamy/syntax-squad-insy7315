package com.mcc.construction.data.remote

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class BackendAction {
    USER_PROVISION,
    USER_ROLE,
    USER_ACTIVE,
    SYSTEM_SETTINGS,

    PROJECT_CREATE,
    PROJECT_UPDATE,
    PROJECT_MEMBER,
    SUPPLIER_CREATE,
    MATERIAL_CREATE,

    TASK_CREATE,
    TASK_ASSIGN,
    TASK_STATUS,

    CLOCK_IN,
    CLOCK_OUT,
    REPORT_SUBMIT,
    REPORT_APPROVE,

    INSPECTION_SUBMIT,
    INCIDENT_SUBMIT,
    STOCK_ADJUST,

    MESSAGE_SEND,
    THREAD_READ,
    NOTIFICATION_READ,
    NOTIFICATIONS_READ,
    SYNC_RECORD,

    FILE_RESERVE,
    DOCUMENT_FINALIZE,
    TASK_PHOTO_FINALIZE,
    TASK_PHOTO_REMOVE
}

/**
 * Save the complete operation when queuing an offline change.
 *
 * Retrying must preserve operationId, action and payload.
 */
@Serializable
data class BackendOperation(
    val operationId: String,
    val authUserId: String,
    val action: BackendAction,
    val payload: JsonObject
) {
    init {
        require(
            UUID.fromString(operationId).toString() == operationId
        ) {
            "Operation ID must be a canonical UUID."
        }

        require(
            UUID.fromString(authUserId).toString() == authUserId
        ) {
            "Auth user ID must be a canonical UUID."
        }
    }

    companion object {
        fun create(
            authUserId: String,
            action: BackendAction,
            payload: JsonObject
        ): BackendOperation {
            return BackendOperation(
                operationId = UUID.randomUUID().toString(),
                authUserId = authUserId,
                action = action,
                payload = payload
            )
        }
    }
}