package com.kevin.repository

import com.kevin.db.UserMemos
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Memo Repository for database operations
 * Handles CRUD operations for user_memos table
 */
class MemoRepository {

    /**
     * Create new memo
     */
    fun createMemo(
        userId: Int,
        content: String,
        artworkId: Int?,
        docentSessionId: Int?,
        inputMethod: String,
        voiceUrl: String?,
        voiceDurationSeconds: Int?,
        tags: List<String>?,
        category: String?,
        isSharedWithCompanion: Boolean
    ): Int {
        return transaction {
            val now = Clock.System.now()

            // Convert tags list to comma-separated string
            val tagsString = tags?.joinToString(",")

            UserMemos.insertAndGetId {
                it[UserMemos.userId] = userId
                it[UserMemos.content] = content
                it[UserMemos.artworkId] = artworkId
                it[UserMemos.docentSessionId] = docentSessionId
                it[UserMemos.inputMethod] = inputMethod
                it[UserMemos.voiceUrl] = voiceUrl
                it[UserMemos.voiceDurationSeconds] = voiceDurationSeconds
                it[UserMemos.tags] = tagsString
                it[UserMemos.category] = category
                it[UserMemos.isSharedWithCompanion] = isSharedWithCompanion
                it[createdAt] = now
                it[updatedAt] = now
            }.value
        }
    }

    /**
     * Find memo by ID
     */
    fun findMemoById(memoId: Int): MemoResponse? {
        return transaction {
            UserMemos.selectAll()
                .where { UserMemos.id eq memoId }
                .map { rowToMemo(it) }
                .singleOrNull()
        }
    }

