package com.kevin.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Database initialization and migration utilities
 * Handle table creation, seeding, and cleanup for 14 tables
 */
object DatabaseInitializer {

    /**
     * Initialize all database tables
     * Creates tables if they don't exist
     */
    fun initializeTables(database: Database) {
        transaction(database) {
            // Create tables in the correct order (respecting foreign key constraints)
            SchemaUtils.create(
                Users,
                UserProfiles,
                UserContexts,
                UserPreferences,
                Artworks,
                ArtworkSearches,
                DocentSessions,
                DocentFeedbacks,
                UserLinks,
                UserMemos,
                VoiceRecordings,
                FewShotExamples,
                UserCompanions,
                SchemaMigrations
            )
        }
    }

    /**
     * Drop all database tables
     * Use with caution - deletes all data!
     */
    fun dropAllTables(database: Database) {
        transaction(database) {
            // Drop in reverse order of dependencies
            SchemaUtils.drop(
                SchemaMigrations,
                UserCompanions,
                FewShotExamples,
                VoiceRecordings,
                UserMemos,
                UserLinks,
                DocentFeedbacks,
                DocentSessions,
                ArtworkSearches,
                Artworks,
                UserPreferences,
                UserContexts,
                UserProfiles,
                Users
            )
        }
    }

    /**
     * Reset database (drop and recreate all tables)
     */
    fun resetDatabase(database: Database) {
        dropAllTables(database)
        initializeTables(database)
    }

