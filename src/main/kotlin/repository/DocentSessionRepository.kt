package com.kevin.repository

import com.kevin.db.Artworks
import com.kevin.db.DocentSessions
import com.kevin.db.FewShotExamples
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

/**
 * Docent Session Repository for database operations
 * Handles CRUD operations for docent_sessions table
 */
class DocentSessionRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Create a new docent session
     */
    fun createSession(
        userId: Int,
        artworkId: Int,
        promptTemplate: String,
        promptPersona: String?,
        promptTask: String?,
        promptContext: String?,
        promptForm: String?,
        fewShotExamples: List<FewShotExample> = emptyList(),
        generatedText: String,
        geminiModel: String?,
        geminiTemperature: Double?,
        geminiTopP: Double?,
        geminiTopK: Int?,
        generationTimeMs: Int?,
        status: String = "COMPLETED"
    ): DocentSessionResponse? {
        return transaction {
            val now = Clock.System.now()
            val fewShotJson = if (fewShotExamples.isEmpty()) {
                "[]"
            } else {
                json.encodeToString(fewShotExamples)
            }

            val sessionId = DocentSessions.insertAndGetId {
                it[DocentSessions.userId] = userId
                it[DocentSessions.artworkId] = artworkId
                it[DocentSessions.promptTemplate] = promptTemplate
                it[DocentSessions.promptPersona] = promptPersona
                it[DocentSessions.promptTask] = promptTask
                it[DocentSessions.promptContext] = promptContext
                it[DocentSessions.promptForm] = promptForm
                it[DocentSessions.fewShotExamples] = fewShotJson
                it[DocentSessions.generatedText] = generatedText
                it[DocentSessions.geminiModel] = geminiModel
                it[DocentSessions.geminiTemperature] = geminiTemperature?.toBigDecimal()
                it[DocentSessions.geminiTopP] = geminiTopP?.toBigDecimal()
                it[DocentSessions.geminiTopK] = geminiTopK
                it[DocentSessions.generationTimeMs] = generationTimeMs
                it[DocentSessions.status] = status
                it[createdAt] = now
            }

            findSessionById(sessionId.value)
        }
    }

    /**
     * Find session by ID
     */
    fun findSessionById(sessionId: Int): DocentSessionResponse? {
        return transaction {
            DocentSessions.join(
                Artworks,
                JoinType.LEFT,
                DocentSessions.artworkId,
                Artworks.id
            )
                .selectAll()
                .where { DocentSessions.id eq sessionId }
                .map { rowToDocentSession(it) }
                .singleOrNull()
        }
    }

    /**
     * Find sessions by user ID with pagination
     */
    fun findSessionsByUserId(
        userId: Int,
        page: Int = 1,
        limit: Int = 20
    ): DocentHistoryResponse {
        return transaction {
            // Build query
            val query = DocentSessions.join(
                Artworks,
                JoinType.LEFT,
                DocentSessions.artworkId,
                Artworks.id
            )
                .selectAll()
                .where { DocentSessions.userId eq userId }

            // Get total count
            val totalCount = query.count()

            // Calculate pagination
            val totalPages = ((totalCount + limit - 1) / limit).toInt()
            val offset = ((page - 1) * limit).toLong()

            // Get paginated results
            val sessions = query
                .orderBy(DocentSessions.createdAt, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { rowToDocentSession(it) }

            DocentHistoryResponse(
                sessions = sessions,
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
     * Update session status
     */
    fun updateSessionStatus(sessionId: Int, status: String): Boolean {
        return transaction {
            DocentSessions.update({ DocentSessions.id eq sessionId }) {
                it[DocentSessions.status] = status
            } > 0
        }
    }

    /**
     * Update TTS audio information
     */
    fun updateTTSAudio(sessionId: Int, audioUrl: String, durationSeconds: Int): Boolean {
        return transaction {
            DocentSessions.update({ DocentSessions.id eq sessionId }) {
                it[ttsAudioUrl] = audioUrl
                it[ttsDurationSeconds] = durationSeconds
            } > 0
        }
    }

    /**
     * Update play statistics
     */
    fun updatePlayStats(
        sessionId: Int,
        playCount: Int? = null,
        totalListeningSeconds: Int? = null,
        completionRate: Double? = null
    ): DocentSessionResponse? {
        transaction {
            val now = Clock.System.now()
            DocentSessions.update({ DocentSessions.id eq sessionId }) {
                playCount?.let { count ->
                    it[DocentSessions.playCount] = count
                }
                totalListeningSeconds?.let { seconds ->
                    it[DocentSessions.totalListeningSeconds] = seconds
                }
                completionRate?.let { rate ->
                    it[DocentSessions.completionRate] = rate.toBigDecimal()
                }
                it[lastPlayedAt] = now
            }
        }
        return findSessionById(sessionId)
    }

    /**
     * Get few-shot examples for a specific artwork or general use
     */
    fun getFewShotExamples(artworkId: Int? = null, limit: Int = 3): List<FewShotExample> {
        return transaction {
            var query = FewShotExamples
                .selectAll()
                .where { FewShotExamples.isActive eq true }

            // If artworkId is provided, prefer examples for that artwork
            artworkId?.let {
                query = query.andWhere { FewShotExamples.artworkId eq it }
            }

            query
                .orderBy(FewShotExamples.qualityScore, SortOrder.DESC)
                .limit(limit)
                .map {
                    FewShotExample(
                        userContextSummary = it[FewShotExamples.userContextSummary],
                        exemplarText = it[FewShotExamples.exemplarText],
                        qualityScore = it[FewShotExamples.qualityScore].toDouble()
                    )
                }
        }
    }

    /**
     * Increment artwork's docent generation count
     */
    fun incrementArtworkDocentCount(artworkId: Int): Boolean {
        return transaction {
            Artworks.update({ Artworks.id eq artworkId }) {
                it[docentGenerationCount] = docentGenerationCount + 1
            } > 0
        }
    }

    /**
     * Convert database row to DocentSessionResponse
     */
    private fun rowToDocentSession(row: ResultRow): DocentSessionResponse {
        val fewShotExamplesJson = row[DocentSessions.fewShotExamples]
        val fewShotExamples = try {
            if (fewShotExamplesJson.isNotBlank() && fewShotExamplesJson != "[]") {
                json.decodeFromString<List<FewShotExample>>(fewShotExamplesJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        return DocentSessionResponse(
            id = row[DocentSessions.id].value,
            userId = row[DocentSessions.userId].value,
            artworkId = row[DocentSessions.artworkId].value,
            artworkTitle = row.getOrNull(Artworks.title),
            artworkArtist = row.getOrNull(Artworks.artist),
            artworkImageUrl = row.getOrNull(Artworks.thumbnailUrl) ?: row.getOrNull(Artworks.imageUrl),
            promptTemplate = row[DocentSessions.promptTemplate],
            promptPersona = row[DocentSessions.promptPersona],
            promptTask = row[DocentSessions.promptTask],
            promptContext = row[DocentSessions.promptContext],
            promptForm = row[DocentSessions.promptForm],
            fewShotExamples = fewShotExamples,
            generatedText = row[DocentSessions.generatedText],
            geminiModel = row[DocentSessions.geminiModel],
            geminiTemperature = row[DocentSessions.geminiTemperature]?.toDouble(),
            geminiTopP = row[DocentSessions.geminiTopP]?.toDouble(),
            geminiTopK = row[DocentSessions.geminiTopK],
            generationTimeMs = row[DocentSessions.generationTimeMs],
            ttsAudioUrl = row[DocentSessions.ttsAudioUrl],
            ttsDurationSeconds = row[DocentSessions.ttsDurationSeconds],
            playCount = row[DocentSessions.playCount],
            totalListeningSeconds = row[DocentSessions.totalListeningSeconds],
            completionRate = row[DocentSessions.completionRate]?.toDouble(),
            status = row[DocentSessions.status],
            createdAt = row[DocentSessions.createdAt].toString(),
            lastPlayedAt = row[DocentSessions.lastPlayedAt]?.toString()
        )
    }
}