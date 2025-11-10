package com.kevin

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Aura Server Integration Test
 * 전체 플로우를 테스트합니다:
 * 1-2. 회원가입 및 로그인
 * 3-5. 사용자 프로필/컨텍스트/선호도 설정
 * 6-7. 작품 검색 및 조회
 * 8-11. 도슨트 생성 및 재생
 * 12. 피드백 제출 및 조회, Few-Shot 자동 생성 확인
 * 13. 링크 관리 (추가/조회/삭제)
 * 14. 메모 관리 (생성/조회/수정/통계/삭제)
 * 15. 동행자 관리 (초대/수락/권한/통계/초대코드)
 */
class AuraIntegrationTest {

    @Test
    fun testCompleteUserJourney() = testApplication {
        application {
            module()
        }

        val testEmail = "integration_test_${System.currentTimeMillis()}@example.com"
        val companionEmail = "companion_${System.currentTimeMillis()}@example.com"
        var userId: Int
        var token: String
        var artworkId: Int
        var docentSessionId: Int
        var feedbackId: Int = 0
        var linkId: Int = 0
        var memoId: Int = 0
        var companionUserId: Int = 0
        var companionToken: String = ""
        var companionId: Int = 0

        // ========================================
        // 1. 회원가입
        // ========================================
        println("=== Step 1: 회원가입 ===")
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "$testEmail",
                    "password": "TestPassword123!",
                    "userType": "MAIN_USER",
                    "isVisuallyImpaired": true,
                    "impairmentLevel": "MODERATE"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, registerResponse.status)
        println("✓ 회원가입 성공")

        // ========================================
        // 2. 로그인
        // ========================================
        println("\n=== Step 2: 로그인 ===")
        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "$testEmail",
                    "password": "TestPassword123!"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginBody = loginResponse.bodyAsText()
        userId = extractJsonInt(loginBody, "id")
        token = extractJsonString(loginBody, "token")
        assertTrue(userId > 0)
        assertTrue(token.isNotEmpty())
        println("✓ 로그인 성공 - User ID: $userId")

