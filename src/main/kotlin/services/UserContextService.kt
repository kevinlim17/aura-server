package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.UserContextRepository
import com.kevin.repository.UserRepository

/**
 * User Context Service
 * Handles business logic for user context management
 */
class UserContextService(
    private val userContextRepository: UserContextRepository = UserContextRepository(),
    private val userRepository: UserRepository = UserRepository()
) {

    /**
     * Validate context type
     */
    private fun isValidContextType(contextType: String): Boolean {
        val validTypes = listOf(
            "MEMORY",
            "EMOTION",
            "GOAL",
            "PHYSICAL_STATE",
            "COMPANION_OBSERVATION",
            "OTHER"
        )
        return validTypes.contains(contextType.uppercase())
    }

    /**
     * Validate emotional tone
     */
    private fun isValidEmotionalTone(emotionalTone: String): Boolean {
        val validTones = listOf("POSITIVE", "NEUTRAL", "NEGATIVE", "MIXED")
        return validTones.contains(emotionalTone.uppercase())
    }

    /**
     * Create a new user context
     *
     * @param userId User ID
     * @param request CreateUserContextRequest
     * @return ApiResponse with UserContextResponse or error
     */
    fun createContext(userId: Int, request: CreateUserContextRequest): ApiResponse<UserContextResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Validate context type
        if (!isValidContextType(request.contextType)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid context type. Must be one of: MEMORY, EMOTION, GOAL, PHYSICAL_STATE, COMPANION_OBSERVATION, OTHER"
            )
        }

        // Validate emotional tone if provided
        if (request.emotionalTone != null && !isValidEmotionalTone(request.emotionalTone)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid emotional tone. Must be one of: POSITIVE, NEUTRAL, NEGATIVE, MIXED"
            )
        }

        // Validate content
        if (request.content.isBlank()) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Content cannot be empty"
            )
        }

        // Validate companion input
        if (request.isCompanionInput && request.companionUserId == null) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Companion user ID is required when isCompanionInput is true"
            )
        }

        // Create context
        val context = userContextRepository.createContext(userId, request)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Failed to create context"
            )

        return ApiResponse(
            success = true,
            data = context,
            message = "Context created successfully"
        )
    }

    /**
     * Get user contexts with pagination and filters
     *
     * @param userId User ID
     * @param page Page number (default: 1)
     * @param limit Items per page (default: 20)
     * @param contextType Context type filter
     * @return ApiResponse with UserContextsListResponse or error
     */
    fun getContexts(
        userId: Int,
        page: Int = 1,
        limit: Int = 20,
        contextType: String? = null
    ): ApiResponse<UserContextsListResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Validate pagination parameters
        if (page < 1) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Page must be greater than 0"
            )
        }

        if (limit < 1 || limit > 100) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Limit must be between 1 and 100"
            )
        }

        // Validate context type filter if provided
        if (contextType != null && !isValidContextType(contextType)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid context type. Must be one of: MEMORY, EMOTION, GOAL, PHYSICAL_STATE, COMPANION_OBSERVATION, OTHER"
            )
        }

        // Get contexts
        val result = userContextRepository.findContextsByUserId(
            userId = userId,
            page = page,
            limit = limit,
            contextType = contextType
        )

        return ApiResponse(
            success = true,
            data = result,
            message = "Contexts retrieved successfully"
        )
    }

    /**
     * Get specific user context by ID
     *
     * @param userId User ID
     * @param contextId Context ID
     * @return ApiResponse with UserContextResponse or error
     */
    fun getContext(userId: Int, contextId: Int): ApiResponse<UserContextResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Check if context exists and belongs to user
        if (!userContextRepository.existsByIdAndUserId(contextId, userId)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Context not found"
            )
        }

        // Get context
        val context = userContextRepository.findContextById(contextId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Context not found"
            )

        return ApiResponse(
            success = true,
            data = context,
            message = "Context retrieved successfully"
        )
    }

    /**
     * Update user context
     *
     * @param userId User ID
     * @param contextId Context ID
     * @param request UpdateUserContextRequest
     * @return ApiResponse with UserContextResponse or error
     */
    fun updateContext(userId: Int, contextId: Int, request: UpdateUserContextRequest): ApiResponse<UserContextResponse> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Check if context exists and belongs to user
        if (!userContextRepository.existsByIdAndUserId(contextId, userId)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Context not found"
            )
        }

        // Validate context type if provided
        if (request.contextType != null && !isValidContextType(request.contextType)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid context type. Must be one of: MEMORY, EMOTION, GOAL, PHYSICAL_STATE, COMPANION_OBSERVATION, OTHER"
            )
        }

        // Validate emotional tone if provided
        if (request.emotionalTone != null && !isValidEmotionalTone(request.emotionalTone)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid emotional tone. Must be one of: POSITIVE, NEUTRAL, NEGATIVE, MIXED"
            )
        }

        // Validate content if provided
        if (request.content != null && request.content.isBlank()) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Content cannot be empty"
            )
        }

        // Update context
        val context = userContextRepository.updateContext(contextId, userId, request)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Failed to update context"
            )

        return ApiResponse(
            success = true,
            data = context,
            message = "Context updated successfully"
        )
    }

    /**
     * Delete user context
     *
     * @param userId User ID
     * @param contextId Context ID
     * @return ApiResponse with success status
     */
    fun deleteContext(userId: Int, contextId: Int): ApiResponse<Unit> {
        // Check if user exists
        val user = userRepository.findUserById(userId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "User not found"
            )

        // Check if context exists and belongs to user
        if (!userContextRepository.existsByIdAndUserId(contextId, userId)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Context not found"
            )
        }

        // Delete context
        val deleted = userContextRepository.deleteContext(contextId, userId)
        if (!deleted) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to delete context"
            )
        }

        return ApiResponse(
            success = true,
            data = Unit,
            message = "Context deleted successfully"
        )
    }
}