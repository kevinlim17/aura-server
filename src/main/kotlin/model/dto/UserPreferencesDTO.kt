package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * User Preferences DTOs
 * Based on existing DatabaseSchema UserPreferences table
 */

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Create user preferences request
 * POST /api/users/{userId}/preferences
 */
@Serializable
data class CreateUserPreferencesRequest(
    val narrativeStyle: String = "LITERARY", // SCHOLARLY, LITERARY, CONVERSATIONAL, POETIC
    val preferredLength: String = "MEDIUM", // SHORT, MEDIUM, LONG
    val ttsSpeed: Double = 1.0, // 0.5 ~ 2.0
    val ttsPitch: Double = 1.0, // 0.5 ~ 2.0
    val ttsVoice: String = "NEUTRAL", // MALE, FEMALE, NEUTRAL
    val preferredLanguage: String = "ko-KR",
    val enableHapticFeedback: Boolean = true,
    val enableAudioDescriptions: Boolean = true,
    val highContrastMode: Boolean = false,
    val enablePushNotifications: Boolean = true
)

/**
 * Update user preferences request
 * PUT /api/users/{userId}/preferences
 */
@Serializable
data class UpdateUserPreferencesRequest(
    val narrativeStyle: String? = null,
    val preferredLength: String? = null,
    val ttsSpeed: Double? = null,
    val ttsPitch: Double? = null,
    val ttsVoice: String? = null,
    val preferredLanguage: String? = null,
    val enableHapticFeedback: Boolean? = null,
    val enableAudioDescriptions: Boolean? = null,
    val highContrastMode: Boolean? = null,
    val enablePushNotifications: Boolean? = null
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * User preferences response
 * Used for GET /api/users/{userId}/preferences
 */
@Serializable
data class UserPreferencesResponse(
    val id: Int,
    val userId: Int,
    val narrativeStyle: String,
    val preferredLength: String,
    val ttsSpeed: Double,
    val ttsPitch: Double,
    val ttsVoice: String,
    val preferredLanguage: String,
    val enableHapticFeedback: Boolean,
    val enableAudioDescriptions: Boolean,
    val highContrastMode: Boolean,
    val enablePushNotifications: Boolean,
    val isVisuallyImpaired: Boolean = false, // From Users table
    val createdAt: String,
    val updatedAt: String
)