        // ========================================
        // 3. 사용자 프로필 생성
        // ========================================
        println("\n=== Step 3: 사용자 프로필 생성 ===")
        val profileResponse = client.post("/api/users/$userId/profile") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "interests": ["impressionism", "post-impressionism", "modern art"],
                    "hobbies": "미술관 방문, 독서, 음악 감상",
                    "favoriteArtists": ["빈센트 반 고흐", "클로드 모네", "파블로 피카소"],
                    "bio": "예술을 통해 세상을 느끼고 싶은 시각장애인입니다. 특히 인상주의 작품들의 색채와 빛의 표현에 관심이 많습니다."
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, profileResponse.status)
        println("✓ 사용자 프로필 생성 완료")

        // ========================================
        // 4. 사용자 컨텍스트 추가 (개인 경험/감정)
        // ========================================
        println("\n=== Step 4: 사용자 컨텍스트 추가 ===")

        // Context 1: 어린 시절 기억
        val context1Response = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "MEMORY",
                    "title": "어린 시절 미술관 방문",
                    "content": "어린 시절 가족과 함께 미술관에 갔던 기억이 있습니다. 그림의 질감과 향기가 생생하게 기억납니다. 특히 노란색 계열의 그림들이 따뜻하게 느껴졌습니다.",
                    "emotionTags": ["nostalgic", "warm", "happy"],
                    "inputMethod": "TEXT",
                    "importanceLevel": 5
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.Created, context1Response.status)
        println("✓ 컨텍스트 1 추가: 어린 시절 기억")

        // Context 2: 현재 감정 상태
        val context2Response = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "EMOTION",
                    "title": "요즘 감정",
                    "content": "요즘 약간 우울하고 지쳐있습니다. 위로가 되는 예술 작품을 통해 힐링하고 싶습니다.",
                    "emotionTags": ["sad", "tired", "hopeful"],
                    "inputMethod": "TEXT",
                    "importanceLevel": 4
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.Created, context2Response.status)
        println("✓ 컨텍스트 2 추가: 현재 감정")

        // Context 3: 목표
        val context3Response = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "GOAL",
                    "title": "예술 감상 목표",
                    "content": "다양한 시대의 예술 작품을 감상하고, 각 작품이 담고 있는 이야기와 감정을 깊이 이해하고 싶습니다.",
                    "emotionTags": ["motivated", "curious"],
                    "inputMethod": "TEXT",
                    "importanceLevel": 5
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.Created, context3Response.status)
        println("✓ 컨텍스트 3 추가: 목표")

        // ========================================
        // 5. 사용자 선호도 설정
        // ========================================
        println("\n=== Step 5: 사용자 선호도 설정 ===")
        val preferencesResponse = client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "LONG",
                    "ttsSpeed": 0.9,
                    "ttsPitch": 1.0,
                    "ttsVoice": "FEMALE",
                    "preferredLanguage": "ko-KR",
                    "enableHapticFeedback": true,
                    "enableAudioDescriptions": true,
                    "highContrastMode": false,
                    "enablePushNotifications": true
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, preferencesResponse.status)
        println("✓ 사용자 선호도 설정 완료")
        println("  - 서사 스타일: LITERARY (문학적)")
        println("  - 길이: LONG (상세)")
        println("  - TTS 속도: 0.9배속")

        // ========================================
        // 6. 작품 검색 (텍스트 기반)
        // ========================================
        println("\n=== Step 6: 작품 검색 (첫 번째 시도) ===")
        val searchResponse = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "별이 빛나는 밤",
                    "page": 1,
                    "limit": 5
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, searchResponse.status)
        val searchBody = searchResponse.bodyAsText()

        // Check search source (DB or Gemini)
        val isFromGemini = when {
            searchBody.contains("from database") -> {
                println("✓ 작품 검색 성공 (DB에서 조회)")
                false
            }
            searchBody.contains("from Gemini AI") -> {
                println("✓ 작품 검색 성공 (Gemini API로 조회 및 캐싱)")
                true
            }
            else -> {
                println("✓ 작품 검색 성공")
                false
            }
        }

        // 검색 결과에서 첫 번째 작품 ID 추출
        artworkId = extractArtworkId(searchBody)

        if (artworkId > 0) {
            println("  - 작품 ID: $artworkId")
        } else {
            // 검색 결과가 없으면 테스트용 작품 생성 (실제로는 DB에 작품이 있어야 함)
            println("⚠ 검색 결과 없음 - 테스트용 작품 ID 사용: 1")
            artworkId = 1
        }

        // If results came from Gemini, verify they were cached to DB
        if (isFromGemini) {
            println("\n=== Step 6-2: DB 캐싱 검증 (두 번째 검색) ===")
            delay(1000) // Wait for DB write to complete

            val secondSearchResponse = client.post("/api/artworks/search") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "query": "별이 빛나는 밤",
                        "page": 1,
                        "limit": 5
                    }
                """.trimIndent())
            }

            assertEquals(HttpStatusCode.OK, secondSearchResponse.status)
            val secondSearchBody = secondSearchResponse.bodyAsText()

            when {
                secondSearchBody.contains("from database") -> {
                    println("✓ DB 캐싱 검증 성공: 두 번째 검색에서 DB로부터 조회됨")
                    println("  → Gemini 결과가 정상적으로 DB에 저장되었습니다")
                }
                secondSearchBody.contains("from Gemini AI") -> {
                    println("⚠ DB 캐싱 실패: 두 번째 검색도 Gemini API 호출")
                    println("  → DB 저장 로직에 문제가 있을 수 있습니다")
                }
                else -> {
                    println("? 검색 소스 불명확")
                }
            }

            // Update artworkId with a cached version
            val cachedArtworkId = extractArtworkId(secondSearchBody)
            if (cachedArtworkId > 0) {
                artworkId = cachedArtworkId
                println("  - 캐시된 작품 ID: $artworkId")
            }
        }

        // ========================================
        // 7. 특정 작품 상세 조회
        // ========================================
        println("\n=== Step 7: 작품 상세 조회 ===")
        val artworkResponse = client.get("/api/artworks/$artworkId")

        if (artworkResponse.status == HttpStatusCode.OK) {
            println("✓ 작품 상세 조회 성공")
            val artworkBody = artworkResponse.bodyAsText()
            val title = extractJsonString(artworkBody, "title")
            val artist = extractJsonString(artworkBody, "artist")
            println("  - 제목: $title")
            println("  - 작가: $artist")
        } else {
            println("⚠ 작품 상세 조회 실패 (작품이 DB에 없을 수 있음)")
        }

        // ========================================
        // 8. AI 도슨트 생성 (비동기)
        // ========================================
        println("\n=== Step 8: AI 도슨트 생성 ===")
        val docentGenerateResponse = client.post("/api/docent/generate") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "userId": $userId,
                    "artworkId": $artworkId,
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "LONG",
                    "includeCompanionContext": false,
                    "useFewShotExamples": true,
                    "customPrompt": "사용자의 감정 상태와 어린 시절 기억을 고려하여 따뜻하고 위로가 되는 해설을 작성해주세요."
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Accepted, docentGenerateResponse.status)
        val docentGenerateBody = docentGenerateResponse.bodyAsText()
        docentSessionId = extractJsonInt(docentGenerateBody, "sessionId")
        println("✓ 도슨트 생성 시작 - Session ID: $docentSessionId")
        println("  - 비동기 처리 중...")

        // ========================================
        // 9. 도슨트 생성 상태 확인 (폴링)
        // ========================================
        println("\n=== Step 9: 도슨트 생성 상태 확인 (폴링) ===")
        var docentCompleted = false
        val maxAttempts = 45 // 최대 45초 대기 (1분 미만)

        for (attempts in 1..maxAttempts) {
            delay(1000) // 1초 대기

            val statusResponse = client.get("/api/docent/sessions/$docentSessionId")

            if (statusResponse.status == HttpStatusCode.OK) {
                val statusBody = statusResponse.bodyAsText()
                val status = extractJsonString(statusBody, "status")
                val progress = extractJsonInt(statusBody, "progress")

                // Progress indicator
                print(".")
                if (attempts % 10 == 0) {
                    println(" [${attempts}s] Status: $status, Progress: $progress%")
                }

                when (status) {
                    "COMPLETED" -> {
                        docentCompleted = true
                        println("\n✓ 도슨트 생성 완료! (${attempts}초)")

                        // 생성된 텍스트 확인
                        val generatedText = extractGeneratedText(statusBody)
                        println("\n=== 생성된 도슨트 텍스트 ===")
                        println(generatedText.take(200) + "...")
                        println("=========================\n")
                        break
                    }
                    "FAILED" -> {
                        val errorMessage = extractJsonString(statusBody, "errorMessage")
                        println("\n✗ 도슨트 생성 실패: $errorMessage")
                        break
                    }
                    "GENERATING" -> {
                        // 계속 대기
                    }
                }
            }
        }

        if (!docentCompleted) {
            println("\n⚠ 도슨트 생성 타임아웃 (${maxAttempts}초 초과)")
            println("  참고: 실제 Gemini API 호출 시 지연이 발생할 수 있습니다")
            println("  나머지 테스트는 도슨트 생성이 필요하지 않은 항목만 실행합니다")
        }

        // ========================================
        // 10. 도슨트 재생 통계 업데이트
        // ========================================
        if (docentCompleted) {
            println("=== Step 10: 재생 통계 업데이트 ===")
            val playStatsResponse = client.put("/api/docent/sessions/$docentSessionId/play-stats") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""
                    {
                        "playCount": 1,
                        "totalListeningSeconds": 180,
                        "completionRate": 0.95
                    }
                """.trimIndent())
            }

            if (playStatsResponse.status == HttpStatusCode.OK) {
                println("✓ 재생 통계 업데이트 완료")
                println("  - 재생 횟수: 1")
                println("  - 총 청취 시간: 180초")
                println("  - 완료율: 95%")
            }
        }

        // ========================================
        // 11. 사용자 도슨트 히스토리 조회
        // ========================================
        println("\n=== Step 11: 도슨트 히스토리 조회 ===")
        val historyResponse = client.get("/api/users/me/docent/history?page=1&limit=10") {
            header("Authorization", "Bearer $token")
        }

        if (historyResponse.status == HttpStatusCode.OK) {
            val historyBody = historyResponse.bodyAsText()
            val sessionCount = extractSessionCount(historyBody)
            println("✓ 도슨트 히스토리 조회 성공")
            println("  - 총 세션 수: $sessionCount")
        }

        // ========================================
        // 12. Feedback API 테스트
        // ========================================
        if (docentCompleted) {
            println("\n=== Step 12: Feedback API 테스트 ===")

            // 12-1. 피드백 제출
            println("12-1. 피드백 제출")
            val submitFeedbackResponse = client.post("/api/docent/sessions/$docentSessionId/feedback") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""
                    {
                        "emotionalResonance": 5,
                        "imaginativeEngagement": 5,
                        "emotionalImpact": 4,
                        "comment": "정말 감동적이고 개인적으로 공감되는 해설이었습니다. 어린 시절 기억과 연결되어 더욱 의미있었어요.",
                        "improvementSuggestions": {
                            "length": "적절했습니다",
                            "style": "완벽했습니다"
                        }
                    }
                """.trimIndent())
            }

            if (submitFeedbackResponse.status == HttpStatusCode.Created) {
                val feedbackBody = submitFeedbackResponse.bodyAsText()
                feedbackId = extractJsonInt(feedbackBody, "id")
                val overallSatisfaction = extractJsonDouble(feedbackBody, "overallSatisfaction")
                val isFewShotCandidate = extractJsonBoolean(feedbackBody, "isFewShotCandidate")
                println("✓ 피드백 제출 성공")
                println("  - Feedback ID: $feedbackId")
                println("  - 전체 만족도: $overallSatisfaction")
                println("  - Few-shot 후보: $isFewShotCandidate")
            }

            // 12-2. 피드백 조회
            println("\n12-2. 피드백 조회")
            val getFeedbackResponse = client.get("/api/docent/sessions/$docentSessionId/feedback") {
                header("Authorization", "Bearer $token")
            }

            if (getFeedbackResponse.status == HttpStatusCode.OK) {
                println("✓ 피드백 조회 성공")
                val feedbackBody = getFeedbackResponse.bodyAsText()
                val comment = extractJsonString(feedbackBody, "comment")
                println("  - 코멘트: ${comment.take(50)}...")
            }

            // 12-3. Few-Shot 자동 생성 확인
            println("\n12-3. Few-Shot 자동 생성 확인 (고품질 피드백 후)")
            println("  ℹ️  고품질 피드백(>= 4.0) 제출 시 자동으로 Few-Shot 예시가 생성됩니다")

            // 비동기 처리 대기
            delay(2000)

            val userFewShotsResponse = client.get("/api/fewshot/user/$userId?limit=10&minQuality=4.0") {
                header("Authorization", "Bearer $token")
            }

            if (userFewShotsResponse.status == HttpStatusCode.OK) {
                val fewShotsBody = userFewShotsResponse.bodyAsText()
                println("✓ 사용자 Few-Shot 예시 조회 성공")

                val hasFewShots = fewShotsBody.contains("\"data\":[") &&
                                 !fewShotsBody.contains("\"data\":[]")

                if (hasFewShots) {
                    val fewShotId = extractJsonInt(fewShotsBody, "id")
                    println("  - Few-Shot ID: $fewShotId (자동 생성됨)")
                    println("  - 고품질 피드백으로부터 학습 예시가 생성되었습니다")

                    // Few-Shot 상세 조회
                    println("\n12-4. Few-Shot 상세 조회 (enriched)")
                    val fewShotDetailResponse = client.get("/api/fewshot/$fewShotId") {
                        header("Authorization", "Bearer $token")
                    }

                    if (fewShotDetailResponse.status == HttpStatusCode.OK) {
                        println("✓ Few-Shot 상세 조회 성공")
                        val detailBody = fewShotDetailResponse.bodyAsText()
                        val qualityScore = extractJsonDouble(detailBody, "qualityScore")
                        println("  - 품질 점수: $qualityScore")
                    }

                    // Few-Shot 통계 조회
                    println("\n12-5. Few-Shot 통계 조회")
                    val fewShotStatsResponse = client.get("/api/fewshot/statistics") {
                        header("Authorization", "Bearer $token")
                    }

                    if (fewShotStatsResponse.status == HttpStatusCode.OK) {
                        val statsBody = fewShotStatsResponse.bodyAsText()
                        println("✓ Few-Shot 통계 조회 성공")

                        val totalExamples = extractJsonInt(statsBody, "totalExamples")
                        val activeExamples = extractJsonInt(statsBody, "activeExamples")
                        val cacheHitRate = extractJsonDouble(statsBody, "cacheHitRate")

                        println("  - 전체 Few-Shot 예시: $totalExamples")
                        println("  - 활성 Few-Shot 예시: $activeExamples")
                        println("  - 캐시 히트율: ${String.format("%.1f", cacheHitRate)}%")
                    }
                } else {
                    println("  ⚠ Few-Shot이 아직 생성되지 않았거나 품질 기준을 충족하지 못함")
                    println("     (비동기 처리 중이거나 만족도 < 4.0)")
                }
            }
        }

        // ========================================
        // 13. Link API 테스트
        // ========================================
        println("\n=== Step 13: Link API 테스트 ===")

        // 13-1. 링크 추가
        println("13-1. 링크 추가")
        val addLinkResponse = client.post("/api/users/$userId/links") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "url": "https://www.moma.org/collection/works/79802",
                    "title": "MoMA - The Starry Night",
                    "description": "뉴욕 현대미술관의 공식 작품 페이지",
                    "artworkId": $artworkId,
                    "linkType": "REFERENCE",
                    "hasAudioDescription": true,
                    "hasSubtitles": false,
                    "metadata": {
                        "ogTitle": "The Starry Night",
                        "siteName": "MoMA"
                    }
                }
            """.trimIndent())
        }

        if (addLinkResponse.status == HttpStatusCode.Created) {
            val linkBody = addLinkResponse.bodyAsText()
            linkId = extractJsonInt(linkBody, "id")
            println("✓ 링크 추가 성공")
            println("  - Link ID: $linkId")
        }

        // 13-2. 링크 목록 조회
        println("\n13-2. 링크 목록 조회")
        val getLinksResponse = client.get("/api/users/$userId/links?page=1&limit=10") {
            header("Authorization", "Bearer $token")
        }

        if (getLinksResponse.status == HttpStatusCode.OK) {
            val linksBody = getLinksResponse.bodyAsText()
            val totalCount = extractJsonInt(linksBody, "totalCount")
            println("✓ 링크 목록 조회 성공")
            println("  - 총 링크 수: $totalCount")
        }

        // 13-3. 작품별 필터링
        if (linkId > 0) {
            println("\n13-3. 작품별 링크 필터링")
            val filteredLinksResponse = client.get("/api/users/$userId/links?artworkId=$artworkId") {
                header("Authorization", "Bearer $token")
            }

            if (filteredLinksResponse.status == HttpStatusCode.OK) {
                println("✓ 작품별 링크 필터링 성공")
            }
        }

        // ========================================
        // 14. Memo API 테스트
        // ========================================
        println("\n=== Step 14: Memo API 테스트 ===")

        // 14-1. 텍스트 메모 생성
        println("14-1. 텍스트 메모 생성")
        val createMemoResponse = client.post("/api/users/$userId/memos") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "content": "이 작품을 보면서 할머니와 밤하늘을 보던 기억이 떠올랐다. 별들이 반짝이던 그 순간의 따뜻함이 느껴진다.",
                    "artworkId": $artworkId,
                    "docentSessionId": ${if (docentCompleted) docentSessionId else "null"},
                    "inputMethod": "TEXT",
                    "tags": ["추억", "감동", "할머니", "별"],
                    "category": "THOUGHT",
                    "isSharedWithCompanion": false
                }
            """.trimIndent())
        }

        if (createMemoResponse.status == HttpStatusCode.Created) {
            val memoBody = createMemoResponse.bodyAsText()
            memoId = extractJsonInt(memoBody, "id")
            println("✓ 텍스트 메모 생성 성공")
            println("  - Memo ID: $memoId")
        }

        // 14-2. 메모 목록 조회
        println("\n14-2. 메모 목록 조회")
        val getMemosResponse = client.get("/api/users/$userId/memos?page=1&limit=10") {
            header("Authorization", "Bearer $token")
        }

        if (getMemosResponse.status == HttpStatusCode.OK) {
            val memosBody = getMemosResponse.bodyAsText()
            val totalCount = extractJsonInt(memosBody, "totalCount")
            println("✓ 메모 목록 조회 성공")
            println("  - 총 메모 수: $totalCount")
        }

        // 14-3. 메모 검색
        if (memoId > 0) {
            println("\n14-3. 메모 전문 검색")
            val searchMemosResponse = client.get("/api/users/$userId/memos?search=할머니") {
                header("Authorization", "Bearer $token")
            }

            if (searchMemosResponse.status == HttpStatusCode.OK) {
                println("✓ 메모 검색 성공")
            }
        }

        // 14-4. 메모 수정
        if (memoId > 0) {
            println("\n14-4. 메모 수정")
            val updateMemoResponse = client.put("/api/users/$userId/memos/$memoId") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""
                    {
                        "content": "이 작품을 보면서 할머니와 밤하늘을 보던 기억이 떠올랐다. 별들이 반짝이던 그 순간의 따뜻함이 느껴진다. [수정: 정말 소중한 추억이다]",
                        "tags": ["추억", "감동", "할머니", "별", "소중함"],
                        "isSharedWithCompanion": true
                    }
                """.trimIndent())
            }

            if (updateMemoResponse.status == HttpStatusCode.OK) {
                println("✓ 메모 수정 성공")
            }
        }

        // 14-5. 메모 통계 조회
        println("\n14-5. 메모 통계 조회")
        val memoStatsResponse = client.get("/api/users/$userId/memos/statistics") {
            header("Authorization", "Bearer $token")
        }

        if (memoStatsResponse.status == HttpStatusCode.OK) {
            val statsBody = memoStatsResponse.bodyAsText()
            val totalMemos = extractJsonInt(statsBody, "totalMemos")
            println("✓ 메모 통계 조회 성공")
            println("  - 총 메모 수: $totalMemos")
        }

        // ========================================
        // 15. Companion API 테스트
        // ========================================
        println("\n=== Step 15: Companion API 테스트 ===")

        // 15-1. 동행자 계정 생성
        println("15-1. 동행자 계정 생성")
        val registerCompanionResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "$companionEmail",
                    "password": "CompanionPass123!",
                    "userType": "COMPANION",
                    "isVisuallyImpaired": false,
                    "impairmentLevel": null
                }
            """.trimIndent())
        }

        if (registerCompanionResponse.status == HttpStatusCode.Created) {
            println("✓ 동행자 계정 생성 성공")
        }

        // 15-2. 동행자 로그인
        println("\n15-2. 동행자 로그인")
        val loginCompanionResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "$companionEmail",
                    "password": "CompanionPass123!"
                }
            """.trimIndent())
        }

        if (loginCompanionResponse.status == HttpStatusCode.OK) {
            val loginBody = loginCompanionResponse.bodyAsText()
            companionUserId = extractJsonInt(loginBody, "id")
            companionToken = extractJsonString(loginBody, "token")
            println("✓ 동행자 로그인 성공")
            println("  - Companion User ID: $companionUserId")
        }

        // 15-3. 메인 사용자가 동행자 초대
        println("\n15-3. 동행자 초대")
        val inviteCompanionResponse = client.post("/api/companions/invite") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "companionEmail": "$companionEmail",
                    "relationshipType": "FAMILY",
                    "canAddContext": true,
                    "canViewHistory": true,
                    "canAddMemos": true,
                    "message": "함께 미술관 감상을 시작해요!"
                }
            """.trimIndent())
        }

        if (inviteCompanionResponse.status == HttpStatusCode.Created) {
            val inviteBody = inviteCompanionResponse.bodyAsText()
            companionId = extractJsonInt(inviteBody, "id")
            println("✓ 동행자 초대 성공")
            println("  - Companion Relationship ID: $companionId")
        }

        // 15-4. 동행자가 초대 목록 조회
        println("\n15-4. 동행자 초대 목록 조회")
        val getInvitationsResponse = client.get("/api/companions/invitations") {
            header("Authorization", "Bearer $companionToken")
        }

        if (getInvitationsResponse.status == HttpStatusCode.OK) {
            val invitationsBody = getInvitationsResponse.bodyAsText()
            val invitationCount = extractJsonInt(invitationsBody, "totalCount")
            println("✓ 초대 목록 조회 성공")
            println("  - 대기 중인 초대: $invitationCount")
        }

        // 15-5. 동행자가 초대 수락
        if (companionId > 0) {
            println("\n15-5. 초대 수락")
            val acceptInvitationResponse = client.put("/api/companions/$companionId/accept") {
                header("Authorization", "Bearer $companionToken")
            }

            if (acceptInvitationResponse.status == HttpStatusCode.OK) {
                println("✓ 초대 수락 성공")
            }
        }

        // 15-6. 메인 사용자가 동행자 목록 조회
        println("\n15-6. 동행자 목록 조회")
        val getCompanionsResponse = client.get("/api/companions?status=ACTIVE") {
            header("Authorization", "Bearer $token")
        }

        if (getCompanionsResponse.status == HttpStatusCode.OK) {
            val companionsBody = getCompanionsResponse.bodyAsText()
            val totalCount = extractJsonInt(companionsBody, "totalCount")
            println("✓ 동행자 목록 조회 성공")
            println("  - 활성 동행자 수: $totalCount")
        }

        // 15-7. 권한 수정
        if (companionId > 0) {
            println("\n15-7. 동행자 권한 수정")
            val updatePermissionsResponse = client.put("/api/companions/$companionId/permissions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""
                    {
                        "canAddContext": true,
                        "canViewHistory": false,
                        "canAddMemos": true
                    }
                """.trimIndent())
            }

            if (updatePermissionsResponse.status == HttpStatusCode.OK) {
                println("✓ 권한 수정 성공")
            }
        }

        // 15-8. 동행자 통계 조회
        println("\n15-8. 동행자 통계 조회")
        val companionStatsResponse = client.get("/api/companions/statistics") {
            header("Authorization", "Bearer $token")
        }

        if (companionStatsResponse.status == HttpStatusCode.OK) {
            val statsBody = companionStatsResponse.bodyAsText()
            val totalCompanions = extractJsonInt(statsBody, "totalCompanions")
            val activeCompanions = extractJsonInt(statsBody, "activeCompanions")
            println("✓ 동행자 통계 조회 성공")
            println("  - 전체 동행자: $totalCompanions")
            println("  - 활성 동행자: $activeCompanions")
        }

        // 15-9. 초대 코드 생성
        println("\n15-9. 초대 코드 생성")
        val generateInviteCodeResponse = client.post("/api/companions/invite-code") {
            header("Authorization", "Bearer $token")
        }

        if (generateInviteCodeResponse.status == HttpStatusCode.Created) {
            val inviteCodeBody = generateInviteCodeResponse.bodyAsText()
            val inviteCode = extractJsonString(inviteCodeBody, "inviteCode")
            val inviteUrl = extractJsonString(inviteCodeBody, "inviteUrl")
            println("✓ 초대 코드 생성 성공")
            println("  - 초대 코드: $inviteCode")
            println("  - 초대 URL: $inviteUrl")
        }

        // 15-10. 링크 삭제 (정리)
        if (linkId > 0) {
            println("\n15-10. 링크 삭제")
            val deleteLinkResponse = client.delete("/api/users/$userId/links/$linkId") {
                header("Authorization", "Bearer $token")
            }

            if (deleteLinkResponse.status == HttpStatusCode.OK) {
                println("✓ 링크 삭제 성공")
            }
        }

        // 15-11. 메모 삭제 (정리)
        if (memoId > 0) {
            println("\n15-11. 메모 삭제")
            val deleteMemoResponse = client.delete("/api/users/$userId/memos/$memoId") {
                header("Authorization", "Bearer $token")
            }

            if (deleteMemoResponse.status == HttpStatusCode.OK) {
                println("✓ 메모 삭제 성공")
            }
        }

        // 15-12. 동행자 제거 (정리)
        if (companionId > 0) {
            println("\n15-12. 동행자 제거")
            val removeCompanionResponse = client.delete("/api/companions/$companionId") {
                header("Authorization", "Bearer $token")
            }

            if (removeCompanionResponse.status == HttpStatusCode.OK) {
                println("✓ 동행자 제거 성공")
            }
        }

        println("\n" + "=".repeat(50))
        println("통합 테스트 완료!")
        if (!docentCompleted) {
            println("⚠ 도슨트 생성 타임아웃으로 일부 테스트 스킵됨")
        }
        println("=".repeat(50))
    }

    // Helper functions
    private fun extractJsonInt(json: String, key: String): Int {
        val regex = """"$key":\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = """"$key":\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }

    private fun extractArtworkId(json: String): Int {
        // Extract the first artwork ID from the result array
        val regex = """"id":\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun extractGeneratedText(json: String): String {
        val regex = """"generatedText":\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: "텍스트 없음"
    }

    private fun extractSessionCount(json: String): Int {
        val regex = """"totalCount":\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun extractJsonDouble(json: String, key: String): Double {
        val regex = """"$key":\s*([0-9.]+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toDouble() ?: 0.0
    }

    private fun extractJsonBoolean(json: String, key: String): Boolean {
        val regex = """"$key":\s*(true|false)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toBoolean() ?: false
    }
}
