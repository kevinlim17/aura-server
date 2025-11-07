package com.kevin.repository

import com.kevin.db.UserContexts
import com.kevin.db.UserProfiles
import com.kevin.db.Users
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * User Profile Repository for database operations
 * Handles CRUD operations for user_profiles table
 */
class UserProfileRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Create a new user profile
     */
    fun createProfile(userId: Int, request: CreateUserProfileRequest): UserProfileResponse? {
        return transaction {
            // Check if profile already exists
            val existingProfile = findProfileByUserId(userId)
            if (existingProfile != null) {
                return@transaction null
            }

            val now = Clock.System.now()
            val profileId = UserProfiles.insertAndGetId {
                it[UserProfiles.userId] = userId
                it[interests] = json.encodeToString(request.interests)
                it[hobbies] = request.hobbies
                it[hobbiesVoiceUrl] = request.hobbiesVoiceUrl
                it[favoriteArtists] = json.encodeToString(request.favoriteArtists)
                it[bio] = request.bio
                it[createdAt] = now
                it[updatedAt] = now
            }

            findProfileById(profileId.value)
        }
    }

    /**
     * Find profile by user ID
     */
    fun findProfileByUserId(userId: Int): UserProfileResponse? {
        return transaction {
            UserProfiles.selectAll()
                .where { UserProfiles.userId eq userId }
                .map { rowToUserProfile(it) }
                .singleOrNull()
        }
    }

    /**
     * Find profile by profile ID
     */
    fun findProfileById(profileId: Int): UserProfileResponse? {
        return transaction {
            UserProfiles.selectAll()
                .where { UserProfiles.id eq profileId }
                .map { rowToUserProfile(it) }
                .singleOrNull()
        }
    }

    /**
     * Update user profile
     */
    fun updateProfile(userId: Int, request: UpdateUserProfileRequest): UserProfileResponse? {
        transaction {
            val now = Clock.System.now()
            UserProfiles.update({ UserProfiles.userId eq userId }) {
                request.interests?.let { interests ->
                    it[UserProfiles.interests] = json.encodeToString(interests)
                }
                request.hobbies?.let { hobbies ->
                    it[UserProfiles.hobbies] = hobbies
                }
                request.hobbiesVoiceUrl?.let { voiceUrl ->
                    it[UserProfiles.hobbiesVoiceUrl] = voiceUrl
                }
                request.favoriteArtists?.let { artists ->
                    it[UserProfiles.favoriteArtists] = json.encodeToString(artists)
                }
                request.bio?.let { bio ->
                    it[UserProfiles.bio] = bio
                }
                it[UserProfiles.updatedAt] = now
            }
        }
        return findProfileByUserId(userId)
    }

    /**
     * Get complete profile with user info and contexts
     */
    fun getCompleteProfile(userId: Int): CompleteUserProfileResponse? {
        return transaction {
            // Get user basic info
            val userRow = Users.selectAll()
                .where { Users.id eq userId }
                .singleOrNull() ?: return@transaction null

            val userBasicInfo = UserBasicInfo(
                id = userRow[Users.id].value,
                email = userRow[Users.email],
                userType = userRow[Users.userType],
                isVisuallyImpaired = userRow[Users.isVisuallyImpaired],
                impairmentLevel = userRow[Users.impairmentLevel],
                isOnboardingCompleted = userRow[Users.isOnboardingCompleted],
                createdAt = userRow[Users.createdAt].toString()
            )

            // Get user profile
            val profile = findProfileByUserId(userId) ?: return@transaction null

            // Get user contexts
            val contexts = UserContexts.selectAll()
                .where { UserContexts.userId eq userId }
                .orderBy(UserContexts.createdAt, SortOrder.DESC)
                .map { row ->
                    UserContextSummary(
                        id = row[UserContexts.id].value,
                        contextType = row[UserContexts.contextType],
                        title = row[UserContexts.title],
                        content = row[UserContexts.content],
                        voiceUrl = row[UserContexts.voiceUrl],
                        inputMethod = row[UserContexts.inputMethod],
                        createdAt = row[UserContexts.createdAt].toString()
                    )
                }

            CompleteUserProfileResponse(
                user = userBasicInfo,
                profile = profile,
                contexts = contexts
            )
        }
    }

    /**
     * Delete user profile
     */
    fun deleteProfile(userId: Int): Boolean {
        return transaction {
            UserProfiles.deleteWhere { UserProfiles.userId eq userId } > 0
        }
    }

    /**
     * Check if profile exists for user
     */
    fun existsByUserId(userId: Int): Boolean {
        return transaction {
            UserProfiles.selectAll()
                .where { UserProfiles.userId eq userId }
                .count() > 0
        }
    }

    /**
     * Convert database row to UserProfileResponse
     */
    private fun rowToUserProfile(row: ResultRow): UserProfileResponse {
        val interestsJson = row[UserProfiles.interests]
        val interests = try {
            json.decodeFromString<List<String>>(interestsJson)
        } catch (e: Exception) {
            emptyList()
        }

        val artistsJson = row[UserProfiles.favoriteArtists] ?: ""
        val favoriteArtists = try {
            json.decodeFromString<List<String>>(artistsJson)
        } catch (e: Exception) {
            emptyList()
        }

        return UserProfileResponse(
            id = row[UserProfiles.id].value,
            userId = row[UserProfiles.userId].value,
            interests = interests,
            hobbies = row[UserProfiles.hobbies],
            hobbiesVoiceUrl = row[UserProfiles.hobbiesVoiceUrl],
            favoriteArtists = favoriteArtists,
            bio = row[UserProfiles.bio],
            createdAt = row[UserProfiles.createdAt].toString(),
            updatedAt = row[UserProfiles.updatedAt].toString()
        )
    }
}