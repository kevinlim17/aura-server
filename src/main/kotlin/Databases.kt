package com.kevin

import com.kevin.db.DatabaseConfig
import com.kevin.db.DatabaseInitializer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configure database connection and initialization
 * Uses complete 14-table schema from src/main/kotlin/database/
 */
fun Application.configureDatabases() {
    log.info("========================================")
    log.info("Starting database configuration...")
    log.info("========================================")
    log.info("Database URL: ${DatabaseConfig.dbUrl}")
    log.info("Database User: ${DatabaseConfig.dbUser}")
    log.info("Database Driver: ${DatabaseConfig.dbDriver}")

    // Connect to PostgresSQL database
    val database = try {
        DatabaseConfig.connect()
    } catch (e: Exception) {
        log.error("❌ Failed to connect to database", e)
        throw e
    }

    log.info("✓ Database connected successfully")

    // Initialize database tables on startup
    log.info("Initializing database schema with 14 tables...")
    try {
        DatabaseInitializer.initializeTables(database)
        log.info("✓ Database schema initialized successfully")
        log.info("  • Users table")
        log.info("  • UserProfiles table")
        log.info("  • UserContexts table")
        log.info("  • UserPreferences table")
        log.info("  • Artworks table")
        log.info("  • ArtworkSearches table")
        log.info("  • DocentSessions table")
        log.info("  • DocentFeedbacks table")
        log.info("  • UserLinks table")
        log.info("  • UserMemos table")
        log.info("  • VoiceRecordings table")
        log.info("  • FewShotExamples table")
        log.info("  • UserCompanions table")
        log.info("  • SchemaMigrations table")
    } catch (e: Exception) {
        log.error("❌ Failed to initialize database schema", e)
        throw e
    }

    // Optional: Seed sample data for development
    // Uncomment to populate with test data
    /*
    if (environment.developmentMode) {
        log.info("Seeding sample data for development...")
        try {
            DatabaseInitializer.seedSampleData(database)
            DatabaseInitializer.seedArtworks(database)
            log.info("✓ Sample data seeded successfully")
        } catch (e: Exception) {
            log.error("Failed to seed sample data", e)
        }
    }
    */

    log.info("========================================")
    log.info("Database configuration completed!")
    log.info("========================================")

    // Add database-related routes
    routing {
        // Health check endpoint
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf<String, Any>(
                "status" to "healthy",
                "database" to "connected",
                // "timestamp" to System.currentTimeMillis()
            ))
        }

        // Database info endpoint
        get("/db-info") {
            call.respond(HttpStatusCode.OK, DatabaseConfig.getConnectionInfo())
        }

        // Database statistics endpoint
        get("/db-stats") {
            call.respond(HttpStatusCode.OK, mapOf<String, Any>(
                "tables" to listOf(
                    "users",
                    "user_profiles",
                    "user_contexts",
                    "user_preferences",
                    "artworks",
                    "artwork_searches",
                    "docent_sessions",
                    "docent_feedbacks",
                    "user_links",
                    "user_memos",
                    "voice_recordings",
                    "few_shot_examples",
                    "user_companions",
                    "schema_migrations"
                ),
                /**
                "status" to "initialized",
                "total_tables" to 14
                */
            ))
        }
    }
}