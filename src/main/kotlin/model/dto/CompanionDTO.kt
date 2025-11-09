package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * Companion DTOs for User Companions Management
 * Supports inviting and managing companions for visually impaired users
 */

/**
 * Relationship types
 */
enum class CompanionRelationshipType {
    FAMILY,      // 가족
    FRIEND,      // 친구
    CAREGIVER,   // 돌봄 제공자
    VOLUNTEER,   // 자원봉사자
    OTHER        // 기타
}

/**
 * Companion status
 */
enum class CompanionStatus {
    PENDING,   // 초대 대기 중
    ACTIVE,    // 활성
    INACTIVE   // 비활성
}

/**
 * Request to invite a companion
 */
@Serializable
data class InviteCompanionRequest(
    val companionEmail: String,                  // 동행자 이메일
    val relationshipType: String,                // FAMILY | FRIEND | CAREGIVER | VOLUNTEER | OTHER
    val canAddContext: Boolean = true,           // 컨텍스트 추가 권한
    val canViewHistory: Boolean = true,          // 히스토리 조회 권한
    val canAddMemos: Boolean = true,             // 메모 추가 권한
    val message: String? = null                  // 초대 메시지
)

/**
 * Request to update companion permissions
 */
@Serializable
data class UpdateCompanionPermissionsRequest(
    val canAddContext: Boolean? = null,
    val canViewHistory: Boolean? = null,
    val canAddMemos: Boolean? = null
)

/**
 * Companion response
 */
@Serializable
data class CompanionResponse(
    val id: Int,
    val mainUserId: Int,
    val companionUserId: Int,

    // Companion user details
    val companionEmail: String,
    val companionName: String?,

    // Relationship
    val relationshipType: String?,

    // Permissions
    val canAddContext: Boolean,
    val canViewHistory: Boolean,
    val canAddMemos: Boolean,

    // Status
    val status: String,

    // Timestamps
    val createdAt: String,
    val updatedAt: String
)

/**
 * Companion list response with pagination
 */
@Serializable
data class CompanionListResponse(
    val companions: List<CompanionResponse>,
    val pagination: PaginationInfo
)

/**
 * Invitation details
 */
@Serializable
data class InvitationResponse(
    val id: Int,
    val mainUserId: Int,
    val mainUserEmail: String,
    val mainUserName: String?,
    val relationshipType: String?,
    val message: String?,
    val status: String,
    val createdAt: String
)

/**
 * Pending invitations for a companion user
 */
@Serializable
data class PendingInvitationsResponse(
    val invitations: List<InvitationResponse>,
    val totalCount: Long
)

/**
 * Companion statistics
 */
@Serializable
data class CompanionStatistics(
    val totalCompanions: Long,
    val activeCompanions: Long,
    val pendingInvitations: Long,
    val companionsByRelationship: Map<String, Long>
)

/**
 * Invite code response
 */
@Serializable
data class InviteCodeResponse(
    val inviteCode: String,
    val expiresAt: String,
    val inviteUrl: String
)