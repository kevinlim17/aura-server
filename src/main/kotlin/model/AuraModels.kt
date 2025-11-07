package com.kevin.model

import kotlinx.serialization.Serializable

/**
 * User data class
 */
@Serializable
data class User(
    val id: Int,
    val email: String,
    val userType: String = "MAIN_USER",
    val isVisuallyImpaired: Boolean = true,
    val impairmentLevel: String? = null,
    val isActive: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val lastLoginAt: String? = null
)

@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val userType: String = "MAIN_USER",
    val isVisuallyImpaired: Boolean = true,
    val impairmentLevel: String? = null
)

/**
 * User profile data class
 */
@Serializable
data class UserProfile(
    val id: Int,
    val userId: Int,
    val interests: List<String> = emptyList(),
    val hobbies: String? = null,
    val hobbiesVoiceUrl: String? = null,
    val favoriteArtists: String? = null,
    val bio: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateUserProfileRequest(
    val interests: List<String> = emptyList(),
    val hobbies: String? = null,
    val hobbiesVoiceUrl: String? = null,
    val favoriteArtists: String? = null,
    val bio: String? = null
)

/**
 * User context data class
 */
@Serializable
data class UserContext(
    val id: Int,
    val userId: Int,
    val contextType: String,
    val title: String? = null,
    val content: String,
    val voiceUrl: String? = null,
    val voiceDurationSeconds: Int? = null,
    val inputMethod: String = "TEXT",
    val emotionTags: String? = null,
    val importanceLevel: Int? = null,
    val isCompanionInput: Boolean = false,
    val companionUserId: Int? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateUserContextRequest(
    val contextType: String,
    val title: String? = null,
    val content: String,
    val voiceUrl: String? = null,
    val voiceDurationSeconds: Int? = null,
    val inputMethod: String = "TEXT",
    val emotionTags: String? = null,
    val importanceLevel: Int? = null,
    val isCompanionInput: Boolean = false,
    val companionUserId: Int? = null
)

/**
 * User preferences data class
 */
@Serializable
data class UserPreference(
    val id: Int,
    val userId: Int,
    val narrativeStyle: String = "LITERARY",
    val preferredLength: String = "MEDIUM",
    val ttsSpeed: Double = 1.0,
    val ttsPitch: Double = 1.0,
    val ttsVoice: String = "default",
    val preferredLanguage: String = "ko-KR",
    val enableHapticFeedback: Boolean = true,
    val enableAudioDescriptions: Boolean = true,
    val highContrastMode: Boolean = false,
    val enablePushNotifications: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class UpdateUserPreferenceRequest(
    val narrativeStyle: String? = null,
    val preferredLength: String? = null,
    val ttsSpeed: Double? = null,
    val ttsPitch: Double? = null,
    val ttsVoice: String? = null,
    val preferredLanguage: String? = null,
    val enableHapticFeedback: Boolean? = null,
    val enableAudioDescriptions: Boolean? = null,
    val highContrastMode: Boolean? = null,
    val enablePushNotifications: Boolean? = null
)

/**
 * Artwork data class
 */
@Serializable
data class Artwork(
    val id: Int,
    val title: String,
    val titleEn: String? = null,
    val artist: String,
    val artistEn: String? = null,
    val artworkType: String? = null,
    val genre: String? = null,
    val creationYear: Int? = null,
    val creationPeriod: String? = null,
    val medium: String? = null,
    val dimensions: String? = null,
    val museum: String? = null,
    val museumEn: String? = null,
    val museumLocation: String? = null,
    val currentLocation: String? = null,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    val highResUrl: String? = null,
    val description: String? = null,
    val historicalContext: String? = null,
    val wikipediaUrl: String? = null,
    val museumWebsiteUrl: String? = null,
    val viewCount: Int = 0,
    val docentGenerationCount: Int = 0,
    val averageRating: Double? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateArtworkRequest(
    val title: String,
    val titleEn: String? = null,
    val artist: String,
    val artistEn: String? = null,
    val artworkType: String? = null,
    val genre: String? = null,
    val creationYear: Int? = null,
    val creationPeriod: String? = null,
    val medium: String? = null,
    val dimensions: String? = null,
    val museum: String? = null,
    val museumEn: String? = null,
    val museumLocation: String? = null,
    val currentLocation: String? = null,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    val highResUrl: String? = null,
    val description: String? = null,
    val historicalContext: String? = null,
    val wikipediaUrl: String? = null,
    val museumWebsiteUrl: String? = null
)

/**
 * Artwork search data class
 */
@Serializable
data class ArtworkSearch(
    val id: Int,
    val userId: Int,
    val searchMethod: String,
    val searchQuery: String? = null,
    val searchQueryVoiceUrl: String? = null,
    val capturedImageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val resultsCount: Int = 0,
    val selectedArtworkId: Int? = null,
    val createdAt: String
)

@Serializable
data class CreateArtworkSearchRequest(
    val searchMethod: String,
    val searchQuery: String? = null,
    val searchQueryVoiceUrl: String? = null,
    val capturedImageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val resultsCount: Int = 0,
    val selectedArtworkId: Int? = null
)

/**
 * Docent session data class
 */
@Serializable
data class DocentSession(
    val id: Int,
    val userId: Int,
    val artworkId: Int,
    val promptTemplate: String,
    val promptPersona: String? = null,
    val promptTask: String? = null,
    val promptContext: String? = null,
    val promptForm: String? = null,
    val generatedText: String,
    val geminiModel: String? = null,
    val geminiTemperature: Double? = null,
    val geminiTopP: Double? = null,
    val geminiTopK: Int? = null,
    val generationTimeMs: Int? = null,
    val ttsAudioUrl: String? = null,
    val ttsDurationSeconds: Int? = null,
    val playCount: Int = 0,
    val totalListeningSeconds: Int = 0,
    val completionRate: Double? = null,
    val status: String = "COMPLETED",
    val createdAt: String,
    val lastPlayedAt: String? = null
)

@Serializable
data class CreateDocentSessionRequest(
    val artworkId: Int,
    val promptTemplate: String,
    val promptPersona: String? = null,
    val promptTask: String? = null,
    val promptContext: String? = null,
    val promptForm: String? = null,
    val generatedText: String,
    val geminiModel: String? = null,
    val geminiTemperature: Double? = null,
    val geminiTopP: Double? = null,
    val geminiTopK: Int? = null,
    val generationTimeMs: Int? = null
)

/**
 * Docent feedback data class
 */
@Serializable
data class DocentFeedback(
    val id: Int,
    val docentSessionId: Int,
    val userId: Int,
    val emotionalResonance: Int,
    val imaginativeEngagement: Int? = null,
    val emotionalImpact: Int? = null,
    val overallSatisfaction: Double? = null,
    val comment: String? = null,
    val isFewShotCandidate: Boolean = false,
    val fewShotSelectedAt: String? = null,
    val createdAt: String
)

@Serializable
data class CreateDocentFeedbackRequest(
    val docentSessionId: Int,
    val emotionalResonance: Int,
    val imaginativeEngagement: Int? = null,
    val emotionalImpact: Int? = null,
    val comment: String? = null
)

/**
 * User link data class
 */
@Serializable
data class UserLink(
    val id: Int,
    val userId: Int,
    val artworkId: Int? = null,
    val docentSessionId: Int? = null,
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val linkType: String? = null,
    val thumbnailUrl: String? = null,
    val hasAudioDescription: Boolean = false,
    val hasSubtitles: Boolean = false,
    val createdAt: String
)

@Serializable
data class CreateUserLinkRequest(
    val artworkId: Int? = null,
    val docentSessionId: Int? = null,
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val linkType: String? = null,
    val thumbnailUrl: String? = null,
    val hasAudioDescription: Boolean = false,
    val hasSubtitles: Boolean = false
)

/**
 * User memo data class
 */
@Serializable
data class UserMemo(
    val id: Int,
    val userId: Int,
    val artworkId: Int? = null,
    val docentSessionId: Int? = null,
    val content: String,
    val voiceUrl: String? = null,
    val voiceDurationSeconds: Int? = null,
    val inputMethod: String = "TEXT",
    val tags: String? = null,
    val category: String? = null,
    val isSharedWithCompanion: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateUserMemoRequest(
    val artworkId: Int? = null,
    val docentSessionId: Int? = null,
    val content: String,
    val voiceUrl: String? = null,
    val voiceDurationSeconds: Int? = null,
    val inputMethod: String = "TEXT",
    val tags: String? = null,
    val category: String? = null,
    val isSharedWithCompanion: Boolean = false
)

/**
 * Voice recording data class
 */
@Serializable
data class VoiceRecording(
    val id: Int,
    val userId: Int,
    val fileUrl: String,
    val fileSizeBytes: Long? = null,
    val fileFormat: String = "mp3",
    val durationSeconds: Int,
    val sampleRate: Int? = null,
    val bitRate: Int? = null,
    val transcription: String? = null,
    val transcriptionConfidence: Double? = null,
    val transcriptionLanguage: String = "ko-KR",
    val relatedEntityType: String? = null,
    val relatedEntityId: Int? = null,
    val processingStatus: String = "UPLOADED",
    val createdAt: String,
    val processedAt: String? = null
)

@Serializable
data class CreateVoiceRecordingRequest(
    val fileUrl: String,
    val fileSizeBytes: Long? = null,
    val fileFormat: String = "mp3",
    val durationSeconds: Int,
    val sampleRate: Int? = null,
    val bitRate: Int? = null,
    val relatedEntityType: String? = null,
    val relatedEntityId: Int? = null
)

/**
 * Few-shot example data class
 */
@Serializable
data class FewShotExample(
    val id: Int,
    val artworkId: Int,
    val userContextSummary: String,
    val exemplarText: String,
    val sourceDocentSessionId: Int? = null,
    val sourceFeedbackId: Int? = null,
    val qualityScore: Double,
    val userRating: Int? = null,
    val usageCount: Int = 0,
    val lastUsedAt: String? = null,
    val category: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

/**
 * User companion data class
 */
@Serializable
data class UserCompanion(
    val id: Int,
    val mainUserId: Int,
    val companionUserId: Int,
    val relationshipType: String? = null,
    val canAddContext: Boolean = true,
    val canViewHistory: Boolean = true,
    val canAddMemos: Boolean = true,
    val status: String = "ACTIVE",
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateUserCompanionRequest(
    val companionUserId: Int,
    val relationshipType: String? = null,
    val canAddContext: Boolean = true,
    val canViewHistory: Boolean = true,
    val canAddMemos: Boolean = true
)