    /**
     * Seed sample data for testing
     */
    fun seedSampleData(database: Database) {
        transaction(database) {
            // Create a sample main user
            val mainUserIdValue = Users.insert {
                it[email] = "user@example.com"
                it[passwordHash] = "\$2a\$10\$samplehashforpassword123456"
                it[userType] = "MAIN_USER"
                it[isVisuallyImpaired] = true
                it[impairmentLevel] = "TOTAL_BLINDNESS"
                it[isOnboardingCompleted] = true
            } get Users.id

            // Create a companion user
            val companionId = Users.insert {
                it[email] = "companion@example.com"
                it[passwordHash] = "\$2a\$10\$samplehashforpassword123456"
                it[userType] = "COMPANION"
                it[isVisuallyImpaired] = false
                it[isOnboardingCompleted] = true
            } get Users.id

            // Create user profile
            UserProfiles.insert {
                it[userId] = mainUserIdValue
                it[interests] = """["예술", "음악", "자연", "역사"]"""
                it[hobbies] = "산책하며 새소리 듣기를 좋아합니다"
                it[favoriteArtists] = "빈센트 반 고흐, 클로드 모네"
                it[bio] = "미술을 사랑하는 시각장애인입니다"
            }

            // Create user contexts
            UserContexts.insert {
                it[userId] = mainUserIdValue
                it[contextType] = "MEMORY"
                it[title] = "바다 여행"
                it[content] = "폭풍우 속에서 차가운 바다 물보라가 얼굴에 닿던 느낌을 기억합니다"
                it[inputMethod] = "TEXT"
                it[emotionTags] = "평온함,감동"
                it[importanceLevel] = 5
            }

            UserContexts.insert {
                it[userId] = mainUserIdValue
                it[contextType] = "EXPERIENCE"
                it[title] = "어린시절 기억"
                it[content] = "할머니와 함께 정원에서 장미 향기를 맡던 기억"
                it[inputMethod] = "VOICE"
                it[importanceLevel] = 4
            }

            // Create user preferences
            UserPreferences.insert {
                it[userId] = mainUserIdValue
                it[narrativeStyle] = "LITERARY"
                it[preferredLength] = "MEDIUM"
                it[ttsSpeed] = 1.0.toBigDecimal()
                it[ttsPitch] = 1.0.toBigDecimal()
                it[ttsVoice] = "default"
                it[preferredLanguage] = "ko-KR"
            }

            // Create sample artworks
            val artwork1Id = Artworks.insert {
                it[title] = "별이 빛나는 밤"
                it[titleEn] = "The Starry Night"
                it[artist] = "빈센트 반 고흐"
                it[artistEn] = "Vincent van Gogh"
                it[creationYear] = 1889
                it[artworkType] = "PAINTING"
                it[genre] = "후기 인상주의"
                it[medium] = "캔버스에 유채"
                it[dimensions] = "73.7 × 92.1 cm"
                it[museum] = "뉴욕 현대미술관"
                it[museumEn] = "Museum of Modern Art (MoMA)"
                it[museumLocation] = "New York, USA"
                it[imageUrl] = "https://example.com/starry-night.jpg"
                it[thumbnailUrl] = "https://example.com/starry-night-thumb.jpg"
                it[description] = "소용돌이치는 밤하늘과 사이프러스 나무가 특징적인 후기 인상주의 걸작"
                it[historicalContext] = "반 고흐가 생레미의 정신병원에 입원 중이던 1889년 6월에 그린 작품"
                it[metadata] = """{"colors": ["blue", "yellow", "white"], "themes": ["nature", "night", "emotion"]}"""
            } get Artworks.id

            val artwork2Id = Artworks.insert {
                it[title] = "모나리자"
                it[titleEn] = "Mona Lisa"
                it[artist] = "레오나르도 다 빈치"
                it[artistEn] = "Leonardo da Vinci"
                it[creationYear] = 1503
                it[artworkType] = "PAINTING"
                it[genre] = "르네상스"
                it[medium] = "포플러 나무 패널에 유채"
                it[dimensions] = "77 × 53 cm"
                it[museum] = "루브르 박물관"
                it[museumEn] = "Louvre Museum"
                it[museumLocation] = "Paris, France"
                it[imageUrl] = "https://example.com/mona-lisa.jpg"
                it[description] = "세계에서 가장 유명한 초상화. 미스터리한 미소로 유명함"
                it[metadata] = """{"colors": ["brown", "green", "skin"], "themes": ["portrait", "mystery"]}"""
            } get Artworks.id

            // Create artwork search
            val searchId = ArtworkSearches.insert {
                it[userId] = mainUserIdValue
                it[searchMethod] = "TEXT"
                it[searchQuery] = "별이 빛나는 밤"
                it[resultsCount] = 1
                it[selectedArtworkId] = artwork1Id
            } get ArtworkSearches.id

            // Create docent session
            val sessionId = DocentSessions.insert {
                it[userId] = mainUserIdValue
                it[artworkId] = artwork1Id
                it[promptTemplate] = "당신은 시각장애인을 위한 맞춤형 도슨트입니다. 사용자의 개인적 경험과 감성을 연결하여 작품을 설명해주세요."
                it[promptContext] = "사용자는 바다와 물보라 관련 기억, 폭풍우 경험이 있습니다"
                it[generatedText] = "상상해보세요. 밤하늘이 마치 살아있는 것처럼 소용돌이치고 있습니다. 당신이 경험했던 폭풍우 속 파도처럼, 반 고흐의 하늘도 역동적으로 움직입니다..."
                it[geminiModel] = "gemini-2.0-flash-exp"
                it[geminiTemperature] = 0.8.toBigDecimal()
                it[fewShotExamples] = """[]"""
                it[status] = "COMPLETED"
                it[playCount] = 3
                it[totalListeningSeconds] = 180
            } get DocentSessions.id

            // Create feedback
            val feedbackId = DocentFeedbacks.insert {
                it[docentSessionId] = sessionId
                it[userId] = mainUserIdValue
                it[emotionalResonance] = 5
                it[imaginativeEngagement] = 5
                it[emotionalImpact] = 5
                it[overallSatisfaction] = 5.0.toBigDecimal()
                it[comment] = "정말 감동적이었습니다. 마치 제가 그림 속에 있는 것 같았어요."
                it[improvementSuggestions] = """{}"""
                it[isFewShotCandidate] = true
            } get DocentFeedbacks.id

            // Create user link
            UserLinks.insert {
                it[userId] = mainUserIdValue
                it[artworkId] = artwork1Id
                it[url] = "https://www.moma.org/collection/works/79802"
                it[title] = "MoMA - The Starry Night"
                it[description] = "뉴욕 현대미술관의 공식 작품 페이지"
                it[linkType] = "REFERENCE"
                it[metadata] = """{"source": "museum"}"""
            }

            // Create user memo
            UserMemos.insert {
                it[userId] = mainUserIdValue
                it[docentSessionId] = sessionId
                it[content] = "이 작품을 들으면서 어린시절 할머니와 함께 밤하늘을 보던 기억이 떠올랐다"
                it[inputMethod] = "TEXT"
                it[category] = "THOUGHT"
                it[tags] = "추억,감동,할머니"
            }

            // Create voice recording
            VoiceRecordings.insert {
                it[userId] = mainUserIdValue
                it[fileUrl] = "https://example.com/recordings/voice-001.mp3"
                it[fileSizeBytes] = 256000
                it[fileFormat] = "mp3"
                it[durationSeconds] = 45
                it[transcription] = "할머니와 함께 정원에서 장미 향기를 맡던 기억"
                it[transcriptionConfidence] = 0.95.toBigDecimal()
                it[relatedEntityType] = "USER_CONTEXT"
                it[relatedEntityId] = 2
                it[processingStatus] = "COMPLETED"
            }

            // Create few-shot example
            FewShotExamples.insert {
                it[artworkId] = artwork1Id
                it[userContextSummary] = "폭풍우와 바다 경험이 있는 시각장애인"
                it[exemplarText] = "상상해보세요. 밤하늘이 마치 살아있는 것처럼 소용돌이치고 있습니다..."
                it[sourceDocentSessionId] = sessionId
                it[sourceFeedbackId] = feedbackId
                it[qualityScore] = 5.0.toBigDecimal()
                it[userRating] = 5
                it[category] = "EMOTIONAL"
                it[isActive] = true
            }

            // Create a user companion relationship
            UserCompanions.insert {
                it[mainUserId] = mainUserIdValue
                it[companionUserId] = companionId
                it[relationshipType] = "FAMILY"
                it[canAddContext] = true
                it[canViewHistory] = true
                it[canAddMemos] = true
                it[status] = "ACTIVE"
            }

            // Record schema migration
            SchemaMigrations.insert {
                it[version] = "2025_01_01_initial_schema"
                it[description] = "Initial 14-table schema creation"
            }
        }
    }

