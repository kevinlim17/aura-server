package com.kevin

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Authentication Routes Unit Tests
 */
class AuthRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testRegisterSuccess() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "test@example.com",
                    "password": "Password123!",
                    "userType": "MAIN_USER",
                    "isVisuallyImpaired": true,
                    "impairmentLevel": "MODERATE"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("success"))
    }

    @Test
    fun testRegisterDuplicateEmail() = testApplication {
        application {
            module()
        }

        // First registration
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "duplicate@example.com",
                    "password": "Password123!",
                    "userType": "MAIN_USER"
                }
            """.trimIndent())
        }

        // Second registration with same email
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "duplicate@example.com",
                    "password": "Password123!",
                    "userType": "MAIN_USER"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testLoginSuccess() = testApplication {
        application {
            module()
        }

        // Register user first
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "login@example.com",
                    "password": "Password123!",
                    "userType": "MAIN_USER"
                }
            """.trimIndent())
        }

        // Login
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "login@example.com",
                    "password": "Password123!"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("token"))
        assertTrue(responseBody.contains("userId"))
    }

    @Test
    fun testLoginInvalidCredentials() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "nonexistent@example.com",
                    "password": "WrongPassword123!"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testRegisterInvalidEmail() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "invalid-email",
                    "password": "Password123!",
                    "userType": "MAIN_USER"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testRegisterWeakPassword() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "email": "weak@example.com",
                    "password": "123",
                    "userType": "MAIN_USER"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
