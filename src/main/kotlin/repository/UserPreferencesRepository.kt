package com.kevin.repository

import com.kevin.db.UserPreferences
import com.kevin.db.Users
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

/**
 * User Preferences Repository for database operations
 * Handles CRUD operations for user_preferences table
 */
class UserPreferencesRepository {

    /**
     * Create new user preferences
     */
    fun createPreferences(userId: Int, request: CreateUserPreferencesRequest): UserPreferencesResponse? {
        return transaction {
            // Check if preferences already exist
            val existingPreferences = findPreferencesByUserId(userId)
            if (existingPreferences != null) {
                return@transaction null
            }

            val now = Clock.System.now()
            val preferencesId = UserPreferences.insertAndGetId {
                it[UserPreferences.userId] = userId
                it[narrativeStyle] = request.narrativeStyle
                it[preferredLength] = request.preferredLength
                it[ttsSpeed] = BigDecimal.valueOf(request.ttsSpeed)
                it[ttsPitch] = BigDecimal.valueOf(request.ttsPitch)
                it[ttsVoice] = request.ttsVoice
                it[preferredLanguage] = request.preferredLanguage
                it[enableHapticFeedback] = request.enableHapticFeedback
                it[enableAudioDescriptions] = request.enableAudioDescriptions
                it[highContrastMode] = request.highContrastMode
                it[enablePushNotifications] = request.enablePushNotifications
                it[createdAt] = now
                it[updatedAt] = now
            }

            findPreferencesById(preferencesId.value)
        }
    }

    /**
     * Find preferences by user ID
     */
    fun findPreferencesByUserId(userId: Int): UserPreferencesResponse? {
        return transaction {
            UserPreferences.join(
                Users,
                JoinType.INNER,
                UserPreferences.userId,
                Users.id
            )
                .selectAll()
                .where { UserPreferences.userId eq userId }
                .map { rowToUserPreferences(it) }
                .singleOrNull()
        }
    }

    /**
     * Find preferences by preferences ID
     */
    fun findPreferencesById(preferencesId: Int): UserPreferencesResponse? {
        return transaction {
            UserPreferences.join(
                Users,
                JoinType.INNER,
                UserPreferences.userId,
                Users.id
            )
                .selectAll()
                .where { UserPreferences.id eq preferencesId }
                .map { rowToUserPreferences(it) }
                .singleOrNull()
        }
    }

    /**
     * Update user preferences
     */
    fun updatePreferences(userId: Int, request: UpdateUserPreferencesRequest): UserPreferencesResponse? {
        transaction {
            val now = Clock.System.now()
            UserPreferences.update({ UserPreferences.userId eq userId }) {
                request.narrativeStyle?.let { style ->
                    it[narrativeStyle] = style
                }
                request.preferredLength?.let { length ->
                    it[preferredLength] = length
                }
                request.ttsSpeed?.let { speed ->
                    it[ttsSpeed] = BigDecimal.valueOf(speed)
                }
                request.ttsPitch?.let { pitch ->
                    it[ttsPitch] = BigDecimal.valueOf(pitch)
                }
                request.ttsVoice?.let { voice ->
                    it[ttsVoice] = voice
                }
                request.preferredLanguage?.let { language ->
                    it[preferredLanguage] = language
                }
                request.enableHapticFeedback?.let { haptic ->
                    it[enableHapticFeedback] = haptic
                }
                request.enableAudioDescriptions?.let { audio ->
                    it[enableAudioDescriptions] = audio
                }
                request.highContrastMode?.let { contrast ->
                    it[highContrastMode] = contrast
                }
                request.enablePushNotifications?.let { notifications ->
                    it[enablePushNotifications] = notifications
                }
                it[updatedAt] = now
            }
        }
        return findPreferencesByUserId(userId)
    }

    /**
     * Delete user preferences
     */
    fun deletePreferences(userId: Int): Boolean {
        return transaction {
            UserPreferences.deleteWhere { UserPreferences.userId eq userId } > 0
        }
    }

    /**
     * Check if preferences exist for user
     */
    fun existsByUserId(userId: Int): Boolean {
        return transaction {
            UserPreferences.selectAll()
                .where { UserPreferences.userId eq userId }
                .count() > 0
        }
    }

    /**
     * Convert database row to UserPreferencesResponse
     */
    private fun rowToUserPreferences(row: ResultRow): UserPreferencesResponse {
        return UserPreferencesResponse(
            id = row[UserPreferences.id].value,
            userId = row[UserPreferences.userId].value,
            narrativeStyle = row[UserPreferences.narrativeStyle],
            preferredLength = row[UserPreferences.preferredLength],
            ttsSpeed = row[UserPreferences.ttsSpeed].toDouble(),
            ttsPitch = row[UserPreferences.ttsPitch].toDouble(),
            ttsVoice = row[UserPreferences.ttsVoice],
            preferredLanguage = row[UserPreferences.preferredLanguage],
            enableHapticFeedback = row[UserPreferences.enableHapticFeedback],
            enableAudioDescriptions = row[UserPreferences.enableAudioDescriptions],
            highContrastMode = row[UserPreferences.highContrastMode],
            enablePushNotifications = row[UserPreferences.enablePushNotifications],
            isVisuallyImpaired = row.getOrNull(Users.isVisuallyImpaired) ?: false,
            createdAt = row[UserPreferences.createdAt].toString(),
            updatedAt = row[UserPreferences.updatedAt].toString()
        )
    }
}
