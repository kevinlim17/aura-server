package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * User Profile DTOs
 * Based on existing DatabaseSchema UserProfiles table
 */

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Create user profile request
 * POST /api/users/{userId}/profile
 */
@Serializable
data class CreateUserProfileRequest(
    val interests: List<String> = emptyList(),
    val hobbies: String? = null,
    val hobbiesVoiceUrl: String? = null,
    val favoriteArtists: List<String> = emptyList(),
    val bio: String? = null
)

/**
 * Update user profile request
 * PUT /api/users/{userId}/profile
 */
@Serializable
data class UpdateUserProfileRequest(
    val interests: List<String>? = null,
    val hobbies: String? = null,
    val hobbiesVoiceUrl: String? = null,
    val favoriteArtists: List<String>? = null,
    val bio: String? = null
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * User profile response
 * Used for GET /api/users/{userId}/profile
 */
@Serializable
data class UserProfileResponse(
    val id: Int,
    val userId: Int,
    val interests: List<String>,
    val hobbies: String? = null,
    val hobbiesVoiceUrl: String? = null,
    val favoriteArtists: List<String>,
    val bio: String? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Complete user profile response with user context
 * Used for GET /api/users/{userId}/profile/complete
 */
@Serializable
data class CompleteUserProfileResponse(
    val user: UserBasicInfo,
    val profile: UserProfileResponse,
    val contexts: List<UserContextSummary>
)

/**
 * Basic user information
 */
@Serializable
data class UserBasicInfo(
    val id: Int,
    val email: String,
    val userType: String,
    val isVisuallyImpaired: Boolean,
    val impairmentLevel: String? = null,
    val isOnboardingCompleted: Boolean,
    val createdAt: String
)

/**
 * User context summary for complete profile
 */
@Serializable
data class UserContextSummary(
    val id: Int,
    val contextType: String,
    val title: String? = null,
    val content: String,
    val voiceUrl: String? = null,
    val inputMethod: String,
    val createdAt: String
)