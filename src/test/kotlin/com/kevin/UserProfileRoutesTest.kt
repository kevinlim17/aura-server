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
 * User Profile Routes Unit Tests
 */
class UserProfileRoutesTest {

    private fun registerAndLogin(client: HttpClient): Pair<Int, String> = runBlocking {
        // Register
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "profile_test_${System.currentTimeMillis()}@example.com",
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
                    "email": "profile_test_${System.currentTimeMillis()}@example.com",
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
    fun testCreateProfile() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        val response = client.post("/api/users/$userId/profile") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "interests": ["painting", "sculpture", "modern art"],
                    "hobbies": "사진 촬영, 여행",
                    "favoriteArtists": ["피카소", "모네", "고흐"],
                    "bio": "예술을 사랑하는 사람입니다."
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("interests"))
    }

    @Test
    fun testGetProfile() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create profile first
        client.post("/api/users/$userId/profile") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "interests": ["painting"],
                    "hobbies": "독서"
                }
            """.trimIndent())
        }

        // Get profile
        val response = client.get("/api/users/$userId/profile") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("painting"))
    }

    @Test
    fun testUpdateProfile() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create profile
        client.post("/api/users/$userId/profile") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "interests": ["painting"],
                    "hobbies": "독서"
                }
            """.trimIndent())
        }

        // Update profile
        val response = client.put("/api/users/$userId/profile") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "interests": ["painting", "sculpture"],
                    "hobbies": "독서, 여행"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("sculpture"))
    }

    @Test
    fun testDeleteProfile() = testApplication {
        application {
            module()
        }

        val (userId, token) = registerAndLogin(client)

        // Create profile
        client.post("/api/users/$userId/profile") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""
                {
                    "interests": ["painting"]
                }
            """.trimIndent())
        }

        // Delete profile
        val response = client.delete("/api/users/$userId/profile") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testUnauthorizedAccess() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/users/1/profile")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
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
