package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * User Context DTOs
 * Based on the existing DatabaseSchema UserContexts table
 */

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Create user context request
 * POST /api/users/{userId}/contexts
 */
@Serializable
data class CreateUserContextRequest(
    val contextType: String, // MEMORY, EMOTION, GOAL, PHYSICAL_STATE, COMPANION_OBSERVATION, OTHER
    val title: String? = null,
    val content: String,
    val emotionalTone: String? = null, // POSITIVE, NEUTRAL, NEGATIVE, MIXED
    val emotionTags: List<String> = emptyList(),
    val voiceUrl: String? = null,
    val voiceDurationSeconds: Int? = null,
    val inputMethod: String = "TEXT", // TEXT, VOICE
    val importanceLevel: Int? = null,
    val isCompanionInput: Boolean = false,
    val companionUserId: Int? = null
)

/**
 * Update user context request
 * PUT /api/users/{userId}/contexts/{contextId}
 */
@Serializable
data class UpdateUserContextRequest(
    val contextType: String? = null,
    val title: String? = null,
    val content: String? = null,
    val emotionalTone: String? = null,
    val emotionTags: List<String>? = null,
    val voiceUrl: String? = null,
    val voiceDurationSeconds: Int? = null,
    val importanceLevel: Int? = null
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * User context response
 * Used for GET /api/users/{userId}/contexts/{contextId}
 */
@Serializable
data class UserContextResponse(
    val id: Int,
    val userId: Int,
    val contextType: String,
    val title: String? = null,
    val content: String,
    val emotionalTone: String? = null,
    val emotionTags: List<String> = emptyList(),
    val voiceUrl: String? = null,
    val voiceDurationSeconds: Int? = null,
    val inputMethod: String,
    val importanceLevel: Int? = null,
    val isCompanionInput: Boolean,
    val companionUserId: Int? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * User contexts list response with pagination
 * Used for GET /api/users/{userId}/contexts
 */
@Serializable
data class UserContextsListResponse(
    val contexts: List<UserContextResponse>,
    val pagination: PaginationInfo
)

/**
 * Pagination information
 */
@Serializable
data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val totalCount: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)