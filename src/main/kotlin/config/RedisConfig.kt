package com.kevin.config

import io.github.cdimascio.dotenv.dotenv
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.serialization.json.Json

/**
 * Redis Configuration for Few-Shot Caching
 * Manages Redis client connection and provides JSON serialization utilities
 */
object RedisConfig {

    // Load environment variables
    private val dotenv = dotenv {
        directory = "./env"
        filename = ".env"
        ignoreIfMissing = true
    }

    // Redis connection settings
    private val redisUrl = dotenv["REDIS"]

    // Initialize Redis client
    val redisClient: RedisClient by lazy {
        RedisClient.create(redisUrl)
    }

    // Get Redis connection
    val connection: StatefulRedisConnection<String, String> by lazy {
        redisClient.connect()
    }

    // Get Redis synchronous commands
    val commands: RedisCommands<String, String> by lazy {
        connection.sync()
    }

    // JSON serializer for Few-Shot objects
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // ========================================================================
    // Cache Key Prefixes
    // ========================================================================

    const val FEW_SHOT_USER_PREFIX = "fewshot:user:"
    const val FEW_SHOT_ARTWORK_PREFIX = "fewshot:artwork:"
    const val FEW_SHOT_DETAIL_PREFIX = "fewshot:detail:"
    const val FEW_SHOT_QUALITY_INDEX = "fewshot:quality:sorted_set"
    const val FEW_SHOT_USAGE_PREFIX = "fewshot:usage:"
    const val FEW_SHOT_STATS_KEY = "fewshot:stats"

    // ========================================================================
    // TTL Configurations (in seconds)
    // ========================================================================

    const val DEFAULT_TTL = 3600L // 1 hour
    const val QUALITY_INDEX_TTL = 86400L // 24 hours
    const val DETAIL_TTL = 7200L // 2 hours
    const val STATS_TTL = 300L // 5 minutes

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Build cache key for user-specific few-shot pool
     */
    fun getUserPoolKey(userId: Int): String {
        return "${FEW_SHOT_USER_PREFIX}${userId}:pool"
    }

    /**
     * Build cache key for artwork-specific few-shot pool
     */
    fun getArtworkPoolKey(artworkId: Int): String {
        return "${FEW_SHOT_ARTWORK_PREFIX}${artworkId}:pool"
    }

    /**
     * Build cache key for user-artwork combination
     */
    fun getUserArtworkKey(userId: Int, artworkId: Int): String {
        return "${FEW_SHOT_USER_PREFIX}${userId}:artwork:${artworkId}"
    }

    /**
     * Build cache key for few-shot detail
     */
    fun getFewShotDetailKey(fewShotId: Int): String {
        return "${FEW_SHOT_DETAIL_PREFIX}${fewShotId}"
    }

    /**
     * Build cache key for few-shot usage counter
     */
    fun getFewShotUsageKey(fewShotId: Int): String {
        return "${FEW_SHOT_USAGE_PREFIX}${fewShotId}"
    }

    /**
     * Close Redis connection gracefully
     */
    fun close() {
        try {
            connection.close()
            redisClient.shutdown()
        } catch (e: Exception) {
            println("⚠ Error closing Redis connection: ${e.message}")
        }
    }

    /**
     * Test Redis connection
     */
    fun testConnection(): Boolean {
        return try {
            commands.ping() == "PONG"
        } catch (e: Exception) {
            println("✗ Redis connection failed: ${e.message}")
            false
        }
    }

    /**
     * Clear all few-shot related caches
     * Use with caution!
     */
    fun clearAllCaches() {
        try {
            val keys = commands.keys("fewshot:*")
            if (keys.isNotEmpty()) {
                commands.del(*keys.toTypedArray())
                println("✓ Cleared ${keys.size} few-shot cache keys")
            }
        } catch (e: Exception) {
            println("⚠ Error clearing caches: ${e.message}")
        }
    }
}
