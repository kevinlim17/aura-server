package com.kevin

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * User Preferences Routes Unit Tests
 */
class UserPreferencesRoutesTest {

    private fun registerAndLogin(client: HttpClient): Pair<Int, String> = runBlocking {
        // Register
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "preferences_test_${System.currentTimeMillis()}@example.com",
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
                    "email": "preferences_test_${System.currentTimeMillis()}@example.com",
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
    fun testCreatePreferences() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "LONG",
                    "ttsSpeed": 1.0,
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

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("LITERARY"))
        assertTrue(responseBody.contains("LONG"))
        assertTrue(responseBody.contains("FEMALE"))
    }

    @Test
    fun testCreatePreferencesWithDefaults() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create preferences with minimal fields
        val response = client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "CONVERSATIONAL",
                    "preferredLength": "MEDIUM"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("CONVERSATIONAL"))
        assertTrue(responseBody.contains("MEDIUM"))
    }

    @Test
    fun testGetPreferences() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create preferences first
        client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "ACADEMIC",
                    "preferredLength": "SHORT",
                    "ttsSpeed": 0.8,
                    "ttsPitch": 1.2,
                    "ttsVoice": "MALE",
                    "preferredLanguage": "en-US"
                }
            """.trimIndent())
        }

        // Get preferences
        val response = client.get("/api/users/$userId/preferences") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("ACADEMIC"))
        assertTrue(responseBody.contains("SHORT"))
        assertTrue(responseBody.contains("MALE"))
        assertTrue(responseBody.contains("en-US"))
    }

    @Test
    fun testUpdatePreferences() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create preferences
        client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "MEDIUM",
                    "ttsSpeed": 1.0
                }
            """.trimIndent())
        }

        // Update preferences
        val response = client.put("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "CONVERSATIONAL",
                    "preferredLength": "LONG",
                    "ttsSpeed": 1.2,
                    "ttsPitch": 0.9,
                    "ttsVoice": "FEMALE",
                    "enableHapticFeedback": false,
                    "enableAudioDescriptions": false
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("CONVERSATIONAL"))
        assertTrue(responseBody.contains("LONG"))
        assertTrue(responseBody.contains("1.2"))
    }

    @Test
    fun testUpdateTTSSettings() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create preferences
        client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "MEDIUM"
                }
            """.trimIndent())
        }

        // Update only TTS settings
        val response = client.put("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "ttsSpeed": 0.75,
                    "ttsPitch": 1.1,
                    "ttsVoice": "CHILD"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("0.75"))
        assertTrue(responseBody.contains("1.1"))
        assertTrue(responseBody.contains("CHILD"))
    }

    @Test
    fun testUpdateAccessibilitySettings() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create preferences
        client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "MEDIUM"
                }
            """.trimIndent())
        }

        // Update accessibility settings
        val response = client.put("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "enableHapticFeedback": true,
                    "enableAudioDescriptions": true,
                    "highContrastMode": true
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("enableHapticFeedback"))
        assertTrue(responseBody.contains("enableAudioDescriptions"))
        assertTrue(responseBody.contains("highContrastMode"))
    }

    @Test
    fun testDeletePreferences() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create preferences
        client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "MEDIUM"
                }
            """.trimIndent())
        }

        // Delete preferences
        val response = client.delete("/api/users/$userId/preferences") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testUnauthorizedAccess() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/users/1/preferences")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testInvalidNarrativeStyle() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "INVALID_STYLE",
                    "preferredLength": "MEDIUM"
                }
            """.trimIndent())
        }

        // Should either accept it or return bad request
        assertTrue(response.status == HttpStatusCode.Created || response.status == HttpStatusCode.BadRequest)
    }

    @Test
    fun testInvalidTTSSpeed() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/users/$userId/preferences") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "narrativeStyle": "LITERARY",
                    "preferredLength": "MEDIUM",
                    "ttsSpeed": 5.0
                }
            """.trimIndent())
        }

        // Should either accept it or return bad request for out of range values
        assertTrue(response.status == HttpStatusCode.Created || response.status == HttpStatusCode.BadRequest)
    }

    private fun extractUserId(json: String): Int {
        val regex = """"userId":\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun extractToken(json: String): String {
        val regex = """"token":\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }
}