package com.kevin.repository

import com.kevin.db.UserCompanions
import com.kevin.db.Users
import com.kevin.model.dto.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Companion Repository for database operations
 * Handles CRUD operations for user_companions table
 */
class CompanionRepository {

    /**
     * Create companion relationship (invitation)
     */
    fun createCompanion(
        mainUserId: Int,
        companionUserId: Int,
        relationshipType: String?,
        canAddContext: Boolean,
        canViewHistory: Boolean,
        canAddMemos: Boolean,
        status: String = "PENDING"
    ): Int {
        return transaction {
            val now = Clock.System.now()

            UserCompanions.insertAndGetId {
                it[UserCompanions.mainUserId] = mainUserId
                it[UserCompanions.companionUserId] = companionUserId
                it[UserCompanions.relationshipType] = relationshipType
                it[UserCompanions.canAddContext] = canAddContext
                it[UserCompanions.canViewHistory] = canViewHistory
                it[UserCompanions.canAddMemos] = canAddMemos
                it[UserCompanions.status] = status
                it[createdAt] = now
                it[updatedAt] = now
            }.value
        }
    }

    /**
     * Find companion relationship by ID
     */
    fun findCompanionById(companionId: Int): CompanionResponse? {
        return transaction {
            // Use explicit join to avoid ambiguity with multiple foreign keys
            UserCompanions
                .join(Users, JoinType.INNER, onColumn = UserCompanions.companionUserId, otherColumn = Users.id)
                .select(
                    UserCompanions.id,
                    UserCompanions.mainUserId,
                    UserCompanions.companionUserId,
                    UserCompanions.relationshipType,
                    UserCompanions.canAddContext,
                    UserCompanions.canViewHistory,
                    UserCompanions.canAddMemos,
                    UserCompanions.status,
                    UserCompanions.createdAt,
                    UserCompanions.updatedAt,
                    Users.email,
                    Users.userType
                )
                .where { UserCompanions.id eq companionId }
                .map { rowToCompanion(it) }
                .singleOrNull()
        }
    }

    /**
     * Find companions by main user ID
     */
    fun findCompanionsByMainUserId(
        mainUserId: Int,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null
    ): Pair<List<CompanionResponse>, Long> {
        return transaction {
            // Build query with filters - use explicit join to avoid ambiguity
            var query = UserCompanions
                .join(Users, JoinType.INNER, onColumn = UserCompanions.companionUserId, otherColumn = Users.id)
                .select(
                    UserCompanions.id,
                    UserCompanions.mainUserId,
                    UserCompanions.companionUserId,
                    UserCompanions.relationshipType,
                    UserCompanions.canAddContext,
                    UserCompanions.canViewHistory,
                    UserCompanions.canAddMemos,
                    UserCompanions.status,
                    UserCompanions.createdAt,
                    UserCompanions.updatedAt,
                    Users.email,
                    Users.userType
                )
                .where { UserCompanions.mainUserId eq mainUserId }

            // Apply status filter if provided
            if (status != null) {
                query = query.andWhere { UserCompanions.status eq status }
            }

            // Get total count
            val totalCount = query.count()

            // Calculate pagination
            val offset = ((page - 1) * limit).toLong()

            // Get paginated results
            val companions = query
                .orderBy(UserCompanions.createdAt, SortOrder.DESC)
                .limit(limit).offset(offset)
                .map { rowToCompanion(it) }

            Pair(companions, totalCount)
        }
    }

    /**
     * Find pending invitations for a companion user
     */
    fun findPendingInvitationsByCompanionUserId(companionUserId: Int): List<InvitationResponse> {
        return transaction {
            // Use explicit join with mainUserId to get main user's email
            UserCompanions
                .join(Users, JoinType.INNER, onColumn = UserCompanions.mainUserId, otherColumn = Users.id)
                .select(
                    UserCompanions.id,
                    UserCompanions.mainUserId,
                    UserCompanions.relationshipType,
                    UserCompanions.status,
                    UserCompanions.createdAt,
                    Users.email,
                    Users.userType
                )
                .where {
                    (UserCompanions.companionUserId eq companionUserId) and
                    (UserCompanions.status eq "PENDING")
                }
                .orderBy(UserCompanions.createdAt, SortOrder.DESC)
                .map { row ->
                    InvitationResponse(
                        id = row[UserCompanions.id].value,
                        mainUserId = row[UserCompanions.mainUserId].value,
                        mainUserEmail = row[Users.email],
                        mainUserName = null, // Could join with UserProfiles if needed
                        relationshipType = row[UserCompanions.relationshipType],
                        message = null, // Could be added to schema if needed
                        status = row[UserCompanions.status],
                        createdAt = row[UserCompanions.createdAt].toString()
                    )
                }
        }
    }

