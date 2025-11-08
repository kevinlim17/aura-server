package com.kevin.routes

import com.kevin.model.dto.*
import com.kevin.services.ArtworkService
import com.kevin.services.ArtworkSearchService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Artwork Routes
 * Implements artwork search and retrieval endpoints
 *
 * Routes:
 * - GET /api/artworks - Get an artwork list with pagination
 * - GET /api/artworks/{artworkId} - Get specific artwork
 * - POST /api/artworks/search - Text-based artwork search
 * - POST /api/artworks/search/voice - Voice-based artwork search
 * - POST /api/artworks/search/camera - Image-based artwork search (async)
 * - GET /api/artworks/search/{sessionId} - Get an image search result (polling)
 * - GET /api/artworks/{artworkId}/similar - Get similar artworks
 */
fun Route.artworkRoutes() {
    val artworkService = ArtworkService()
    val artworkSearchService = ArtworkSearchService()

    route("/api/artworks") {

        /**
         * GET /api/artworks
         * Get artwork list with pagination
         *
         * Query Parameters:
         *   - page: Page number (default: 1)
         *   - limit: Items per page (default: 20, max: 100)
         * Response: 200 OK with ArtworkListResponse or 400
         */
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                val result = artworkService.getArtworks(page = page, limit = limit)

                if (result.success) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    val errorResponse = ApiErrorResponse(
                        error = ErrorDetail(
                            code = "VALIDATION_ERROR",
                            message = result.message ?: "Failed to get artworks"
                        )
                    )
                    call.respond(HttpStatusCode.BadRequest, errorResponse)
                }
            } catch (e: Exception) {
                call.application.log.error("Get artworks error", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiErrorResponse(
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "An internal error occurred"
                        )
                    )
                )
            }
        }

        /**
         * GET /api/artworks/{artworkId}
         * Get specific artwork by ID
         *
         * Response: 200 OK with ArtworkResponse or 404
         */
        get("/{artworkId}") {
            try {
                val artworkId = call.parameters["artworkId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "INVALID_ARTWORK_ID",
                                message = "Invalid artwork ID"
                            )
                        )
                    )

                val result = artworkService.getArtwork(artworkId)

                if (result.success) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    val errorResponse = ApiErrorResponse(
                        error = ErrorDetail(
                            code = "ARTWORK_NOT_FOUND",
                            message = result.message ?: "Artwork not found"
                        )
                    )
                    call.respond(HttpStatusCode.NotFound, errorResponse)
                }
            } catch (e: Exception) {
                call.application.log.error("Get artwork error", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiErrorResponse(
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "An internal error occurred"
                        )
                    )
                )
            }
        }

        /**
         * GET /api/artworks/{artworkId}/similar
         * Get similar artworks based on artist, genre, and type
         *
         * Query Parameters:
         *   - limit: Maximum number of similar artworks (default: 10, max: 50)
         * Response: 200 OK with SimilarArtworksResponse or 404
         */
        get("/{artworkId}/similar") {
            try {
                val artworkId = call.parameters["artworkId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "INVALID_ARTWORK_ID",
                                message = "Invalid artwork ID"
                            )
                        )
                    )

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

                val result = artworkService.getSimilarArtworks(artworkId, limit)

                if (result.success) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    val statusCode = when {
                        result.message?.contains("not found", ignoreCase = true) == true ->
                            HttpStatusCode.NotFound
                        else -> HttpStatusCode.BadRequest
                    }

                    val errorResponse = ApiErrorResponse(
                        error = ErrorDetail(
                            code = when (statusCode) {
                                HttpStatusCode.NotFound -> "ARTWORK_NOT_FOUND"
                                else -> "VALIDATION_ERROR"
                            },
                            message = result.message ?: "Failed to get similar artworks"
                        )
                    )
                    call.respond(statusCode, errorResponse)
                }
            } catch (e: Exception) {
                call.application.log.error("Get similar artworks error", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiErrorResponse(
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "An internal error occurred"
                        )
                    )
                )
            }
        }

        /**
         * POST /api/artworks/search
         * Text-based artwork search with full-text search
         *
         * Request Body: ArtworkTextSearchRequest
         * Response: 200 OK with ArtworkSearchResultsResponse or 400
         */
        post("/search") {
            try {
                val request = call.receive<ArtworkTextSearchRequest>()
                val result = artworkService.searchArtworks(request)

                if (result.success) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    val errorResponse = ApiErrorResponse(
                        error = ErrorDetail(
                            code = "VALIDATION_ERROR",
                            message = result.message ?: "Search failed"
                        )
                    )
                    call.respond(HttpStatusCode.BadRequest, errorResponse)
                }
            } catch (e: Exception) {
                call.application.log.error("Text search error", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiErrorResponse(
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "An internal error occurred"
                        )
                    )
                )
            }
        }

        /**
         * POST /api/artworks/search/voice
         * Voice-based artwork search using Google Gemini API
         * Frontend sends STT-completed text
         *
         * Requires: Authorization header with Bearer token
         * Request Body: ArtworkVoiceSearchRequest
         * Response: 200 OK with VoiceSearchResponse or 400
         */
        authenticate("auth-jwt") {
            post("/search/voice") {
                try {
                    val request = call.receive<ArtworkVoiceSearchRequest>()

                    // Validate request
                    if (request.transcribedText.isBlank()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "VALIDATION_ERROR",
                                    message = "Transcribed text cannot be empty"
                                )
                            )
                        )
                    }

                    // Call search service
                    val result = artworkSearchService.searchByVoice(request)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = "VOICE_SEARCH_ERROR",
                                message = result.message ?: "Voice search failed"
                            )
                        )
                        call.respond(HttpStatusCode.InternalServerError, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Voice search error", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "INTERNAL_ERROR",
                                message = "An internal error occurred"
                            )
                        )
                    )
                }
            }
        }

        /**
         * POST /api/artworks/search/camera
         * Image-based artwork search using Google Vision AI API
         * Returns 202 Accepted with session ID for async processing
         *
         * Requires: Authorization header with Bearer token
         * Request: multipart/form-data with an image file and userId
         * Response: 202 Accepted with ImageSearchSessionResponse
         */
        authenticate("auth-jwt") {
            post("/search/camera") {
                try {
                    val multipart = call.receiveMultipart()
                    var imageBytes: ByteArray? = null
                    var userId: Int? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                if (part.name == "image") {
                                    imageBytes = withContext(Dispatchers.IO) {
                                        part.provider().readByteArray(1)
                                    }
                                }
                            }
                            is PartData.FormItem -> {
                                if (part.name == "userId") {
                                    userId = part.value.toIntOrNull()
                                }
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    // Validate inputs
                    if (imageBytes == null) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "VALIDATION_ERROR",
                                    message = "Image file is required"
                                )
                            )
                        )
                    }

                    if (userId == null) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "VALIDATION_ERROR",
                                    message = "User ID is required"
                                )
                            )
                        )
                    }

                    // Start async search
                    val result = artworkSearchService.searchByImage(imageBytes, userId!!)
                    call.respond(HttpStatusCode.Accepted, result)

                } catch (e: Exception) {
                    call.application.log.error("Image search error", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "INTERNAL_ERROR",
                                message = "An internal error occurred"
                            )
                        )
                    )
                }
            }
        }

        /**
         * GET /api/artworks/search/{sessionId}
         * Get an image search result by session ID (polling endpoint)
         *
         * Response: 200 OK with ImageSearchResultResponse or 404
         */
        get("/search/{sessionId}") {
            try {
                val sessionId = call.parameters["sessionId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "INVALID_SESSION_ID",
                                message = "Invalid session ID"
                            )
                        )
                    )

                val result = artworkSearchService.getImageSearchResult(sessionId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "SESSION_NOT_FOUND",
                                message = "Search session not found"
                            )
                        )
                    )

                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.application.log.error("Get image search result error", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiErrorResponse(
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "An internal error occurred"
                        )
                    )
                )
            }
        }
    }
}