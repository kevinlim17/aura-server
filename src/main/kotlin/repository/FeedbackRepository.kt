package com.kevin.repository

import com.kevin.db.DocentFeedbacks
import com.kevin.db.DocentSessions
import com.kevin.db.Artworks
import com.kevin.model.dto.*
import io.ktor.util.collections.getValue
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Feedback Repository for database operations
 * Handles CRUD operations for docent_feedbacks table
 */
class FeedbackRepository {

    /**
     * Create new feedback for a docent session
     */
    fun createFeedback(
        docentSessionId: Int,
        userId: Int,
        emotionalResonance: Int,
        imaginativeEngagement: Int?,
        emotionalImpact: Int?,
        comment: String?,
        improvementSuggestions: Map<String, String>?
    ): Int {
        return transaction {
            val now = Clock.System.now()

            // Calculate overall satisfaction
            val scores = listOfNotNull(
                emotionalResonance,
                imaginativeEngagement,
                emotionalImpact
            )
            val overallSatisfaction = if (scores.isNotEmpty()) {
                scores.average()
            } else {
                emotionalResonance.toDouble()
            }

            // Determine if this is a few-shot candidate (average >= 4.0)
            val isFewShotCandidate = overallSatisfaction >= 4.0

            // Convert improvementSuggestions to JSON string
            val improvementJson = if (improvementSuggestions != null) {
                kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.serializer<Map<String, String>>(),
                    improvementSuggestions
                )
            } else {
                "{}"
            }

            DocentFeedbacks.insertAndGetId {
                it[DocentFeedbacks.docentSessionId] = docentSessionId
                it[DocentFeedbacks.userId] = userId
                it[DocentFeedbacks.emotionalResonance] = emotionalResonance
                it[DocentFeedbacks.imaginativeEngagement] = imaginativeEngagement
                it[DocentFeedbacks.emotionalImpact] = emotionalImpact
                it[DocentFeedbacks.overallSatisfaction] = overallSatisfaction.toBigDecimal()
                it[DocentFeedbacks.comment] = comment
                it[DocentFeedbacks.improvementSuggestions] = improvementJson
                it[DocentFeedbacks.isFewShotCandidate] = isFewShotCandidate
                it[createdAt] = now
            }.value
        }
    }

    /**
     * Find feedback by ID
     */
    fun findFeedbackById(feedbackId: Int): FeedbackResponse? {
        return transaction {
            DocentFeedbacks.selectAll()
                .where { DocentFeedbacks.id eq feedbackId }
                .map { rowToFeedback(it) }
                .singleOrNull()
        }
    }

    /**
     * Find feedback by docent session ID
     */
    fun findFeedbackBySessionId(sessionId: Int): FeedbackResponse? {
        return transaction {
            DocentFeedbacks.selectAll()
                .where { DocentFeedbacks.docentSessionId eq sessionId }
                .map { rowToFeedback(it) }
                .singleOrNull()
        }
    }

    /**
     * Find feedback by user ID with pagination
     */
    fun findFeedbacksByUserId(
        userId: Int,
        page: Int = 1,
        limit: Int = 20
    ): List<FeedbackResponse> {
        return transaction {
            val offset = ((page - 1) * limit).toLong()

            DocentFeedbacks.selectAll()
                .where { DocentFeedbacks.userId eq userId }
                .orderBy(DocentFeedbacks.createdAt, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { rowToFeedback(it) }
        }
    }

    /**
     * Find all feedbacks with pagination (for admin)
     */
    fun findAllFeedbacks(
        page: Int = 1,
        limit: Int = 20
    ): Pair<List<FeedbackWithSessionResponse>, Long> {
        return transaction {
            // Get total count
            val totalCount = DocentFeedbacks.selectAll().count()

            // Calculate pagination
            val offset = ((page - 1) * limit).toLong()

            // Get feedbacks with session details
            val feedbacks = (DocentFeedbacks innerJoin DocentSessions innerJoin Artworks)
                .select(
                    DocentFeedbacks.id,
                    DocentFeedbacks.docentSessionId,
                    DocentFeedbacks.userId,
                    DocentFeedbacks.emotionalResonance,
                    DocentFeedbacks.imaginativeEngagement,
                    DocentFeedbacks.emotionalImpact,
                    DocentFeedbacks.overallSatisfaction,
                    DocentFeedbacks.comment,
                    DocentFeedbacks.improvementSuggestions,
                    DocentFeedbacks.isFewShotCandidate,
                    DocentFeedbacks.fewShotSelectedAt,
                    DocentFeedbacks.createdAt,
                    DocentSessions.id,
                    DocentSessions.artworkId,
                    DocentSessions.generatedText,
                    DocentSessions.status,
                    Artworks.title,
                    Artworks.artist
                )
                .orderBy(DocentFeedbacks.createdAt, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { row ->
                    FeedbackWithSessionResponse(
                        feedback = rowToFeedback(row),
                        session = DocentSessionResponse(
                            id = row[DocentSessions.id].value,
                            userId = row[DocentSessions.userId].value,
                            artworkId = row[DocentSessions.artworkId].value,
                            promptTemplate = row[DocentSessions.promptTemplate],
                            artworkTitle = row[Artworks.title],
                            artworkArtist = row[Artworks.artist],
                            generatedText = row[DocentSessions.generatedText],
                            ttsAudioUrl = null,
                            ttsDurationSeconds = null,
                            playCount = 0,
                            totalListeningSeconds = 0,
                            completionRate = null,
                            status = row[DocentSessions.status],
                            createdAt = row[DocentSessions.createdAt].toString()
                        )
                    )
                }

            Pair(feedbacks, totalCount)
        }
    }

    /**
     * Get feedback statistics
     */
    fun getFeedbackStatistics(): FeedbackStatistics {
        return transaction {
            val totalFeedbacks = DocentFeedbacks.selectAll().count()

            if (totalFeedbacks == 0L) {
                return@transaction FeedbackStatistics(
                    totalFeedbacks = 0,
                    averageEmotionalResonance = 0.0,
                    averageImaginativeEngagement = 0.0,
                    averageEmotionalImpact = 0.0,
                    averageOverallSatisfaction = 0.0,
                    fewShotCandidateCount = 0,
                    feedbackWithCommentCount = 0
                )
            }

            val avgEmotionalResonance = DocentFeedbacks
                .select(DocentFeedbacks.emotionalResonance.avg())
                .first()[DocentFeedbacks.emotionalResonance.avg()]?.toDouble() ?: 0.0

            val avgImaginativeEngagement = DocentFeedbacks
                .select(DocentFeedbacks.imaginativeEngagement.avg())
                .first()[DocentFeedbacks.imaginativeEngagement.avg()]?.toDouble() ?: 0.0

            val avgEmotionalImpact = DocentFeedbacks
                .select(DocentFeedbacks.emotionalImpact.avg())
                .first()[DocentFeedbacks.emotionalImpact.avg()]?.toDouble() ?: 0.0

            val avgOverallSatisfaction = DocentFeedbacks
                .select(DocentFeedbacks.overallSatisfaction.avg())
                .first()[DocentFeedbacks.overallSatisfaction.avg()]?.toDouble() ?: 0.0

            val fewShotCandidateCount = DocentFeedbacks
                .selectAll()
                .where { DocentFeedbacks.isFewShotCandidate eq true }
                .count()

            val feedbackWithCommentCount = DocentFeedbacks
                .selectAll()
                .where { DocentFeedbacks.comment.isNotNull() }
                .count()

            FeedbackStatistics(
                totalFeedbacks = totalFeedbacks,
                averageEmotionalResonance = avgEmotionalResonance,
                averageImaginativeEngagement = avgImaginativeEngagement,
                averageEmotionalImpact = avgEmotionalImpact,
                averageOverallSatisfaction = avgOverallSatisfaction,
                fewShotCandidateCount = fewShotCandidateCount,
                feedbackWithCommentCount = feedbackWithCommentCount
            )
        }
    }

    /**
     * Mark feedback as few-shot example
     */
    fun markAsFewShot(feedbackId: Int): Boolean {
        return transaction {
            val now = Clock.System.now()
            DocentFeedbacks.update({ DocentFeedbacks.id eq feedbackId }) {
                it[isFewShotCandidate] = true
                it[fewShotSelectedAt] = now
            } > 0
        }
    }

    /**
     * Convert database row to FeedbackResponse
     */
    private fun rowToFeedback(row: ResultRow): FeedbackResponse {
        // Parse improvementSuggestions JSON
        val improvementJson = row[DocentFeedbacks.improvementSuggestions]
        val improvementSuggestions = try {
            kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(improvementJson)
        } catch (e: Exception) {
            emptyMap()
        }

        return FeedbackResponse(
            id = row[DocentFeedbacks.id].value,
            docentSessionId = row[DocentFeedbacks.docentSessionId].value,
            userId = row[DocentFeedbacks.userId].value,
            emotionalResonance = row[DocentFeedbacks.emotionalResonance],
            imaginativeEngagement = row[DocentFeedbacks.imaginativeEngagement],
            emotionalImpact = row[DocentFeedbacks.emotionalImpact],
            overallSatisfaction = row[DocentFeedbacks.overallSatisfaction]?.toDouble(),
            comment = row[DocentFeedbacks.comment],
            improvementSuggestions = improvementSuggestions,
            isFewShotCandidate = row[DocentFeedbacks.isFewShotCandidate],
            fewShotSelectedAt = row[DocentFeedbacks.fewShotSelectedAt]?.toString(),
            createdAt = row[DocentFeedbacks.createdAt].toString()
        )
    }
}