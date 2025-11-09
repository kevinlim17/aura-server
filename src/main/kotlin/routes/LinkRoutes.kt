package com.kevin.routes

import com.kevin.model.dto.AddLinkRequest
import com.kevin.services.LinkService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Link Routes
 * API endpoints for user links management
 *
 * Endpoints:
 * - POST /api/users/{userId}/links - Add a new link (authenticated)
 * - GET /api/users/{userId}/links - Get user's links with pagination and filters (authenticated)
 * - DELETE /api/users/{userId}/links/{linkId} - Delete a link (authenticated)
 */
fun Route.linkRoutes() {
    val linkService = LinkService()

    // Authenticated routes
    authenticate("auth-jwt") {

        // ============================================================================
        // POST /api/users/{userId}/links
        // Add a new link to user's collection
        // ============================================================================
        post("/api/users/{userId}/links") {
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

                // Verify user can only add links to their own account
                if (userId != authenticatedUserId) {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You can only add links to your own account")
                    )
                }

                // Parse request body
                val request = call.receive<AddLinkRequest>()

                // Add link
                val response = linkService.addLink(userId, request)

                if (response.success) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to add link: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/users/{userId}/links
        // Get user's links with pagination and optional filters
        // Query parameters:
        // - page: Page number (default: 1)
        // - limit: Items per page (default: 20, max: 100)
        // - artworkId: Filter by artwork ID (optional)
        // - linkType: Filter by link type (optional)
        // ============================================================================
        get("/api/users/{userId}/links") {
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

                // Verify user can only access their own links
                if (userId != authenticatedUserId) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You can only access your own links")
                    )
                }

                // Get pagination and filter parameters
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val artworkId = call.request.queryParameters["artworkId"]?.toIntOrNull()
                val linkType = call.request.queryParameters["linkType"]

                // Get links
                val response = linkService.getLinks(
                    userId = userId,
                    page = page,
                    limit = limit,
                    artworkId = artworkId,
                    linkType = linkType
                )

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve links: ${e.message}")
                )
            }
        }

        // ============================================================================
        // DELETE /api/users/{userId}/links/{linkId}
        // Delete a link from user's collection
        // ============================================================================
        delete("/api/users/{userId}/links/{linkId}") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid user ID")
                    )

                val linkId = call.parameters["linkId"]?.toIntOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid link ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val authenticatedUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Verify user can only delete their own links
                if (userId != authenticatedUserId) {
                    return@delete call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "You can only delete your own links")
                    )
                }

                // Delete link
                val response = linkService.deleteLink(userId, linkId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to delete link: ${e.message}")
                )
            }
        }
    }
}
