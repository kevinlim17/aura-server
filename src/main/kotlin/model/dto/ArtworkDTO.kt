package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * Artwork DTOs
 * Based on the DatabaseSchema Artworks table
 */

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Text-based artwork search request
 * POST /api/artworks/search
 */
@Serializable
data class ArtworkTextSearchRequest(
    val query: String,
    val page: Int = 1,
    val limit: Int = 20
)

/**
 * Voice-based artwork search request
 * POST /api/artworks/search/voice
 * Frontend sends STT-completed text
 */
@Serializable
data class ArtworkVoiceSearchRequest(
    val transcribedText: String,
    val voiceUrl: String? = null,
    val userId: Int
)

/**
 * Image-based artwork search request
 * POST /api/artworks/search/camera
 * Returns session ID for async processing
 */
@Serializable
data class ArtworkImageSearchRequest(
    val userId: Int
)

/**
 * Similar artworks request
 * GET /api/artworks/{artworkId}/similar
 */
@Serializable
data class SimilarArtworksRequest(
    val limit: Int = 10
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * Artwork response
 * Used for GET /api/artworks/{artworkId}
 */
@Serializable
data class ArtworkResponse(
    val id: Int,
    val title: String,
    val titleEn: String? = null,
    val artist: String,
    val artistEn: String? = null,
    val artworkType: String? = null,
    val genre: String? = null,
    val creationYear: Int? = null,
    val creationPeriod: String? = null,
    val medium: String? = null,
    val dimensions: String? = null,
    val museum: String? = null,
    val museumEn: String? = null,
    val museumLocation: String? = null,
    val currentLocation: String? = null,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    val highResUrl: String? = null,
    val description: String? = null,
    val historicalContext: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val wikipediaUrl: String? = null,
    val museumWebsiteUrl: String? = null,
    val viewCount: Int = 0,
    val docentGenerationCount: Int = 0,
    val averageRating: Double? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Artwork list response with pagination
 * Used for GET /api/artworks
 */
@Serializable
data class ArtworkListResponse(
    val artworks: List<ArtworkResponse>,
    val pagination: PaginationInfo
)

/**
 * Artwork search result with relevance score
 * Used for POST /api/artworks/search
 */
@Serializable
data class ArtworkSearchResult(
    val artwork: ArtworkResponse,
    val relevanceScore: Double
)

/**
 * Artwork search results response
 */
@Serializable
data class ArtworkSearchResultsResponse(
    val results: List<ArtworkSearchResult>,
    val query: String,
    val pagination: PaginationInfo
)

/**
 * Voice search response (Gemini API result)
 */
@Serializable
data class VoiceSearchResponse(
    val success: Boolean,
    val matchedArtworks: List<GeminiArtworkMatch>,
    val originalQuery: String,
    val message: String? = null
)

/**
 * Gemini API artwork match result
 */
@Serializable
data class GeminiArtworkMatch(
    val title: String,
    val artist: String,
    val description: String,
    val confidence: Double? = null
)

/**
 * Image search session response (async processing)
 * POST /api/artworks/search/camera returns 202 Accepted with this
 */
@Serializable
data class ImageSearchSessionResponse(
    val sessionId: String,
    val status: String, // PROCESSING, COMPLETED, FAILED
    val pollingUrl: String,
    val message: String
)

/**
 * Image search result (polling response)
 * GET /api/artworks/search/{sessionId}
 */
@Serializable
data class ImageSearchResultResponse(
    val sessionId: String,
    val status: String, // PROCESSING, COMPLETED, FAILED
    val result: ArtworkResponse? = null,
    val ocrText: String? = null,
    val confidence: Double? = null,
    val message: String? = null
)

/**
 * Similar artworks response
 * GET /api/artworks/{artworkId}/similar
 */
@Serializable
data class SimilarArtworksResponse(
    val originalArtwork: ArtworkResponse,
    val similarArtworks: List<ArtworkResponse>
)