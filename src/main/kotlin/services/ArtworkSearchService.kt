package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.ArtworkRepository
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Artwork Search Service
 * Handles voice-based and image-based artwork search using external APIs
 * - Voice: Google Gemini API for finding similar artworks from STT text
 * - Image: Google Vision AI API for OCR text extraction
 */
class ArtworkSearchService(
    private val artworkRepository: ArtworkRepository = ArtworkRepository()
) {
    private val httpClient = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }
    private val dotenv = dotenv {
        directory = "env"
        filename = ".env"
        ignoreIfMissing = true
    }

    // In-memory session storage for async image search
    private val imageSessions = ConcurrentHashMap<String, ImageSearchSession>()

    // Get API keys from environment variables
    private val geminiApiKey = dotenv["GEMINI_API_KEY"] ?: ""
    private val visionApiKey = dotenv["VISION_API_KEY"] ?: ""

    /**
     * Search artworks using Gemini API (used as fallback by ArtworkService)
     *
     * @param query Search query text
     * @return List of GeminiArtworkMatch
     */
    suspend fun searchByGemini(query: String): List<GeminiArtworkMatch> {
        try {
            val prompt = """
                제목: ${query}과 관련된 미술품을 검색하고 가장 유사도가 높은 제목을 가진 미술품의
                title, artist, 간단한 description을 JSON 형식으로 가져와줘.

                최대 3개까지만 반환해주세요.
            """.trimIndent()

            val geminiResponse = callGeminiAPI(prompt)
            return parseGeminiResponse(geminiResponse)
        } catch (e: Exception) {
            println("Gemini search failed: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Voice-based artwork search using Google Gemini API
     * Frontend sends STT-completed text
     */
    suspend fun searchByVoice(request: ArtworkVoiceSearchRequest): VoiceSearchResponse {
        try {
            // Create prompt for Gemini API
            val prompt = """
                제목: ${request.transcribedText}과 관련된 미술품을 검색하고 가장 유사도가 높은 제목을 가진 미술품의
                title, artist, 간단한 description을 JSON 형식으로 가져와줘.

                최대 3개까지만 반환해주세요.
            """.trimIndent()

            // Call Gemini API
            val geminiResponse = callGeminiAPI(prompt)

            // Parse response and extract artwork information
            val matchedArtworks = parseGeminiResponse(geminiResponse)

            // Save search record
            artworkRepository.createArtworkSearch(
                userId = request.userId,
                searchMethod = "VOICE",
                searchQuery = request.transcribedText,
                searchQueryVoiceUrl = request.voiceUrl,
                resultsCount = matchedArtworks.size
            )

            return VoiceSearchResponse(
                success = true,
                matchedArtworks = matchedArtworks,
                originalQuery = request.transcribedText,
                message = "Voice search completed successfully"
            )
        } catch (e: Exception) {
            return VoiceSearchResponse(
                success = false,
                matchedArtworks = emptyList(),
                originalQuery = request.transcribedText,
                message = "Voice search failed: ${e.message}"
            )
        }
    }

    /**
     * Image-based artwork search using Google Vision AI API
     * Returns session ID for async processing (202 Accepted)
     */
    fun searchByImage(imageBytes: ByteArray, userId: Int): ImageSearchSessionResponse {
        // Generate session ID
        val sessionId = UUID.randomUUID().toString()

        // Create session
        val session = ImageSearchSession(
            sessionId = sessionId,
            userId = userId,
            status = "PROCESSING",
            createdAt = System.currentTimeMillis()
        )
        imageSessions[sessionId] = session

        // Launch async processing
        CoroutineScope(Dispatchers.IO).launch {
            processImageSearch(sessionId, imageBytes, userId)
        }

        return ImageSearchSessionResponse(
            sessionId = sessionId,
            status = "PROCESSING",
            pollingUrl = "/api/artworks/search/$sessionId",
            message = "Image search is being processed. Poll the provided URL for results."
        )
    }

    /**
     * Get image search result by session ID (polling)
     */
    fun getImageSearchResult(sessionId: String): ImageSearchResultResponse? {
        val session = imageSessions[sessionId] ?: return null

        return ImageSearchResultResponse(
            sessionId = sessionId,
            status = session.status,
            result = session.result,
            ocrText = session.ocrText,
            confidence = session.confidence,
            message = session.message
        )
    }

    /**
     * Process image search asynchronously
     */
    private suspend fun processImageSearch(sessionId: String, imageBytes: ByteArray, userId: Int) {
        val session = imageSessions[sessionId] ?: return

        try {
            // Call Vision AI API for OCR
            val ocrText = callVisionAI(imageBytes)

            if (ocrText.isBlank()) {
                session.status = "FAILED"
                session.message = "No text detected in image"
                return
            }

            // Search for artwork using OCR text
            val searchResults = artworkRepository.searchArtworks(ocrText, page = 1, limit = 1)

            if (searchResults.results.isEmpty()) {
                session.status = "COMPLETED"
                session.ocrText = ocrText
                session.message = "No matching artwork found for detected text: $ocrText"
            } else {
                val topResult = searchResults.results.first()
                session.status = "COMPLETED"
                session.result = topResult.artwork
                session.ocrText = ocrText
                session.confidence = topResult.relevanceScore / 100.0 // Normalize to 0-1
                session.message = "Artwork found successfully"

                // Save search record
                artworkRepository.createArtworkSearch(
                    userId = userId,
                    searchMethod = "CAMERA",
                    searchQuery = ocrText,
                    resultsCount = 1,
                    selectedArtworkId = topResult.artwork.id
                )
            }
        } catch (e: Exception) {
            session.status = "FAILED"
            session.message = "Image processing failed: ${e.message}"
        }
    }

    /**
     * Call Google Gemini API
     */
    private suspend fun callGeminiAPI(prompt: String): String {
        if (geminiApiKey.isBlank()) {
            throw Exception("GEMINI_API_KEY environment variable not set")
        }

        val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent"

        val requestBody = """
            {
                "contents": [{
                    "parts": [{
                        "text": "$prompt"
                    }]
                }]
            }
        """.trimIndent()

        val response: HttpResponse = httpClient.post(url) {
            header("x-goog-api-key", geminiApiKey)
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        return response.bodyAsText()
    }

    /**
     * Call Google Vision AI API for OCR
     */
    private suspend fun callVisionAI(imageBytes: ByteArray): String {
        if (visionApiKey.isBlank()) {
            throw Exception("VISION_API_KEY environment variable not set")
        }

        val url = "https://vision.googleapis.com/v1/images:annotate?key=$visionApiKey"

        // Encode image to base64
        val base64Image = Base64.getEncoder().encodeToString(imageBytes)

        val requestBody = """
            {
                "requests": [{
                    "image": {
                        "content": "$base64Image"
                    },
                    "features": [{
                        "type": "TEXT_DETECTION"
                    }]
                }]
            }
        """.trimIndent()

        val response: HttpResponse = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val responseText = response.bodyAsText()

        // Parse Vision AI response to extract text
        return parseVisionAIResponse(responseText)
    }

    /**
     * Parse Gemini API response to extract artwork information
     */
    private fun parseGeminiResponse(response: String): List<GeminiArtworkMatch> {
        try {
            @Serializable
            data class GeminiPart(
                val text: String
            )
            @Serializable
            data class GeminiContent(
                val parts: List<GeminiPart>
            )

            @Serializable
            data class GeminiCandidate(
                val content: GeminiContent
            )

            @Serializable
            data class GeminiResponse(
                val candidates: List<GeminiCandidate>
            )

            @Serializable
            data class ArtworkData(
                val title: String,
                val artist: String,
                val description: String
            )

            @Serializable
            data class ArtworksWrapper(
                val artworks: List<ArtworkData>
            )

            // Parse Gemini response
            val geminiResponse = json.decodeFromString<GeminiResponse>(response)
            val textContent = geminiResponse.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text ?: return emptyList()


            // Extract JSON from text (Gemini might wrap it in Markdown code blocks)
            val jsonText = extractJsonFromText(textContent)

            // Parse artwork data
            val artworksWrapper = json.decodeFromString<List<ArtworkData>>(jsonText)

            return artworksWrapper.map { artwork ->
                GeminiArtworkMatch(
                    title = artwork.title,
                    artist = artwork.artist,
                    description = artwork.description,
                    confidence = 0.5
                )
            }
        } catch (e: Exception) {
            println("Failed to parse Gemini response: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Extract JSON from text (removes markdown code blocks if present)
     */
    private fun extractJsonFromText(text: String): String {
        // Remove markdown code blocks (```json ... ```)
        val jsonPattern = "```json\\s*([\\s\\S]*?)```".toRegex()
        val match = jsonPattern.find(text)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // Remove generic code blocks (``` ... ```)
        val codePattern = "```\\s*([\\s\\S]*?)```".toRegex()
        val codeMatch = codePattern.find(text)
        if (codeMatch != null) {
            return codeMatch.groupValues[1].trim()
        }

        // Try to find JSON object directly
        val jsonObjectPattern = "\\{[\\s\\S]*\\}".toRegex()
        val jsonMatch = jsonObjectPattern.find(text)
        if (jsonMatch != null) {
            return jsonMatch.value
        }

        return text.trim()
    }

    /**
     * Parse Vision AI response to extract detected text
     */
    private fun parseVisionAIResponse(response: String): String {
        try {
            @Serializable
            data class TextAnnotation(
                val description: String
            )
            @Serializable
            data class AnnotationResponse(
                val textAnnotations: List<TextAnnotation>? = null
            )

            @Serializable
            data class VisionResponse(
                val responses: List<AnnotationResponse>
            )

            val visionResponse = json.decodeFromString<VisionResponse>(response)

            // First text annotation contains the full detected text
            return visionResponse.responses.firstOrNull()
                ?.textAnnotations?.firstOrNull()
                ?.description ?: ""
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Clean up old sessions (call this periodically)
     */
    fun cleanupOldSessions(maxAgeMs: Long = 3600000) { // 1 hour
        val now = System.currentTimeMillis()
        imageSessions.entries.removeIf { (_, session) ->
            now - session.createdAt > maxAgeMs
        }
    }
}

/**
 * Image search session data
 */
data class ImageSearchSession(
    val sessionId: String,
    val userId: Int,
    var status: String, // PROCESSING, COMPLETED, FAILED
    var result: ArtworkResponse? = null,
    var ocrText: String? = null,
    var confidence: Double? = null,
    var message: String? = null,
    val createdAt: Long
)