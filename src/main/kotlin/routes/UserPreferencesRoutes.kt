package com.kevin.routes

import com.kevin.model.dto.*
import com.kevin.services.UserPreferencesService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * User Preferences Routes
 * Implements user preferences management endpoints
 *
 * Routes:
 * - POST /api/users/{userId}/preferences - Create preferences
 * - GET /api/users/{userId}/preferences - Get preferences
 * - PUT /api/users/{userId}/preferences - Update preferences
 */
fun Route.userPreferencesRoutes() {
    val userPreferencesService = UserPreferencesService()

    route("/api/users/{userId}/preferences") {

        /**
         * POST /api/users/{userId}/preferences
         * Create new user preferences
         *
         * Requires: Authorization header with Bearer token
         * Request Body: CreateUserPreferencesRequest
         * Response: 201 Created with UserPreferencesResponse or 400/404/409/422
         */
        authenticate("auth-jwt") {
            post {
                try {
                    val userId = call.parameters["userId"]?.toIntOrNull()
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_USER_ID",
                                    message = "Invalid user ID"
                                )
                            )
                        )

                    // Verify JWT token user matches the userId in path
                    val principal = call.principal<JWTPrincipal>()
                    val tokenUserId = principal?.payload?.getClaim("userId")?.asInt()

                    if (tokenUserId != userId) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "FORBIDDEN",
                                    message = "You can only create your own preferences"
                                )
                            )
                        )
                    }

                    val request = call.receive<CreateUserPreferencesRequest>()
                    val result = userPreferencesService.createPreferences(userId, request)

                    if (result.success) {
                        call.respond(HttpStatusCode.Created, result)
                    } else {
                        val statusCode = when {
                            result.message?.contains("not found", ignoreCase = true) == true ->
                                HttpStatusCode.NotFound
                            result.message?.contains("already exist", ignoreCase = true) == true ->
                                HttpStatusCode.Conflict
                            result.message?.contains("Invalid", ignoreCase = true) == true ->
                                HttpStatusCode.BadRequest
                            else -> HttpStatusCode.UnprocessableEntity
                        }

                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = when (statusCode) {
                                    HttpStatusCode.NotFound -> "USER_NOT_FOUND"
                                    HttpStatusCode.Conflict -> "PREFERENCES_ALREADY_EXIST"
                                    HttpStatusCode.BadRequest -> "VALIDATION_ERROR"
                                    else -> "CREATION_ERROR"
                                },
                                message = result.message ?: "Failed to create preferences"
                            )
                        )
                        call.respond(statusCode, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Create preferences error", e)
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
         * GET /api/users/{userId}/preferences
         * Get user preferences
         *
         * Requires: Authorization header with Bearer token
         * Response: 200 OK with UserPreferencesResponse or 404
         */
        authenticate("auth-jwt") {
            get {
                try {
                    val userId = call.parameters["userId"]?.toIntOrNull()
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_USER_ID",
                                    message = "Invalid user ID"
                                )
                            )
                        )

                    val result = userPreferencesService.getPreferences(userId)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = if (result.message?.contains("not found", ignoreCase = true) == true)
                                    "PREFERENCES_NOT_FOUND" else "ERROR",
                                message = result.message ?: "Failed to get preferences"
                            )
                        )
                        call.respond(HttpStatusCode.NotFound, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Get preferences error", e)
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
         * PUT /api/users/{userId}/preferences
         * Update user preferences
         *
         * Requires: Authorization header with Bearer token
         * Request Body: UpdateUserPreferencesRequest
         * Response: 200 OK with UserPreferencesResponse or 400/404/422
         */
        authenticate("auth-jwt") {
            put {
                try {
                    val userId = call.parameters["userId"]?.toIntOrNull()
                        ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_USER_ID",
                                    message = "Invalid user ID"
                                )
                            )
                        )

                    // Verify JWT token user matches the userId in path
                    val principal = call.principal<JWTPrincipal>()
                    val tokenUserId = principal?.payload?.getClaim("userId")?.asInt()

                    if (tokenUserId != userId) {
                        return@put call.respond(
                            HttpStatusCode.Forbidden,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "FORBIDDEN",
                                    message = "You can only update your own preferences"
                                )
                            )
                        )
                    }

                    val request = call.receive<UpdateUserPreferencesRequest>()
                    val result = userPreferencesService.updatePreferences(userId, request)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val statusCode = when {
                            result.message?.contains("not found", ignoreCase = true) == true ->
                                HttpStatusCode.NotFound
                            result.message?.contains("Invalid", ignoreCase = true) == true ->
                                HttpStatusCode.BadRequest
                            else -> HttpStatusCode.UnprocessableEntity
                        }

                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = when (statusCode) {
                                    HttpStatusCode.NotFound -> "PREFERENCES_NOT_FOUND"
                                    HttpStatusCode.BadRequest -> "VALIDATION_ERROR"
                                    else -> "UPDATE_ERROR"
                                },
                                message = result.message ?: "Failed to update preferences"
                            )
                        )
                        call.respond(statusCode, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Update preferences error", e)
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
