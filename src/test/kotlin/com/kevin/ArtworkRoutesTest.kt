package com.kevin

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Artwork Routes Unit Tests
 */
class ArtworkRoutesTest {

    @Test
    fun testGetArtworksList() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/artworks?page=1&limit=10")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("artworks") || responseBody.contains("results"))
    }

    @Test
    fun testGetArtworksWithPagination() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/artworks?page=2&limit=5")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        // Should contain pagination metadata
        assertTrue(responseBody.isNotEmpty())
    }

    @Test
    fun testGetArtworkById() = testApplication {
        application {
            module()
        }

        // Assuming artwork with ID 1 exists
        val response = client.get("/api/artworks/1")

        // Should return either OK with artwork data or NotFound
        assertTrue(
            response.status == HttpStatusCode.OK ||
            response.status == HttpStatusCode.NotFound
        )

        if (response.status == HttpStatusCode.OK) {
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("title") || responseBody.contains("artist"))
        }
    }

    @Test
    fun testGetArtworkByIdNotFound() = testApplication {
        application {
            module()
        }

        // Very unlikely to exist
        val response = client.get("/api/artworks/999999")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testTextBasedSearch() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "별이 빛나는 밤",
                    "page": 1,
                    "limit": 5
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()

        // Should contain results structure
        assertTrue(responseBody.contains("results") || responseBody.contains("artworks"))

        // Should contain pagination info
        assertTrue(responseBody.contains("pagination"))

        // Response might come from DB or Gemini API (both are valid)
        assertTrue(
            responseBody.contains("from database") ||
            responseBody.contains("from Gemini AI") ||
            responseBody.contains("No artworks found")
        )
    }

    @Test
    fun testTextBasedSearchWithEmptyQuery() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "",
                    "page": 1,
                    "limit": 5
                }
            """.trimIndent())
        }

        // Should return bad request for empty query
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("cannot be empty"))
    }

    @Test
    fun testTextBasedSearchWithFilters() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "모네",
                    "artworkType": "painting",
                    "genre": "impressionism",
                    "page": 1,
                    "limit": 10
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testGetSimilarArtworks() = testApplication {
        application {
            module()
        }

        // Assuming artwork with ID 1 exists
        val response = client.get("/api/artworks/1/similar?limit=5")

        // Should return OK with similar artworks or NotFound if artwork doesn't exist
        assertTrue(
            response.status == HttpStatusCode.OK ||
            response.status == HttpStatusCode.NotFound
        )

        if (response.status == HttpStatusCode.OK) {
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("artworks") || responseBody.contains("results") || responseBody.contains("["))
        }
    }

    @Test
    fun testGetSimilarArtworksWithInvalidId() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/artworks/999999/similar?limit=5")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testSearchByArtist() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "빈센트 반 고흐",
                    "page": 1,
                    "limit": 10
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testSearchByGenre() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "인상주의",
                    "genre": "impressionism",
                    "page": 1,
                    "limit": 10
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testSearchWithYearRange() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "풍경화",
                    "startYear": 1800,
                    "endYear": 1900,
                    "page": 1,
                    "limit": 10
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testSearchWithGeminiFallback() = testApplication {
        application {
            module()
        }

        // Search for an artwork that is very unlikely to be in DB
        // This should trigger Gemini API fallback (if API key is configured)
        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "매우 희귀한 작품 테스트 ${System.currentTimeMillis()}",
                    "page": 1,
                    "limit": 5
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()

        // Should contain pagination
        assertTrue(responseBody.contains("pagination"))

        // Response could be:
        // 1. Empty results (if Gemini also found nothing)
        // 2. Gemini results (if API key is configured)
        // 3. Error message (if Gemini API fails)
        assertTrue(responseBody.contains("results"))
    }

    @Test
    fun testSearchResultStructure() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "test",
                    "page": 1,
                    "limit": 10
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()

        // Verify response structure
        assertTrue(responseBody.contains("\"success\":"))
        assertTrue(responseBody.contains("\"data\":"))
        assertTrue(responseBody.contains("\"message\":"))

        // Data should contain query, results, and pagination
        assertTrue(responseBody.contains("\"query\":"))
        assertTrue(responseBody.contains("\"results\":"))
        assertTrue(responseBody.contains("\"pagination\":"))
    }

    @Test
    fun testSearchPaginationInfo() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/artworks/search") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "query": "art",
                    "page": 1,
                    "limit": 5
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()

        // Pagination should include all required fields
        assertTrue(responseBody.contains("\"page\":"))
        assertTrue(responseBody.contains("\"limit\":"))
        assertTrue(responseBody.contains("\"totalCount\":"))
        assertTrue(responseBody.contains("\"totalPages\":"))
        assertTrue(responseBody.contains("\"hasNext\":"))
        assertTrue(responseBody.contains("\"hasPrevious\":"))
    }
}
