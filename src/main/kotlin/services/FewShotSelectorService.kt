package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.ArtworkRepository
import com.kevin.repository.UserContextRepository
import com.kevin.repository.UserPreferencesRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.*

/**
 * Few-Shot Selector Service
 * Intelligently selects optimal few-shot examples for docent generation
 *
 * Features:
 * - Redis caching integration
 * - Multi-dimensional scoring
 * - Diversity filtering (MMR algorithm)
 * - TF-IDF context similarity
 */
class FewShotSelectorService(
    private val fewShotBuilderService: FewShotBuilderService = FewShotBuilderService(),
    private val redisCacheService: RedisFewShotCacheService = RedisFewShotCacheService(),
    private val artworkRepository: ArtworkRepository = ArtworkRepository(),
    private val userPreferencesRepository: UserPreferencesRepository = UserPreferencesRepository(),
    private val userContextRepository: UserContextRepository = UserContextRepository()
) {

    // ========================================================================
    // 1. Main Selection Algorithm
    // ========================================================================

    /**
     * Select optimal few-shot examples for docent generation
     *
     * 5-Step Process:
     * 1. Check Redis cache
     * 2. Load candidates from DB (if cache miss)
     * 3. Calculate composite scores
     * 4. Apply diversity filtering
     * 5. Cache results
     */
    fun selectOptimalFewShots(
        request: GenerateDocentRequest,
        userId: Int
    ): List<EnrichedFewShot> {
        // Step 1: Redis 캐시 확인
        val cached = redisCacheService.getFewShotPool(userId, request.artworkId)

        if (cached != null && cached.isNotEmpty()) {
            println("✓ Cache HIT: Using ${cached.size} cached few-shots")
            return cached.take(5)
        }

        println("⊘ Cache MISS: Loading from database...")

        // Step 2: DB 조회 및 스코어링
        val artwork = artworkRepository.findArtworkById(request.artworkId)
            ?: throw IllegalArgumentException("Artwork not found: ${request.artworkId}")

        val userPreferences = userPreferencesRepository.findPreferencesByUserId(userId)
            ?: throw IllegalArgumentException("User preferences not found: $userId")

        val criteria = FewShotSelectionCriteria(
            userId = userId,
            artworkId = request.artworkId,
            artworkStyle = artwork.genre, // Using genre as style
            artworkEra = artwork.creationYear?.toString(),
            preferredContextTypes = emptyList(), // Could be extended
            minQualityScore = 4.0,
            maxResults = 10, // Load more for filtering
            ensureDiversity = false // We'll apply our own diversity filter
        )

        val candidates = fewShotBuilderService.selectTopFewShots(criteria)

        if (candidates.isEmpty()) {
            println("⚠ No few-shot candidates found")
            return emptyList()
        }

        // Enrich candidates with resources
        val enrichedCandidates = candidates.map { candidate ->
            fewShotBuilderService.enrichFewShotWithResources(candidate.id)
        }

        // Step 3: 다차원 스코어링
        val scoredCandidates = enrichedCandidates.map { fewShot ->
            val score = calculateCompositeScore(fewShot, request, userId, artwork, userPreferences)
            ScoredFewShot(fewShot, score.totalScore, score)
        }

        println("→ Scored ${scoredCandidates.size} candidates")

        // Step 4: 다양성 필터링 적용
        val diverseSelection = ensureDiversity(scoredCandidates, 5)

        println("✓ Selected ${diverseSelection.size} diverse few-shots")

        // Step 5: Redis에 캐싱
        val selectedFewShots = diverseSelection.map { it.fewShot }
        redisCacheService.cacheFewShotPool(
            userId = userId,
            artworkId = request.artworkId,
            fewShots = selectedFewShots
        )

        return selectedFewShots
    }

    // ========================================================================
    // 2. Composite Score Calculation
    // ========================================================================

    /**
     * Calculate multi-dimensional composite score
     *
     * Scoring breakdown:
     * - Quality Score (35%): Feedback-based quality
     * - Relevance Score (30%): Artwork + context similarity
     * - Recency Score (15%): Recent creation/usage
     * - User Preference Alignment (10%): Narrative style match
     * - Resource Richness (10%): Number of linked resources
     */
    fun calculateCompositeScore(
        fewShot: EnrichedFewShot,
        request: GenerateDocentRequest,
        userId: Int,
        artwork: ArtworkResponse,
        userPreferences: UserPreferencesResponse
    ): ScoreBreakdown {
        // a. Quality Score (35%)
        val qualityScore = fewShot.example.qualityScore / 5.0

        // b. Relevance Score (30%)
        val relevanceScore = calculateRelevanceScore(fewShot, artwork, userId)

        // c. Recency Score (15%)
        val recencyScore = calculateRecencyScore(fewShot.example.createdAt)

        // d. User Preference Alignment (10%)
        val preferenceScore = calculatePreferenceAlignment(
            fewShot,
            request.narrativeStyle ?: userPreferences.narrativeStyle
        )

        // e. Resource Richness (10%)
        val resourceScore = calculateResourceRichness(fewShot)

        // Weighted composite score
        val totalScore = (
            qualityScore * 0.35 +
            relevanceScore * 0.30 +
            recencyScore * 0.15 +
            preferenceScore * 0.10 +
            resourceScore * 0.10
        )

        return ScoreBreakdown(
            qualityScore = qualityScore,
            relevanceScore = relevanceScore,
            recencyScore = recencyScore,
            preferenceAlignmentScore = preferenceScore,
            resourceRichnessScore = resourceScore,
            totalScore = totalScore
        )
    }

    /**
     * Calculate relevance score based on artwork similarity and context
     */
    private fun calculateRelevanceScore(
        fewShot: EnrichedFewShot,
        artwork: ArtworkResponse,
        userId: Int
    ): Double {
        var score = 0.0

        // Same artist bonus (+0.3)
        if (fewShot.example.artworkInfo.artist.equals(artwork.artist, ignoreCase = true)) {
            score += 0.3
        }

        // Same era bonus (+0.2)
        val fewShotYear = fewShot.example.artworkInfo.creationYear
        val artworkYear = artwork.creationYear
        if (fewShotYear != null && artworkYear != null) {
            val yearDiff = abs(fewShotYear - artworkYear)
            if (yearDiff <= 10) {
                score += 0.2
            } else if (yearDiff <= 50) {
                score += 0.1
            }
        }

        // Same style/genre bonus (+0.2)
        val fewShotGenre = fewShot.example.artworkInfo.genre
        val artworkGenre = artwork.genre
        if (fewShotGenre != null && artworkGenre != null &&
            fewShotGenre.equals(artworkGenre, ignoreCase = true)) {
            score += 0.2
        }

        // Context similarity (+0.3)
        val userContextsResponse = userContextRepository.findContextsByUserId(userId, page = 1, limit = 100)
        val contextSimilarity = calculateContextSimilarity(fewShot.contexts, userContextsResponse.contexts)
        score += contextSimilarity * 0.3

        return min(score, 1.0)
    }

    /**
     * Calculate recency score (recent examples preferred)
     */
    private fun calculateRecencyScore(createdAtStr: String): Double {
        return try {
            val createdAt = Instant.parse(createdAtStr)
            val now = Clock.System.now()
            val daysSinceCreation = (now - createdAt).inWholeDays

            // Exponential decay: score = 1 / (1 + days/30)
            1.0 / (1.0 + daysSinceCreation.toDouble() / 30.0)
        } catch (e: Exception) {
            0.5 // Default score if parsing fails
        }
    }

    /**
     * Calculate user preference alignment
     */
    private fun calculatePreferenceAlignment(
        fewShot: EnrichedFewShot,
        narrativeStyle: String
    ): Double {
        // Parse metadata to check if narrative style matches
        // For now, use a simple heuristic based on text characteristics
        val text = fewShot.example.exemplarText

        val score = when (narrativeStyle.uppercase()) {
            "LITERARY" -> {
                // Check for literary indicators: longer sentences, metaphors
                if (text.length > 500 && text.contains(Regex("[,;:]"))) 0.8 else 0.5
            }
            "CONVERSATIONAL" -> {
                // Check for conversational indicators: questions, informal tone
                if (text.contains("?") || text.contains(Regex("\\b(요|죠|네)\\b"))) 0.8 else 0.5
            }
            "POETIC" -> {
                // Check for poetic indicators: metaphors, imagery
                if (text.contains(Regex("처럼|듯이|같은|마치"))) 0.8 else 0.5
            }
            "ANALYTICAL" -> {
                // Check for analytical indicators: factual, structured
                if (text.contains(Regex("\\d+|년|세기|시기"))) 0.8 else 0.5
            }
            else -> 0.5
        }

        return score
    }

    /**
     * Calculate resource richness score
     * More resources = better, but not too many
     */
    private fun calculateResourceRichness(fewShot: EnrichedFewShot): Double {
        val totalResources = fewShot.contexts.size + fewShot.links.size + fewShot.memos.size

        return when {
            totalResources == 0 -> 0.0
            totalResources in 1..3 -> 0.5
            totalResources in 4..7 -> 1.0  // Optimal range
            totalResources in 8..10 -> 0.8
            else -> 0.6  // Too many resources might be overwhelming
        }
    }

    // ========================================================================
    // 3. Diversity Filtering (MMR Algorithm)
    // ========================================================================

    /**
     * Ensure diversity using Maximal Marginal Relevance (MMR)
     *
     * MMR = λ * Relevance - (1-λ) * MaxSimilarity
     * λ = 0.3 (balance between relevance and diversity)
     */
    fun ensureDiversity(
        scoredFewShots: List<ScoredFewShot>,
        targetCount: Int
    ): List<ScoredFewShot> {
        if (scoredFewShots.size <= targetCount) {
            return scoredFewShots
        }

        val selected = mutableListOf<ScoredFewShot>()
        val remaining = scoredFewShots.toMutableList()

        val lambda = 0.3 // Diversity vs. relevance trade-off

        // Step 1: Select the highest scoring example first
        val first = remaining.maxByOrNull { it.compositeScore }
        if (first != null) {
            selected.add(first)
            remaining.remove(first)
        }

        // Step 2: Iteratively select diverse examples
        while (selected.size < targetCount && remaining.isNotEmpty()) {
            val nextCandidate = remaining.maxByOrNull { candidate ->
                val relevance = candidate.compositeScore

                // Calculate max similarity to already selected examples
                val maxSimilarity = selected.maxOfOrNull { selectedExample ->
                    calculateFewShotSimilarity(candidate.fewShot, selectedExample.fewShot)
                } ?: 0.0

                // MMR score
                lambda * relevance - (1 - lambda) * maxSimilarity
            }

            if (nextCandidate != null) {
                selected.add(nextCandidate)
                remaining.remove(nextCandidate)
            } else {
                break
            }
        }

        println("→ Diversity filter: ${scoredFewShots.size} → ${selected.size}")
        return selected
    }

    /**
     * Calculate similarity between two few-shot examples
     */
    private fun calculateFewShotSimilarity(
        fewShot1: EnrichedFewShot,
        fewShot2: EnrichedFewShot
    ): Double {
        var similarity = 0.0

        // Same artwork (+0.5)
        if (fewShot1.example.artworkId == fewShot2.example.artworkId) {
            similarity += 0.5
        }

        // Same category (+0.3)
        if (fewShot1.example.category != null &&
            fewShot1.example.category == fewShot2.example.category) {
            similarity += 0.3
        }

        // Context similarity (+0.2)
        val contextSimilarity = calculateContextSimilarity(fewShot1.contexts, fewShot2.contexts)
        similarity += contextSimilarity * 0.2

        return min(similarity, 1.0)
    }

    // ========================================================================
    // 4. Context Similarity (TF-IDF + Cosine Similarity)
    // ========================================================================

    /**
     * Calculate context similarity using TF-IDF and cosine similarity
     */
    fun calculateContextSimilarity(
        contexts1: List<UserContextResponse>,
        contexts2: List<UserContextResponse>
    ): Double {
        if (contexts1.isEmpty() || contexts2.isEmpty()) {
            return 0.0
        }

        // Extract text content
        val text1 = contexts1.joinToString(" ") { it.content }
        val text2 = contexts2.joinToString(" ") { it.content }

        // Tokenize
        val tokens1 = tokenize(text1)
        val tokens2 = tokenize(text2)

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0
        }

        // Build vocabulary
        val vocabulary = (tokens1 + tokens2).distinct()

        // Calculate TF-IDF vectors
        val vector1 = calculateTfIdfVector(tokens1, vocabulary, listOf(tokens1, tokens2))
        val vector2 = calculateTfIdfVector(tokens2, vocabulary, listOf(tokens1, tokens2))

        // Calculate cosine similarity
        return cosineSimilarity(vector1, vector2)
    }

    /**
     * Tokenize text into words (simple Korean + English tokenization)
     */
    private fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .replace(Regex("[^가-힣a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 }
    }

    /**
     * Calculate TF-IDF vector for a document
     */
    private fun calculateTfIdfVector(
        tokens: List<String>,
        vocabulary: List<String>,
        allDocuments: List<List<String>>
    ): DoubleArray {
        val vector = DoubleArray(vocabulary.size)

        vocabulary.forEachIndexed { index, term ->
            // Term Frequency (TF)
            val tf = tokens.count { it == term }.toDouble() / tokens.size

            // Inverse Document Frequency (IDF)
            val documentsWithTerm = allDocuments.count { doc -> doc.contains(term) }
            val idf = ln(allDocuments.size.toDouble() / (1 + documentsWithTerm))

            // TF-IDF
            vector[index] = tf * idf
        }

        return vector
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private fun cosineSimilarity(vector1: DoubleArray, vector2: DoubleArray): Double {
        if (vector1.size != vector2.size) {
            return 0.0
        }

        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            magnitude1 += vector1[i] * vector1[i]
            magnitude2 += vector2[i] * vector2[i]
        }

        magnitude1 = sqrt(magnitude1)
        magnitude2 = sqrt(magnitude2)

        return if (magnitude1 > 0 && magnitude2 > 0) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0.0
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Print detailed score breakdown for debugging
     */
    fun printScoreBreakdown(scored: List<ScoredFewShot>) {
        println("\n=== Few-Shot Score Breakdown ===")
        scored.forEachIndexed { index, item ->
            println("\n[$index] Few-Shot ID: ${item.fewShot.example.id}")
            println("  Quality:     ${String.format("%.3f", item.scoreBreakdown.qualityScore)} (35%)")
            println("  Relevance:   ${String.format("%.3f", item.scoreBreakdown.relevanceScore)} (30%)")
            println("  Recency:     ${String.format("%.3f", item.scoreBreakdown.recencyScore)} (15%)")
            println("  Preference:  ${String.format("%.3f", item.scoreBreakdown.preferenceAlignmentScore)} (10%)")
            println("  Resources:   ${String.format("%.3f", item.scoreBreakdown.resourceRichnessScore)} (10%)")
            println("  ─────────────────────────────────")
            println("  TOTAL:       ${String.format("%.3f", item.scoreBreakdown.totalScore)}")
        }
        println("\n================================\n")
    }
}