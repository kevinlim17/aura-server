package com.kevin.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Complete Aura Database Schema - 14 Tables
 * Based on: project_schema/aura_schema.sql
 *
 * Note: JSONB fields are stored as TEXT and should be serialized/deserialized in application layer
 */

// ============================================================================
// 1. USERS TABLE
// ============================================================================
object Users : IntIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)

    // User type
    val userType = varchar("user_type", 20).default("MAIN_USER")

    // Accessibility settings
    val isVisuallyImpaired = bool("is_visually_impaired").default(true)
    val impairmentLevel = varchar("impairment_level", 20).nullable()

    // Account status
    val isActive = bool("is_active").default(true)
    val isOnboardingCompleted = bool("is_onboarding_completed").default(false)

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    val lastLoginAt = timestamp("last_login_at").nullable()
}

// ============================================================================
// 2. USER PROFILES TABLE
// ============================================================================
object UserProfiles : IntIdTable("user_profiles") {
    val userId = reference("user_id", Users).uniqueIndex()

    // Interests (JSON array - stored as TEXT, serialize/deserialize in app)
    val interests = text("interests").default("[]")

    // Hobbies
    val hobbies = text("hobbies").nullable()
    val hobbiesVoiceUrl = text("hobbies_voice_url").nullable()

    // Favorite artists
    val favoriteArtists = text("favorite_artists").nullable()

    // Additional information
    val bio = text("bio").nullable()

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 3. USER CONTEXTS TABLE
// ============================================================================
object UserContexts : IntIdTable("user_contexts") {
    val userId = reference("user_id", Users)

    // Context type
    val contextType = varchar("context_type", 50)

    // Context title
    val title = varchar("title", 255).nullable()

    // Content
    val content = text("content")

    // Voice recording
    val voiceUrl = text("voice_url").nullable()
    val voiceDurationSeconds = integer("voice_duration_seconds").nullable()

    // Input method
    val inputMethod = varchar("input_method", 20).default("TEXT")

    // Emotion tags
    val emotionTags = text("emotion_tags").nullable()

    // Importance level
    val importanceLevel = integer("importance_level").nullable()

    // Companion input
    val isCompanionInput = bool("is_companion_input").default(false)
    val companionUserId = reference("companion_user_id", Users).nullable()

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 4. USER PREFERENCES TABLE
// ============================================================================
object UserPreferences : IntIdTable("user_preferences") {
    val userId = reference("user_id", Users).uniqueIndex()

    // Preferred commentary style
    val narrativeStyle = varchar("narrative_style", 50).default("LITERARY")

    // Preferred length
    val preferredLength = varchar("preferred_length", 20).default("MEDIUM")

    // TTS settings
    val ttsSpeed = decimal("tts_speed", 3, 1).default(1.0.toBigDecimal())
    val ttsPitch = decimal("tts_pitch", 3, 1).default(1.0.toBigDecimal())
    val ttsVoice = varchar("tts_voice", 50).default("NEUTRAL")

    // Language setting
    val preferredLanguage = varchar("preferred_language", 10).default("ko-KR")

    // Accessibility settings
    val enableHapticFeedback = bool("enable_haptic_feedback").default(true)
    val enableAudioDescriptions = bool("enable_audio_descriptions").default(true)
    val highContrastMode = bool("high_contrast_mode").default(false)

    // Notification settings
    val enablePushNotifications = bool("enable_push_notifications").default(true)

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 5. ARTWORKS TABLE
// ============================================================================
object Artworks : IntIdTable("artworks") {
    // Basic artwork information
    val title = varchar("title", 500)
    val titleEn = varchar("title_en", 500).nullable()
    val artist = varchar("artist", 255)
    val artistEn = varchar("artist_en", 255).nullable()

    // Artwork classification
    val artworkType = varchar("artwork_type", 50).nullable()
    val genre = varchar("genre", 100).nullable()

    // Artwork details
    val creationYear = integer("creation_year").nullable()
    val creationPeriod = varchar("creation_period", 100).nullable()
    val medium = varchar("medium", 255).nullable()
    val dimensions = varchar("dimensions", 100).nullable()

    // Collection information
    val museum = varchar("museum", 255).nullable()
    val museumEn = varchar("museum_en", 255).nullable()
    val museumLocation = varchar("museum_location", 255).nullable()
    val currentLocation = varchar("current_location", 255).nullable()

