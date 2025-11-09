package com.kevin.routes

import com.kevin.model.dto.InviteCompanionRequest
import com.kevin.model.dto.UpdateCompanionPermissionsRequest
import com.kevin.services.CompanionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Companion Routes
 * API endpoints for companion management
 *
 * Endpoints:
 * - POST /api/companions/invite - Invite a companion (main user)
 * - GET /api/companions - Get companions list (main user)
 * - GET /api/companions/invitations - Get pending invitations (companion user)
 * - PUT /api/companions/{companionId}/permissions - Update permissions (main user)
 * - PUT /api/companions/{companionId}/accept - Accept invitation (companion user)
 * - PUT /api/companions/{companionId}/reject - Reject invitation (companion user)
 * - DELETE /api/companions/{companionId} - Remove companion (main user)
 * - GET /api/companions/statistics - Get companion statistics (main user)
 * - POST /api/companions/invite-code - Generate invite code (main user)
 */
fun Route.companionRoutes() {
    val companionService = CompanionService()

    // Authenticated routes
    authenticate("auth-jwt") {

        // ============================================================================
        // POST /api/companions/invite
        // Invite a companion via email
        // ============================================================================
        post("/api/companions/invite") {
            try {
                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val mainUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Parse request body
                val request = call.receive<InviteCompanionRequest>()

                // Invite companion
                val response = companionService.inviteCompanion(mainUserId, request)

                if (response.success) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to invite companion: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/companions
        // Get companions list for the authenticated main user
        // Query parameters:
        // - page: Page number (default: 1)
        // - limit: Items per page (default: 20, max: 100)
        // - status: Filter by status (PENDING | ACTIVE | INACTIVE) (optional)
        // ============================================================================
        get("/api/companions") {
            try {
                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val mainUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Get pagination and filter parameters
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val status = call.request.queryParameters["status"]

                // Get companions
                val response = companionService.getCompanions(
                    mainUserId = mainUserId,
                    page = page,
                    limit = limit,
                    status = status
                )

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve companions: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/companions/invitations
        // Get pending invitations for the authenticated companion user
        // ============================================================================
        get("/api/companions/invitations") {
            try {
                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val companionUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Get pending invitations
                val response = companionService.getPendingInvitations(companionUserId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve invitations: ${e.message}")
                )
            }
        }

        // ============================================================================
        // PUT /api/companions/{companionId}/permissions
        // Update companion permissions (main user only)
        // ============================================================================
        put("/api/companions/{companionId}/permissions") {
            try {
                val companionId = call.parameters["companionId"]?.toIntOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid companion ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val mainUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Parse request body
                val request = call.receive<UpdateCompanionPermissionsRequest>()

                // Update permissions
                val response = companionService.updateCompanionPermissions(
                    mainUserId = mainUserId,
                    companionId = companionId,
                    request = request
                )

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to update permissions: ${e.message}")
                )
            }
        }

        // ============================================================================
        // PUT /api/companions/{companionId}/accept
        // Accept companion invitation (companion user only)
        // ============================================================================
        put("/api/companions/{companionId}/accept") {
            try {
                val companionId = call.parameters["companionId"]?.toIntOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid companion ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val companionUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Accept invitation
                val response = companionService.acceptInvitation(companionUserId, companionId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to accept invitation: ${e.message}")
                )
            }
        }

        // ============================================================================
        // PUT /api/companions/{companionId}/reject
        // Reject companion invitation (companion user only)
        // ============================================================================
        put("/api/companions/{companionId}/reject") {
            try {
                val companionId = call.parameters["companionId"]?.toIntOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid companion ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val companionUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Reject invitation
                val response = companionService.rejectInvitation(companionUserId, companionId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to reject invitation: ${e.message}")
                )
            }
        }

        // ============================================================================
        // DELETE /api/companions/{companionId}
        // Remove companion relationship (main user only)
        // ============================================================================
        delete("/api/companions/{companionId}") {
            try {
                val companionId = call.parameters["companionId"]?.toIntOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid companion ID")
                    )

                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val mainUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Remove companion
                val response = companionService.removeCompanion(mainUserId, companionId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to remove companion: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/companions/statistics
        // Get companion statistics for the authenticated main user
        // ============================================================================
        get("/api/companions/statistics") {
            try {
                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val mainUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Get statistics
                val response = companionService.getCompanionStatistics(mainUserId)

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

        // ============================================================================
        // POST /api/companions/invite-code
        // Generate an invite code for the authenticated main user
        // ============================================================================
        post("/api/companions/invite-code") {
            try {
                // Get authenticated user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val mainUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Generate invite code
                val response = companionService.generateInviteCode(mainUserId)

                if (response.success) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to generate invite code: ${e.message}")
                )
            }
        }
    }
}