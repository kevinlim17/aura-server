package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.MemoRepository

/**
 * Memo Service
 * Handles business logic for user memos management
 *
 * Features:
 * - Create, update, retrieve, and delete memos
 * - Support for text and voice memos
 * - Filtering by artwork, category, and input method
 * - Full-text search in memo content
 * - Pagination support
 * - Memo statistics
 */
class MemoService(
    private val memoRepository: MemoRepository = MemoRepository()
) {

    /**
     * Create a new memo
     *
     * @param userId User ID
     * @param request Memo creation request
     * @return ApiResponse with MemoResponse or error
     */
    fun createMemo(userId: Int, request: CreateMemoRequest): ApiResponse<MemoResponse> {
        // Validate content
        if (request.content.isBlank()) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Memo content cannot be empty"
            )
        }

        // Validate inputMethod
        try {
            MemoInputMethod.valueOf(request.inputMethod)
        } catch (e: IllegalArgumentException) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid inputMethod. Must be TEXT or VOICE"
            )
        }

        // Validate voice memo fields
        if (request.inputMethod == "VOICE") {
            if (request.voiceUrl.isNullOrBlank()) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Voice URL is required for VOICE memos"
                )
            }
        }

        try {
            // Create memo
            val memoId = memoRepository.createMemo(
                userId = userId,
                content = request.content,
                artworkId = request.artworkId,
                docentSessionId = request.docentSessionId,
                inputMethod = request.inputMethod,
                voiceUrl = request.voiceUrl,
                voiceDurationSeconds = request.voiceDurationSeconds,
                tags = request.tags,
                category = request.category,
                isSharedWithCompanion = request.isSharedWithCompanion
            )

            // Retrieve created memo
            val memo = memoRepository.findMemoById(memoId)
                ?: return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to retrieve created memo"
                )

            return ApiResponse(
                success = true,
                data = memo,
                message = "Memo created successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to create memo: ${e.message}"
            )
        }
    }

    /**
     * Get memos for a user with pagination and filters
     *
     * @param userId User ID
     * @param page Page number
     * @param limit Items per page
     * @param artworkId Filter by artwork ID (optional)
     * @param category Filter by category (optional)
     * @param inputMethod Filter by input method (optional)
     * @param searchQuery Search in memo content (optional)
     * @return ApiResponse with MemoListResponse or error
     */
    fun getMemos(
        userId: Int,
        page: Int = 1,
        limit: Int = 20,
        artworkId: Int? = null,
        category: String? = null,
        inputMethod: String? = null,
        searchQuery: String? = null
    ): ApiResponse<MemoListResponse> {
        // Validate pagination parameters
        if (page < 1) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Page must be greater than 0"
            )
        }

        if (limit !in 1..100) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Limit must be between 1 and 100"
            )
        }

        // Validate inputMethod if provided
        if (inputMethod != null) {
            try {
                MemoInputMethod.valueOf(inputMethod)
            } catch (e: IllegalArgumentException) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Invalid inputMethod. Must be TEXT or VOICE"
                )
            }
        }

        try {
            // Get memos with filters
            val (memos, totalCount) = memoRepository.findMemosByUserId(
                userId = userId,
                page = page,
                limit = limit,
                artworkId = artworkId,
                category = category,
                inputMethod = inputMethod,
                searchQuery = searchQuery
            )

            // Calculate pagination info
            val totalPages = ((totalCount + limit - 1) / limit).toInt()

            val response = MemoListResponse(
                memos = memos,
                pagination = PaginationInfo(
                    page = page,
                    limit = limit,
                    totalCount = totalCount,
                    totalPages = totalPages,
                    hasNext = page < totalPages,
                    hasPrevious = page > 1
                )
            )

            return ApiResponse(
                success = true,
                data = response,
                message = "Memos retrieved successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to retrieve memos: ${e.message}"
            )
        }
    }

    /**
     * Update a memo
     *
     * @param userId User ID
     * @param memoId Memo ID
     * @param request Memo update request
     * @return ApiResponse with updated MemoResponse or error
     */
    fun updateMemo(
        userId: Int,
        memoId: Int,
        request: UpdateMemoRequest
    ): ApiResponse<MemoResponse> {
        // Check if memo exists and belongs to user
        val exists = memoRepository.isMemoOwnedByUser(memoId, userId)
        if (!exists) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Memo not found or access denied"
            )
        }

        // Validate content if provided
        if (request.content != null && request.content.isBlank()) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Memo content cannot be empty"
            )
        }

        try {
            // Update memo
            val updated = memoRepository.updateMemo(
                memoId = memoId,
                userId = userId,
                content = request.content,
                tags = request.tags,
                category = request.category,
                isSharedWithCompanion = request.isSharedWithCompanion
            )

            if (!updated) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to update memo"
                )
            }

            // Retrieve updated memo
            val memo = memoRepository.findMemoById(memoId)
                ?: return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to retrieve updated memo"
                )

            return ApiResponse(
                success = true,
                data = memo,
                message = "Memo updated successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to update memo: ${e.message}"
            )
        }
    }

    /**
     * Delete a memo
     *
     * @param userId User ID
     * @param memoId Memo ID
     * @return ApiResponse with success status
     */
    fun deleteMemo(userId: Int, memoId: Int): ApiResponse<Unit> {
        // Check if memo exists and belongs to user
        val exists = memoRepository.isMemoOwnedByUser(memoId, userId)
        if (!exists) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Memo not found or access denied"
            )
        }

        try {
            val deleted = memoRepository.deleteMemo(memoId, userId)
            if (!deleted) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to delete memo"
                )
            }

            return ApiResponse(
                success = true,
                data = Unit,
                message = "Memo deleted successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to delete memo: ${e.message}"
            )
        }
    }

    /**
     * Get memo statistics for a user
     *
     * @param userId User ID
     * @return ApiResponse with MemoStatistics or error
     */
    fun getMemoStatistics(userId: Int): ApiResponse<MemoStatistics> {
        try {
            val statistics = memoRepository.getMemoStatistics(userId)

            return ApiResponse(
                success = true,
                data = statistics,
                message = "Memo statistics retrieved successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to retrieve memo statistics: ${e.message}"
            )
        }
    }
}
