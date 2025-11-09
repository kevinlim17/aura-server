package com.kevin.repository

import com.kevin.db.UserLinks
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Link Repository for database operations
 * Handles CRUD operations for user_links table
 */
class LinkRepository {

    /**
     * Create new link
     */
    fun createLink(
        userId: Int,
        url: String,
        title: String?,
        description: String?,
        artworkId: Int?,
        docentSessionId: Int?,
        linkType: String?,
        thumbnailUrl: String?,
        hasAudioDescription: Boolean,
        hasSubtitles: Boolean,
        metadata: Map<String, String>?
    ): Int {
        return transaction {
            val now = Clock.System.now()

            // Convert metadata to JSON string
            val metadataJson = if (metadata != null) {
                kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.serializer<Map<String, String>>(),
                    metadata
                )
            } else {
                "{}"
            }

            UserLinks.insertAndGetId {
                it[UserLinks.userId] = userId
                it[UserLinks.url] = url
                it[UserLinks.title] = title
                it[UserLinks.description] = description
                it[UserLinks.artworkId] = artworkId
                it[UserLinks.docentSessionId] = docentSessionId
                it[UserLinks.linkType] = linkType
                it[UserLinks.thumbnailUrl] = thumbnailUrl
                it[UserLinks.hasAudioDescription] = hasAudioDescription
                it[UserLinks.hasSubtitles] = hasSubtitles
                it[UserLinks.metadata] = metadataJson
                it[createdAt] = now
            }.value
        }
    }

    /**
     * Find link by ID
     */
    fun findLinkById(linkId: Int): LinkResponse? {
        return transaction {
            UserLinks.selectAll()
                .where { UserLinks.id eq linkId }
                .map { rowToLink(it) }
                .singleOrNull()
        }
    }

    /**
     * Find links by user ID with pagination and filters
     */
    fun findLinksByUserId(
        userId: Int,
        page: Int = 1,
        limit: Int = 20,
        artworkId: Int? = null,
        linkType: String? = null
    ): Pair<List<LinkResponse>, Long> {
        return transaction {
            // Build query with filters
            var query = UserLinks.selectAll().where { UserLinks.userId eq userId }

            // Apply artworkId filter if provided
            if (artworkId != null) {
                query = query.andWhere { UserLinks.artworkId eq artworkId }
            }

            // Apply linkType filter if provided
            if (linkType != null) {
                query = query.andWhere { UserLinks.linkType eq linkType }
            }

            // Get total count
            val totalCount = query.count()

            // Calculate pagination
            val offset = ((page - 1) * limit).toLong()

            // Get paginated results
            val links = query
                .orderBy(UserLinks.createdAt, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { rowToLink(it) }

            Pair(links, totalCount)
        }
    }

    /**
     * Delete link by ID
     */
    fun deleteLink(linkId: Int, userId: Int): Boolean {
        return transaction {
            UserLinks.deleteWhere {
                (UserLinks.id eq linkId) and (UserLinks.userId eq userId)
            } > 0
        }
    }

    /**
     * Check if link exists and belongs to user
     */
    fun isLinkOwnedByUser(linkId: Int, userId: Int): Boolean {
        return transaction {
            UserLinks.selectAll()
                .where { (UserLinks.id eq linkId) and (UserLinks.userId eq userId) }
                .count() > 0
        }
    }

    /**
     * Count links by user ID
     */
    fun countLinksByUserId(userId: Int): Long {
        return transaction {
            UserLinks.selectAll()
                .where { UserLinks.userId eq userId }
                .count()
        }
    }

    /**
     * Find links by artwork ID
     */
    fun findLinksByArtworkId(artworkId: Int, limit: Int = 10): List<LinkResponse> {
        return transaction {
            UserLinks.selectAll()
                .where { UserLinks.artworkId eq artworkId }
                .orderBy(UserLinks.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { rowToLink(it) }
        }
    }

    /**
     * Find links by docent session ID
     */
    fun findLinksByDocentSessionId(sessionId: Int, limit: Int = 10): List<LinkResponse> {
        return transaction {
            UserLinks.selectAll()
                .where { UserLinks.docentSessionId eq sessionId }
                .orderBy(UserLinks.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { rowToLink(it) }
        }
    }

    /**
     * Convert database row to LinkResponse
     */
    private fun rowToLink(row: ResultRow): LinkResponse {
        // Parse metadata JSON
        val metadataJson = row[UserLinks.metadata]
        val metadata = try {
            kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(metadataJson)
        } catch (e: Exception) {
            emptyMap()
        }

        return LinkResponse(
            id = row[UserLinks.id].value,
            userId = row[UserLinks.userId].value,
            url = row[UserLinks.url],
            title = row[UserLinks.title],
            description = row[UserLinks.description],
            artworkId = row[UserLinks.artworkId]?.value,
            docentSessionId = row[UserLinks.docentSessionId]?.value,
            linkType = row[UserLinks.linkType],
            metadata = metadata,
            thumbnailUrl = row[UserLinks.thumbnailUrl],
            hasAudioDescription = row[UserLinks.hasAudioDescription],
            hasSubtitles = row[UserLinks.hasSubtitles],
            createdAt = row[UserLinks.createdAt].toString()
        )
    }
}