    // Images
    val imageUrl = text("image_url")
    val thumbnailUrl = text("thumbnail_url").nullable()
    val highResUrl = text("high_res_url").nullable()

    // Description
    val description = text("description").nullable()
    val historicalContext = text("historical_context").nullable()

    // Metadata (JSON - stored as TEXT)
    val metadata = text("metadata").default("{}")

    // External links
    val wikipediaUrl = text("wikipedia_url").nullable()
    val museumWebsiteUrl = text("museum_website_url").nullable()

    // Statistics
    val viewCount = integer("view_count").default(0)
    val docentGenerationCount = integer("docent_generation_count").default(0)
    val averageRating = decimal("average_rating", 3, 2).nullable()

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 6. ARTWORK SEARCHES TABLE
// ============================================================================
object ArtworkSearches : IntIdTable("artwork_searches") {
    val userId = reference("user_id", Users)

    // Search method
    val searchMethod = varchar("search_method", 20)

    // Search query
    val searchQuery = text("search_query").nullable()
    val searchQueryVoiceUrl = text("search_query_voice_url").nullable()

    // Camera capture
    val capturedImageUrl = text("captured_image_url").nullable()

    // Location-based search
    val latitude = decimal("latitude", 10, 8).nullable()
    val longitude = decimal("longitude", 11, 8).nullable()
    val locationName = varchar("location_name", 255).nullable()

    // Search results
    val resultsCount = integer("results_count").default(0)
    val selectedArtworkId = reference("selected_artwork_id", Artworks).nullable()

    // Timestamp
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 7. DOCENT SESSIONS TABLE
// ============================================================================
object DocentSessions : IntIdTable("docent_sessions") {
    val userId = reference("user_id", Users)
    val artworkId = reference("artwork_id", Artworks)

    // Prompt information
    val promptTemplate = text("prompt_template")
    val promptPersona = text("prompt_persona").nullable()
    val promptTask = text("prompt_task").nullable()
    val promptContext = text("prompt_context").nullable()
    val promptForm = text("prompt_form").nullable()

    // Few-shot examples (JSON - stored as TEXT)
    val fewShotExamples = text("few_shot_examples").default("[]")

    // Generated docent text
    val generatedText = text("generated_text")

    // Gemini API response metadata
    val geminiModel = varchar("gemini_model", 50).nullable()
    val geminiTemperature = decimal("gemini_temperature", 3, 2).nullable()
    val geminiTopP = decimal("gemini_top_p", 3, 2).nullable()
    val geminiTopK = integer("gemini_top_k").nullable()
    val generationTimeMs = integer("generation_time_ms").nullable()

    // TTS audio
    val ttsAudioUrl = text("tts_audio_url").nullable()
    val ttsDurationSeconds = integer("tts_duration_seconds").nullable()

    // Playback statistics
    val playCount = integer("play_count").default(0)
    val totalListeningSeconds = integer("total_listening_seconds").default(0)
    val completionRate = decimal("completion_rate", 5, 2).nullable()

    // Status
    val status = varchar("status", 20).default("COMPLETED")

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val lastPlayedAt = timestamp("last_played_at").nullable()
}

// ============================================================================
// 8. DOCENT FEEDBACKS TABLE
// ============================================================================
object DocentFeedbacks : IntIdTable("docent_feedbacks") {
    val docentSessionId = reference("docent_session_id", DocentSessions)
    val userId = reference("user_id", Users)

    // Ratings (1-5)
    val emotionalResonance = integer("emotional_resonance")
    val imaginativeEngagement = integer("imaginative_engagement").nullable()
    val emotionalImpact = integer("emotional_impact").nullable()

    // Overall satisfaction (auto-calculated)
    val overallSatisfaction = decimal("overall_satisfaction", 3, 2).nullable()

    // Text feedback
    val comment = text("comment").nullable()

    // Improvement suggestions (JSON - stored as TEXT)
    val improvementSuggestions = text("improvement_suggestions").default("{}")

    // Few-shot learning candidate
    val isFewShotCandidate = bool("is_few_shot_candidate").default(false)
    val fewShotSelectedAt = timestamp("few_shot_selected_at").nullable()

    // Timestamp
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 9. USER LINKS TABLE
// ============================================================================
object UserLinks : IntIdTable("user_links") {
    val userId = reference("user_id", Users)

    // Connected entities
    val artworkId = reference("artwork_id", Artworks).nullable()
    val docentSessionId = reference("docent_session_id", DocentSessions).nullable()