    /**
     * Seed more artworks for testing
     */
    fun seedArtworks(database: Database) {
        transaction(database) {
            val artworks = listOf(
                mapOf(
                    "title" to "진주 귀걸이를 한 소녀",
                    "titleEn" to "Girl with a Pearl Earring",
                    "artist" to "요하네스 베르메르",
                    "artistEn" to "Johannes Vermeer",
                    "year" to 1665,
                    "museum" to "마우리츠하위스 미술관",
                    "type" to "PAINTING",
                    "genre" to "바로크"
                ),
                mapOf(
                    "title" to "절규",
                    "titleEn" to "The Scream",
                    "artist" to "에드바르 뭉크",
                    "artistEn" to "Edvard Munch",
                    "year" to 1893,
                    "museum" to "노르웨이 국립미술관",
                    "type" to "PAINTING",
                    "genre" to "표현주의"
                ),
                mapOf(
                    "title" to "게르니카",
                    "titleEn" to "Guernica",
                    "artist" to "파블로 피카소",
                    "artistEn" to "Pablo Picasso",
                    "year" to 1937,
                    "museum" to "레이나 소피아 미술관",
                    "type" to "PAINTING",
                    "genre" to "입체파"
                ),
                mapOf(
                    "title" to "키스",
                    "titleEn" to "The Kiss",
                    "artist" to "구스타프 클림트",
                    "artistEn" to "Gustav Klimt",
                    "year" to 1908,
                    "museum" to "벨베데레 궁전",
                    "type" to "PAINTING",
                    "genre" to "아르누보"
                ),
                mapOf(
                    "title" to "아비뇽의 처녀들",
                    "titleEn" to "Les Demoiselles d'Avignon",
                    "artist" to "파블로 피카소",
                    "artistEn" to "Pablo Picasso",
                    "year" to 1907,
                    "museum" to "뉴욕 현대미술관",
                    "type" to "PAINTING",
                    "genre" to "입체파"
                )
            )

            artworks.forEach { artwork ->
                Artworks.insert {
                    it[title] = artwork["title"] as String
                    it[titleEn] = artwork["titleEn"] as String
                    it[artist] = artwork["artist"] as String
                    it[artistEn] = artwork["artistEn"] as String
                    it[creationYear] = artwork["year"] as Int
                    it[artworkType] = artwork["type"] as String
                    it[genre] = artwork["genre"] as String
                    it[museum] = artwork["museum"] as String
                    it[imageUrl] = "https://example.com/${artwork["titleEn"]}.jpg"
                    it[description] = "${artwork["artist"]}의 대표작 중 하나"
                    it[metadata] = """{"type": "${artwork["type"]}", "genre": "${artwork["genre"]}"}"""
                }
            }
        }
    }

    /**
     * Clean all data from a database (keep schema)
     */
    fun cleanAllData(database: Database) {
        transaction(database) {
            // Delete it in reverse order of dependencies
            SchemaMigrations.deleteAll()
            UserCompanions.deleteAll()
            FewShotExamples.deleteAll()
            VoiceRecordings.deleteAll()
            UserMemos.deleteAll()
            UserLinks.deleteAll()
            DocentFeedbacks.deleteAll()
            DocentSessions.deleteAll()
            ArtworkSearches.deleteAll()
            Artworks.deleteAll()
            UserPreferences.deleteAll()
            UserContexts.deleteAll()
            UserProfiles.deleteAll()
            Users.deleteAll()
        }
    }
}
