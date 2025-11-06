package com.kevin.routes

import com.kevin.model.dto.*
import com.kevin.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Authentication Routes
 * Implements authentication endpoints according to REST API specification
 *
 * Routes:
 * - POST /api/auth/register - User registration
 * - POST /api/auth/login - User login
 * - GET /api/auth/validate-token - Validate JWT token
 * - POST /api/auth/refresh-token - Refresh JWT token
 * - POST /api/auth/logout - User logout
 */
fun Route.authRoutes() {
    val authService = AuthService()

    route("/api/auth") {

        /**
         * POST /api/auth/register
         * Register a new user account
         *
         * Request Body: RegisterRequest
         * Response: 201 Created with AuthResponse or 409 Conflict / 422 Unprocessable Entity
         */
        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()

                val result = authService.register(request)

                if (result.success) {
                    call.respond(HttpStatusCode.Created, result)
                } else {
                    // Determine appropriate status code based on error message
                    val statusCode = when {
                        result.message?.contains("already exists", ignoreCase = true) == true ->
                            HttpStatusCode.Conflict
                        result.message?.contains("Invalid", ignoreCase = true) == true ||
                        result.message?.contains("must", ignoreCase = true) == true ->
                            HttpStatusCode.UnprocessableEntity
                        else -> HttpStatusCode.BadRequest
                    }

                    // Create error response
                    val errorResponse = ApiErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = when (statusCode) {
                                HttpStatusCode.Conflict -> "EMAIL_ALREADY_EXISTS"
                                HttpStatusCode.UnprocessableEntity -> "VALIDATION_ERROR"
                                else -> "BAD_REQUEST"
                            },
                            message = result.message ?: "Registration failed"
                        )
                    )
                    call.respond(statusCode, errorResponse)
                }
            } catch (e: Exception) {
                call.application.log.error("Registration error", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiErrorResponse(
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "An internal error occurred during registration"
                        )
                    )
                )
            }
        }

        /**
         * POST /api/auth/login
         * Authenticate user and generate JWT token
         *
         * Request Body: LoginRequest
         * Response: 200 OK with AuthResponse or 401 Unauthorized
         */
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()

                val result = authService.login(request)

                if (result.success) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    // Login failed - return 401 Unauthorized
                    val errorResponse = ApiErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_CREDENTIALS",
                            message = result.message ?: "Invalid email or password"
                        )
                    )
                    call.respond(HttpStatusCode.Unauthorized, errorResponse)
                }
            } catch (e: Exception) {
                call.application.log.error("Login error", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiErrorResponse(
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "An internal error occurred during login"
                        )
                    )
                )
            }
        }

        /**
         * GET /api/auth/validate-token
         * Validate JWT token and return token information
         * Requires: Authorization header with Bearer token
         *
         * Response: 200 OK with TokenValidationResponse or 401 Unauthorized
         */
        authenticate("auth-jwt") {
            get("/validate-token") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asInt()
                    val expiresAt = principal?.expiresAt?.toString()

                    if (userId != null && expiresAt != null) {
                        val response = ApiResponse(
                            success = true,
                            data = TokenValidationResponse(
                                valid = true,
                                userId = userId,
                                expiresAt = expiresAt
                            )
                        )
                        call.respond(HttpStatusCode.OK, response)
                    } else {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_TOKEN",
                                    message = "Invalid or malformed token"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.application.log.error("Token validation error", e)
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "TOKEN_VALIDATION_FAILED",
                                message = "Token validation failed"
                            )
                        )
                    )
                }
            }
        }

        /**
         * POST /api/auth/refresh-token
         * Refresh JWT token before expiration
         * Requires: Authorization header with Bearer token
         *
         * Response: 200 OK with TokenRefreshResponse or 401 Unauthorized
         */
        authenticate("auth-jwt") {
            post("/refresh-token") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asInt()

                    if (userId != null) {
                        val newToken = authService.refreshToken(userId)

                        if (newToken != null) {
                            val response = ApiResponse(
                                success = true,
                                data = newToken
                            )
                            call.respond(HttpStatusCode.OK, response)
                        } else {
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                ApiErrorResponse(
                                    error = ErrorDetail(
                                        code = "USER_NOT_FOUND",
                                        message = "User not found"
                                    )
                                )
                            )
                        }
                    } else {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiErrorResponse(
                                error = ErrorDetail(
                                    code = "INVALID_TOKEN",
                                    message = "Invalid token"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.application.log.error("Token refresh error", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiErrorResponse(
                            error = ErrorDetail(
                                code = "INTERNAL_ERROR",
                                message = "An internal error occurred during token refresh"
                            )
                        )
                    )
                }
            }
        }

        /**
         * POST /api/auth/logout
         * Logout user (client-side token invalidation)
         * Requires: Authorization header with Bearer token
         *
         * Response: 204 No Content
         *
         * Note: For stateless JWT, logout is handled client-side by removing the token.
         * This endpoint can be extended to add token to a blacklist if needed.
         */
        authenticate("auth-jwt") {
            post("/logout") {
                try {
                    // Token blacklisting can be implemented here if needed
                    // For now, we just return success as logout is handled client-side

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asInt()

                    call.application.log.info("User $userId logged out")

                    // Return 204 No Content as per API spec
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: Exception) {
                    call.application.log.error("Logout error", e)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}