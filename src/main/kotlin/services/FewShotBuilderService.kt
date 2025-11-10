package com.kevin.services

import com.kevin.db.extraTables.ResourceType
import com.kevin.model.dto.*
import com.kevin.repository.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import kotlin.math.exp
import kotlin.math.min

/**
 * Few-Shot Builder Service
 * Manages creation, selection, and quality evaluation of few-shot examples
 */
class FewShotBuilderService(
    private val fewShotRepository: FewShotRepository = FewShotRepository(),
    private val docentSessionRepository: DocentSessionRepository = DocentSessionRepository(),
    private val feedbackRepository: FeedbackRepository = FeedbackRepository(),
    private val artworkRepository: ArtworkRepository = ArtworkRepository(),
    private val userContextRepository: UserContextRepository = UserContextRepository(),
    private val linkRepository: LinkRepository = LinkRepository(),
    private val memoRepository: MemoRepository = MemoRepository()
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 1. Build few-shot example from a docent session and feedback
     */
    fun buildFewShotFromSession(
        docentSessionId: Int,
        feedbackId: Int,
        companionInput: String? = null,
        category: String? = null
    ): FewShotExampleDTO {
        // Get docent session
        val session = docentSessionRepository.findSessionById(docentSessionId)
            ?: throw Exception("Docent session not found: $docentSessionId")

        // Get feedback
        val feedback = feedbackRepository.findFeedbackById(feedbackId)
            ?: throw Exception("Feedback not found: $feedbackId")

        // Ensure this is a high-quality session (feedback >= 4.0)
        val qualityScore = feedback.overallSatisfaction ?: 0.0
        if (qualityScore < 4.0) {
            throw Exception("Feedback score too low to create few-shot example: $qualityScore")
        }

        // Extract resources used in the session
        val userContextIds = extractContextIdsFromPrompt(session.promptContext ?: "")
        val userLinkIds = emptyList<Int>() // Could be extracted if stored in session
        val userMemoIds = emptyList<Int>() // Could be extracted if stored in session

        // Build prompt metadata
        val promptMetadata = buildPromptMetadata(session)

        // Calculate initial scores
        val effectivenessScore = calculateEffectivenessScoreForNew(qualityScore ?: 4.0)
        val diversityScore = calculateInitialDiversityScore(session.artworkId)

        // Create few-shot example
        val fewShotId = fewShotRepository.createFewShot(
            artworkId = session.artworkId,
            userContextSummary = session.promptContext ?: "No context provided",
            exemplarText = session.generatedText,
            sourceDocentSessionId = docentSessionId,
            sourceFeedbackId = feedbackId,
            userContextIds = userContextIds,
            userLinkIds = userLinkIds,
            userMemoIds = userMemoIds,
            companionInput = companionInput,
            promptMetadata = promptMetadata,
            qualityScore = qualityScore ?: 4.0,
            effectivenessScore = effectivenessScore,
            diversityScore = diversityScore,
            category = category ?: inferCategory(session)
        )

        // Add resource links
        userContextIds.forEach { contextId ->
            fewShotRepository.addResource(
                fewShotExampleId = fewShotId,
                resourceType = ResourceType.CONTEXT,
                resourceId = contextId,
                relevanceScore = 0.8 // Could be calculated based on usage
            )
        }

        return fewShotRepository.findFewShotById(fewShotId)
            ?: throw Exception("Failed to retrieve created few-shot example")
    }

    /**
     * 2. Calculate effectiveness score for a few-shot example
     */
    fun calculateEffectivenessScore(fewShotId: Int): FewShotEffectivenessMetrics {
        val fewShot = fewShotRepository.findFewShotById(fewShotId)
            ?: throw Exception("Few-shot example not found: $fewShotId")

        // 1. Feedback Score (40% weight)
        val feedbackScore = fewShot.qualityScore / 5.0 // Normalize to 0-1

        // 2. Usage Frequency (30% weight)
        // More usage = more effective (up to a point)
        val usageFrequency = min(fewShot.usageCount.toDouble() / 20.0, 1.0)

        // 3. Recency (20% weight)
        // More recent examples are generally more relevant
        val recency = calculateRecencyScore(fewShot.createdAt)

        // 4. Resource Diversity (10% weight)
        // More diverse resources = more versatile example
        val resources = fewShotRepository.getResources(fewShotId)
        val resourceTypes = resources.map { it.resourceType }.distinct().size
        val resourceDiversity = min(resourceTypes.toDouble() / 3.0, 1.0) // Max 3 types

        // Calculate weighted overall effectiveness
        val overallEffectiveness = (
                feedbackScore * 0.4 +
                        usageFrequency * 0.3 +
                        recency * 0.2 +
                        resourceDiversity * 0.1
                ) * 100.0 // Scale to 0-100

        // Update the score in database
        fewShotRepository.updateScores(
            fewShotId = fewShotId,
            effectivenessScore = overallEffectiveness,
            diversityScore = null // Keep existing diversity score
        )

        return FewShotEffectivenessMetrics(
            fewShotId = fewShotId,
            feedbackScore = feedbackScore * 100,
            usageFrequency = usageFrequency * 100,
            recency = recency * 100,
            resourceDiversity = resourceDiversity * 100,
            overallEffectiveness = overallEffectiveness
        )
    }

    /**
     * 3. Select top few-shot examples based on criteria
     */
    fun selectTopFewShots(criteria: FewShotSelectionCriteria): List<FewShotExampleDTO> {
        // Get all high-quality examples
        val candidates = fewShotRepository.findTopFewShots(
            minQualityScore = criteria.minQualityScore,
            limit = criteria.maxResults * 3 // Get more candidates for filtering
        )

        // Score each candidate based on criteria
        val scoredCandidates = candidates.map { fewShot ->
            val score = scoreFewShotForSelection(fewShot, criteria)
            Pair(fewShot, score)
        }.sortedByDescending { it.second }

        // Apply diversity filter if requested
        val selected = if (criteria.ensureDiversity) {
            applyDiversityFilter(scoredCandidates, criteria.maxResults)
        } else {
            scoredCandidates.take(criteria.maxResults).map { it.first }
        }

        // Update usage count for selected examples
        selected.forEach { fewShot ->
            fewShotRepository.updateUsage(fewShot.id)
        }

        return selected
    }

    /**
     * 4. Update few-shot quality based on new feedback
     */
    fun updateFewShotQuality(fewShotId: Int, newFeedbackScore: Double) {
        val fewShot = fewShotRepository.findFewShotById(fewShotId)
            ?: throw Exception("Few-shot example not found: $fewShotId")

        // Calculate new average quality (weighted towards new feedback)
        val currentQuality = fewShot.qualityScore
        val usageCount = fewShot.usageCount
        val newQuality = if (usageCount > 0) {
            (currentQuality * usageCount + newFeedbackScore) / (usageCount + 1)
        } else {
            newFeedbackScore
        }

        // Recalculate effectiveness
        val effectiveness = calculateEffectivenessScore(fewShotId)

        // Deactivate if quality drops below threshold
        if (newQuality < 3.5 || effectiveness.overallEffectiveness < 50.0) {
            fewShotRepository.deactivateFewShot(fewShotId)
        }
    }

    /**
     * 5. Enrich few-shot with full resource details
     */
    fun enrichFewShotWithResources(fewShotId: Int): EnrichedFewShot {
        val fewShot = fewShotRepository.findFewShotById(fewShotId)
            ?: throw Exception("Few-shot example not found: $fewShotId")

        val resources = fewShotRepository.getResources(fewShotId)

        // Load contexts
        val contexts = resources
            .filter { it.resourceType == "CONTEXT" }
            .mapNotNull { resource ->
                userContextRepository.findContextById(resource.resourceId)
            }

        // Load links
        val links = resources
            .filter { it.resourceType == "LINK" }
            .mapNotNull { resource ->
                linkRepository.findLinkById(resource.resourceId)
            }

        // Load memos
        val memos = resources
            .filter { it.resourceType == "MEMO" }
            .mapNotNull { resource ->
                memoRepository.findMemoById(resource.resourceId)
            }

        // Get companion input from DB
        // (This would need to be retrieved from the few_shot_examples table)
        val companionInput = null // TODO: Retrieve from DB

        return EnrichedFewShot(
            example = fewShot,
            contexts = contexts,
            links = links,
            memos = memos,
            companionInput = companionInput,
            promptMetadata = "{}" // TODO: Parse from DB
        )
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Extract context IDs from prompt context string
     * This is a simplified version - in production, IDs should be stored explicitly
     */
    private fun extractContextIdsFromPrompt(promptContext: String): List<Int> {
        // In a real implementation, context IDs should be stored in a structured way
        // For now, return empty list
        return emptyList()
    }

    /**
     * Build prompt metadata from session
     */
    private fun buildPromptMetadata(session: DocentSessionResponse): String {
        val metadata = buildJsonObject {
            put("persona", session.promptPersona ?: "")
            put("task", session.promptTask ?: "")
            put("form", session.promptForm ?: "")
            put("geminiModel", session.geminiModel ?: "")
            put("temperature", session.geminiTemperature ?: 0.0)
            put("createdAt", session.createdAt)
        }
        return metadata.toString()
    }

    /**
     * Calculate effectiveness score for newly created few-shot
     */
    private fun calculateEffectivenessScoreForNew(qualityScore: Double): Double {
        // New examples get a base score based on quality
        return (qualityScore / 5.0) * 80.0 // 80% of max for new examples
    }

    /**
     * Calculate initial diversity score
     */
    private fun calculateInitialDiversityScore(artworkId: Int): Double {
        // Check how many examples already exist for this artwork
        val existingExamples = fewShotRepository.findFewShotsByArtworkId(artworkId, limit = 100)
        val count = existingExamples.size

        // More examples = less diverse
        return if (count == 0) {
            100.0
        } else {
            100.0 * exp(-count / 10.0) // Exponential decay
        }
    }

    /**
     * Calculate recency score (more recent = higher score)
     */
    private fun calculateRecencyScore(createdAtStr: String): Double {
        return try {
            val createdAt = Instant.parse(createdAtStr)
            val now = Clock.System.now()
            val daysSinceCreation = (now - createdAt).inWholeDays

            // Decay over 180 days (6 months)
            exp(-daysSinceCreation.toDouble() / 180.0)
        } catch (e: Exception) {
            0.5 // Default middle score if parsing fails
        }
    }

    /**
     * Infer category from session
     */
    private fun inferCategory(session: DocentSessionResponse): String {
        return when {
            session.promptPersona?.contains("emotional", ignoreCase = true) == true -> "EMOTIONAL"
            session.promptPersona?.contains("analytical", ignoreCase = true) == true -> "ANALYTICAL"
            session.promptPersona?.contains("poetic", ignoreCase = true) == true -> "POETIC"
            else -> "LITERARY"
        }
    }

    /**
     * Score a few-shot for selection based on criteria
     */
    private fun scoreFewShotForSelection(
        fewShot: FewShotExampleDTO,
        criteria: FewShotSelectionCriteria
    ): Double {
        var score = 0.0

        // Base quality score (30%)
        score += (fewShot.qualityScore / 5.0) * 30.0

        // Effectiveness score (30%)
        score += ((fewShot.effectivenessScore ?: 50.0) / 100.0) * 30.0

        // Artwork match (20%)
        if (criteria.artworkId != null && fewShot.artworkId == criteria.artworkId) {
            score += 20.0
        } else if (criteria.artworkStyle != null &&
            fewShot.artworkInfo.genre?.contains(criteria.artworkStyle, ignoreCase = true) == true
        ) {
            score += 10.0
        }

        // Diversity score (20%)
        score += ((fewShot.diversityScore ?: 50.0) / 100.0) * 20.0

        return score
    }

    /**
     * Apply diversity filter to ensure variety in selected examples
     */
    private fun applyDiversityFilter(
        scoredCandidates: List<Pair<FewShotExampleDTO, Double>>,
        maxResults: Int
    ): List<FewShotExampleDTO> {
        val selected = mutableListOf<FewShotExampleDTO>()
        val usedArtworks = mutableSetOf<Int>()
        val usedCategories = mutableSetOf<String?>()

        for ((fewShot, _) in scoredCandidates) {
            if (selected.size >= maxResults) break

            // Prefer diverse artworks and categories
            val artworkUsed = usedArtworks.contains(fewShot.artworkId)
            val categoryUsed = usedCategories.contains(fewShot.category)

            if (!artworkUsed || !categoryUsed || selected.size < maxResults / 2) {
                selected.add(fewShot)
                usedArtworks.add(fewShot.artworkId)
                usedCategories.add(fewShot.category)
            }
        }

        // Fill remaining slots if needed
        if (selected.size < maxResults) {
            scoredCandidates
                .map { it.first }
                .filterNot { selected.contains(it) }
                .take(maxResults - selected.size)
                .forEach { selected.add(it) }
        }

        return selected
    }

    /**
     * Get few-shot examples for a specific user
     */
    fun getUserFewShots(
        userId: Int,
        limit: Int = 20,
        minQualityScore: Double = 4.0
    ): List<FewShotExampleDTO> {
        return fewShotRepository.findFewShotsByUserId(userId, limit, minQualityScore)
    }

    /**
     * Get user-specific few-shot statistics
     */
    fun getUserFewShotStatistics(userId: Int): Map<String, Any> {
        val userFewShots = fewShotRepository.findFewShotsByUserId(userId, limit = 1000, minQualityScore = 0.0)

        val totalCount = userFewShots.size
        val activeCount = userFewShots.count { it.isActive }
        val avgQuality = if (totalCount > 0) {
            userFewShots.map { it.qualityScore }.average()
        } else {
            0.0
        }
        val avgEffectiveness = if (totalCount > 0) {
            userFewShots.mapNotNull { it.effectivenessScore }.average()
        } else {
            0.0
        }
        val totalUsage = userFewShots.sumOf { it.usageCount }
        val byCategory = userFewShots
            .filter { it.isActive }
            .groupBy { it.category ?: "UNCATEGORIZED" }
            .mapValues { it.value.size }

        return mapOf(
            "totalExamples" to totalCount,
            "activeExamples" to activeCount,
            "averageQualityScore" to avgQuality,
            "averageEffectivenessScore" to avgEffectiveness,
            "totalUsageCount" to totalUsage,
            "examplesByCategory" to byCategory
        )
    }

    /**
     * Deactivate a few-shot example (with user verification)
     */
    fun deactivateFewShot(fewShotId: Int, userId: Int) {
        val fewShot = fewShotRepository.findFewShotById(fewShotId)
            ?: throw Exception("Few-shot example not found: $fewShotId")

        // Verify ownership via source session
        if (fewShot.artworkId > 0) {
            val session = docentSessionRepository.findSessionById(fewShot.artworkId)
            if (session != null && session.userId != userId) {
                throw Exception("Unauthorized: User does not own this few-shot example")
            }
        }

        fewShotRepository.deactivateFewShot(fewShotId)
    }

    /**
     * Recalculate quality scores for all few-shot examples
     */
    fun recalculateAllFewShotQuality() {
        val allFewShots = fewShotRepository.findAllFewShots(page = 1, limit = 1000, activeOnly = false).first

        allFewShots.forEach { fewShot ->
            try {
                calculateEffectivenessScore(fewShot.id)
            } catch (e: Exception) {
                println("⚠ Failed to recalculate quality for Few-Shot ${fewShot.id}: ${e.message}")
            }
        }

        println("✓ Recalculated quality for ${allFewShots.size} few-shot examples")
    }

    /**
     * Clean up low quality few-shot examples
     */
    fun cleanupLowQualityFewShots(minQualityThreshold: Double = 3.0): Int {
        val allFewShots = fewShotRepository.findAllFewShots(page = 1, limit = 1000, activeOnly = true).first

        var deactivatedCount = 0

        allFewShots.forEach { fewShot ->
            if (fewShot.qualityScore < minQualityThreshold) {
                try {
                    fewShotRepository.deactivateFewShot(fewShot.id)
                    deactivatedCount++
                } catch (e: Exception) {
                    println("⚠ Failed to deactivate Few-Shot ${fewShot.id}: ${e.message}")
                }
            }
        }

        println("✓ Deactivated $deactivatedCount low-quality few-shot examples (threshold: $minQualityThreshold)")
        return deactivatedCount
    }

    /**
     * Get few-shot statistics
     */
    fun getStatistics(): FewShotStatistics {
        return fewShotRepository.getStatistics()
    }
}