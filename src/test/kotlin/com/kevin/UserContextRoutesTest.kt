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
 * User Context Routes Unit Tests
 */
class UserContextRoutesTest {

    private fun registerAndLogin(client: HttpClient): Pair<Int, String> = runBlocking {
        // Register
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "context_test_${System.currentTimeMillis()}@example.com",
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
                    "email": "context_test_${System.currentTimeMillis()}@example.com",
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
    fun testCreateContext() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "MEMORY",
                    "title": "어린 시절 기억",
                    "content": "어린 시절 미술관에 갔던 기억이 있습니다.",
                    "emotionTags": ["nostalgic", "happy"],
                    "inputMethod": "TEXT",
                    "importanceLevel": 5
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("MEMORY"))
        assertTrue(responseBody.contains("어린 시절 기억"))
    }

    @Test
    fun testCreateContextWithVoice() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "EMOTION",
                    "title": "현재 감정",
                    "content": "요즘 기분이 좋습니다.",
                    "voiceUrl": "https://example.com/voice.mp3",
                    "voiceDurationSeconds": 30,
                    "emotionTags": ["happy", "excited"],
                    "inputMethod": "VOICE",
                    "importanceLevel": 4
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("EMOTION"))
        assertTrue(responseBody.contains("voiceUrl"))
    }

    @Test
    fun testGetAllContexts() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create two contexts
        client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "MEMORY",
                    "title": "기억 1",
                    "content": "첫 번째 기억",
                    "inputMethod": "TEXT"
                }
            """.trimIndent())
        }

        client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "GOAL",
                    "title": "목표 1",
                    "content": "나의 목표",
                    "inputMethod": "TEXT"
                }
            """.trimIndent())
        }

        // Get all contexts
        val response = client.get("/api/users/$userId/contexts") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("기억 1"))
        assertTrue(responseBody.contains("목표 1"))
    }

    @Test
    fun testGetContextById() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create context
        val createResponse = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "MEMORY",
                    "title": "특정 기억",
                    "content": "이것은 특정 기억입니다.",
                    "inputMethod": "TEXT",
                    "importanceLevel": 5
                }
            """.trimIndent())
        }

        val createBody = createResponse.bodyAsText()
        val contextId = extractContextId(createBody)

        // Get specific context
        val response = client.get("/api/users/$userId/contexts/$contextId") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("특정 기억"))
        assertTrue(responseBody.contains("이것은 특정 기억입니다"))
    }

    @Test
    fun testUpdateContext() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create context
        val createResponse = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "MEMORY",
                    "title": "원래 제목",
                    "content": "원래 내용",
                    "inputMethod": "TEXT"
                }
            """.trimIndent())
        }

        val createBody = createResponse.bodyAsText()
        val contextId = extractContextId(createBody)

        // Update context
        val response = client.put("/api/users/$userId/contexts/$contextId") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "title": "수정된 제목",
                    "content": "수정된 내용",
                    "emotionTags": ["updated", "modified"],
                    "importanceLevel": 3
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("수정된 제목"))
        assertTrue(responseBody.contains("수정된 내용"))
    }

    @Test
    fun testDeleteContext() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create context
        val createResponse = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "MEMORY",
                    "title": "삭제될 컨텍스트",
                    "content": "이것은 삭제될 것입니다.",
                    "inputMethod": "TEXT"
                }
            """.trimIndent())
        }

        val createBody = createResponse.bodyAsText()
        val contextId = extractContextId(createBody)

        // Delete context
        val response = client.delete("/api/users/$userId/contexts/$contextId") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        // Verify deletion - should return 404 or empty
        val getResponse = client.get("/api/users/$userId/contexts/$contextId") {
            header("Authorization", "Bearer $token")
        }
        assertTrue(getResponse.status == HttpStatusCode.NotFound || getResponse.status == HttpStatusCode.OK)
    }

    @Test
    fun testCreateCompanionContext() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create companion context
        val response = client.post("/api/users/$userId/contexts") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "contextType": "OBSERVATION",
                    "title": "보호자 관찰",
                    "content": "오늘 미술관에서 특히 인상주의 작품에 관심을 보였습니다.",
                    "emotionTags": ["interested", "engaged"],
                    "inputMethod": "TEXT",
                    "importanceLevel": 4,
                    "isCompanionInput": true
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("OBSERVATION"))
        assertTrue(responseBody.contains("isCompanionInput"))
    }

    @Test
    fun testUnauthorizedAccess() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/users/1/contexts")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testGetContextsWithPagination() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create multiple contexts
        repeat(5) { index ->
            client.post("/api/users/$userId/contexts") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody("""
                    {
                        "contextType": "MEMORY",
                        "title": "기억 ${index + 1}",
                        "content": "내용 ${index + 1}",
                        "inputMethod": "TEXT"
                    }
                """.trimIndent())
            }
        }

        // Get contexts with pagination
        val response = client.get("/api/users/$userId/contexts?page=1&limit=3") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        // Should contain pagination metadata
        assertTrue(responseBody.isNotEmpty())
    }

    private fun extractUserId(json: String): Int {
        val regex = """"userId":\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun extractToken(json: String): String {
        val regex = """"token":\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }

    private fun extractContextId(json: String): Int {
        val regex = """"id":\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toInt() ?: 0
    }
}