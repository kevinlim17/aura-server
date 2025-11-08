package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * Docent DTOs
 * Based on the DatabaseSchema DocentSessions table
 */

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Generate docent request
 * POST /api/docent/generate
 */
@Serializable
data class GenerateDocentRequest(
    val userId: Int,
    val artworkId: Int,
    val narrativeStyle: String? = null, // LITERARY, CONVERSATIONAL, POETIC, ANALYTICAL (optional, uses user preference if not provided)
    val preferredLength: String? = null, // SHORT, MEDIUM, LONG (optional, uses user preference if not provided)
    val includeCompanionContext: Boolean = false,
    val useFewShotExamples: Boolean = true,
    val customPrompt: String? = null // Optional custom instructions
)

/**
 * Update play stats request
 * PUT /api/docent/sessions/{sessionId}/play-stats
 */
@Serializable
data class UpdatePlayStatsRequest(
    val playCount: Int? = null,
    val totalListeningSeconds: Int? = null,
    val completionRate: Double? = null
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * Docent session response
 * Used for GET /api/docent/sessions/{sessionId}
 */
@Serializable
data class DocentSessionResponse(
    val id: Int,
    val userId: Int,
    val artworkId: Int,
    val artworkTitle: String? = null,
    val artworkArtist: String? = null,
    val artworkImageUrl: String? = null,

    // Prompt information
    val promptTemplate: String,
    val promptPersona: String? = null,
    val promptTask: String? = null,
    val promptContext: String? = null,
    val promptForm: String? = null,
    val fewShotExamples: List<FewShotExample> = emptyList(),

    // Generated content
    val generatedText: String,

    // Gemini API metadata
    val geminiModel: String? = null,
    val geminiTemperature: Double? = null,
    val geminiTopP: Double? = null,
    val geminiTopK: Int? = null,
    val generationTimeMs: Int? = null,

    // TTS audio
    val ttsAudioUrl: String? = null,
    val ttsDurationSeconds: Int? = null,

    // Playback statistics
    val playCount: Int = 0,
    val totalListeningSeconds: Int = 0,
    val completionRate: Double? = null,

    // Status
    val status: String, // GENERATING, COMPLETED, FAILED

    // Timestamps
    val createdAt: String,
    val lastPlayedAt: String? = null
)

/**
 * Few-shot example structure
 */
@Serializable
data class FewShotExample(
    val userContextSummary: String,
    val exemplarText: String,
    val qualityScore: Double
)

/**
 * Docent generation session response (async processing)
 * POST /api/docent/generate returns 202 Accepted with this
 */
@Serializable
data class DocentGenerationSessionResponse(
    val sessionId: Int,
    val status: String, // GENERATING, COMPLETED, FAILED
    val pollingUrl: String,
    val message: String
)

/**
 * User docent history response
 * GET /api/users/me/docent/history
 */
@Serializable
data class DocentHistoryResponse(
    val sessions: List<DocentSessionResponse>,
    val pagination: PaginationInfo
)

/**
 * Docent generation result (polling response)
 * GET /api/docent/sessions/{sessionId}
 */
@Serializable
data class DocentGenerationResultResponse(
    val sessionId: Int,
    val status: String, // GENERATING, COMPLETED, FAILED
    val result: DocentSessionResponse? = null,
    val errorMessage: String? = null,
    val progress: Int? = null // 0-100 percentage
)

/**
 * Gemini API configuration
 */
@Serializable
data class GeminiConfig(
    val model: String = "gemini-pro",
    val temperature: Double = 0.7,
    val topP: Double = 0.9,
    val topK: Int = 40,
    val maxOutputTokens: Int = 2048
)

/**
 * TTS configuration
 */
@Serializable
data class TTSConfig(
    val voice: String = "NEUTRAL",
    val speed: Double = 1.0,
    val pitch: Double = 1.0,
    val language: String = "ko-KR"
)