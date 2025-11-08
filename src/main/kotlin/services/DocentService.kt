package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.*
import com.kevin.utils.PromptBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Docent Service
 * Handles the complete docent generation process
 *
 * Process:
 * 1. Load user context and preferences
 * 2. Include companion input (optional)
 * 3. Use few-shot examples (optional)
 * 4. Generate docent text using Gemini API
 * 5. Generate TTS audio
 * 6. Save and return result
 */
class DocentService(
    private val docentSessionRepository: DocentSessionRepository = DocentSessionRepository(),
    private val artworkRepository: ArtworkRepository = ArtworkRepository(),
    private val userPreferencesRepository: UserPreferencesRepository = UserPreferencesRepository(),
    private val userContextRepository: UserContextRepository = UserContextRepository(),
    private val geminiService: GeminiService = GeminiService(),
    private val ttsService: TTSService = TTSService()
) {
    // In-memory session storage for async generation
    private val generationSessions = ConcurrentHashMap<Int, DocentGenerationStatus>()

    /**
     * Generate docent (async)
     * Returns session ID immediately and processes generation in the background
     */
    fun generateDocentAsync(request: GenerateDocentRequest): DocentGenerationSessionResponse {
        // Create an initial session with GENERATING status
        val session = docentSessionRepository.createSession(
            userId = request.userId,
            artworkId = request.artworkId,
            promptTemplate = "Generating...",
            promptPersona = null,
            promptTask = null,
            promptContext = null,
            promptForm = null,
            fewShotExamples = emptyList(),
            generatedText = "",
            geminiModel = null,
            geminiTemperature = null,
            geminiTopP = null,
            geminiTopK = null,
            generationTimeMs = null,
            status = "GENERATING"
        ) ?: throw Exception("Failed to create docent session")

        val sessionId = session.id

        // Store generation status
        generationSessions[sessionId] = DocentGenerationStatus(
            sessionId = sessionId,
            status = "GENERATING",
            progress = 0
        )

        // Launch async generation
        CoroutineScope(Dispatchers.IO).launch {
            processDocentGeneration(sessionId, request)
        }

        return DocentGenerationSessionResponse(
            sessionId = sessionId,
            status = "GENERATING",
            pollingUrl = "/api/docent/sessions/$sessionId",
            message = "Docent generation started. Poll the provided URL for results."
        )
    }

    /**
     * Process docent generation asynchronously
     */
    private suspend fun processDocentGeneration(sessionId: Int, request: GenerateDocentRequest) {
        try {
            updateProgress(sessionId, 10, "Loading user data...")

            // 1. Load artwork
            val artwork = artworkRepository.findArtworkById(request.artworkId)
                ?: throw Exception("Artwork not found")

            updateProgress(sessionId, 20, "Loading preferences...")

            // 2. Load user preferences
            val userPreferences = userPreferencesRepository.findPreferencesByUserId(request.userId)
                ?: throw Exception("User preferences not found")

            updateProgress(sessionId, 30, "Loading user context...")

            // 3. Load user contexts
            val userContexts = userContextRepository.findContextsByUserId(
                userId = request.userId,
                page = 1,
                limit = 10
            ).contexts

            // 4. Load companion contexts (if requested)
            val companionContexts = if (request.includeCompanionContext) {
                userContexts.filter { it.isCompanionInput }
            } else {
                emptyList()
            }

            updateProgress(sessionId, 40, "Loading few-shot examples...")

            // 5. Load few-shot examples (if requested)
            val fewShotExamples = if (request.useFewShotExamples) {
                docentSessionRepository.getFewShotExamples(request.artworkId, limit = 3)
            } else {
                emptyList()
            }

            updateProgress(sessionId, 50, "Building prompt...")

            // 6. Build prompt
            val narrativeStyle = request.narrativeStyle ?: userPreferences.narrativeStyle
            val preferredLength = request.preferredLength ?: userPreferences.preferredLength

            val adjustedPreferences = userPreferences.copy(
                narrativeStyle = narrativeStyle,
                preferredLength = preferredLength
            )

            val promptComponents = PromptBuilder.buildDocentPrompt(
                artwork = artwork,
                userPreferences = adjustedPreferences,
                userContexts = userContexts,
                companionContexts = companionContexts,
                fewShotExamples = fewShotExamples,
                customPrompt = request.customPrompt
            )

            updateProgress(sessionId, 60, "Generating docent text...")

            // 7. Generate text using Gemini API
            val geminiConfig = GeminiConfig(
                model = "gemini-2.5-flash",
                temperature = 0.7,
                topP = 0.9,
                topK = 40,
                maxOutputTokens = when (preferredLength.uppercase()) {
                    "SHORT" -> 512
                    "MEDIUM" -> 1024
                    "LONG" -> 2048
                    else -> 1024
                }
            )

            val geminiResult = geminiService.generateText(
                prompt = promptComponents.fullPrompt,
                config = geminiConfig
            )

            if (!geminiResult.success) {
                throw Exception(geminiResult.errorMessage ?: "Gemini generation failed")
            }

            updateProgress(sessionId, 80, "Generating audio...")

            // 8. Generate TTS audio
            val ttsConfig = TTSConfig(
                voice = userPreferences.ttsVoice,
                speed = userPreferences.ttsSpeed,
                pitch = userPreferences.ttsPitch,
                language = userPreferences.preferredLanguage
            )

            val ttsResult = ttsService.generateAudio(
                text = geminiResult.generatedText,
                config = ttsConfig
            )

            updateProgress(sessionId, 90, "Saving results...")

            // 9. Update session with results
            val updatedSession = docentSessionRepository.createSession(
                userId = request.userId,
                artworkId = request.artworkId,
                promptTemplate = promptComponents.fullPrompt,
                promptPersona = promptComponents.persona,
                promptTask = promptComponents.task,
                promptContext = promptComponents.context,
                promptForm = promptComponents.form,
                fewShotExamples = fewShotExamples,
                generatedText = geminiResult.generatedText,
                geminiModel = geminiResult.model,
                geminiTemperature = geminiResult.temperature,
                geminiTopP = geminiResult.topP,
                geminiTopK = geminiResult.topK,
                generationTimeMs = geminiResult.generationTimeMs,
                status = "COMPLETED"
            )

            // Update TTS audio if generated successfully
            if (ttsResult.success && ttsResult.audioUrl != null && ttsResult.durationSeconds != null) {
                docentSessionRepository.updateTTSAudio(
                    sessionId = sessionId,
                    audioUrl = ttsResult.audioUrl,
                    durationSeconds = ttsResult.durationSeconds
                )
            }

            // Increment artwork's docent generation count
            docentSessionRepository.incrementArtworkDocentCount(request.artworkId)

            updateProgress(sessionId, 100, "Completed")

            // Update generation status
            generationSessions[sessionId] = DocentGenerationStatus(
                sessionId = sessionId,
                status = "COMPLETED",
                progress = 100,
                result = updatedSession
            )

        } catch (e: Exception) {
            // Update session status to FAILED
            docentSessionRepository.updateSessionStatus(sessionId, "FAILED")

            // Update generation status
            generationSessions[sessionId] = DocentGenerationStatus(
                sessionId = sessionId,
                status = "FAILED",
                progress = 0,
                errorMessage = e.message
            )
        }
    }

    /**
     * Get a docent session by ID
     */
    fun getDocentSession(sessionId: Int): ApiResponse<DocentSessionResponse> {
        val session = docentSessionRepository.findSessionById(sessionId)
            ?: return ApiResponse(
                success = false,
                data = null,
                message = "Docent session not found"
            )

        return ApiResponse(
            success = true,
            data = session,
            message = "Session retrieved successfully"
        )
    }

    /**
     * Get docent generation result (for polling)
     */
    fun getDocentGenerationResult(sessionId: Int): DocentGenerationResultResponse {
        val status = generationSessions[sessionId]

        if (status == null) {
            // Check if the session exists in the database
            val session = docentSessionRepository.findSessionById(sessionId)
            return if (session != null) {
                DocentGenerationResultResponse(
                    sessionId = sessionId,
                    status = session.status,
                    result = session,
                    errorMessage = null,
                    progress = if (session.status == "COMPLETED") 100 else null
                )
            } else {
                DocentGenerationResultResponse(
                    sessionId = sessionId,
                    status = "NOT_FOUND",
                    result = null,
                    errorMessage = "Session not found",
                    progress = null
                )
            }
        }

        return DocentGenerationResultResponse(
            sessionId = sessionId,
            status = status.status,
            result = status.result,
            errorMessage = status.errorMessage,
            progress = status.progress
        )
    }

    /**
     * Update play statistics
     */
    fun updatePlayStats(sessionId: Int, request: UpdatePlayStatsRequest): ApiResponse<DocentSessionResponse> {
        val session = docentSessionRepository.updatePlayStats(
            sessionId = sessionId,
            playCount = request.playCount,
            totalListeningSeconds = request.totalListeningSeconds,
            completionRate = request.completionRate
        ) ?: return ApiResponse(
            success = false,
            data = null,
            message = "Failed to update play stats"
        )

        return ApiResponse(
            success = true,
            data = session,
            message = "Play stats updated successfully"
        )
    }

    /**
     * Get user docent history
     */
    fun getUserDocentHistory(
        userId: Int,
        page: Int = 1,
        limit: Int = 20
    ): ApiResponse<DocentHistoryResponse> {
        // Validate pagination
        if (page < 1) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Page must be greater than 0"
            )
        }

        if (limit !in 1..100) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Limit must be between 1 and 100"
            )
        }

        val history = docentSessionRepository.findSessionsByUserId(
            userId = userId,
            page = page,
            limit = limit
        )

        return ApiResponse(
            success = true,
            data = history,
            message = "History retrieved successfully"
        )
    }

    /**
     * Update progress helper
     */
    private fun updateProgress(sessionId: Int, progress: Int, status: String) {
        generationSessions[sessionId] = DocentGenerationStatus(
            sessionId = sessionId,
            status = "GENERATING",
            progress = progress,
            statusMessage = status
        )
    }

    /**
     * Clean up old sessions (call this periodically)
     */
    fun cleanupOldSessions(maxAgeMs: Long = 3600000) { // 1 hour
        // val now = System.currentTimeMillis()
        generationSessions.entries.removeIf { (_, status) ->
            status.status == "COMPLETED" || status.status == "FAILED"
        }
    }
}

/**
 * Docent generation status data
 */
data class DocentGenerationStatus(
    val sessionId: Int,
    val status: String, // GENERATING, COMPLETED, FAILED
    val progress: Int, // 0-100
    val result: DocentSessionResponse? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)