    /**
     * Find memos by user ID with pagination and filters
     */
    fun findMemosByUserId(
        userId: Int,
        page: Int = 1,
        limit: Int = 20,
        artworkId: Int? = null,
        category: String? = null,
        inputMethod: String? = null,
        searchQuery: String? = null
    ): Pair<List<MemoResponse>, Long> {
        return transaction {
            // Build query with filters
            var query = UserMemos.selectAll().where { UserMemos.userId eq userId }

            // Apply artworkId filter if provided
            if (artworkId != null) {
                query = query.andWhere { UserMemos.artworkId eq artworkId }
            }

            // Apply category filter if provided
            if (category != null) {
                query = query.andWhere { UserMemos.category eq category }
            }

            // Apply inputMethod filter if provided
            if (inputMethod != null) {
                query = query.andWhere { UserMemos.inputMethod eq inputMethod }
            }

            // Apply search query if provided (search in content field)
            if (searchQuery != null && searchQuery.isNotBlank()) {
                val searchPattern = "%${searchQuery}%"
                query = query.andWhere { UserMemos.content like searchPattern }
            }

            // Get total count
            val totalCount = query.count()

            // Calculate pagination
            val offset = ((page - 1) * limit).toLong()

            // Get paginated results
            val memos = query
                .orderBy(UserMemos.updatedAt, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { rowToMemo(it) }

            Pair(memos, totalCount)
        }
    }

    /**
     * Update memo
     */
    fun updateMemo(
        memoId: Int,
        userId: Int,
        content: String?,
        tags: List<String>?,
        category: String?,
        isSharedWithCompanion: Boolean?
    ): Boolean {
        return transaction {
            val now = Clock.System.now()

            // Build update statement
            val updateCount = UserMemos.update({
                (UserMemos.id eq memoId) and (UserMemos.userId eq userId)
            }) {
                if (content != null) it[UserMemos.content] = content
                if (tags != null) it[UserMemos.tags] = tags.joinToString(",")
                if (category != null) it[UserMemos.category] = category
                if (isSharedWithCompanion != null) it[UserMemos.isSharedWithCompanion] = isSharedWithCompanion
                it[updatedAt] = now
            }

            updateCount > 0
        }
    }

    /**
     * Delete memo
     */
    fun deleteMemo(memoId: Int, userId: Int): Boolean {
        return transaction {
            UserMemos.deleteWhere {
                (UserMemos.id eq memoId) and (UserMemos.userId eq userId)
            } > 0
        }
    }

    /**
     * Check if memo exists and belongs to user
     */
    fun isMemoOwnedByUser(memoId: Int, userId: Int): Boolean {
        return transaction {
            UserMemos.selectAll()
                .where { (UserMemos.id eq memoId) and (UserMemos.userId eq userId) }
                .count() > 0
        }
    }

    /**
     * Count memos by user ID
     */
    fun countMemosByUserId(userId: Int): Long {
        return transaction {
            UserMemos.selectAll()
                .where { UserMemos.userId eq userId }
                .count()
        }
    }

    /**
     * Find memos by artwork ID
     */
    fun findMemosByArtworkId(artworkId: Int, userId: Int, limit: Int = 10): List<MemoResponse> {
        return transaction {
            UserMemos.selectAll()
                .where { (UserMemos.artworkId eq artworkId) and (UserMemos.userId eq userId) }
                .orderBy(UserMemos.updatedAt, SortOrder.DESC)
                .limit(limit)
                .map { rowToMemo(it) }
        }
    }

    /**
     * Find memos by docent session ID
     */
    fun findMemosByDocentSessionId(sessionId: Int, userId: Int, limit: Int = 10): List<MemoResponse> {
        return transaction {
            UserMemos.selectAll()
                .where { (UserMemos.docentSessionId eq sessionId) and (UserMemos.userId eq userId) }
                .orderBy(UserMemos.updatedAt, SortOrder.DESC)
                .limit(limit)
                .map { rowToMemo(it) }
        }
    }

    /**
     * Get memo statistics for a user
     */
    fun getMemoStatistics(userId: Int): MemoStatistics {
        return transaction {
            val totalMemos = UserMemos.selectAll()
                .where { UserMemos.userId eq userId }
                .count()

            val textMemos = UserMemos.selectAll()
                .where { (UserMemos.userId eq userId) and (UserMemos.inputMethod eq "TEXT") }
                .count()

            val voiceMemos = UserMemos.selectAll()
                .where { (UserMemos.userId eq userId) and (UserMemos.inputMethod eq "VOICE") }
                .count()

            val memosWithArtwork = UserMemos.selectAll()
                .where { (UserMemos.userId eq userId) and UserMemos.artworkId.isNotNull() }
                .count()

            val memosWithSession = UserMemos.selectAll()
                .where { (UserMemos.userId eq userId) and UserMemos.docentSessionId.isNotNull() }
                .count()

            val sharedMemos = UserMemos.selectAll()
                .where { (UserMemos.userId eq userId) and (UserMemos.isSharedWithCompanion eq true) }
                .count()

            MemoStatistics(
                totalMemos = totalMemos,
                textMemos = textMemos,
                voiceMemos = voiceMemos,
                memosWithArtwork = memosWithArtwork,
                memosWithSession = memosWithSession,
                sharedMemos = sharedMemos
            )
        }
    }

    /**
     * Convert database row to MemoResponse
     */
    private fun rowToMemo(row: ResultRow): MemoResponse {
        // Parse tags from comma-separated string
        val tagsString = row[UserMemos.tags]
        val tags = if (tagsString != null && tagsString.isNotBlank()) {
            tagsString.split(",").map { it.trim() }
        } else {
            emptyList()
        }

        return MemoResponse(
            id = row[UserMemos.id].value,
            userId = row[UserMemos.userId].value,
            artworkId = row[UserMemos.artworkId]?.value,
            docentSessionId = row[UserMemos.docentSessionId]?.value,
            content = row[UserMemos.content],
            voiceUrl = row[UserMemos.voiceUrl],
            voiceDurationSeconds = row[UserMemos.voiceDurationSeconds],
            inputMethod = row[UserMemos.inputMethod],
            tags = tags,
            category = row[UserMemos.category],
            isSharedWithCompanion = row[UserMemos.isSharedWithCompanion],
            createdAt = row[UserMemos.createdAt].toString(),
            updatedAt = row[UserMemos.updatedAt].toString()
        )
    }
}