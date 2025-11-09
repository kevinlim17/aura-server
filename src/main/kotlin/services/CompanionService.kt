package com.kevin.services

import com.kevin.model.dto.*
import com.kevin.repository.CompanionRepository
import com.kevin.repository.UserRepository
import java.util.UUID
import kotlin.time.Duration.Companion.days

/**
 * Companion Service
 * Handles business logic for companion management
 *
 * Features:
 * - Invite companions via email
 * - Accept/reject invitations
 * - Manage companion permissions
 * - View companion list and pending invitations
 * - Generate invite codes
 */
class CompanionService(
    private val companionRepository: CompanionRepository = CompanionRepository(),
    private val userRepository: UserRepository = UserRepository()
) {

    /**
     * Invite a companion
     *
     * @param mainUserId Main user ID (visually impaired user)
     * @param request Invitation request
     * @return ApiResponse with CompanionResponse or error
     */
    fun inviteCompanion(mainUserId: Int, request: InviteCompanionRequest): ApiResponse<CompanionResponse> {
        // Validate relationship type
        try {
            CompanionRelationshipType.valueOf(request.relationshipType)
        } catch (e: IllegalArgumentException) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Invalid relationshipType. Must be one of: FAMILY, FRIEND, CAREGIVER, VOLUNTEER, OTHER"
            )
        }

        // Find companion user by email
        val companionUser = userRepository.findUserByEmail(request.companionEmail)
        if (companionUser == null) {
            return ApiResponse(
                success = false,
                data = null,
                message = "User with email ${request.companionEmail} not found"
            )
        }

        // Check if user is trying to invite themselves
        if (mainUserId == companionUser.id) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Cannot invite yourself as a companion"
            )
        }

        // Check if relationship already exists
        val exists = companionRepository.isCompanionRelationshipExists(mainUserId, companionUser.id)
        if (exists) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Companion relationship already exists"
            )
        }

        try {
            // Create companion relationship with PENDING status
            val companionId = companionRepository.createCompanion(
                mainUserId = mainUserId,
                companionUserId = companionUser.id,
                relationshipType = request.relationshipType,
                canAddContext = request.canAddContext,
                canViewHistory = request.canViewHistory,
                canAddMemos = request.canAddMemos,
                status = "PENDING"
            )

            // TODO: Send invitation email if needed
            // sendInvitationEmail(companionUser.email, request.message)

            // Retrieve created companion relationship
            val companion = companionRepository.findCompanionById(companionId)
                ?: return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to retrieve created companion relationship"
                )

            return ApiResponse(
                success = true,
                data = companion,
                message = "Companion invitation sent successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to invite companion: ${e.message}"
            )
        }
    }

    /**
     * Get companions for a main user
     *
     * @param mainUserId Main user ID
     * @param page Page number
     * @param limit Items per page
     * @param status Filter by status (optional)
     * @return ApiResponse with CompanionListResponse or error
     */
    fun getCompanions(
        mainUserId: Int,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null
    ): ApiResponse<CompanionListResponse> {
        // Validate pagination parameters
        if (page < 1) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Page must be greater than 0"
            )
        }

        if (limit !in 1..100) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Limit must be between 1 and 100"
            )
        }

        // Validate status if provided
        if (status != null) {
            try {
                CompanionStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Invalid status. Must be one of: PENDING, ACTIVE, INACTIVE"
                )
            }
        }

        try {
            // Get companions
            val (companions, totalCount) = companionRepository.findCompanionsByMainUserId(
                mainUserId = mainUserId,
                page = page,
                limit = limit,
                status = status
            )

            // Calculate pagination info
            val totalPages = ((totalCount + limit - 1) / limit).toInt()

            val response = CompanionListResponse(
                companions = companions,
                pagination = PaginationInfo(
                    page = page,
                    limit = limit,
                    totalCount = totalCount,
                    totalPages = totalPages,
                    hasNext = page < totalPages,
                    hasPrevious = page > 1
                )
            )

            return ApiResponse(
                success = true,
                data = response,
                message = "Companions retrieved successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to retrieve companions: ${e.message}"
            )
        }
    }

    /**
     * Get pending invitations for a companion user
     *
     * @param companionUserId Companion user ID
     * @return ApiResponse with PendingInvitationsResponse or error
     */
    fun getPendingInvitations(companionUserId: Int): ApiResponse<PendingInvitationsResponse> {
        try {
            val invitations = companionRepository.findPendingInvitationsByCompanionUserId(companionUserId)

            val response = PendingInvitationsResponse(
                invitations = invitations,
                totalCount = invitations.size.toLong()
            )

            return ApiResponse(
                success = true,
                data = response,
                message = "Pending invitations retrieved successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to retrieve pending invitations: ${e.message}"
            )
        }
    }

    /**
     * Update companion permissions
     *
     * @param mainUserId Main user ID
     * @param companionId Companion relationship ID
     * @param request Permission update request
     * @return ApiResponse with updated CompanionResponse or error
     */
    fun updateCompanionPermissions(
        mainUserId: Int,
        companionId: Int,
        request: UpdateCompanionPermissionsRequest
    ): ApiResponse<CompanionResponse> {
        // Check if companion relationship exists and belongs to user
        val owned = companionRepository.isCompanionOwnedByUser(companionId, mainUserId)
        if (!owned) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Companion relationship not found or access denied"
            )
        }

        try {
            // Update permissions
            val updated = companionRepository.updateCompanionPermissions(
                companionId = companionId,
                mainUserId = mainUserId,
                canAddContext = request.canAddContext,
                canViewHistory = request.canViewHistory,
                canAddMemos = request.canAddMemos
            )

            if (!updated) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to update companion permissions"
                )
            }

            // Retrieve updated companion
            val companion = companionRepository.findCompanionById(companionId)
                ?: return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to retrieve updated companion"
                )

            return ApiResponse(
                success = true,
                data = companion,
                message = "Companion permissions updated successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to update companion permissions: ${e.message}"
            )
        }
    }

    /**
     * Accept companion invitation
     *
     * @param companionUserId Companion user ID
     * @param companionId Companion relationship ID
     * @return ApiResponse with updated CompanionResponse or error
     */
    fun acceptInvitation(companionUserId: Int, companionId: Int): ApiResponse<CompanionResponse> {
        try {
            // Update status to ACTIVE
            val updated = companionRepository.updateCompanionStatus(
                companionId = companionId,
                userId = companionUserId,
                newStatus = "ACTIVE"
            )

            if (!updated) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Invitation not found or already processed"
                )
            }

            // Retrieve updated companion
            val companion = companionRepository.findCompanionById(companionId)
                ?: return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to retrieve updated companion"
                )

            return ApiResponse(
                success = true,
                data = companion,
                message = "Invitation accepted successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to accept invitation: ${e.message}"
            )
        }
    }

    /**
     * Reject companion invitation
     *
     * @param companionUserId Companion user ID
     * @param companionId Companion relationship ID
     * @return ApiResponse with success status
     */
    fun rejectInvitation(companionUserId: Int, companionId: Int): ApiResponse<Unit> {
        try {
            // Update status to INACTIVE
            val updated = companionRepository.updateCompanionStatus(
                companionId = companionId,
                userId = companionUserId,
                newStatus = "INACTIVE"
            )

            if (!updated) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Invitation not found or already processed"
                )
            }

            return ApiResponse(
                success = true,
                data = Unit,
                message = "Invitation rejected successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to reject invitation: ${e.message}"
            )
        }
    }

    /**
     * Remove companion
     *
     * @param mainUserId Main user ID
     * @param companionId Companion relationship ID
     * @return ApiResponse with success status
     */
    fun removeCompanion(mainUserId: Int, companionId: Int): ApiResponse<Unit> {
        // Check if companion relationship exists and belongs to user
        val owned = companionRepository.isCompanionOwnedByUser(companionId, mainUserId)
        if (!owned) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Companion relationship not found or access denied"
            )
        }

        try {
            val deleted = companionRepository.deleteCompanion(companionId, mainUserId)
            if (!deleted) {
                return ApiResponse(
                    success = false,
                    data = null,
                    message = "Failed to remove companion"
                )
            }

            return ApiResponse(
                success = true,
                data = Unit,
                message = "Companion removed successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to remove companion: ${e.message}"
            )
        }
    }

    /**
     * Get companion statistics
     *
     * @param mainUserId Main user ID
     * @return ApiResponse with CompanionStatistics or error
     */
    fun getCompanionStatistics(mainUserId: Int): ApiResponse<CompanionStatistics> {
        try {
            val statistics = companionRepository.getCompanionStatistics(mainUserId)

            return ApiResponse(
                success = true,
                data = statistics,
                message = "Companion statistics retrieved successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to retrieve companion statistics: ${e.message}"
            )
        }
    }

    /**
     * Generate invite code for a user
     * This can be used to invite companions without knowing their email
     *
     * @param mainUserId Main user ID
     * @return ApiResponse with InviteCodeResponse or error
     */
    fun generateInviteCode(mainUserId: Int): ApiResponse<InviteCodeResponse> {
        try {
            // Generate a unique invite code
            val inviteCode = UUID.randomUUID().toString().substring(0, 8).uppercase()

            // Calculate expiration (7 days from now)
            val expiresAt = kotlinx.datetime.Clock.System.now()
                .plus(7.days)
                .toString()

            // Generate invite URL
            val inviteUrl = "https://aura.app/invite/$inviteCode"

            // TODO: Store invite code in database with expiration
            // For now, just return the code

            val response = InviteCodeResponse(
                inviteCode = inviteCode,
                expiresAt = expiresAt,
                inviteUrl = inviteUrl
            )

            return ApiResponse(
                success = true,
                data = response,
                message = "Invite code generated successfully"
            )
        } catch (e: Exception) {
            return ApiResponse(
                success = false,
                data = null,
                message = "Failed to generate invite code: ${e.message}"
            )
        }
    }
}