    /**
     * Update companion permissions
     */
    fun updateCompanionPermissions(
        companionId: Int,
        mainUserId: Int,
        canAddContext: Boolean?,
        canViewHistory: Boolean?,
        canAddMemos: Boolean?
    ): Boolean {
        return transaction {
            val now = Clock.System.now()

            val updateCount = UserCompanions.update({
                (UserCompanions.id eq companionId) and (UserCompanions.mainUserId eq mainUserId)
            }) {
                if (canAddContext != null) it[UserCompanions.canAddContext] = canAddContext
                if (canViewHistory != null) it[UserCompanions.canViewHistory] = canViewHistory
                if (canAddMemos != null) it[UserCompanions.canAddMemos] = canAddMemos
                it[updatedAt] = now
            }

            updateCount > 0
        }
    }

    /**
     * Update companion status
     */
    fun updateCompanionStatus(companionId: Int, userId: Int, newStatus: String): Boolean {
        return transaction {
            val now = Clock.System.now()

            val updateCount = UserCompanions.update({
                (UserCompanions.id eq companionId) and (UserCompanions.companionUserId eq userId)
            }) {
                it[status] = newStatus
                it[updatedAt] = now
            }

            updateCount > 0
        }
    }

    /**
     * Delete companion relationship
     */
    fun deleteCompanion(companionId: Int, mainUserId: Int): Boolean {
        return transaction {
            UserCompanions.deleteWhere {
                (UserCompanions.id eq companionId) and (UserCompanions.mainUserId eq mainUserId)
            } > 0
        }
    }

    /**
     * Check if companion relationship exists
     */
    fun isCompanionRelationshipExists(mainUserId: Int, companionUserId: Int): Boolean {
        return transaction {
            UserCompanions.selectAll()
                .where {
                    (UserCompanions.mainUserId eq mainUserId) and (UserCompanions.companionUserId eq companionUserId)
                }
                .count() > 0
        }
    }

    /**
     * Check if user owns companion relationship
     */
    fun isCompanionOwnedByUser(companionId: Int, mainUserId: Int): Boolean {
        return transaction {
            UserCompanions.selectAll()
                .where { (UserCompanions.id eq companionId) and (UserCompanions.mainUserId eq mainUserId) }
                .count() > 0
        }
    }

    /**
     * Get companion statistics for a main user
     */
    fun getCompanionStatistics(mainUserId: Int): CompanionStatistics {
        return transaction {
            val totalCompanions = UserCompanions.selectAll()
                .where { UserCompanions.mainUserId eq mainUserId }
                .count()

            val activeCompanions = UserCompanions.selectAll()
                .where { (UserCompanions.mainUserId eq mainUserId) and (UserCompanions.status eq "ACTIVE") }
                .count()

            val pendingInvitations = UserCompanions.selectAll()
                .where { (UserCompanions.mainUserId eq mainUserId) and (UserCompanions.status eq "PENDING") }
                .count()

            // Count by relationship type
            val companionsByRelationship = UserCompanions.selectAll()
                .where { UserCompanions.mainUserId eq mainUserId }
                .groupBy { it[UserCompanions.relationshipType] ?: "UNKNOWN" }
                .mapValues { it.value.size.toLong() }

            CompanionStatistics(
                totalCompanions = totalCompanions,
                activeCompanions = activeCompanions,
                pendingInvitations = pendingInvitations,
                companionsByRelationship = companionsByRelationship
            )
        }
    }

    /**
     * Convert database row to CompanionResponse
     */
    private fun rowToCompanion(row: ResultRow): CompanionResponse {
        return CompanionResponse(
            id = row[UserCompanions.id].value,
            mainUserId = row[UserCompanions.mainUserId].value,
            companionUserId = row[UserCompanions.companionUserId].value,
            companionEmail = row[Users.email],
            companionName = null, // Could join with UserProfiles if needed
            relationshipType = row[UserCompanions.relationshipType],
            canAddContext = row[UserCompanions.canAddContext],
            canViewHistory = row[UserCompanions.canViewHistory],
            canAddMemos = row[UserCompanions.canAddMemos],
            status = row[UserCompanions.status],
            createdAt = row[UserCompanions.createdAt].toString(),
            updatedAt = row[UserCompanions.updatedAt].toString()
        )
    }
}