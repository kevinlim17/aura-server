package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.UserProfileRepository
import com.kevin.repository.UserRepository

/**
 * User Profile Service
 * Handles business logic for user profile management
 */
class UserProfileService(
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(),
    private val userRepository: UserRepository = UserRepository()
) {

    /**
     * Create a new user profile
     *
     * @param userId User ID
     * @param request CreateUserProfileRequest
     * @return ApiResponse with UserProfileResponse or error
     */
    fun createProfile(userId: Int, request: CreateUserProfileRequest): ApiResponse<UserProfileResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Check if profile already exists
        if (userProfileRepository.existsByUserId(userId)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Profile already exists for this user"
            )
        }

        // Create profile
        val profile = userProfileRepository.createProfile(userId, request)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Failed to create profile"
            )

        return ApiResponse(
            success = true,
            data = profile,
            message = "Profile created successfully"
        )
    }

    /**
     * Get user profile by user ID
     *
     * @param userId User ID
     * @return ApiResponse with UserProfileResponse or error
     */
    fun getProfile(userId: Int): ApiResponse<UserProfileResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Get profile
        val profile = userProfileRepository.findProfileByUserId(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Profile not found"
            )

        return ApiResponse(
            success = true,
            data = profile,
            message = "Profile retrieved successfully"
        )
    }

    /**
     * Update user profile
     *
     * @param userId User ID
     * @param request UpdateUserProfileRequest
     * @return ApiResponse with UserProfileResponse or error
     */
    fun updateProfile(userId: Int, request: UpdateUserProfileRequest): ApiResponse<UserProfileResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Check if profile exists
        if (!userProfileRepository.existsByUserId(userId)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Profile not found"
            )
        }

        // Update profile
        val profile = userProfileRepository.updateProfile(userId, request)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Failed to update profile"
            )

        return ApiResponse(
            success = true,
            data = profile,
            message = "Profile updated successfully"
        )
    }

    /**
     * Get complete user profile with context
     *
     * @param userId User ID
     * @return ApiResponse with CompleteUserProfileResponse or error
     */
    fun getCompleteProfile(userId: Int): ApiResponse<CompleteUserProfileResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Get complete profile
        val completeProfile = userProfileRepository.getCompleteProfile(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Profile not found"
            )

        return ApiResponse(
            success = true,
            data = completeProfile,
            message = "Complete profile retrieved successfully"
        )
    }

    /**
     * Delete user profile
     *
     * @param userId User ID
     * @return ApiResponse with success status
     */
    fun deleteProfile(userId: Int): ApiResponse<Unit> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Delete profile
        val deleted = userProfileRepository.deleteProfile(userId)
        if (!deleted) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Profile not found or failed to delete"
            )
        }

        return ApiResponse(
            success = true,
            data = Unit,
            message = "Profile deleted successfully"
        )
    }
}