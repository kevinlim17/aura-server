package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.UserPreferencesRepository
import com.kevin.repository.UserRepository

/**
 * User Preferences Service
 * Handles business logic for user preferences management
 */
class UserPreferencesService(
    private val userPreferencesRepository: UserPreferencesRepository = UserPreferencesRepository(),
    private val userRepository: UserRepository = UserRepository()
) {

    /**
     * Validate narrative style
     */
    private fun isValidNarrativeStyle(style: String): Boolean {
        val validStyles = listOf("SCHOLARLY", "LITERARY", "CONVERSATIONAL", "POETIC")
        return validStyles.contains(style.uppercase())
    }

    /**
     * Validate preferred length
     */
    private fun isValidPreferredLength(length: String): Boolean {
        val validLengths = listOf("SHORT", "MEDIUM", "LONG")
        return validLengths.contains(length.uppercase())
    }

    /**
     * Validate TTS voice
     */
    private fun isValidTtsVoice(voice: String): Boolean {
        val validVoices = listOf("MALE", "FEMALE", "NEUTRAL")
        return validVoices.contains(voice.uppercase())
    }

    /**
     * Validate TTS speed (0.5 ~ 2.0)
     */
    private fun isValidTtsSpeed(speed: Double): Boolean {
        return speed in 0.5..2.0
    }

    /**
     * Validate TTS pitch (0.5 ~ 2.0)
     */
    private fun isValidTtsPitch(pitch: Double): Boolean {
        return pitch in 0.5..2.0
    }

    /**
     * Create new user preferences
     *
     * @param userId User ID
     * @param request CreateUserPreferencesRequest
     * @return ApiResponse with UserPreferencesResponse or error
     */
    fun createPreferences(userId: Int, request: CreateUserPreferencesRequest): ApiResponse<UserPreferencesResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Check if preferences already exist
        if (userPreferencesRepository.existsByUserId(userId)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Preferences already exist for this user"
            )
        }

        // Validate narrative style
        if (!isValidNarrativeStyle(request.narrativeStyle)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid narrative style. Must be one of: SCHOLARLY, LITERARY, CONVERSATIONAL, POETIC"
            )
        }

        // Validate preferred length
        if (!isValidPreferredLength(request.preferredLength)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid preferred length. Must be one of: SHORT, MEDIUM, LONG"
            )
        }

        // Validate TTS voice
        if (!isValidTtsVoice(request.ttsVoice)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid TTS voice. Must be one of: MALE, FEMALE, NEUTRAL"
            )
        }

        // Validate TTS speed
        if (!isValidTtsSpeed(request.ttsSpeed)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid TTS speed. Must be between 0.5 and 2.0"
            )
        }

        // Validate TTS pitch
        if (!isValidTtsPitch(request.ttsPitch)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid TTS pitch. Must be between 0.5 and 2.0"
            )
        }

        // Create preferences
        val preferences = userPreferencesRepository.createPreferences(userId, request)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Failed to create preferences"
            )

        return ApiResponse(
            success = true,
            data = preferences,
            message = "Preferences created successfully"
        )
    }

    /**
     * Get user preferences by user ID
     *
     * @param userId User ID
     * @return ApiResponse with UserPreferencesResponse or error
     */
    fun getPreferences(userId: Int): ApiResponse<UserPreferencesResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Get preferences
        val preferences = userPreferencesRepository.findPreferencesByUserId(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Preferences not found"
            )

        return ApiResponse(
            success = true,
            data = preferences,
            message = "Preferences retrieved successfully"
        )
    }

    /**
     * Update user preferences
     *
     * @param userId User ID
     * @param request UpdateUserPreferencesRequest
     * @return ApiResponse with UserPreferencesResponse or error
     */
    fun updatePreferences(userId: Int, request: UpdateUserPreferencesRequest): ApiResponse<UserPreferencesResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Check if preferences exist
        if (!userPreferencesRepository.existsByUserId(userId)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Preferences not found"
            )
        }

        // Validate narrative style if provided
        if (request.narrativeStyle != null && !isValidNarrativeStyle(request.narrativeStyle)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid narrative style. Must be one of: SCHOLARLY, LITERARY, CONVERSATIONAL, POETIC"
            )
        }

        // Validate preferred length if provided
        if (request.preferredLength != null && !isValidPreferredLength(request.preferredLength)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid preferred length. Must be one of: SHORT, MEDIUM, LONG"
            )
        }

        // Validate TTS voice if provided
        if (request.ttsVoice != null && !isValidTtsVoice(request.ttsVoice)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid TTS voice. Must be one of: MALE, FEMALE, NEUTRAL"
            )
        }

        // Validate TTS speed if provided
        if (request.ttsSpeed != null && !isValidTtsSpeed(request.ttsSpeed)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid TTS speed. Must be between 0.5 and 2.0"
            )
        }

        // Validate TTS pitch if provided
        if (request.ttsPitch != null && !isValidTtsPitch(request.ttsPitch)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid TTS pitch. Must be between 0.5 and 2.0"
            )
        }

        // Update preferences
        val preferences = userPreferencesRepository.updatePreferences(userId, request)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Failed to update preferences"
            )

        return ApiResponse(
            success = true,
            data = preferences,
            message = "Preferences updated successfully"
        )
    }

    /**
     * Delete user preferences
     *
     * @param userId User ID
     * @return ApiResponse with success status
     */
    fun deletePreferences(userId: Int): ApiResponse<Unit> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Delete preferences
        val deleted = userPreferencesRepository.deletePreferences(userId)
        if (!deleted) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Preferences not found or failed to delete"
            )
        }

        return ApiResponse(
            success = true,
            data = Unit,
            message = "Preferences deleted successfully"
        )
    }
}