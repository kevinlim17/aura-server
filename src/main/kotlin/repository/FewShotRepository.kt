package com.kevin.repository

import com.kevin.db.extraTables.FewShotExamplesTable
import com.kevin.db.extraTables.FewShotExampleResourcesTable
import com.kevin.db.extraTables.ResourceType
import com.kevin.db.*
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Few-Shot Repository for database operations
 * Handles CRUD operations for few_shot_examples and few_shot_example_resources tables
 */
class FewShotRepository {

    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

    /**
     * Create a new few-shot example
     */
    fun createFewShot(
        artworkId: Int,
        userContextSummary: String,
        exemplarText: String,
        sourceDocentSessionId: Int?,
        sourceFeedbackId: Int?,
        userContextIds: List<Int> = emptyList(),
        userLinkIds: List<Int> = emptyList(),
        userMemoIds: List<Int> = emptyList(),
        companionInput: String? = null,
        promptMetadata: String = "{}",
        qualityScore: Double,
        effectivenessScore: Double? = null,
        diversityScore: Double? = null,
        category: String? = null
    ): Int {
        return transaction {
            val now = Clock.System.now()

            FewShotExamplesTable.insertAndGetId {
                it[FewShotExamplesTable.artworkId] = artworkId
                it[FewShotExamplesTable.userContextSummary] = userContextSummary
                it[FewShotExamplesTable.exemplarText] = exemplarText
                it[FewShotExamplesTable.sourceDocentSessionId] = sourceDocentSessionId
                it[FewShotExamplesTable.sourceFeedbackId] = sourceFeedbackId
                it[FewShotExamplesTable.userContextIds] = userContextIds
                it[FewShotExamplesTable.userLinkIds] = userLinkIds
                it[FewShotExamplesTable.userMemoIds] = userMemoIds
                it[FewShotExamplesTable.companionInput] = companionInput
                it[FewShotExamplesTable.promptMetadata] = promptMetadata
                it[FewShotExamplesTable.qualityScore] = qualityScore.toBigDecimal()
                it[FewShotExamplesTable.effectivenessScore] = effectivenessScore?.toBigDecimal()
                it[FewShotExamplesTable.diversityScore] = diversityScore?.toBigDecimal()
                it[FewShotExamplesTable.category] = category
                it[FewShotExamplesTable.isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }.value
        }
    }

    /**
     * Add resource link to few-shot example
     */
    fun addResource(
        fewShotExampleId: Int,
        resourceType: ResourceType,
        resourceId: Int,
        relevanceScore: Double
    ): Int {
        return transaction {
            val now = Clock.System.now()

            FewShotExampleResourcesTable.insertAndGetId {
                it[FewShotExampleResourcesTable.fewShotExampleId] = fewShotExampleId
                it[FewShotExampleResourcesTable.resourceType] = resourceType.value
                it[FewShotExampleResourcesTable.resourceId] = resourceId
                it[FewShotExampleResourcesTable.relevanceScore] = relevanceScore.toBigDecimal()
                it[createdAt] = now
            }.value
        }
    }

    /**
     * Find few-shot example by ID
     */
    fun findFewShotById(fewShotId: Int): FewShotExampleDTO? {
        return transaction {
            (FewShotExamplesTable innerJoin Artworks)
                .selectAll()
                .where { FewShotExamplesTable.id eq fewShotId }
                .map { rowToFewShot(it) }
                .singleOrNull()
        }
    }

    /**
     * Find few-shot examples by artwork ID
     */
    fun findFewShotsByArtworkId(
        artworkId: Int,
        limit: Int = 10
    ): List<FewShotExampleDTO> {
        return transaction {
            (FewShotExamplesTable innerJoin Artworks)
                .selectAll()
                .where {
                    (FewShotExamplesTable.artworkId eq artworkId) and
                            (FewShotExamplesTable.isActive eq true)
                }
                .orderBy(FewShotExamplesTable.qualityScore, SortOrder.DESC)
                .limit(limit)
                .map { rowToFewShot(it) }
        }
    }

    /**
     * Find top few-shot examples by quality score
     */
    fun findTopFewShots(
        minQualityScore: Double = 4.0,
        limit: Int = 10
    ): List<FewShotExampleDTO> {
        return transaction {
            (FewShotExamplesTable innerJoin Artworks)
                .selectAll()
                .where {
                    (FewShotExamplesTable.qualityScore greaterEq minQualityScore.toBigDecimal()) and
                            (FewShotExamplesTable.isActive eq true)
                }
                .orderBy(FewShotExamplesTable.effectivenessScore, SortOrder.DESC)
                .orderBy(FewShotExamplesTable.qualityScore, SortOrder.DESC)
                .limit(limit)
                .map { rowToFewShot(it) }
        }
    }

    /**
     * Find few-shot examples with pagination
     */
    fun findAllFewShots(
        page: Int = 1,
        limit: Int = 20,
        activeOnly: Boolean = true
    ): Pair<List<FewShotExampleDTO>, Long> {
        return transaction {
            val offset = ((page - 1) * limit).toLong()

            // Build query
            val query = (FewShotExamplesTable innerJoin Artworks)
                .selectAll()

            // Apply active filter
            val filteredQuery = if (activeOnly) {
                query.where { FewShotExamplesTable.isActive eq true }
            } else {
                query
            }

            // Get total count
            val totalCount = filteredQuery.count()

            // Get examples
            val examples = filteredQuery
                .orderBy(FewShotExamplesTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .offset(offset)
                .map { rowToFewShot(it) }

            Pair(examples, totalCount)
        }
    }

    /**
     * Update few-shot effectiveness and diversity scores
     */
    fun updateScores(
        fewShotId: Int,
        effectivenessScore: Double?,
        diversityScore: Double?
    ): Boolean {
        return transaction {
            val now = Clock.System.now()

            FewShotExamplesTable.update({ FewShotExamplesTable.id eq fewShotId }) {
                if (effectivenessScore != null) {
                    it[FewShotExamplesTable.effectivenessScore] = effectivenessScore.toBigDecimal()
                }
                if (diversityScore != null) {
                    it[FewShotExamplesTable.diversityScore] = diversityScore.toBigDecimal()
                }
                it[updatedAt] = now
            } > 0
        }
    }

    /**
     * Update usage count and last used timestamp
     */
    fun updateUsage(fewShotId: Int): Boolean {
        return transaction {
            val now = Clock.System.now()

            // Get current usage count
            val currentUsage = FewShotExamplesTable
                .select(FewShotExamplesTable.usageCount)
                .where { FewShotExamplesTable.id eq fewShotId }
                .singleOrNull()
                ?.get(FewShotExamplesTable.usageCount) ?: 0

            FewShotExamplesTable.update({ FewShotExamplesTable.id eq fewShotId }) {
                it[usageCount] = currentUsage + 1
                it[lastUsedAt] = now
            } > 0
        }
    }

    /**
     * Deactivate few-shot example
     */
    fun deactivateFewShot(fewShotId: Int): Boolean {
        return transaction {
            val now = Clock.System.now()

            FewShotExamplesTable.update({ FewShotExamplesTable.id eq fewShotId }) {
                it[isActive] = false
                it[updatedAt] = now
            } > 0
        }
    }

    /**
     * Get resources for a few-shot example
     */
    fun getResources(fewShotExampleId: Int): List<FewShotResource> {
        return transaction {
            FewShotExampleResourcesTable
                .selectAll()
                .where { FewShotExampleResourcesTable.fewShotExampleId eq fewShotExampleId }
                .map { row ->
                    FewShotResource(
                        id = row[FewShotExampleResourcesTable.id].value,
                        fewShotExampleId = row[FewShotExampleResourcesTable.fewShotExampleId].value,
                        resourceType = row[FewShotExampleResourcesTable.resourceType],
                        resourceId = row[FewShotExampleResourcesTable.resourceId],
                        relevanceScore = row[FewShotExampleResourcesTable.relevanceScore].toDouble(),
                        createdAt = row[FewShotExampleResourcesTable.createdAt]
                            .toJavaInstant()
                            .atZone(ZoneId.systemDefault())
                            .format(dateFormatter)
                    )
                }
        }
    }

    /**
     * Find few-shot examples by source session ID
     */
    fun findBySourceSessionId(sessionId: Int): FewShotExampleDTO? {
        return transaction {
            (FewShotExamplesTable innerJoin Artworks)
                .selectAll()
                .where { FewShotExamplesTable.sourceDocentSessionId eq sessionId }
                .map { rowToFewShot(it) }
                .singleOrNull()
        }
    }

    /**
     * Find few-shot examples by user (via source docent sessions)
     */
    fun findFewShotsByUserId(
        userId: Int,
        limit: Int = 20,
        minQualityScore: Double = 4.0
    ): List<FewShotExampleDTO> {
        return transaction {
            val docentRepository = DocentSessionRepository()

            // Get user's docent session IDs
            val userSessionIds = docentRepository.findSessionsByUserId(userId, 1, 1000)
                .sessions
                .map { it.id }

            // Find few-shots created from those sessions
            (FewShotExamplesTable innerJoin Artworks)
                .selectAll()
                .where {
                    (FewShotExamplesTable.sourceDocentSessionId inList userSessionIds) and
                    (FewShotExamplesTable.qualityScore greaterEq minQualityScore.toBigDecimal()) and
                    (FewShotExamplesTable.isActive eq true)
                }
                .orderBy(FewShotExamplesTable.qualityScore, SortOrder.DESC)
                .limit(limit)
                .map { rowToFewShot(it) }
        }
    }

    /**
     * Get few-shot statistics
     */
    fun getStatistics(): FewShotStatistics {
        return transaction {
            val totalExamples = FewShotExamplesTable.selectAll().count().toInt()
            val activeExamples = FewShotExamplesTable
                .selectAll()
                .where { FewShotExamplesTable.isActive eq true }
                .count()
                .toInt()

            // Average scores
            val avgQuality = FewShotExamplesTable
                .select(FewShotExamplesTable.qualityScore.avg())
                .where { FewShotExamplesTable.isActive eq true }
                .firstOrNull()
                ?.get(FewShotExamplesTable.qualityScore.avg())
                ?.toDouble() ?: 0.0

            val avgEffectiveness = FewShotExamplesTable
                .select(FewShotExamplesTable.effectivenessScore.avg())
                .where {
                    (FewShotExamplesTable.isActive eq true) and
                            (FewShotExamplesTable.effectivenessScore.isNotNull())
                }
                .firstOrNull()
                ?.get(FewShotExamplesTable.effectivenessScore.avg())
                ?.toDouble() ?: 0.0

            // Total usage
            val totalUsage = FewShotExamplesTable
                .select(FewShotExamplesTable.usageCount.sum())
                .where { FewShotExamplesTable.isActive eq true }
                .firstOrNull()
                ?.get(FewShotExamplesTable.usageCount.sum()) ?: 0

            // Examples by category
            val byCategory = FewShotExamplesTable
                .select(FewShotExamplesTable.category, FewShotExamplesTable.id.count())
                .where {
                    (FewShotExamplesTable.isActive eq true) and
                            (FewShotExamplesTable.category.isNotNull())
                }
                .groupBy(FewShotExamplesTable.category)
                .associate { row ->
                    (row[FewShotExamplesTable.category] ?: "UNCATEGORIZED") to row[FewShotExamplesTable.id.count()].toInt()
                }

            // Top performing
            val topExamples = findTopFewShots(limit = 5)

            FewShotStatistics(
                totalExamples = totalExamples,
                activeExamples = activeExamples,
                averageQualityScore = avgQuality,
                averageEffectivenessScore = avgEffectiveness,
                totalUsageCount = totalUsage.toInt(),
                examplesByCategory = byCategory,
                topPerformingExamples = topExamples
            )
        }
    }

    /**
     * Convert database row to FewShotExampleDTO
     */
    private fun rowToFewShot(row: ResultRow): FewShotExampleDTO {
        return FewShotExampleDTO(
            id = row[FewShotExamplesTable.id].value,
            artworkId = row[FewShotExamplesTable.artworkId].value,
            artworkInfo = ArtworkSummary(
                id = row[Artworks.id].value,
                title = row[Artworks.title],
                artist = row[Artworks.artist],
                artworkType = row[Artworks.artworkType],
                genre = row[Artworks.genre],
                creationYear = row[Artworks.creationYear],
                museum = row[Artworks.museum]
            ),
            userContextSummary = row[FewShotExamplesTable.userContextSummary],
            exemplarText = row[FewShotExamplesTable.exemplarText],
            qualityScore = row[FewShotExamplesTable.qualityScore].toDouble(),
            effectivenessScore = row[FewShotExamplesTable.effectivenessScore]?.toDouble(),
            diversityScore = row[FewShotExamplesTable.diversityScore]?.toDouble(),
            usageCount = row[FewShotExamplesTable.usageCount],
            lastUsedAt = row[FewShotExamplesTable.lastUsedAt]
                ?.toJavaInstant()
                ?.atZone(ZoneId.systemDefault())
                ?.format(dateFormatter),
            createdAt = row[FewShotExamplesTable.createdAt]
                .toJavaInstant()
                .atZone(ZoneId.systemDefault())
                .format(dateFormatter),
            category = row[FewShotExamplesTable.category],
            isActive = row[FewShotExamplesTable.isActive]
        )
    }
}