package com.kevin.services

import com.kevin.model.dto.TTSConfig
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

/**
 * TTS Service
 * Handles Text-to-Speech conversion using Google Cloud TTS API
 * Reference: https://cloud.google.com/text-to-speech/docs/quickstart-client-libraries
 */
class TTSService {
    private val httpClient = HttpClient(CIO) {
        expectSuccess = false
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val dotenv = dotenv {
        directory = "env"
        filename = ".env"
        ignoreIfMissing = true
    }

    private val ttsApiKey = dotenv["TTS_API_KEY"] ?: ""
    private val audioStoragePath = dotenv["AUDIO_STORAGE_PATH"] ?: "storage/audio"

    /**
     * Generate audio from text using Google Cloud TTS API
     *
     * @param text Text to convert to speech
     * @param config TTS configuration (voice, speed, pitch, language)
     * @return TTSResult with audio file URL and duration
     */
    suspend fun generateAudio(
        text: String,
        config: TTSConfig = TTSConfig()
    ): TTSResult {
        if (ttsApiKey.isBlank()) {
            // Return mock result if API key is not set (for development)
            return TTSResult(
                success = true,
                audioUrl = "https://example.com/mock-audio.mp3",
                durationSeconds = estimateDuration(text),
                errorMessage = null
            )
        }

        try {
            val audioBytes = callTTSAPI(text, config)
            val audioUrl = saveAudioFile(audioBytes)
            val duration = estimateDuration(text)

            return TTSResult(
                success = true,
                audioUrl = audioUrl,
                durationSeconds = duration,
                errorMessage = null
            )
        } catch (e: Exception) {
            return TTSResult(
                success = false,
                audioUrl = null,
                durationSeconds = null,
                errorMessage = "TTS generation failed: ${e.message}"
            )
        }
    }

    /**
     * Call Google Cloud TTS API
     */
    private suspend fun callTTSAPI(
        text: String,
        config: TTSConfig
    ): ByteArray {
        val url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=$ttsApiKey"

        val requestBody = TTSRequest(
            input = TTSInput(text = text),
            voice = VoiceSelection(
                languageCode = config.language,
                name = getVoiceName(config.language, config.voice),
                ssmlGender = config.voice
            ),
            audioConfig = AudioConfig(
                audioEncoding = "MP3",
                speakingRate = config.speed,
                pitch = config.pitch
            )
        )

        val requestJson = json.encodeToString(TTSRequest.serializer(), requestBody)

        val response: HttpResponse = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestJson)
        }

        if (response.status.value !in 200..299) {
            val errorText = response.bodyAsText()
            throw Exception("TTS API returned ${response.status.value}: $errorText")
        }

        val responseText = response.bodyAsText()
        val ttsResponse = json.decodeFromString<TTSResponse>(responseText)

        return Base64.getDecoder().decode(ttsResponse.audioContent)
    }

    /**
     * Save audio file to storage
     */
    private fun saveAudioFile(audioBytes: ByteArray): String {
        // Create storage directory if it doesn't exist
        val storageDir = File(audioStoragePath)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        // Generate unique filename
        val filename = "docent_${UUID.randomUUID()}.mp3"
        val filePath = "$audioStoragePath/$filename"

        // Save file
        File(filePath).writeBytes(audioBytes)

        // Return URL (adjust based on your server configuration)
        return "/audio/$filename"
    }

    /**
     * Estimate audio duration based on text length
     * Rough estimate: average speaking rate is ~150 words per minute
     */
    private fun estimateDuration(text: String): Int {
        val wordCount = text.split(Regex("\\s+")).size
        val durationMinutes = wordCount / 150.0
        return (durationMinutes * 60).toInt()
    }

    /**
     * Get voice name based on language and voice type
     */
    private fun getVoiceName(languageCode: String, voice: String): String {
        return when (languageCode) {
            "ko-KR" -> when (voice) {
                "MALE" -> "ko-KR-Standard-C"
                "FEMALE" -> "ko-KR-Standard-A"
                else -> "ko-KR-Standard-A"
            }
            "en-US" -> when (voice) {
                "MALE" -> "en-US-Standard-B"
                "FEMALE" -> "en-US-Standard-C"
                else -> "en-US-Standard-C"
            }
            else -> "$languageCode-Standard-A"
        }
    }

    // ============================================================================
    // Data classes for TTS API
    // ============================================================================

    @Serializable
    data class TTSRequest(
        val input: TTSInput,
        val voice: VoiceSelection,
        val audioConfig: AudioConfig
    )

    @Serializable
    data class TTSInput(
        val text: String
    )

    @Serializable
    data class VoiceSelection(
        val languageCode: String,
        val name: String,
        val ssmlGender: String
    )

    @Serializable
    data class AudioConfig(
        val audioEncoding: String,
        val speakingRate: Double,
        val pitch: Double
    )

    @Serializable
    data class TTSResponse(
        val audioContent: String
    )
}

/**
 * TTS generation result
 */
data class TTSResult(
    val success: Boolean,
    val audioUrl: String?,
    val durationSeconds: Int?,
    val errorMessage: String?
)