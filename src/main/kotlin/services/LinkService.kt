package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.LinkRepository
import java.net.URL

/**
 * Link Service
 * Handles business logic for user links management
 *
 * Features:
 * - Add, retrieve, and delete links
 * - URL validation
 * - Open Graph metadata extraction (optional)
 * - Filtering by artwork and link type
 * - Pagination support
 */
class LinkService(
    private val linkRepository: LinkRepository = LinkRepository()
) {

    /**
     * Add a new link
     *
     * @param userId User ID
     * @param request Link creation request
     * @return ApiResponse with LinkResponse or error
     */
    fun addLink(userId: Int, request: AddLinkRequest): ApiResponse<LinkResponse> {
        // Validate URL
        if (request.url.isBlank()) {
            return ApiResponse(
                success = false,
                data = null,
                message = "URL cannot be empty"
            )
        }

        // Validate URL format
        if (!isValidUrl(request.url)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid URL format"
            )
        }

        // Validate linkType if provided
        if (request.linkType != null) {
            try {
                LinkType.valueOf(request.linkType)
            } catch (e: IllegalArgumentException) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Invalid linkType. Must be one of: ARTICLE, VIDEO, AUDIO, REFERENCE, INSPIRATION, OTHER"
                )
            }
        }

        try {
            // TODO: Optionally fetch Open Graph metadata if not provided
            // val metadata = request.metadata ?: fetchOpenGraphMetadata(request.url)

            // Create link
            val linkId = linkRepository.createLink(
                userId = userId,
                url = request.url,
                title = request.title,
                description = request.description,
                artworkId = request.artworkId,
                docentSessionId = request.docentSessionId,
                linkType = request.linkType,
                thumbnailUrl = request.thumbnailUrl,
                hasAudioDescription = request.hasAudioDescription,
                hasSubtitles = request.hasSubtitles,
                metadata = request.metadata
            )

            // Retrieve created link
            val link = linkRepository.findLinkById(linkId)
                ?: return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to retrieve created link"
                )

            return ApiResponse(
                success = true,
                data = link,
                message = "Link added successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to add link: ${e.message}"
            )
        }
    }

    /**
     * Get links for a user with pagination and filters
     *
     * @param userId User ID
     * @param page Page number
     * @param limit Items per page
     * @param artworkId Filter by artwork ID (optional)
     * @param linkType Filter by link type (optional)
     * @return ApiResponse with LinkListResponse or error
     */
    fun getLinks(
        userId: Int,
        page: Int = 1,
        limit: Int = 20,
        artworkId: Int? = null,
        linkType: String? = null
    ): ApiResponse<LinkListResponse> {
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

        // Validate linkType if provided
        if (linkType != null) {
            try {
                LinkType.valueOf(linkType)
            } catch (e: IllegalArgumentException) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Invalid linkType. Must be one of: ARTICLE, VIDEO, AUDIO, REFERENCE, INSPIRATION, OTHER"
                )
            }
        }

        try {
            // Get links with filters
            val (links, totalCount) = linkRepository.findLinksByUserId(
                userId = userId,
                page = page,
                limit = limit,
                artworkId = artworkId,
                linkType = linkType
            )

            // Calculate pagination info
            val totalPages = ((totalCount + limit - 1) / limit).toInt()

            val response = LinkListResponse(
                links = links,
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
                message = "Links retrieved successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to retrieve links: ${e.message}"
            )
        }
    }

    /**
     * Delete a link
     *
     * @param userId User ID
     * @param linkId Link ID
     * @return ApiResponse with success status
     */
    fun deleteLink(userId: Int, linkId: Int): ApiResponse<Unit> {
        // Check if link exists and belongs to user
        val exists = linkRepository.isLinkOwnedByUser(linkId, userId)
        if (!exists) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Link not found or access denied"
            )
        }

        try {
            val deleted = linkRepository.deleteLink(linkId, userId)
            if (!deleted) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to delete link"
                )
            }

            return ApiResponse(
                success = true,
                data = Unit,
                message = "Link deleted successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to delete link: ${e.message}"
            )
        }
    }

    /**
     * Validate URL format
     */
    private fun isValidUrl(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            // Check if protocol is http or https
            url.protocol in listOf("http", "https")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetch Open Graph metadata from URL
     * TODO: Implement Open Graph metadata extraction
     *
     * This would require:
     * 1. HTTP client to fetch the URL
     * 2. HTML parser to extract meta tags
     * 3. Error handling for unreachable URLs
     *
     * Example implementation with Ktor client and JSoup:
     * ```
     * suspend fun fetchOpenGraphMetadata(url: String): Map<String, String> {
     *     val client = HttpClient()
     *     val html = client.get(url).bodyAsText()
     *     val doc = Jsoup.parse(html)
     *
     *     return mapOf(
     *         "ogTitle" to doc.select("meta[property=og:title]").attr("content"),
     *         "ogDescription" to doc.select("meta[property=og:description]").attr("content"),
     *         "ogImage" to doc.select("meta[property=og:image]").attr("content"),
     *         "siteName" to doc.select("meta[property=og:site_name]").attr("content")
     *     )
     * }
     * ```
     */
}
