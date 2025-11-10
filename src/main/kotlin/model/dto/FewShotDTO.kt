package com.kevin.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Few-Shot Learning DTOs
 * For managing and selecting few-shot examples
 */

// ============================================================================
// Core Data Classes
// ============================================================================

/**
 * Few-Shot Example summary
 * Used for displaying and selecting examples
 */
@Serializable
data class FewShotExampleDTO(
    val id: Int,
    val artworkId: Int,
    val artworkInfo: ArtworkSummary,
    val userContextSummary: String,
    val exemplarText: String,
    val qualityScore: Double,
    val effectivenessScore: Double? = null,
    val diversityScore: Double? = null,
    val usageCount: Int = 0,
    val lastUsedAt: String? = null,
    val createdAt: String,
    val category: String? = null,
    val isActive: Boolean = true
)

/**
 * Artwork summary for few-shot examples
 */
@Serializable
data class ArtworkSummary(
    val id: Int,
    val title: String,
    val artist: String,
    val artworkType: String? = null,
    val genre: String? = null,
    val creationYear: Int? = null,
    val museum: String? = null
)

/**
 * Enriched Few-Shot with all resources loaded
 * Ready to be used in Gemini prompts
 */
@Serializable
data class EnrichedFewShot(
    val example: FewShotExampleDTO,
    val contexts: List<UserContextResponse> = emptyList(),
    val links: List<LinkResponse> = emptyList(),
    val memos: List<MemoResponse> = emptyList(),
    val companionInput: String? = null,
    val promptMetadata: String = "{}" // JSON string for now
)

/**
 * Resource with relevance score
 * Used in few_shot_example_resources table
 */
@Serializable
data class FewShotResource(
    val id: Int,
    val fewShotExampleId: Int,
    val resourceType: String, // CONTEXT, LINK, MEMO
    val resourceId: Int,
    val relevanceScore: Double,
    val createdAt: String
)

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Build few-shot from session request
 * POST /api/few-shot/build
 */
@Serializable
data class BuildFewShotRequest(
    val docentSessionId: Int,
    val feedbackId: Int,
    val companionInput: String? = null,
    val category: String? = null
)

/**
 * Few-Shot selection criteria
 * POST /api/few-shot/select
 */
@Serializable
data class FewShotSelectionCriteria(
    val userId: Int,
    val artworkId: Int? = null,
    val artworkStyle: String? = null,
    val artworkEra: String? = null,
    val preferredContextTypes: List<String> = emptyList(),
    val minQualityScore: Double = 4.0,
    val maxResults: Int = 5,
    val ensureDiversity: Boolean = true
)

/**
 * Update few-shot quality request
 * PUT /api/few-shot/{fewShotId}/quality
 */
@Serializable
data class UpdateFewShotQualityRequest(
    val newFeedbackScore: Double
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * Few-shot list response
 * GET /api/few-shot
 */
@Serializable
data class FewShotListResponse(
    val examples: List<FewShotExampleDTO>,
    val pagination: PaginationInfo
)

/**
 * Few-shot selection response
 * POST /api/few-shot/select
 */
@Serializable
data class FewShotSelectionResponse(
    val selectedExamples: List<EnrichedFewShot>,
    val selectionCriteria: FewShotSelectionCriteria,
    val totalAvailable: Int
)

/**
 * Few-shot effectiveness metrics
 */
@Serializable
data class FewShotEffectivenessMetrics(
    val fewShotId: Int,
    val feedbackScore: Double,       // 40% weight
    val usageFrequency: Double,      // 30% weight
    val recency: Double,             // 20% weight
    val resourceDiversity: Double,   // 10% weight
    val overallEffectiveness: Double
)

/**
 * Few-shot statistics
 * GET /api/few-shot/statistics
 */
@Serializable
data class FewShotStatistics(
    val totalExamples: Int,
    val activeExamples: Int,
    val averageQualityScore: Double,
    val averageEffectivenessScore: Double,
    val totalUsageCount: Int,
    val examplesByCategory: Map<String, Int>,
    val topPerformingExamples: List<FewShotExampleDTO>
)

/**
 * Redis cache statistics for few-shot examples
 * Used for monitoring cache performance
 */
@Serializable
data class FewShotCacheStats(
    val totalCached: Int,
    val cacheHitRate: Double,
    val averageRetrievalTimeMs: Long,
    val topUsedFewShots: List<TopUsedFewShot>,
    val cacheSizeBytes: Long
)

/**
 * Top used few-shot example with usage count
 */
@Serializable
data class TopUsedFewShot(
    val fewShotId: Int,
    val usageCount: Int
)

// ============================================================================
// Few-Shot Selection DTOs
// ============================================================================

/**
 * Scored few-shot example with composite score
 * Used by FewShotSelectorService for ranking and selection
 */
@Serializable
data class ScoredFewShot(
    val fewShot: EnrichedFewShot,
    val compositeScore: Double,
    val scoreBreakdown: ScoreBreakdown
)

/**
 * Detailed score breakdown for transparency
 * Shows how each component contributes to the final score
 */
@Serializable
data class ScoreBreakdown(
    val qualityScore: Double,           // 35% weight: Feedback-based quality
    val relevanceScore: Double,          // 30% weight: Artwork + context similarity
    val recencyScore: Double,            // 15% weight: Recent creation/usage
    val preferenceAlignmentScore: Double, // 10% weight: Narrative style match
    val resourceRichnessScore: Double,   // 10% weight: Number of resources
    val totalScore: Double               // Weighted sum
)