package com.kevin.repository

import com.kevin.db.UserContexts
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * User Context Repository for database operations
 * Handles CRUD operations for user_contexts table
 */
class UserContextRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Create a new user context
     */
    fun createContext(userId: Int, request: CreateUserContextRequest): UserContextResponse? {
        return transaction {
            val now = Clock.System.now()
            val contextId = UserContexts.insertAndGetId {
                it[UserContexts.userId] = userId
                it[contextType] = request.contextType
                it[title] = request.title
                it[content] = request.content
                it[emotionTags] = if (request.emotionTags.isNotEmpty()) {
                    json.encodeToString(request.emotionTags)
                } else {
                    null
                }
                it[voiceUrl] = request.voiceUrl
                it[voiceDurationSeconds] = request.voiceDurationSeconds
                it[inputMethod] = request.inputMethod
                it[importanceLevel] = request.importanceLevel
                it[isCompanionInput] = request.isCompanionInput
                it[companionUserId] = request.companionUserId
                it[createdAt] = now
                it[updatedAt] = now
            }

            findContextById(contextId.value)
        }
    }

    /**
     * Find context by ID
     */
    fun findContextById(contextId: Int): UserContextResponse? {
        return transaction {
            UserContexts.selectAll()
                .where { UserContexts.id eq contextId }
                .map { rowToUserContext(it) }
                .singleOrNull()
        }
    }

    /**
     * Find contexts by user ID with pagination and filters
     */
    fun findContextsByUserId(
        userId: Int,
        page: Int = 1,
        limit: Int = 20,
        contextType: String? = null
    ): UserContextsListResponse {
        return transaction {
            // Build query with filters
            var query = UserContexts.selectAll().where { UserContexts.userId eq userId }

            // Apply contextType filter
            if (contextType != null) {
                query = query.andWhere { UserContexts.contextType eq contextType }
            }

            // Get total count
            val totalCount = query.count()

            // Calculate pagination
            val totalPages = ((totalCount + limit - 1) / limit).toInt()
            val offset = ((page - 1) * limit).toLong()

            // Get paginated results
            val contexts = query
                .orderBy(UserContexts.createdAt, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { rowToUserContext(it) }

            UserContextsListResponse(
                contexts = contexts,
                pagination = PaginationInfo(
                    page = page,
                    limit = limit,
                    totalCount = totalCount,
                    totalPages = totalPages,
                    hasNext = page < totalPages,
                    hasPrevious = page > 1
                )
            )
        }
    }

    /**
     * Update user context
     */
    fun updateContext(contextId: Int, userId: Int, request: UpdateUserContextRequest): UserContextResponse? {
        transaction {
            val now = Clock.System.now()
            UserContexts.update({ (UserContexts.id eq contextId) and (UserContexts.userId eq userId) }) {
                request.contextType?.let { contextType ->
                    it[UserContexts.contextType] = contextType
                }
                request.title?.let { title ->
                    it[UserContexts.title] = title
                }
                request.content?.let { content ->
                    it[UserContexts.content] = content
                }
                request.emotionTags?.let { tags ->
                    it[emotionTags] = if (tags.isNotEmpty()) {
                        json.encodeToString(tags)
                    } else {
                        null
                    }
                }
                request.voiceUrl?.let { voiceUrl ->
                    it[UserContexts.voiceUrl] = voiceUrl
                }
                request.voiceDurationSeconds?.let { duration ->
                    it[voiceDurationSeconds] = duration
                }
                request.importanceLevel?.let { level ->
                    it[importanceLevel] = level
                }
                it[updatedAt] = now
            }
        }
        return findContextById(contextId)
    }

    /**
     * Delete user context
     */
    fun deleteContext(contextId: Int, userId: Int): Boolean {
        return transaction {
            UserContexts.deleteWhere {
                (UserContexts.id eq contextId) and (UserContexts.userId eq userId)
            } > 0
        }
    }

    /**
     * Check if context exists and belongs to user
     */
    fun existsByIdAndUserId(contextId: Int, userId: Int): Boolean {
        return transaction {
            UserContexts.selectAll()
                .where { (UserContexts.id eq contextId) and (UserContexts.userId eq userId) }
                .count() > 0
        }
    }

    /**
     * Convert database row to UserContextResponse
     */
    private fun rowToUserContext(row: ResultRow): UserContextResponse {
        val emotionTagsJson = row[UserContexts.emotionTags]
        val emotionTags = try {
            if (emotionTagsJson != null) {
                json.decodeFromString<List<String>>(emotionTagsJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        // Note: emotionalTone is derived from emotionTags for now
        // You can implement logic to determine tone from tags if needed
        val emotionalTone = deriveEmotionalTone(emotionTags)

        return UserContextResponse(
            id = row[UserContexts.id].value,
            userId = row[UserContexts.userId].value,
            contextType = row[UserContexts.contextType],
            title = row[UserContexts.title],
            content = row[UserContexts.content],
            emotionalTone = emotionalTone,
            emotionTags = emotionTags,
            voiceUrl = row[UserContexts.voiceUrl],
            voiceDurationSeconds = row[UserContexts.voiceDurationSeconds],
            inputMethod = row[UserContexts.inputMethod],
            importanceLevel = row[UserContexts.importanceLevel],
            isCompanionInput = row[UserContexts.isCompanionInput],
            companionUserId = row[UserContexts.companionUserId]?.value,
            createdAt = row[UserContexts.createdAt].toString(),
            updatedAt = row[UserContexts.updatedAt].toString()
        )
    }

    /**
     * Derive emotional tone from emotion tags
     * This is a simple implementation - you can enhance this logic
     */
    private fun deriveEmotionalTone(emotionTags: List<String>): String? {
        if (emotionTags.isEmpty()) return null

        val positiveTags = listOf("happy", "joyful", "excited", "grateful", "hopeful", "proud", "행복", "기쁨", "감사")
        val negativeTags = listOf("sad", "angry", "anxious", "frustrated", "worried", "scared", "슬픔", "화남", "불안")
        val neutralTags = listOf("calm", "neutral", "thoughtful", "평온", "중립")

        val hasPositive = emotionTags.any { tag -> positiveTags.any { it.equals(tag, ignoreCase = true) } }
        val hasNegative = emotionTags.any { tag -> negativeTags.any { it.equals(tag, ignoreCase = true) } }
        val hasNeutral = emotionTags.any { tag -> neutralTags.any { it.equals(tag, ignoreCase = true) } }

        return when {
            hasPositive && hasNegative -> "MIXED"
            hasPositive -> "POSITIVE"
            hasNegative -> "NEGATIVE"
            hasNeutral -> "NEUTRAL"
            else -> null
        }
    }
}