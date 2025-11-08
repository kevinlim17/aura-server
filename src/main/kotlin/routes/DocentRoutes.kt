package com.kevin.routes

import com.kevin.model.dto.*
import com.kevin.services.DocentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Docent Routes
 * Implements AI docent generation and management endpoints
 *
 * Routes:
 * - POST /api/docent/generate - Generate docent (async)
 * - GET /api/docent/sessions/{sessionId} - Get docent session
 * - PUT /api/docent/sessions/{sessionId}/play-stats - Update play statistics
 * - GET /api/users/me/docent/history - Get user docent history
 */
fun Route.docentRoutes() {
    val docentService = DocentService()

    route("/api/docent") {

        /**
         * POST /api/docent/generate
         * Generate AI docent for artwork (async processing)
         *
         * Requires: Authorization header with Bearer token
         * Request Body: GenerateDocentRequest
         * Response: 202 Accepted with DocentGenerationSessionResponse
         */
        authenticate("auth-jwt") {
            post("/generate") {
                try {
                    val request = call.receive<GenerateDocentRequest>()

                    // Verify JWT token user matches the userId in request
                    val principal = call.principal<JWTPrincipal>()
                    val tokenUserId = principal?.payload?.getClaim("userId")?.asInt()

                    if (tokenUserId != request.userId) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "FORBIDDEN",
                                    message = "You can only generate docents for your own account"
                                )
                            )
                        )
                    }

                    // Validate request
                    if (request.artworkId <= 0) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "VALIDATION_ERROR",
                                    message = "Invalid artwork ID"
                                )
                            )
                        )
                    }

                    // Start async generation
                    val result = docentService.generateDocentAsync(request)

                    call.respond(HttpStatusCode.Accepted, result)
                } catch (e: Exception) {
                    call.application.log.error("Generate docent error", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "INTERNAL_ERROR",
                                message = "An internal error occurred: ${e.message}"
                            )
                        )
                    )
                }
            }
        }

        /**
         * GET /api/docent/sessions/{sessionId}
         * Get docent session by ID (polling endpoint for async generation)
         *
         * Response: 200 OK with DocentGenerationResultResponse or 404
         */
        get("/sessions/{sessionId}") {
            try {
                val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "INVALID_SESSION_ID",
                                message = "Invalid session ID"
                            )
                        )
                    )

                val result = docentService.getDocentGenerationResult(sessionId)

                if (result.status == "NOT_FOUND") {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "SESSION_NOT_FOUND",
                                message = "Docent session not found"
                            )
                        )
                    )
                }

                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.application.log.error("Get docent session error", e)
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
         * PUT /api/docent/sessions/{sessionId}/play-stats
         * Update play statistics for a docent session
         *
         * Requires: Authorization header with Bearer token
         * Request Body: UpdatePlayStatsRequest
         * Response: 200 OK with updated DocentSessionResponse or 404
         */
        authenticate("auth-jwt") {
            put("/sessions/{sessionId}/play-stats") {
                try {
                    val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                        ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_SESSION_ID",
                                    message = "Invalid session ID"
                                )
                            )
                        )

                    val request = call.receive<UpdatePlayStatsRequest>()

                    // Validate request
                    if (request.playCount != null && request.playCount < 0) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "VALIDATION_ERROR",
                                    message = "Play count cannot be negative"
                                )
                            )
                        )
                    }

                    if (request.totalListeningSeconds != null && request.totalListeningSeconds < 0) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "VALIDATION_ERROR",
                                    message = "Total listening seconds cannot be negative"
                                )
                            )
                        )
                    }

                    if (request.completionRate != null && (request.completionRate < 0 || request.completionRate > 1)) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "VALIDATION_ERROR",
                                    message = "Completion rate must be between 0 and 1"
                                )
                            )
                        )
                    }

                    val result = docentService.updatePlayStats(sessionId, request)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = "UPDATE_ERROR",
                                message = result.message ?: "Failed to update play stats"
                            )
                        )
                        call.respond(HttpStatusCode.NotFound, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Update play stats error", e)
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

    /**
     * GET /api/users/me/docent/history
     * Get user's docent generation history
     *
     * Requires: Authorization header with Bearer token
     * Query Parameters:
     *   - page: Page number (default: 1)
     *   - limit: Items per page (default: 20, max: 100)
     * Response: 200 OK with DocentHistoryResponse
     */
    route("/api/users/me/docent") {
        authenticate("auth-jwt") {
            get("/history") {
                try {
                    // Get user ID from JWT token
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asInt()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "UNAUTHORIZED",
                                    message = "Invalid or missing authentication token"
                                )
                            )
                        )

                    // Parse query parameters
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                    val result = docentService.getUserDocentHistory(
                        userId = userId,
                        page = page,
                        limit = limit
                    )

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = "VALIDATION_ERROR",
                                message = result.message ?: "Failed to get docent history"
                            )
                        )
                        call.respond(HttpStatusCode.BadRequest, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Get docent history error", e)
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
}