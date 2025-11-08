package com.kevin.repository

import com.kevin.db.Artworks
import com.kevin.db.ArtworkSearches
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Artwork Repository for database operations
 * Handles CRUD operations for artworks' table
 */
class ArtworkRepository {

    /**
     * Find artwork by ID
     */
    fun findArtworkById(artworkId: Int): ArtworkResponse? {
        return transaction {
            Artworks.selectAll()
                .where { Artworks.id eq artworkId }
                .map { rowToArtwork(it) }
                .singleOrNull()
        }
    }

    /**
     * Find artwork by title and artist (for duplicate checking)
     */
    fun findArtworkByTitleAndArtist(title: String, artist: String): ArtworkResponse? {
        return transaction {
            Artworks.selectAll()
                .where { (Artworks.title eq title) and (Artworks.artist eq artist) }
                .map { rowToArtwork(it) }
                .singleOrNull()
        }
    }

    /**
     * Find artworks with pagination
     */
    fun findArtworks(
        page: Int = 1,
        limit: Int = 20
    ): ArtworkListResponse {
        return transaction {
            // Get total count
            val totalCount = Artworks.selectAll().count()

            // Calculate pagination
            val totalPages = ((totalCount + limit - 1) / limit).toInt()
            val offset = ((page - 1) * limit).toLong()

            // Get paginated results
            val artworks = Artworks.selectAll()
                .orderBy(Artworks.createdAt, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { rowToArtwork(it) }

            ArtworkListResponse(
                artworks = artworks,
                pagination = PaginationInfo(
                    page = page,
                    limit = limit,
                    totalCount = totalCount,
                    totalPages = totalPages,
                    hasNext = page < totalPages,
                    hasPrevious = page > 1
                )
            )
        }
    }

    /**
     * Search artworks using PostgresSQL full-text search
     * Uses tsvector for Korean and English text search
     */
    fun searchArtworks(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): ArtworkSearchResultsResponse {
        return transaction {
            // Build full-text search query
            // Search in title, titleEn, artist, artistEn, description fields
            val searchPattern = "%${query}%"

            val searchQuery = Artworks.selectAll().where {
                (Artworks.title like searchPattern) or
                (Artworks.titleEn like searchPattern) or
                (Artworks.artist like searchPattern) or
                (Artworks.artistEn like searchPattern) or
                (Artworks.description like searchPattern) or
                (Artworks.genre like searchPattern) or
                (Artworks.artworkType like searchPattern)
            }

            // Get total count
            val totalCount = searchQuery.count()

            // Calculate pagination
            val totalPages = ((totalCount + limit - 1) / limit).toInt()
            val offset = ((page - 1) * limit).toLong()

            // Get paginated results
            val results = searchQuery
                .orderBy(Artworks.viewCount, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { row ->
                    val artwork = rowToArtwork(row)
                    // Calculate a simple relevance score based on field match
                    val relevanceScore = calculateRelevanceScore(row, query)
                    ArtworkSearchResult(
                        artwork = artwork,
                        relevanceScore = relevanceScore
                    )
                }
                .sortedByDescending { it.relevanceScore }

            ArtworkSearchResultsResponse(
                results = results,
                query = query,
                pagination = PaginationInfo(
                    page = page,
                    limit = limit,
                    totalCount = totalCount,
                    totalPages = totalPages,
                    hasNext = page < totalPages,
                    hasPrevious = page > 1
                )
            )
        }
    }

    /**
     * Find similar artworks based on artist, genre, and artwork type
     */
    fun findSimilarArtworks(artworkId: Int, limit: Int = 10): List<ArtworkResponse> {
        return transaction {
            val originalArtwork = findArtworkById(artworkId) ?: return@transaction emptyList()

            Artworks.selectAll().where {
                (Artworks.id neq artworkId) and (
                    (Artworks.artist eq originalArtwork.artist) or
                    (Artworks.genre eq (originalArtwork.genre ?: "")) or
                    (Artworks.artworkType eq (originalArtwork.artworkType ?: ""))
                )
            }
            .orderBy(Artworks.viewCount, SortOrder.DESC)
            .limit(limit)
            .map { rowToArtwork(it) }
        }
    }

    /**
     * Increment view count for artwork
     */
    fun incrementViewCount(artworkId: Int): Boolean {
        return transaction {
            Artworks.update({ Artworks.id eq artworkId }) {
                it[viewCount] = viewCount + 1
            } > 0
        }
    }

    /**
     * Create artwork search record
     */
    fun createArtworkSearch(
        userId: Int,
        searchMethod: String,
        searchQuery: String? = null,
        searchQueryVoiceUrl: String? = null,
        capturedImageUrl: String? = null,
        resultsCount: Int = 0,
        selectedArtworkId: Int? = null
    ): Int {
        return transaction {
            val now = Clock.System.now()
            ArtworkSearches.insertAndGetId {
                it[ArtworkSearches.userId] = userId
                it[ArtworkSearches.searchMethod] = searchMethod
                it[ArtworkSearches.searchQuery] = searchQuery
                it[ArtworkSearches.searchQueryVoiceUrl] = searchQueryVoiceUrl
                it[ArtworkSearches.capturedImageUrl] = capturedImageUrl
                it[ArtworkSearches.resultsCount] = resultsCount
                it[ArtworkSearches.selectedArtworkId] = selectedArtworkId
                it[createdAt] = now
            }.value
        }
    }

    /**
     * Create new artwork from Gemini API results
     * Used for caching Gemini search results
     */
    fun createArtwork(
        title: String,
        artist: String,
        description: String?,
        imageUrl: String
    ): Int {
        return transaction {
            val now = Clock.System.now()
            Artworks.insertAndGetId {
                it[Artworks.title] = title
                it[Artworks.artist] = artist
                it[Artworks.description] = description
                it[Artworks.imageUrl] = imageUrl
                it[createdAt] = now
                it[updatedAt] = now
            }.value
        }
    }

    /**
     * Calculate relevance score for search result
     * Higher score = better match
     */
    private fun calculateRelevanceScore(row: ResultRow, query: String): Double {
        val lowerQuery = query.lowercase()
        var score = 0.0

        // Title match (the highest priority)
        val title = row[Artworks.title].lowercase()
        val titleEn = row[Artworks.titleEn]?.lowercase()
        if (title.contains(lowerQuery)) score += 10.0
        if (title == lowerQuery) score += 20.0
        if (titleEn != null && titleEn.contains(lowerQuery)) score += 10.0
        if (titleEn == lowerQuery) score += 20.0

        // Artist match (high priority)
        val artist = row[Artworks.artist].lowercase()
        val artistEn = row[Artworks.artistEn]?.lowercase()
        if (artist.contains(lowerQuery)) score += 8.0
        if (artist == lowerQuery) score += 15.0
        if (artistEn != null && artistEn.contains(lowerQuery)) score += 8.0
        if (artistEn == lowerQuery) score += 15.0

        // Genre and type match (medium priority)
        val genre = row[Artworks.genre]?.lowercase()
        val artworkType = row[Artworks.artworkType]?.lowercase()
        if (genre != null && genre.contains(lowerQuery)) score += 5.0
        if (artworkType != null && artworkType.contains(lowerQuery)) score += 5.0

        // Description match (lower priority)
        val description = row[Artworks.description]?.lowercase()
        if (description != null && description.contains(lowerQuery)) score += 3.0

        // Popularity boost
        val viewCount = row[Artworks.viewCount]
        score += (viewCount * 0.001) // Small boost for popular artworks

        return score
    }

    /**
     * Convert database row to ArtworkResponse
     */
    private fun rowToArtwork(row: ResultRow): ArtworkResponse {
        // Parse metadata JSON
        val metadataJson = row[Artworks.metadata]
        val metadata = try {
            kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(metadataJson)
        } catch (e: Exception) {
            emptyMap()
        }

        return ArtworkResponse(
            id = row[Artworks.id].value,
            title = row[Artworks.title],
            titleEn = row[Artworks.titleEn],
            artist = row[Artworks.artist],
            artistEn = row[Artworks.artistEn],
            artworkType = row[Artworks.artworkType],
            genre = row[Artworks.genre],
            creationYear = row[Artworks.creationYear],
            creationPeriod = row[Artworks.creationPeriod],
            medium = row[Artworks.medium],
            dimensions = row[Artworks.dimensions],
            museum = row[Artworks.museum],
            museumEn = row[Artworks.museumEn],
            museumLocation = row[Artworks.museumLocation],
            currentLocation = row[Artworks.currentLocation],
            imageUrl = row[Artworks.imageUrl],
            thumbnailUrl = row[Artworks.thumbnailUrl],
            highResUrl = row[Artworks.highResUrl],
            description = row[Artworks.description],
            historicalContext = row[Artworks.historicalContext],
            metadata = metadata,
            wikipediaUrl = row[Artworks.wikipediaUrl],
            museumWebsiteUrl = row[Artworks.museumWebsiteUrl],
            viewCount = row[Artworks.viewCount],
            docentGenerationCount = row[Artworks.docentGenerationCount],
            averageRating = row[Artworks.averageRating]?.toDouble(),
            createdAt = row[Artworks.createdAt].toString(),
            updatedAt = row[Artworks.updatedAt].toString()
        )
    }
}