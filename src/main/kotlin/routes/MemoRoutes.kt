package com.kevin.routes

import com.kevin.model.dto.CreateMemoRequest
import com.kevin.model.dto.UpdateMemoRequest
import com.kevin.services.MemoService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Memo Routes
 * API endpoints for user memos management
 *
 * Endpoints:
 * - POST /api/users/{userId}/memos - Create a new memo (authenticated)
 * - GET /api/users/{userId}/memos - Get user's memos with pagination and filters (authenticated)
 * - PUT /api/users/{userId}/memos/{memoId} - Update a memo (authenticated)
 * - DELETE /api/users/{userId}/memos/{memoId} - Delete a memo (authenticated)
 * - GET /api/users/{userId}/memos/statistics - Get memo statistics (authenticated)
 */
fun Route.memoRoutes() {
    val memoService = MemoService()

    // Authenticated routes
    authenticate("auth-jwt") {

        // ============================================================================
        // POST /api/users/{userId}/memos
        // Create a new memo
        // ============================================================================
        post("/api/users/{userId}/memos") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid user ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val authenticatedUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Verify user can only create memos for their own account
                if (userId != authenticatedUserId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You can only create memos for your own account")
                    )
                }

                // Parse request body
                val request = call.receive<CreateMemoRequest>()

                // Create memo
                val response = memoService.createMemo(userId, request)

                if (response.success) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to create memo: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/users/{userId}/memos
        // Get user's memos with pagination and optional filters
        // Query parameters:
        // - page: Page number (default: 1)
        // - limit: Items per page (default: 20, max: 100)
        // - artworkId: Filter by artwork ID (optional)
        // - category: Filter by category (optional)
        // - inputMethod: Filter by input method (TEXT | VOICE) (optional)
        // - search: Search in memo content (optional)
        // ============================================================================
        get("/api/users/{userId}/memos") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid user ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val authenticatedUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Verify user can only access their own memos
                if (userId != authenticatedUserId) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You can only access your own memos")
                    )
                }

                // Get pagination and filter parameters
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val artworkId = call.request.queryParameters["artworkId"]?.toIntOrNull()
                val category = call.request.queryParameters["category"]
                val inputMethod = call.request.queryParameters["inputMethod"]
                val searchQuery = call.request.queryParameters["search"]

                // Get memos
                val response = memoService.getMemos(
                    userId = userId,
                    page = page,
                    limit = limit,
                    artworkId = artworkId,
                    category = category,
                    inputMethod = inputMethod,
                    searchQuery = searchQuery
                )

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve memos: ${e.message}")
                )
            }
        }

        // ============================================================================
        // PUT /api/users/{userId}/memos/{memoId}
        // Update a memo
        // ============================================================================
        put("/api/users/{userId}/memos/{memoId}") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid user ID")
                    )

                val memoId = call.parameters["memoId"]?.toIntOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid memo ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val authenticatedUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Verify user can only update their own memos
                if (userId != authenticatedUserId) {
                    return@put call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You can only update your own memos")
                    )
                }

                // Parse request body
                val request = call.receive<UpdateMemoRequest>()

                // Update memo
                val response = memoService.updateMemo(userId, memoId, request)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to update memo: ${e.message}")
                )
            }
        }

        // ============================================================================
        // DELETE /api/users/{userId}/memos/{memoId}
        // Delete a memo
        // ============================================================================
        delete("/api/users/{userId}/memos/{memoId}") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid user ID")
                    )

                val memoId = call.parameters["memoId"]?.toIntOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid memo ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val authenticatedUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Verify user can only delete their own memos
                if (userId != authenticatedUserId) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You can only delete your own memos")
                    )
                }

                // Delete memo
                val response = memoService.deleteMemo(userId, memoId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to delete memo: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/users/{userId}/memos/statistics
        // Get memo statistics for a user
        // ============================================================================
        get("/api/users/{userId}/memos/statistics") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid user ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val authenticatedUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Verify user can only access their own statistics
                if (userId != authenticatedUserId) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You can only access your own statistics")
                    )
                }

                // Get statistics
                val response = memoService.getMemoStatistics(userId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve statistics: ${e.message}")
                )
            }
        }
    }
}