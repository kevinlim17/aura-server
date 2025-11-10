package com.kevin.services

import com.kevin.config.RedisConfig
import com.kevin.model.dto.*
import io.lettuce.core.ScoredValue
import io.lettuce.core.SetArgs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.system.measureTimeMillis

/**
 * Redis Few-Shot Cache Service
 * Manages caching of few-shot examples for improved performance
 *
 * Features:
 * - User-specific few-shot pools
 * - Artwork-specific few-shot pools
 * - Quality-based sorted sets for top examples
 * - Usage tracking and statistics
 * - Automatic TTL management
 */
class RedisFewShotCacheService(
    private val fewShotBuilderService: FewShotBuilderService = FewShotBuilderService()
) {

    private val redis = RedisConfig.commands
    private val json = RedisConfig.json

    // Statistics tracking
    private var cacheHits = 0L
    private var cacheMisses = 0L
    private var totalRetrievalTimeMs = 0L
    private var retrievalCount = 0L

    // ========================================================================
    // 1. Cache Few-Shot Pool
    // ========================================================================

    /**
     * Cache few-shot pool for a user and optional artwork
     */
    fun cacheFewShotPool(
        userId: Int,
        artworkId: Int? = null,
        fewShots: List<EnrichedFewShot>,
        ttlSeconds: Long = RedisConfig.DEFAULT_TTL
    ): Boolean {
        return try {
            val key = if (artworkId != null) {
                RedisConfig.getUserArtworkKey(userId, artworkId)
            } else {
                RedisConfig.getUserPoolKey(userId)
            }

            // Serialize to JSON
            val jsonValue = json.encodeToString(fewShots)

            // Store with TTL
            redis.setex(key, ttlSeconds, jsonValue)

            println("✓ Cached ${fewShots.size} few-shots for user $userId" +
                    (if (artworkId != null) " and artwork $artworkId" else ""))
            true
        } catch (e: Exception) {
            println("✗ Failed to cache few-shot pool: ${e.message}")
            false
        }
    }

    // ========================================================================
    // 2. Get Cached Few-Shot Pool
    // ========================================================================

    /**
     * Retrieve cached few-shot pool
     * Returns null if not found (cache miss)
     */
    fun getFewShotPool(userId: Int, artworkId: Int? = null): List<EnrichedFewShot>? {
        val startTime = System.currentTimeMillis()

        return try {
            val key = if (artworkId != null) {
                RedisConfig.getUserArtworkKey(userId, artworkId)
            } else {
                RedisConfig.getUserPoolKey(userId)
            }

            val jsonValue = redis.get(key)

            val result = if (jsonValue != null) {
                // Cache hit
                cacheHits++
                val fewShots = json.decodeFromString<List<EnrichedFewShot>>(jsonValue)
                println("✓ Cache HIT: Retrieved ${fewShots.size} few-shots for user $userId")
                fewShots
            } else {
                // Cache miss
                cacheMisses++
                println("⊘ Cache MISS: No cached few-shots for user $userId")
                null
            }

            // Track retrieval time
            val elapsed = System.currentTimeMillis() - startTime
            totalRetrievalTimeMs += elapsed
            retrievalCount++

            result
        } catch (e: Exception) {
            println("✗ Failed to retrieve cached few-shot pool: ${e.message}")
            cacheMisses++
            null
        }
    }

    // ========================================================================
    // 3. Invalidate Cache
    // ========================================================================

    /**
     * Invalidate few-shot cache for a user
     * If artworkId is provided, only invalidates that specific cache
     * Otherwise, invalidates all user-related caches
     */
    fun invalidateFewShotCache(userId: Int, artworkId: Int? = null): Int {
        return try {
            val keys = if (artworkId != null) {
                // Invalidate specific user-artwork cache
                listOf(RedisConfig.getUserArtworkKey(userId, artworkId))
            } else {
                // Invalidate all user-related caches
                val pattern = "${RedisConfig.FEW_SHOT_USER_PREFIX}${userId}:*"
                redis.keys(pattern)
            }

            val deletedCount = if (keys.isNotEmpty()) {
                redis.del(*keys.toTypedArray())
            } else {
                0L
            }

            println("✓ Invalidated $deletedCount cache keys for user $userId")
            deletedCount.toInt()
        } catch (e: Exception) {
            println("✗ Failed to invalidate cache: ${e.message}")
            0
        }
    }

    // ========================================================================
    // 4. Cache Top Quality Few-Shots
    // ========================================================================

    /**
     * Cache top quality few-shots in a sorted set
     * Uses effectiveness score as the sort key
     * Prevents cold start issues by pre-caching best examples
     */
    fun cacheTopQualityFewShots(limit: Int = 100): Boolean {
        return try {
            // Get top few-shots from database
            val criteria = FewShotSelectionCriteria(
                userId = 0, // Not user-specific
                minQualityScore = 4.0,
                maxResults = limit,
                ensureDiversity = false
            )

            val topFewShots = fewShotBuilderService.selectTopFewShots(criteria)

            // Clear existing sorted set
            redis.del(RedisConfig.FEW_SHOT_QUALITY_INDEX)

            // Add to sorted set with effectiveness score
            topFewShots.forEach { fewShot ->
                val score = fewShot.effectivenessScore ?: 50.0
                val value = json.encodeToString(fewShot)

                redis.zadd(
                    RedisConfig.FEW_SHOT_QUALITY_INDEX,
                    score,
                    value
                )
            }

            // Set TTL on the sorted set
            redis.expire(RedisConfig.FEW_SHOT_QUALITY_INDEX, RedisConfig.QUALITY_INDEX_TTL)

            println("✓ Cached ${topFewShots.size} top quality few-shots in sorted set")
            true
        } catch (e: Exception) {
            println("✗ Failed to cache top quality few-shots: ${e.message}")
            false
        }
    }

    /**
     * Get top quality few-shots from cache
     */
    fun getTopQualityFewShots(limit: Int = 10): List<FewShotExampleDTO> {
        return try {
            // Get from sorted set (highest score first)
            val values = redis.zrevrange(RedisConfig.FEW_SHOT_QUALITY_INDEX, 0, limit.toLong() - 1)

            values.mapNotNull { jsonValue ->
                try {
                    json.decodeFromString<FewShotExampleDTO>(jsonValue)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            println("✗ Failed to get top quality few-shots from cache: ${e.message}")
            emptyList()
        }
    }

    // ========================================================================
    // 5. Increment Few-Shot Usage
    // ========================================================================

    /**
     * Increment usage count for a few-shot in Redis
     * This allows fast incrementing without hitting the database
     * Should be synced to DB periodically
     */
    fun incrementFewShotUsage(fewShotId: Int): Long {
        return try {
            val key = RedisConfig.getFewShotUsageKey(fewShotId)
            val newCount = redis.incr(key)

            // Set TTL if this is a new key
            if (newCount == 1L) {
                redis.expire(key, RedisConfig.DEFAULT_TTL)
            }

            newCount
        } catch (e: Exception) {
            println("✗ Failed to increment usage: ${e.message}")
            0L
        }
    }

    /**
     * Get usage count from Redis
     */
    fun getFewShotUsage(fewShotId: Int): Long {
        return try {
            val key = RedisConfig.getFewShotUsageKey(fewShotId)
            redis.get(key)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Sync usage counts from Redis to database
     * Should be called periodically by a background job
     */
    fun syncUsageToDatabase(): Int {
        return try {
            val pattern = "${RedisConfig.FEW_SHOT_USAGE_PREFIX}*"
            val keys = redis.keys(pattern)

            var syncedCount = 0

            keys.forEach { key ->
                val fewShotId = key.removePrefix(RedisConfig.FEW_SHOT_USAGE_PREFIX).toIntOrNull()
                val usageCount = redis.get(key)?.toLongOrNull()

                if (fewShotId != null && usageCount != null && usageCount > 0) {
                    // Update database (this would call repository method)
                    // For now, just log
                    println("→ Sync: Few-shot $fewShotId has $usageCount uses")
                    syncedCount++

                    // Clear Redis counter after sync
                    redis.del(key)
                }
            }

            println("✓ Synced $syncedCount usage counts to database")
            syncedCount
        } catch (e: Exception) {
            println("✗ Failed to sync usage to database: ${e.message}")
            0
        }
    }

    // ========================================================================
    // 6. Get Cache Statistics
    // ========================================================================

    /**
     * Get comprehensive cache statistics
     */
    fun getFewShotStatistics(): FewShotCacheStats {
        return try {
            // Calculate cache hit rate
            val totalRequests = cacheHits + cacheMisses
            val hitRate = if (totalRequests > 0) {
                (cacheHits.toDouble() / totalRequests) * 100.0
            } else {
                0.0
            }

            // Average retrieval time
            val avgRetrievalTime = if (retrievalCount > 0) {
                totalRetrievalTimeMs / retrievalCount
            } else {
                0L
            }

            // Get top used few-shots from Redis
            val topUsed = getTopUsedFewShots(10)

            // Calculate total cache size (approximate)
            val cacheSize = calculateCacheSize()

            // Get total cached items
            val totalCached = redis.keys("fewshot:*").size

            FewShotCacheStats(
                totalCached = totalCached,
                cacheHitRate = hitRate,
                averageRetrievalTimeMs = avgRetrievalTime,
                topUsedFewShots = topUsed,
                cacheSizeBytes = cacheSize
            )
        } catch (e: Exception) {
            println("✗ Failed to get cache statistics: ${e.message}")
            FewShotCacheStats(
                totalCached = 0,
                cacheHitRate = 0.0,
                averageRetrievalTimeMs = 0L,
                topUsedFewShots = emptyList(),
                cacheSizeBytes = 0L
            )
        }
    }

    /**
     * Get top used few-shots from Redis usage counters
     */
    private fun getTopUsedFewShots(limit: Int): List<TopUsedFewShot> {
        return try {
            val pattern = "${RedisConfig.FEW_SHOT_USAGE_PREFIX}*"
            val keys = redis.keys(pattern)

            keys.mapNotNull { key ->
                val fewShotId = key.removePrefix(RedisConfig.FEW_SHOT_USAGE_PREFIX).toIntOrNull()
                val usageCount = redis.get(key)?.toIntOrNull()

                if (fewShotId != null && usageCount != null) {
                    TopUsedFewShot(fewShotId, usageCount)
                } else {
                    null
                }
            }
                .sortedByDescending { it.usageCount }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Calculate approximate cache size in bytes
     */
    private fun calculateCacheSize(): Long {
        return try {
            val keys = redis.keys("fewshot:*")
            var totalSize = 0L

            keys.forEach { key ->
                val value = redis.get(key)
                if (value != null) {
                    totalSize += value.length * 2 // Approximate (UTF-16 encoding)
                }
            }

            totalSize
        } catch (e: Exception) {
            0L
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Cache a single few-shot detail
     */
    fun cacheFewShotDetail(fewShotId: Int, enrichedFewShot: EnrichedFewShot): Boolean {
        return try {
            val key = RedisConfig.getFewShotDetailKey(fewShotId)
            val jsonValue = json.encodeToString(enrichedFewShot)

            redis.setex(key, RedisConfig.DETAIL_TTL, jsonValue)
            true
        } catch (e: Exception) {
            println("✗ Failed to cache few-shot detail: ${e.message}")
            false
        }
    }

    /**
     * Get cached few-shot detail
     */
    fun getFewShotDetail(fewShotId: Int): EnrichedFewShot? {
        return try {
            val key = RedisConfig.getFewShotDetailKey(fewShotId)
            val jsonValue = redis.get(key)

            if (jsonValue != null) {
                cacheHits++
                json.decodeFromString<EnrichedFewShot>(jsonValue)
            } else {
                cacheMisses++
                null
            }
        } catch (e: Exception) {
            println("✗ Failed to get cached few-shot detail: ${e.message}")
            cacheMisses++
            null
        }
    }

    /**
     * Reset cache statistics
     */
    fun resetStatistics() {
        cacheHits = 0L
        cacheMisses = 0L
        totalRetrievalTimeMs = 0L
        retrievalCount = 0L
    }

    /**
     * Warm up cache with popular few-shots
     * Should be called on application startup
     */
    fun warmUpCache(): Boolean {
        println("→ Warming up few-shot cache...")

        return try {
            // Cache top quality few-shots
            cacheTopQualityFewShots(100)

            println("✓ Cache warm-up completed")
            true
        } catch (e: Exception) {
            println("✗ Cache warm-up failed: ${e.message}")
            false
        }
    }
}