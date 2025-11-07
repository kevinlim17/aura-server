package com.kevin.routes

import com.kevin.model.dto.*
import com.kevin.services.UserProfileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * User Profile Routes
 * Implements user profile management endpoints
 *
 * Routes:
 * - POST /api/users/{userId}/profile - Create profile
 * - GET /api/users/{userId}/profile - Get profile
 * - PUT /api/users/{userId}/profile - Update profile
 * - GET /api/users/{userId}/profile/complete - Get complete profile with contexts
 */
fun Route.userProfileRoutes() {
    val userProfileService = UserProfileService()

    route("/api/users/{userId}/profile") {

        /**
         * POST /api/users/{userId}/profile
         * Create a new user profile
         *
         * Requires: Authorization header with Bearer token
         * Request Body: CreateUserProfileRequest
         * Response: 201 Created with UserProfileResponse or 404/409/422
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
                                    message = "You can only create your own profile"
                                )
                            )
                        )
                    }

                    val request = call.receive<CreateUserProfileRequest>()
                    val result = userProfileService.createProfile(userId, request)

                    if (result.success) {
                        call.respond(HttpStatusCode.Created, result)
                    } else {
                        val statusCode = when {
                            result.message?.contains("not found", ignoreCase = true) == true ->
                                HttpStatusCode.NotFound
                            result.message?.contains("already exists", ignoreCase = true) == true ->
                                HttpStatusCode.Conflict
                            else -> HttpStatusCode.UnprocessableEntity
                        }

                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = when (statusCode) {
                                    HttpStatusCode.NotFound -> "USER_NOT_FOUND"
                                    HttpStatusCode.Conflict -> "PROFILE_ALREADY_EXISTS"
                                    else -> "VALIDATION_ERROR"
                                },
                                message = result.message ?: "Failed to create profile"
                            )
                        )
                        call.respond(statusCode, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Create profile error", e)
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
         * GET /api/users/{userId}/profile
         * Get user profile
         *
         * Requires: Authorization header with Bearer token
         * Response: 200 OK with UserProfileResponse or 404
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

                    val result = userProfileService.getProfile(userId)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = if (result.message?.contains("not found", ignoreCase = true) == true)
                                    "PROFILE_NOT_FOUND" else "ERROR",
                                message = result.message ?: "Failed to get profile"
                            )
                        )
                        call.respond(HttpStatusCode.NotFound, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Get profile error", e)
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
         * PUT /api/users/{userId}/profile
         * Update user profile
         *
         * Requires: Authorization header with Bearer token
         * Request Body: UpdateUserProfileRequest
         * Response: 200 OK with UserProfileResponse or 404/422
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
                                    message = "You can only update your own profile"
                                )
                            )
                        )
                    }

                    val request = call.receive<UpdateUserProfileRequest>()
                    val result = userProfileService.updateProfile(userId, request)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val statusCode = if (result.message?.contains("not found", ignoreCase = true) == true)
                            HttpStatusCode.NotFound else HttpStatusCode.UnprocessableEntity

                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = if (statusCode == HttpStatusCode.NotFound)
                                    "PROFILE_NOT_FOUND" else "VALIDATION_ERROR",
                                message = result.message ?: "Failed to update profile"
                            )
                        )
                        call.respond(statusCode, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Update profile error", e)
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
         * GET /api/users/{userId}/profile/complete
         * Get complete user profile with user info and contexts
         *
         * Requires: Authorization header with Bearer token
         * Response: 200 OK with CompleteUserProfileResponse or 404
         */
        authenticate("auth-jwt") {
            get("/complete") {
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

                    val result = userProfileService.getCompleteProfile(userId)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = if (result.message?.contains("not found", ignoreCase = true) == true)
                                    "PROFILE_NOT_FOUND" else "ERROR",
                                message = result.message ?: "Failed to get complete profile"
                            )
                        )
                        call.respond(HttpStatusCode.NotFound, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Get complete profile error", e)
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