    // Link information
    val url = text("url")
    val title = varchar("title", 500).nullable()
    val description = text("description").nullable()

    // Link type
    val linkType = varchar("link_type", 50).nullable()

    // Metadata (JSON - stored as TEXT)
    val metadata = text("metadata").default("{}")

    // Thumbnail
    val thumbnailUrl = text("thumbnail_url").nullable()

    // Accessibility
    val hasAudioDescription = bool("has_audio_description").default(false)
    val hasSubtitles = bool("has_subtitles").default(false)

    // Timestamp
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 10. USER MEMOS TABLE
// ============================================================================
object UserMemos : IntIdTable("user_memos") {
    val userId = reference("user_id", Users)

    // Connected entities
    val artworkId = reference("artwork_id", Artworks).nullable()
    val docentSessionId = reference("docent_session_id", DocentSessions).nullable()

    // Memo content
    val content = text("content")

    // Voice memo
    val voiceUrl = text("voice_url").nullable()
    val voiceDurationSeconds = integer("voice_duration_seconds").nullable()

    // Input method
    val inputMethod = varchar("input_method", 20).default("TEXT")

    // Tags
    val tags = text("tags").nullable()

    // Memo category
    val category = varchar("category", 50).nullable()

    // Sharing with companion
    val isSharedWithCompanion = bool("is_shared_with_companion").default(false)

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 11. VOICE RECORDINGS TABLE
// ============================================================================
object VoiceRecordings : IntIdTable("voice_recordings") {
    val userId = reference("user_id", Users)

    // File information
    val fileUrl = text("file_url")
    val fileSizeBytes = long("file_size_bytes").nullable()
    val fileFormat = varchar("file_format", 20).default("mp3")

    // Recording information
    val durationSeconds = integer("duration_seconds")
    val sampleRate = integer("sample_rate").nullable()
    val bitRate = integer("bit_rate").nullable()

    // Transcription
    val transcription = text("transcription").nullable()
    val transcriptionConfidence = decimal("transcription_confidence", 5, 4).nullable()
    val transcriptionLanguage = varchar("transcription_language", 10).default("ko-KR")

    // Related entity (polymorphic)
    val relatedEntityType = varchar("related_entity_type", 50).nullable()
    val relatedEntityId = integer("related_entity_id").nullable()

    // Processing status
    val processingStatus = varchar("processing_status", 20).default("UPLOADED")

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val processedAt = timestamp("processed_at").nullable()
}

// ============================================================================
// 12. FEW-SHOT EXAMPLES TABLE
// ============================================================================
object FewShotExamples : IntIdTable("few_shot_examples") {
    // Few-shot example information
    val artworkId = reference("artwork_id", Artworks)
    val userContextSummary = text("user_context_summary")

    // Exemplar text
    val exemplarText = text("exemplar_text")

    // Original session
    val sourceDocentSessionId = reference("source_docent_session_id", DocentSessions).nullable()
    val sourceFeedbackId = reference("source_feedback_id", DocentFeedbacks).nullable()

    // Quality metrics
    val qualityScore = decimal("quality_score", 3, 2)
    val userRating = integer("user_rating").nullable()

    // Usage statistics
    val usageCount = integer("usage_count").default(0)
    val lastUsedAt = timestamp("last_used_at").nullable()

    // Category
    val category = varchar("category", 50).nullable()

    // Active status
    val isActive = bool("is_active").default(true)

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

// ============================================================================
// 13. USER COMPANIONS TABLE
// ============================================================================
object UserCompanions : IntIdTable("user_companions") {
    // Main user (visually impaired)
    val mainUserId = reference("main_user_id", Users)

    // Companion
    val companionUserId = reference("companion_user_id", Users)

    // Relationship type
    val relationshipType = varchar("relationship_type", 50).nullable()

    // Permission settings
    val canAddContext = bool("can_add_context").default(true)
    val canViewHistory = bool("can_view_history").default(true)
    val canAddMemos = bool("can_add_memos").default(true)

    // Status
    val status = varchar("status", 20).default("ACTIVE")

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex(mainUserId, companionUserId)
    }
}

// ============================================================================
// 14. SCHEMA MIGRATIONS TABLE
// ============================================================================
object SchemaMigrations : org.jetbrains.exposed.sql.Table("schema_migrations") {
    val version = varchar("version", 50)
    val description = text("description").nullable()
    val appliedAt = timestamp("applied_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(version)
}
