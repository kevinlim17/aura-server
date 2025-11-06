package com.kevin.db

import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database

/**
 * Database configuration and connection management
 * Loads configuration from .env file
 */
object DatabaseConfig {

    private val dotenv = dotenv {
        directory = "env"
        filename = ".env"
        ignoreIfMissing = true
        systemProperties = false
    }

    // Database connection settings from the.env file
    val dbUrl: String = dotenv["DB_URL"]
    val dbDriver: String = dotenv["DB_DRIVER"]
    val dbUser: String = dotenv["DB_USER"]
    val dbPassword: String = dotenv["DB_PASSWORD"] // ?: ""

    /**
     * Connect to PostgresSQL database
     * @return Database instance
     */
    fun connect(): Database {
        return Database.connect(
            url = dbUrl,
            driver = dbDriver,
            user = dbUser,
            password = dbPassword
        )
    }

    /**
     * Connect to H2 an in-memory database for testing
     * @return Database instance
     */
    fun connectH2(): Database {
        return Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            user = "root",
            password = ""
        )
    }

    /**
     * Get database connection info (for logging/debugging)
     * Masks password for security
     */
    fun getConnectionInfo(): Map<String, String> {
        return mapOf(
            "url" to dbUrl,
            "driver" to dbDriver,
            "user" to dbUser,
            "password" to "***"
        )
    }
}