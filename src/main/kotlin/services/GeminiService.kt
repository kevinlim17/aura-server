package com.kevin.services

import com.kevin.model.dto.GeminiConfig
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Gemini Service
 * Handles communication with Google Gemini API for text generation
 * Reference: https://ai.google.dev/gemini-api/docs/quickstart?hl=ko
 */
class GeminiService {
    private val httpClient = HttpClient(CIO) {
        expectSuccess = false
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val dotenv = dotenv {
        directory = "env"
        filename = ".env"
        ignoreIfMissing = true
    }

    private val geminiApiKey = dotenv["GEMINI_API_KEY"] ?: ""
    private val maxRetries = 3
    private val retryDelayMs = 1000L

    /**
     * Generate text using Gemini API
     *
     * @param prompt The prompt to send to Gemini
     * @param config Gemini configuration (model, temperature, etc.)
     * @return Generated text and metadata
     */
    suspend fun generateText(
        prompt: String,
        config: GeminiConfig = GeminiConfig()
    ): GeminiGenerationResult {
        if (geminiApiKey.isBlank()) {
            throw Exception("GEMINI_API_KEY environment variable not set")
        }

        val startTime = System.currentTimeMillis()

        try {
            val result = generateWithRetry(prompt, config)
            val endTime = System.currentTimeMillis()

            return GeminiGenerationResult(
                success = true,
                generatedText = result.text,
                model = config.model,
                temperature = config.temperature,
                topP = config.topP,
                topK = config.topK,
                generationTimeMs = (endTime - startTime).toInt(),
                errorMessage = null
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()

            return GeminiGenerationResult(
                success = false,
                generatedText = "",
                model = config.model,
                temperature = config.temperature,
                topP = config.topP,
                topK = config.topK,
                generationTimeMs = (endTime - startTime).toInt(),
                errorMessage = "Gemini API error: ${e.message}"
            )
        }
    }

    /**
     * Generate with retry logic
     */
    private suspend fun generateWithRetry(
        prompt: String,
        config: GeminiConfig,
        attempt: Int = 1
    ): GenerationResponse {
        try {
            return callGeminiAPI(prompt, config)
        } catch (e: Exception) {
            if (attempt < maxRetries) {
                delay(retryDelayMs * attempt)
                return generateWithRetry(prompt, config, attempt + 1)
            } else {
                throw e
            }
        }
    }

    /**
     * Call Gemini API
     */
    private suspend fun callGeminiAPI(
        prompt: String,
        config: GeminiConfig
    ): GenerationResponse {
        val url = "https://generativelanguage.googleapis.com/v1/models/${config.model}:generateContent?key=$geminiApiKey"

        val requestBody = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt)
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = config.temperature,
                topP = config.topP,
                topK = config.topK,
                maxOutputTokens = config.maxOutputTokens
            )
        )

        val requestJson = json.encodeToString(GeminiRequest.serializer(), requestBody)

        val response: HttpResponse = httpClient.post(url) {
            header("x-goog-api-key", geminiApiKey)
            contentType(ContentType.Application.Json)
            setBody(requestJson)
        }

        val responseText = response.bodyAsText()

        if (response.status.value !in 200..299) {
            throw Exception("Gemini API returned ${response.status.value}: $responseText")
        }

        return parseGeminiResponse(responseText)
    }

    /**
     * Parse Gemini API response
     */
    private fun parseGeminiResponse(responseText: String): GenerationResponse {
        try {
            val geminiResponse = json.decodeFromString<GeminiApiResponse>(responseText)

            val text = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: throw Exception("No text in Gemini response")

            return GenerationResponse(
                text = text.trim(),
                finishReason = geminiResponse.candidates?.firstOrNull()?.finishReason
            )
        } catch (e: Exception) {
            throw Exception("Failed to parse Gemini response: ${e.message}")
        }
    }

    /**
     * Validate prompt length
     */
    fun validatePrompt(prompt: String, maxLength: Int = 30000): Boolean {
        return prompt.length <= maxLength
    }

    // ============================================================================
    // Data classes for Gemini API
    // ============================================================================

    @Serializable
    data class GeminiRequest(
        val contents: List<Content>,
        val generationConfig: GenerationConfig? = null
    )

    @Serializable
    data class Content(
        val parts: List<Part>
    )

    @Serializable
    data class Part(
        val text: String
    )

    @Serializable
    data class GenerationConfig(
        val temperature: Double,
        val topP: Double,
        val topK: Int,
        val maxOutputTokens: Int
    )

    @Serializable
    data class GeminiApiResponse(
        val candidates: List<Candidate>? = null,
        val promptFeedback: PromptFeedback? = null
    )

    @Serializable
    data class Candidate(
        val content: Content? = null,
        val finishReason: String? = null,
        val safetyRatings: List<SafetyRating>? = null
    )

    @Serializable
    data class PromptFeedback(
        val blockReason: String? = null,
        val safetyRatings: List<SafetyRating>? = null
    )

    @Serializable
    data class SafetyRating(
        val category: String,
        val probability: String
    )

    data class GenerationResponse(
        val text: String,
        val finishReason: String?
    )
}

/**
 * Gemini generation result
 */
data class GeminiGenerationResult(
    val success: Boolean,
    val generatedText: String,
    val model: String,
    val temperature: Double,
    val topP: Double,
    val topK: Int,
    val generationTimeMs: Int,
    val errorMessage: String?
)