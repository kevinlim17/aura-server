package com.kevin.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kevin.model.dto.*
import com.kevin.models.User
import com.kevin.repository.UserRepository
import io.github.cdimascio.dotenv.dotenv
import kotlinx.datetime.*
import java.util.*

/**
 * Authentication Service
 * Handles user registration, login, JWT token generation, and password hashing
 */
class AuthService(private val userRepository: UserRepository = UserRepository()) {

    companion object {
        // Load environment variables
        private val dotenv = dotenv {
            directory = "env"
            filename = ".env"
            ignoreIfMissing = true
        }

        // JWT Configuration
        private val JWT_SECRET = dotenv["JWT_SECRET"] ?: "aura-secret-key-change-in-production"
        private val JWT_ISSUER = dotenv["JWT_ISSUER"] ?: "aura-server"
        private val JWT_AUDIENCE = dotenv["JWT_AUDIENCE"] ?: "aura-client"
        private val JWT_EXPIRATION_HOURS = dotenv["JWT_EXPIRATION_HOURS"]?.toLongOrNull() ?: 24L

        // BCrypt cost factor (higher = more secure but slower)
        private const val BCRYPT_COST = 12
    }

    /**
     * Register a new user
     *
     * @param request RegisterRequest containing user information
     * @return ApiResponse with AuthResponse or error
     */
    fun register(request: RegisterRequest): ApiResponse<AuthResponse> {
        // Validate email format
        if (!isValidEmail(request.email)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid email format"
            )
        }

        // Validate password strength
        val passwordValidation = validatePassword(request.password)
        if (!passwordValidation.first) {
            return ApiResponse(
                success = false,
                data = null,
                message = passwordValidation.second
            )
        }

        // Check if user already exists
        if (userRepository.existsByEmail(request.email)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Email already exists"
            )
        }

        // Hash password with BCrypt
        val passwordHash = hashPassword(request.password)

        // Create user
        val user = userRepository.createUser(request, passwordHash)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Failed to create user"
            )

        // Generate JWT token
        val token = generateToken(user)
        val expiresAt = calculateExpirationTime()

        // Create response
        val authResponse = AuthResponse(
            user = userToUserResponse(user),
            token = token,
            expiresAt = expiresAt
        )

        return ApiResponse(
            success = true,
            data = authResponse,
            message = "User registered successfully"
        )
    }

    /**
     * Login user
     *
     * @param request LoginRequest containing email and password
     * @return ApiResponse with AuthResponse or error
     */
    fun login(request: LoginRequest): ApiResponse<AuthResponse> {
        // Find user by email
        val user = userRepository.findUserByEmail(request.email)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Invalid email or password"
            )

        // Verify password
        val storedHash = userRepository.getPasswordHash(request.email)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Invalid email or password"
            )

        if (!verifyPassword(request.password, storedHash)) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid email or password"
            )
        }

        // Check if user is active
        if (!user.isActive) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Account is deactivated"
            )
        }

        // Update last login timestamp
        userRepository.updateLastLogin(user.id)

        // Generate JWT token
        val token = generateToken(user)
        val expiresAt = calculateExpirationTime()

        // Create response
        val authResponse = AuthResponse(
            user = userToUserResponse(user),
            token = token,
            expiresAt = expiresAt
        )

        return ApiResponse(
            success = true,
            data = authResponse,
            message = "Login successful"
        )
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token string
     * @return TokenValidationResponse or null if invalid
     */
    fun validateToken(token: String): TokenValidationResponse? {
        return try {
            val verifier = JWT.require(Algorithm.HMAC256(JWT_SECRET))
                .withIssuer(JWT_ISSUER)
                .withAudience(JWT_AUDIENCE)
                .build()

            val decodedJWT = verifier.verify(token)
            val userId = decodedJWT.getClaim("userId").asInt()
            val expiresAt = decodedJWT.expiresAt.toInstant().toString()

            TokenValidationResponse(
                valid = true,
                userId = userId,
                expiresAt = expiresAt
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Refresh JWT token
     *
     * @param userId User ID from current valid token
     * @return TokenRefreshResponse or null if user not found
     */
    fun refreshToken(userId: Int): TokenRefreshResponse? {
        val user = userRepository.findUserById(userId) ?: return null

        val token = generateToken(user)
        val expiresAt = calculateExpirationTime()

        return TokenRefreshResponse(
            token = token,
            expiresAt = expiresAt
        )
    }

    /**
     * Generate JWT token for user
     *
     * @param user User object
     * @return JWT token string
     */
    private fun generateToken(user: User): String {
        val now = Clock.System.now()
        val expiration = now.plus(JWT_EXPIRATION_HOURS, DateTimeUnit.HOUR)

        return JWT.create()
            .withIssuer(JWT_ISSUER)
            .withAudience(JWT_AUDIENCE)
            .withClaim("userId", user.id)
            .withClaim("email", user.email)
            .withClaim("userType", user.userType)
            .withIssuedAt(Date(now.toEpochMilliseconds()))
            .withExpiresAt(Date(expiration.toEpochMilliseconds()))
            .sign(Algorithm.HMAC256(JWT_SECRET))
    }

    /**
     * Calculate token expiration time as ISO string
     */
    private fun calculateExpirationTime(): String {
        return Clock.System.now()
            .plus(JWT_EXPIRATION_HOURS, DateTimeUnit.HOUR)
            .toString()
    }

    /**
     * Hash password using BCrypt
     *
     * @param password Plain text password
     * @return Hashed password
     */
    private fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
    }

    /**
     * Verify password against hash
     *
     * @param password Plain text password
     * @param hash Stored password hash
     * @return true if password matches, false otherwise
     */
    private fun verifyPassword(password: String, hash: String): Boolean {
        val result = BCrypt.verifyer().verify(password.toCharArray(), hash)
        return result.verified
    }

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }

    /**
     * Validate password strength
     *
     * @return Pair<Boolean, String> - (isValid, errorMessage)
     */
    private fun validatePassword(password: String): Pair<Boolean, String> {
        if (password.length < 8) {
            return Pair(false, "Password must be at least 8 characters")
        }
        if (!password.any { it.isUpperCase() }) {
            return Pair(false, "Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            return Pair(false, "Password must contain at least one lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            return Pair(false, "Password must contain at least one digit")
        }
        return Pair(true, "")
    }

    /**
     * Convert User model to UserResponse DTO
     */
    private fun userToUserResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id,
            email = user.email,
            userType = user.userType,
            isVisuallyImpaired = user.isVisuallyImpaired,
            impairmentLevel = user.impairmentLevel,
            isOnboardingCompleted = user.isOnboardingCompleted,
            createdAt = user.createdAt
        )
    }
}