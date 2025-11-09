package com.kevin.routes

import com.kevin.model.dto.SubmitFeedbackRequest
import com.kevin.services.FeedbackService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Feedback Routes
 * API endpoints for docent session feedback
 *
 * Endpoints:
 * - POST /api/docent/sessions/{sessionId}/feedback - Submit feedback (authenticated)
 * - GET /api/docent/sessions/{sessionId}/feedback - Get feedback for session (authenticated)
 * - GET /api/admin/feedbacks - Get all feedbacks (admin only)
 * - PUT /api/admin/feedbacks/{feedbackId}/few-shot - Mark as a few-shot candidate (admin only)
 */
fun Route.feedbackRoutes() {
    val feedbackService = FeedbackService()

    // Authenticated routes
    authenticate("auth-jwt") {

        // ============================================================================
        // POST /api/docent/sessions/{sessionId}/feedback
        // Submit feedback for a docent session
        // ============================================================================
        post("/api/docent/sessions/{sessionId}/feedback") {
            try {
                val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid session ID")
                    )

                // Get user ID from JWT
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Parse request body
                val request = call.receive<SubmitFeedbackRequest>()

                // Submit feedback
                val response = feedbackService.submitFeedback(sessionId, userId, request)

                if (response.success) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to submit feedback: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/docent/sessions/{sessionId}/feedback
        // Get feedback for a specific docent session
        // ============================================================================
        get("/api/docent/sessions/{sessionId}/feedback") {
            try {
                val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid session ID")
                    )

                val response = feedbackService.getFeedbackBySessionId(sessionId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve feedback: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/admin/feedbacks
        // Get all feedbacks with pagination (admin only)
        // TODO: Add admin role check
        // ============================================================================
        get("/api/admin/feedbacks") {
            try {
                // Get pagination parameters
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                val response = feedbackService.getAllFeedbacks(page, limit)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve feedbacks: ${e.message}")
                )
            }
        }

        // ============================================================================
        // PUT /api/admin/feedbacks/{feedbackId}/few-shot
        // Mark feedback as few-shot candidate (admin only)
        // TODO: Add admin role check
        // ============================================================================
        put("/api/admin/feedbacks/{feedbackId}/few-shot") {
            try {
                val feedbackId = call.parameters["feedbackId"]?.toIntOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid feedback ID")
                    )

                val response = feedbackService.markAsFewShot(feedbackId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to mark as few-shot: ${e.message}")
                )
            }
        }
    }
}
