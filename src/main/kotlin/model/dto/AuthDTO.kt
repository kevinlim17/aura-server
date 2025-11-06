package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * Authentication DTOs based on REST API specification
 * Reference: project_schema/rest_api.md
 */

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Register request DTO
 * POST /api/auth/register
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val userType: String = "MAIN_USER",           // "MAIN_USER" | "COMPANION"
    val isVisuallyImpaired: Boolean = true,        // default true
    val impairmentLevel: String? = null            // "TOTAL_BLINDNESS" | "LOW_VISION"
)

/**
 * Login request DTO
 * POST /api/auth/login
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * User response DTO
 * Used in both register and login responses
 */
@Serializable
data class UserResponse(
    val id: Int,
    val email: String,
    val userType: String,
    val isVisuallyImpaired: Boolean,
    val impairmentLevel: String? = null,
    val isOnboardingCompleted: Boolean,
    val createdAt: String
)

/**
 * Authentication response DTO
 * Used for successful register/login
 */
@Serializable
data class AuthResponse(
    val user: UserResponse,
    val token: String,
    val expiresAt: String? = null
)

/**
 * Token validation response DTO
 * GET /api/auth/validate-token
 */
@Serializable
data class TokenValidationResponse(
    val valid: Boolean,
    val userId: Int,
    val expiresAt: String
)

/**
 * Token refresh response DTO
 * POST /api/auth/refresh-token
 */
@Serializable
data class TokenRefreshResponse(
    val token: String,
    val expiresAt: String
)

// ============================================================================
// Standard API Response Wrappers
// ============================================================================

/**
 * Success response wrapper
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

/**
 * Error detail
 */
@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

/**
 * Error response wrapper
 */
@Serializable
data class ApiErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetail
)

// ============================================================================
// Validation Error Details
// ============================================================================

/**
 * Validation error for specific fields
 */
@Serializable
data class ValidationError(
    val field: String,
    val message: String
)

/**
 * Validation error response
 */
@Serializable
data class ValidationErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetail
)