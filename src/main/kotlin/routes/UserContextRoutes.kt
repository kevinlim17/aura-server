package com.kevin.routes

import com.kevin.model.dto.*
import com.kevin.services.UserContextService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * User Context Routes
 * Implements user context management endpoints
 *
 * Routes:
 * - POST /api/users/{userId}/contexts - Create context
 * - GET /api/users/{userId}/contexts - Get contexts list with pagination
 * - GET /api/users/{userId}/contexts/{contextId} - Get specific context
 * - PUT /api/users/{userId}/contexts/{contextId} - Update context
 * - DELETE /api/users/{userId}/contexts/{contextId} - Delete context
 */
fun Route.userContextRoutes() {
    val userContextService = UserContextService()

    route("/api/users/{userId}/contexts") {

        /**
         * POST /api/users/{userId}/contexts
         * Create a new user context
         *
         * Requires: Authorization header with Bearer token
         * Request Body: CreateUserContextRequest
         * Response: 201 Created with UserContextResponse or 400/404/422
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
                                    message = "You can only create contexts for your own account"
                                )
                            )
                        )
                    }

                    val request = call.receive<CreateUserContextRequest>()
                    val result = userContextService.createContext(userId, request)

                    if (result.success) {
                        call.respond(HttpStatusCode.Created, result)
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
                                    HttpStatusCode.NotFound -> "USER_NOT_FOUND"
                                    HttpStatusCode.BadRequest -> "VALIDATION_ERROR"
                                    else -> "CREATION_ERROR"
                                },
                                message = result.message ?: "Failed to create context"
                            )
                        )
                        call.respond(statusCode, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Create context error", e)
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
         * GET /api/users/{userId}/contexts
         * Get user contexts with pagination and filters
         *
         * Requires: Authorization header with Bearer token
         * Query Parameters:
         *   - page: Page number (default: 1)
         *   - limit: Items per page (default: 20, max: 100)
         *   - contextType: Filter by context type
         * Response: 200 OK with UserContextsListResponse or 400/404
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

                    // Parse query parameters
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                    val contextType = call.request.queryParameters["contextType"]

                    val result = userContextService.getContexts(
                        userId = userId,
                        page = page,
                        limit = limit,
                        contextType = contextType
                    )

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val statusCode = when {
                            result.message?.contains("not found", ignoreCase = true) == true ->
                                HttpStatusCode.NotFound
                            result.message?.contains("Invalid", ignoreCase = true) == true ->
                                HttpStatusCode.BadRequest
                            else -> HttpStatusCode.BadRequest
                        }

                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = when (statusCode) {
                                    HttpStatusCode.NotFound -> "USER_NOT_FOUND"
                                    else -> "VALIDATION_ERROR"
                                },
                                message = result.message ?: "Failed to get contexts"
                            )
                        )
                        call.respond(statusCode, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Get contexts error", e)
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
         * GET /api/users/{userId}/contexts/{contextId}
         * Get specific user context
         *
         * Requires: Authorization header with Bearer token
         * Response: 200 OK with UserContextResponse or 400/404
         */
        authenticate("auth-jwt") {
            get("/{contextId}") {
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

                    val contextId = call.parameters["contextId"]?.toIntOrNull()
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_CONTEXT_ID",
                                    message = "Invalid context ID"
                                )
                            )
                        )

                    val result = userContextService.getContext(userId, contextId)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = if (result.message?.contains("not found", ignoreCase = true) == true)
                                    "CONTEXT_NOT_FOUND" else "ERROR",
                                message = result.message ?: "Failed to get context"
                            )
                        )
                        call.respond(HttpStatusCode.NotFound, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Get context error", e)
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
         * PUT /api/users/{userId}/contexts/{contextId}
         * Update user context
         *
         * Requires: Authorization header with Bearer token
         * Request Body: UpdateUserContextRequest
         * Response: 200 OK with UserContextResponse or 400/404/422
         */
        authenticate("auth-jwt") {
            put("/{contextId}") {
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

                    val contextId = call.parameters["contextId"]?.toIntOrNull()
                        ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_CONTEXT_ID",
                                    message = "Invalid context ID"
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
                                    message = "You can only update your own contexts"
                                )
                            )
                        )
                    }

                    val request = call.receive<UpdateUserContextRequest>()
                    val result = userContextService.updateContext(userId, contextId, request)

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
                                    HttpStatusCode.NotFound -> "CONTEXT_NOT_FOUND"
                                    HttpStatusCode.BadRequest -> "VALIDATION_ERROR"
                                    else -> "UPDATE_ERROR"
                                },
                                message = result.message ?: "Failed to update context"
                            )
                        )
                        call.respond(statusCode, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Update context error", e)
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
         * DELETE /api/users/{userId}/contexts/{contextId}
         * Delete user context
         *
         * Requires: Authorization header with Bearer token
         * Response: 200 OK or 400/404
         */
        authenticate("auth-jwt") {
            delete("/{contextId}") {
                try {
                    val userId = call.parameters["userId"]?.toIntOrNull()
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_USER_ID",
                                    message = "Invalid user ID"
                                )
                            )
                        )

                    val contextId = call.parameters["contextId"]?.toIntOrNull()
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_CONTEXT_ID",
                                    message = "Invalid context ID"
                                )
                            )
                        )

                    // Verify JWT token user matches the userId in path
                    val principal = call.principal<JWTPrincipal>()
                    val tokenUserId = principal?.payload?.getClaim("userId")?.asInt()

                    if (tokenUserId != userId) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "FORBIDDEN",
                                    message = "You can only delete your own contexts"
                                )
                            )
                        )
                    }

                    val result = userContextService.deleteContext(userId, contextId)

                    if (result.success) {
                        call.respond(HttpStatusCode.OK, result)
                    } else {
                        val errorResponse = ApiErrorResponse(
                            error = ErrorDetail(
                                code = if (result.message?.contains("not found", ignoreCase = true) == true)
                                    "CONTEXT_NOT_FOUND" else "DELETE_ERROR",
                                message = result.message ?: "Failed to delete context"
                            )
                        )
                        call.respond(HttpStatusCode.NotFound, errorResponse)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Delete context error", e)
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