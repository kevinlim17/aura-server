package com.kevin.routes

import com.kevin.services.FewShotBuilderService
import com.kevin.services.FewShotSelectorService
import com.kevin.services.RedisFewShotCacheService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Few-Shot Management Routes
 * API endpoints for Few-Shot learning system management
 *
 * Endpoints:
 * - GET /api/fewshot/user/{userId} - Get user's few-shot examples (authenticated)
 * - GET /api/fewshot/{fewShotId} - Get few-shot detail (authenticated)
 * - GET /api/fewshot/statistics - Get few-shot statistics (authenticated)
 * - DELETE /api/fewshot/{fewShotId} - Deactivate few-shot (authenticated)
 * - POST /api/fewshot/cache/invalidate - Invalidate cache (authenticated)
 * - POST /api/fewshot/admin/recalculate-quality - Recalculate quality (admin)
 * - DELETE /api/fewshot/admin/cleanup - Clean up low-quality few-shots (admin)
 */
fun Route.fewShotRoutes() {
    val fewShotBuilderService = FewShotBuilderService()
    val fewShotSelectorService = FewShotSelectorService()
    val redisCacheService = RedisFewShotCacheService()

    // Authenticated routes
    authenticate("auth-jwt") {

        // ============================================================================
        // GET /api/fewshot/user/{userId}
        // Get user's few-shot examples
        // ============================================================================
        get("/api/fewshot/user/{userId}") {
            try {
                // Get authenticated user ID
                val principal = call.principal<JWTPrincipal>()
                val authenticatedUserId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                val userId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid user ID")
                    )

                // Verify user can only access their own few-shots
                if (authenticatedUserId != userId) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Access denied")
                    )
                }

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val minQuality = call.request.queryParameters["minQuality"]?.toDoubleOrNull() ?: 4.0

                val userFewShots = fewShotBuilderService.getUserFewShots(
                    userId = userId,
                    limit = limit,
                    minQualityScore = minQuality
                )

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "data" to userFewShots,
                        "message" to "Few-shot examples retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve few-shot examples: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/fewshot/{fewShotId}
        // Get detailed few-shot example with all resources
        // ============================================================================
        get("/api/fewshot/{fewShotId}") {
            try {
                val fewShotId = call.parameters["fewShotId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid few-shot ID")
                    )

                val enriched = fewShotBuilderService.enrichFewShotWithResources(fewShotId)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "data" to enriched,
                        "message" to "Few-shot detail retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve few-shot detail: ${e.message}")
                )
            }
        }

        // ============================================================================
        // GET /api/fewshot/statistics
        // Get user's few-shot statistics and cache stats
        // ============================================================================
        get("/api/fewshot/statistics") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                val userStats = fewShotBuilderService.getUserFewShotStatistics(userId)
                val cacheStats = redisCacheService.getFewShotStatistics()

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "data" to mapOf(
                            "userStats" to userStats,
                            "cacheStats" to cacheStats
                        ),
                        "message" to "Statistics retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to retrieve statistics: ${e.message}")
                )
            }
        }

        // ============================================================================
        // DELETE /api/fewshot/{fewShotId}
        // Deactivate a few-shot example
        // ============================================================================
        delete("/api/fewshot/{fewShotId}") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                val fewShotId = call.parameters["fewShotId"]?.toIntOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid few-shot ID")
                    )

                fewShotBuilderService.deactivateFewShot(fewShotId, userId)

                // Invalidate cache
                redisCacheService.invalidateFewShotCache(userId)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "Few-shot deactivated successfully"
                    )
                )
            } catch (e: Exception) {
                val statusCode = when {
                    e.message?.contains("Unauthorized") == true -> HttpStatusCode.Forbidden
                    e.message?.contains("not found") == true -> HttpStatusCode.NotFound
                    else -> HttpStatusCode.InternalServerError
                }

                call.respond(
                    statusCode,
                    mapOf("error" to (e.message ?: "Failed to deactivate few-shot"))
                )
            }
        }

        // ============================================================================
        // POST /api/fewshot/cache/invalidate
        // Manually invalidate user's few-shot cache
        // ============================================================================
        post("/api/fewshot/cache/invalidate") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                val deletedCount = redisCacheService.invalidateFewShotCache(userId)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "data" to mapOf("deletedCacheKeys" to deletedCount),
                        "message" to "Cache invalidated successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to invalidate cache: ${e.message}")
                )
            }
        }

        // ============================================================================
        // POST /api/fewshot/admin/recalculate-quality
        // Recalculate quality scores for all few-shot examples (admin only)
        // Background task
        // ============================================================================
        post("/api/fewshot/admin/recalculate-quality") {
            try {
                // TODO: Add admin role check
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                // Launch background task
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        fewShotBuilderService.recalculateAllFewShotQuality()
                    } catch (e: Exception) {
                        println("⚠ Quality recalculation failed: ${e.message}")
                    }
                }

                call.respond(
                    HttpStatusCode.Accepted,
                    mapOf(
                        "success" to true,
                        "message" to "Quality recalculation started in background"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to start quality recalculation: ${e.message}")
                )
            }
        }

        // ============================================================================
        // DELETE /api/fewshot/admin/cleanup
        // Clean up low-quality few-shot examples (admin only)
        // ============================================================================
        delete("/api/fewshot/admin/cleanup") {
            try {
                // TODO: Add admin role check
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                    ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "User not authenticated")
                    )

                val minQuality = call.request.queryParameters["minQuality"]?.toDoubleOrNull() ?: 3.0
                val deletedCount = fewShotBuilderService.cleanupLowQualityFewShots(minQuality)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "data" to mapOf(
                            "deletedCount" to deletedCount,
                            "minQuality" to minQuality
                        ),
                        "message" to "Low-quality few-shots cleaned up successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to clean up few-shots: ${e.message}")
                )
            }
        }
    }
}
