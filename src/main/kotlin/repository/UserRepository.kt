package com.kevin.repository

import com.kevin.db.Users
import com.kevin.model.dto.RegisterRequest
import com.kevin.model.User
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * User Repository for database operations
 * Handles CRUD operations for users table
 */
class UserRepository {

    /**
     * Create a new user
     */
    fun createUser(registerRequest: RegisterRequest, passwordHash: String): User? {
        return transaction {
            val now = Clock.System.now()

            val userId = Users.insertAndGetId {
                it[email] = registerRequest.email
                it[Users.passwordHash] = passwordHash
                it[userType] = registerRequest.userType
                it[isVisuallyImpaired] = registerRequest.isVisuallyImpaired
                it[impairmentLevel] = registerRequest.impairmentLevel
                it[isActive] = true
                it[isOnboardingCompleted] = false
                it[createdAt] = now
                it[updatedAt] = now
            }

            // Fetch and return the created user
            findUserById(userId.value)
        }
    }

    /**
     * Find user by email
     */
    fun findUserByEmail(email: String): User? {
        return transaction {
            Users.selectAll()
                .where { Users.email eq email }
                .map { rowToUser(it) }
                .singleOrNull()
        }
    }

    /**
     * Find user by ID
     */
    fun findUserById(userId: Int): User? {
        return transaction {
            Users.selectAll()
                .where { Users.id eq userId }
                .map { rowToUser(it) }
                .singleOrNull()
        }
    }

    /**
     * Check if user exists by email
     */
    fun existsByEmail(email: String): Boolean {
        return transaction {
            Users.selectAll()
                .where { Users.email eq email }
                .count() > 0
        }
    }

    /**
     * Get password hash for user
     */
    fun getPasswordHash(email: String): String? {
        return transaction {
            Users.selectAll()
                .where { Users.email eq email }
                .map { it[Users.passwordHash] }
                .singleOrNull()
        }
    }

    /**
     * Update last login timestamp
     */
    fun updateLastLogin(userId: Int) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[lastLoginAt] = Clock.System.now()
            }
        }
    }

    /**
     * Update user information
     */
    fun updateUser(userId: Int, impairmentLevel: String?, isOnboardingCompletedValue: Boolean?): User? {
        transaction {
            Users.update({ Users.id eq userId }) {
                impairmentLevel?.let { level -> it[Users.impairmentLevel] = level }
                isOnboardingCompletedValue?.let { completed -> it[isOnboardingCompleted] = completed }
                it[updatedAt] = Clock.System.now()
            }
        }
        return findUserById(userId)
    }

    /**
     * Update password
     */
    fun updatePassword(userId: Int, newPasswordHash: String): Boolean {
        return transaction {
            Users.update({ Users.id eq userId }) {
                it[passwordHash] = newPasswordHash
                it[updatedAt] = Clock.System.now()
            } > 0
        }
    }

    /**
     * Delete user (soft delete by setting isActive to false)
     */
    fun deleteUser(userId: Int): Boolean {
        return transaction {
            Users.update({ Users.id eq userId }) {
                it[isActive] = false
                it[updatedAt] = Clock.System.now()
            } > 0
        }
    }

    /**
     * Hard delete user (permanent deletion)
     */
    fun hardDeleteUser(userId: Int): Boolean {
        return transaction {
            Users.deleteWhere { Users.id eq userId } > 0
        }
    }

    /**
     * Convert database row to User model
     */
    private fun rowToUser(row: ResultRow): User {
        return User(
            id = row[Users.id].value,
            email = row[Users.email],
            userType = row[Users.userType],
            isVisuallyImpaired = row[Users.isVisuallyImpaired],
            impairmentLevel = row[Users.impairmentLevel],
            isActive = row[Users.isActive],
            isOnboardingCompleted = row[Users.isOnboardingCompleted],
            createdAt = row[Users.createdAt].toString(),
            updatedAt = row[Users.updatedAt].toString(),
            lastLoginAt = row[Users.lastLoginAt]?.toString()
        )
    }
}
