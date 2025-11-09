package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.FeedbackRepository
import com.kevin.repository.DocentSessionRepository

/**
 * Feedback Service
 * Handles business logic for docent session feedback
 *
 * Features:
 * - Submit and retrieve feedback
 * - Automatic few-shot candidate selection (rating >= 4.0)
 * - Feedback statistics for analytics
 */
class FeedbackService(
    private val feedbackRepository: FeedbackRepository = FeedbackRepository(),
    private val docentRepository: DocentSessionRepository = DocentSessionRepository()
) {

    /**
     * Submit feedback for a docent session
     *
     * @param sessionId Docent session ID
     * @param userId User ID
     * @param request Feedback submission request
     * @return ApiResponse with FeedbackResponse or error
     */
    fun submitFeedback(
        sessionId: Int,
        userId: Int,
        request: SubmitFeedbackRequest
    ): ApiResponse<FeedbackResponse> {
        // Validate ratings
        if (request.emotionalResonance !in 1..5) {
            return ApiResponse(
                success = false,
                data = null,
                message = "emotionalResonance must be between 1 and 5"
            )
        }

        if (request.imaginativeEngagement != null && request.imaginativeEngagement !in 1..5) {
            return ApiResponse(
                success = false,
                data = null,
                message = "imaginativeEngagement must be between 1 and 5"
            )
        }

        if (request.emotionalImpact != null && request.emotionalImpact !in 1..5) {
            return ApiResponse(
                success = false,
                data = null,
                message = "emotionalImpact must be between 1 and 5"
            )
        }

        // Check if session exists
        val session = docentRepository.findSessionById(sessionId)
        if (session == null) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Docent session not found"
            )
        }

        // Check if feedback already exists for this session
        val existingFeedback = feedbackRepository.findFeedbackBySessionId(sessionId)
        if (existingFeedback != null) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Feedback already submitted for this session"
            )
        }

        // Create feedback
        try {
            val feedbackId = feedbackRepository.createFeedback(
                docentSessionId = sessionId,
                userId = userId,
                emotionalResonance = request.emotionalResonance,
                imaginativeEngagement = request.imaginativeEngagement,
                emotionalImpact = request.emotionalImpact,
                comment = request.comment,
                improvementSuggestions = request.improvementSuggestions
            )

            // Retrieve created feedback
            val feedback = feedbackRepository.findFeedbackById(feedbackId)
                ?: return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to retrieve created feedback"
                )

            return ApiResponse(
                success = true,
                data = feedback,
                message = if (feedback.isFewShotCandidate) {
                    "Feedback submitted successfully (marked as few-shot candidate)"
                } else {
                    "Feedback submitted successfully"
                }
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to submit feedback: ${e.message}"
            )
        }
    }

    /**
     * Get feedback for a specific docent session
     *
     * @param sessionId Docent session ID
     * @return ApiResponse with FeedbackResponse or error
     */
    fun getFeedbackBySessionId(sessionId: Int): ApiResponse<FeedbackResponse> {
        // Check if session exists
        val session = docentRepository.findSessionById(sessionId) ?: return ApiResponse(
            success = false,
            data = null,
            message = "Docent session not found"
        )

        // Find feedback
        val feedback = feedbackRepository.findFeedbackBySessionId(sessionId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "No feedback found for this session"
            )

        return ApiResponse(
            success = true,
            data = feedback,
            message = "Feedback retrieved successfully"
        )
    }

    /**
     * Get all feedbacks with pagination (admin only)
     *
     * @param page Page number
     * @param limit Items per page
     * @return ApiResponse with AdminFeedbackListResponse or error
     */
    fun getAllFeedbacks(
        page: Int = 1,
        limit: Int = 20
    ): ApiResponse<AdminFeedbackListResponse> {
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

        try {
            // Get feedbacks with pagination
            val (feedbacks, totalCount) = feedbackRepository.findAllFeedbacks(page, limit)

            // Get statistics
            val statistics = feedbackRepository.getFeedbackStatistics()

            // Calculate pagination info
            val totalPages = ((totalCount + limit - 1) / limit).toInt()

            val response = AdminFeedbackListResponse(
                feedbacks = feedbacks,
                pagination = PaginationInfo(
                    page = page,
                    limit = limit,
                    totalCount = totalCount,
                    totalPages = totalPages,
                    hasNext = page < totalPages,
                    hasPrevious = page > 1
                ),
                statistics = statistics
            )

            return ApiResponse(
                success = true,
                data = response,
                message = "Feedbacks retrieved successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to retrieve feedbacks: ${e.message}"
            )
        }
    }

    /**
     * Mark feedback as few-shot example (admin only)
     *
     * @param feedbackId Feedback ID
     * @return ApiResponse with success status
     */
    fun markAsFewShot(feedbackId: Int): ApiResponse<FeedbackResponse> {
        // Check if feedback exists
        val feedback = feedbackRepository.findFeedbackById(feedbackId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Feedback not found"
            )

        // Mark as few-shot
        val updated = feedbackRepository.markAsFewShot(feedbackId)
        if (!updated) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to mark as few-shot candidate"
            )
        }

        // Return updated feedback
        val updatedFeedback = feedbackRepository.findFeedbackById(feedbackId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Failed to retrieve updated feedback"
            )

        return ApiResponse(
            success = true,
            data = updatedFeedback,
            message = "Marked as few-shot candidate successfully"
        )
    }
}