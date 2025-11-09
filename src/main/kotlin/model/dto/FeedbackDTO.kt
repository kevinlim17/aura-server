package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * Feedback DTOs for Docent Session Feedback
 * Supports few-shot learning candidate selection
 */

/**
 * Request to submit feedback for a docent session
 */
@Serializable
data class SubmitFeedbackRequest(
    val emotionalResonance: Int,          // 1-5: 개인적 공감도
    val imaginativeEngagement: Int? = null, // 1-5: 상상적 몰입도 (optional)
    val emotionalImpact: Int? = null,      // 1-5: 정서적 영향 (optional)
    val comment: String? = null,           // 자유 코멘트
    val improvementSuggestions: Map<String, String>? = null  // 개선 제안
)

/**
 * Feedback response
 */
@Serializable
data class FeedbackResponse(
    val id: Int,
    val docentSessionId: Int,
    val userId: Int,

    // Ratings (1-5)
    val emotionalResonance: Int,
    val imaginativeEngagement: Int?,
    val emotionalImpact: Int?,

    // Overall satisfaction (auto-calculated)
    val overallSatisfaction: Double?,

    // Text feedback
    val comment: String?,
    val improvementSuggestions: Map<String, String>,

    // Few-shot learning candidate
    val isFewShotCandidate: Boolean,
    val fewShotSelectedAt: String?,

    // Timestamp
    val createdAt: String
)

/**
 * Feedback with docent session details
 */
@Serializable
data class FeedbackWithSessionResponse(
    val feedback: FeedbackResponse,
    val session: DocentSessionResponse
)

/**
 * Admin feedback list response with pagination
 */
@Serializable
data class AdminFeedbackListResponse(
    val feedbacks: List<FeedbackWithSessionResponse>,
    val pagination: PaginationInfo,
    val statistics: FeedbackStatistics
)

/**
 * Feedback statistics
 */
@Serializable
data class FeedbackStatistics(
    val totalFeedbacks: Long,
    val averageEmotionalResonance: Double,
    val averageImaginativeEngagement: Double,
    val averageEmotionalImpact: Double,
    val averageOverallSatisfaction: Double,
    val fewShotCandidateCount: Long,
    val feedbackWithCommentCount: Long
)
