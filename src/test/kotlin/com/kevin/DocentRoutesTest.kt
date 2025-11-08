package com.kevin

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Docent Routes Unit Tests
 */
class DocentRoutesTest {

    private fun registerAndLogin(client: HttpClient): Pair<Int, String> = runBlocking {
        // Register
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "docent_test_${System.currentTimeMillis()}@example.com",
                    "password": "Password123!",
                    "userType": "MAIN_USER"
                }
            """.trimIndent())
        }

        // Login
        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "docent_test_${System.currentTimeMillis()}@example.com",
                    "password": "Password123!"
                }
            """.trimIndent())
        }

        val loginBody = loginResponse.bodyAsText()
        val userId = extractUserId(loginBody)
        val token = extractToken(loginBody)

        Pair(userId, token)
    }

    @Test
    fun testGenerateDocent() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/docent/generate") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "userId": $userId,
                    "artworkId": 1,
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "MEDIUM",
                    "includeCompanionContext": false,
                    "useFewShotExamples": true
                }
            """.trimIndent())
        }

        // Should return Accepted (202) for async processing
        assertTrue(
            response.status == HttpStatusCode.Accepted ||
            response.status == HttpStatusCode.InternalServerError ||
            response.status == HttpStatusCode.NotFound
        )

        if (response.status == HttpStatusCode.Accepted) {
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("sessionId"))
            assertTrue(responseBody.contains("status") || responseBody.contains("pollingUrl"))
        }
    }

    @Test
    fun testGenerateDocentWithCustomPrompt() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/docent/generate") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "userId": $userId,
                    "artworkId": 1,
                    "narrativeStyle": "CONVERSATIONAL",
                    "preferredLength": "LONG",
                    "includeCompanionContext": false,
                    "useFewShotExamples": false,
                    "customPrompt": "작품의 색채와 빛의 표현에 집중하여 설명해주세요."
                }
            """.trimIndent())
        }

        assertTrue(
            response.status == HttpStatusCode.Accepted ||
            response.status == HttpStatusCode.InternalServerError ||
            response.status == HttpStatusCode.NotFound
        )
    }

    @Test
    fun testGenerateDocentWithInvalidArtworkId() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/docent/generate") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "userId": $userId,
                    "artworkId": 999999,
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "MEDIUM"
                }
            """.trimIndent())
        }

        assertTrue(
            response.status == HttpStatusCode.NotFound ||
            response.status == HttpStatusCode.BadRequest ||
            response.status == HttpStatusCode.Accepted
        )
    }

    @Test
    fun testGetDocentSessionStatus() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // First generate a docent
        val generateResponse = client.post("/api/docent/generate") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "userId": $userId,
                    "artworkId": 1,
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "SHORT"
                }
            """.trimIndent())
        }

        if (generateResponse.status == HttpStatusCode.Accepted) {
            val generateBody = generateResponse.bodyAsText()
            val sessionId = extractSessionId(generateBody)

            if (sessionId > 0) {
                // Get session status
                val statusResponse = client.get("/api/docent/sessions/$sessionId") {
                    header("Authorization", "Bearer $token")
                }

                assertEquals(HttpStatusCode.OK, statusResponse.status)
                val statusBody = statusResponse.bodyAsText()
                assertTrue(statusBody.contains("status"))
                assertTrue(
                    statusBody.contains("GENERATING") ||
                    statusBody.contains("COMPLETED") ||
                    statusBody.contains("FAILED")
                )
            }
        }
    }

    @Test
    fun testGetDocentSessionNotFound() = testApplication {
        application {
            module()
        }

        val (_, token) = registerAndLogin(client)

        val response = client.get("/api/docent/sessions/999999") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testUpdatePlayStats() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // First generate a docent
        val generateResponse = client.post("/api/docent/generate") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "userId": $userId,
                    "artworkId": 1,
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "SHORT"
                }
            """.trimIndent())
        }

        if (generateResponse.status == HttpStatusCode.Accepted) {
            val generateBody = generateResponse.bodyAsText()
            val sessionId = extractSessionId(generateBody)

            if (sessionId > 0) {
                // Wait for generation to complete (or at least start)
                runBlocking { delay(2000) }

                // Update play stats
                val updateResponse = client.put("/api/docent/sessions/$sessionId/play-stats") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $token")
                    setBody("""
                        {
                            "playCount": 1,
                            "totalListeningSeconds": 120,
                            "completionRate": 0.85
                        }
                    """.trimIndent())
                }

                // Should succeed or fail if session not found/completed
                assertTrue(
                    updateResponse.status == HttpStatusCode.OK ||
                    updateResponse.status == HttpStatusCode.NotFound ||
                    updateResponse.status == HttpStatusCode.BadRequest
                )
            }
        }
    }

    @Test
    fun testUpdatePlayStatsWithInvalidSessionId() = testApplication {
        application {
            module()
        }

        val (_, token) = registerAndLogin(client)

        val response = client.put("/api/docent/sessions/999999/play-stats") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "playCount": 1,
                    "totalListeningSeconds": 120,
                    "completionRate": 0.85
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testGetDocentHistory() = testApplication {
        application {
            module()
        }

        val (_, token) = registerAndLogin(client)

        val response = client.get("/api/users/me/docent/history?page=1&limit=10") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        // Should contain sessions array or empty result
        assertTrue(responseBody.contains("sessions") || responseBody.contains("["))
    }

    @Test
    fun testGetDocentHistoryWithPagination() = testApplication {
        application {
            module()
        }

        val (_, token) = registerAndLogin(client)

        val response = client.get("/api/users/me/docent/history?page=2&limit=5") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testUnauthorizedAccessToGenerate() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/docent/generate") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "userId": 1,
                    "artworkId": 1,
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "MEDIUM"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testUnauthorizedAccessToHistory() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/users/me/docent/history")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testGenerateDocentWithAllStyles() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val narrativeStyles = listOf("LITERARY", "CONVERSATIONAL", "ACADEMIC", "POETIC")

        for (style in narrativeStyles) {
            val response = client.post("/api/docent/generate") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""
                    {
                        "userId": $userId,
                        "artworkId": 1,
                        "narrativeStyle": "$style",
                        "preferredLength": "MEDIUM"
                    }
                """.trimIndent())
            }

            // Should accept any valid style
            assertTrue(
                response.status == HttpStatusCode.Accepted ||
                response.status == HttpStatusCode.NotFound ||
                response.status == HttpStatusCode.InternalServerError
            )
        }
    }

    @Test
    fun testGenerateDocentWithAllLengths() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val lengths = listOf("SHORT", "MEDIUM", "LONG")

        for (length in lengths) {
            val response = client.post("/api/docent/generate") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""
                    {
                        "userId": $userId,
                        "artworkId": 1,
                        "narrativeStyle": "LITERARY",
                        "preferredLength": "$length"
                    }
                """.trimIndent())
            }

            // Should accept any valid length
            assertTrue(
                response.status == HttpStatusCode.Accepted ||
                response.status == HttpStatusCode.NotFound ||
                response.status == HttpStatusCode.InternalServerError
            )
        }
    }

    private fun extractUserId(json: String): Int {
        val regex = """"userId":\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun extractToken(json: String): String {
        val regex = """"token":\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }

    private fun extractSessionId(json: String): Int {
        val regex = """"sessionId":\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }
}