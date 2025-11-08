package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.ArtworkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Artwork Service
 * Handles business logic for artwork management
 *
 * Flow:
 * 1. DB 검색 먼저 시도
 * 2. DB에 결과가 없으면 Gemini API로 폴백
 * 3. Gemini 결과를 클라이언트에 반환하고 비동기로 DB에 저장
 */
class ArtworkService(
    private val artworkRepository: ArtworkRepository = ArtworkRepository(),
    private val artworkSearchService: ArtworkSearchService = ArtworkSearchService(artworkRepository)
) {

    /**
     * Get an artwork list with pagination
     *
     * @param page Page number (default: 1)
     * @param limit Items per page (default: 20)
     * @return ApiResponse with ArtworkListResponse or error
     */
    fun getArtworks(
        page: Int = 1,
        limit: Int = 20
    ): ApiResponse<ArtworkListResponse> {
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

        // Get artworks
        val result = artworkRepository.findArtworks(page = page, limit = limit)

        return ApiResponse(
            success = true,
            data = result,
            message = "Artworks retrieved successfully"
        )
    }

    /**
     * Get specific artwork by ID
     *
     * @param artworkId Artwork ID
     * @return ApiResponse with ArtworkResponse or error
     */
    fun getArtwork(artworkId: Int): ApiResponse<ArtworkResponse> {
        // Find artwork
        val artwork = artworkRepository.findArtworkById(artworkId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Artwork not found"
            )

        // Increment view count
        artworkRepository.incrementViewCount(artworkId)

        return ApiResponse(
            success = true,
            data = artwork,
            message = "Artwork retrieved successfully"
        )
    }

    /**
     * Search artworks by text query with Gemini API fallback
     *
     * Flow:
     * 1. Search DB first
     * 2. If no results, call Gemini API
     * 3. Return Gemini results to a client immediately
     * 4. Save Gemini results to DB asynchronously
     *
     * @param request ArtworkTextSearchRequest
     * @return ApiResponse with ArtworkSearchResultsResponse or error
     */
    fun searchArtworks(request: ArtworkTextSearchRequest): ApiResponse<ArtworkSearchResultsResponse> {
        // Validate search query
        if (request.query.isBlank()) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Search query cannot be empty"
            )
        }

        // Validate pagination parameters
        if (request.page < 1) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Page must be greater than 0"
            )
        }

        if (request.limit !in 1..100) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Limit must be between 1 and 100"
            )
        }

        // Step 1: Search in DB first
        val dbResult = artworkRepository.searchArtworks(
            query = request.query,
            page = request.page,
            limit = request.limit
        )

        // If DB has results, return them immediately
        if (dbResult.results.isNotEmpty()) {
            return ApiResponse(
                success = true,
                data = dbResult,
                message = "Search completed successfully (from database)"
            )
        }

        // Step 2: No DB results, fallback to Gemini API
        return runBlocking {
            try {
                val geminiMatches = artworkSearchService.searchByGemini(request.query)

                if (geminiMatches.isEmpty()) {
                    // Even Gemini found nothing
                    return@runBlocking ApiResponse(
                        success = true,
                        data = ArtworkSearchResultsResponse(
                            query = request.query,
                            results = emptyList(),
                            pagination = PaginationInfo(
                                page = 1,
                                limit = request.limit,
                                totalCount = 0,
                                totalPages = 0,
                                hasNext = false,
                                hasPrevious = false
                            )
                        ),
                        message = "No artworks found matching your query"
                    )
                }

                // Step 3: Convert Gemini results to the search results format
                val searchResults = geminiMatches.map { match ->
                    ArtworkSearchResult(
                        artwork = ArtworkResponse(
                            id = 0, // Temporary ID (will be assigned when saved to DB)
                            title = match.title,
                            titleEn = null,
                            artist = match.artist,
                            artistEn = null,
                            artworkType = null,
                            genre = null,
                            creationYear = null,
                            creationPeriod = null,
                            medium = null,
                            dimensions = null,
                            museum = null,
                            museumEn = null,
                            museumLocation = null,
                            currentLocation = null,
                            imageUrl = "",
                            thumbnailUrl = null,
                            highResUrl = null,
                            description = match.description,
                            historicalContext = null,
                            metadata = emptyMap(),
                            wikipediaUrl = null,
                            museumWebsiteUrl = null,
                            viewCount = 0,
                            docentGenerationCount = 0,
                            averageRating = null,
                            createdAt = "",
                            updatedAt = ""
                        ),
                        relevanceScore = match.confidence?.times(100) ?: 0.0
                    )
                }

                val response = ArtworkSearchResultsResponse(
                    query = request.query,
                    results = searchResults,
                    pagination = PaginationInfo(
                        page = 1,
                        limit = searchResults.size,
                        totalCount = searchResults.size.toLong(),
                        totalPages = 1,
                        hasNext = false,
                        hasPrevious = false
                    )
                )

                // Step 4: Save Gemini results to DB synchronously for reliability
                val savedCount = saveGeminiResultsToDB(geminiMatches)

                ApiResponse(
                    success = true,
                    data = response,
                    message = "Search completed successfully (from Gemini AI - results will be cached)"
                )
            } catch (e: Exception) {
                ApiResponse(
                    success = false,
                    data = ArtworkSearchResultsResponse(
                        query = request.query,
                        results = emptyList(),
                        pagination = PaginationInfo(
                            page = request.page,
                            limit = request.limit,
                            totalCount = 0,
                            totalPages = 0,
                            hasNext = false,
                            hasPrevious = false
                        )
                    ),
                    message = "Search failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Save Gemini results to DB with duplicate checking
     * @return Number of artworks successfully saved
     */
    private fun saveGeminiResultsToDB(geminiMatches: List<GeminiArtworkMatch>): Int {
        var savedCount = 0
        try {
            geminiMatches.forEach { match ->
                // Check if artwork already exists (by title and artist)
                val existing = artworkRepository.findArtworkByTitleAndArtist(match.title, match.artist)

                if (existing == null) {
                    // Only save if it doesn't exist
                    artworkRepository.createArtwork(
                        title = match.title,
                        artist = match.artist,
                        description = match.description,
                        imageUrl = "" // Gemini doesn't provide image URL
                    )
                    savedCount++
                    println("✓ Saved artwork: ${match.title} by ${match.artist}")
                } else {
                    println("⊙ Artwork already exists: ${match.title} by ${match.artist} (ID: ${existing.id})")
                }
            }
            println("✓ Saved $savedCount new artworks to DB (${geminiMatches.size - savedCount} duplicates skipped)")
            return savedCount
        } catch (e: Exception) {
            println("✗ Failed to save Gemini results to DB: ${e.message}")
            e.printStackTrace()
            throw e // Re-throw to make the error visible
        }
    }

    /**
     * Get similar artworks
     *
     * @param artworkId Original artwork ID
     * @param limit Maximum number of similar artworks to return
     * @return ApiResponse with SimilarArtworksResponse or error
     */
    fun getSimilarArtworks(artworkId: Int, limit: Int = 10): ApiResponse<SimilarArtworksResponse> {
        // Validate limit
        if (limit !in 1..50) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Limit must be between 1 and 50"
            )
        }

        // Find original artwork
        val originalArtwork = artworkRepository.findArtworkById(artworkId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Artwork not found"
            )

        // Find similar artworks
        val similarArtworks = artworkRepository.findSimilarArtworks(artworkId, limit)

        return ApiResponse(
            success = true,
            data = SimilarArtworksResponse(
                originalArtwork = originalArtwork,
                similarArtworks = similarArtworks
            ),
            message = "Similar artworks retrieved successfully"
        )
    }
}