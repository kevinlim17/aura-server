package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * Memo DTOs for User Memos Management
 * Supports text and voice memos with automatic transcription
 */

/**
 * Input method for memos
 */
enum class MemoInputMethod {
    TEXT,   // 텍스트 입력
    VOICE   // 음성 입력
}

/**
 * Request to create a new memo
 */
@Serializable
data class CreateMemoRequest(
    val content: String,                      // 메모 내용 (텍스트 또는 음성 전사 텍스트)
    val artworkId: Int? = null,               // 연결된 작품 ID
    val docentSessionId: Int? = null,         // 연결된 도슨트 세션 ID
    val inputMethod: String = "TEXT",         // TEXT | VOICE
    val voiceUrl: String? = null,             // 음성 메모 URL
    val voiceDurationSeconds: Int? = null,    // 음성 메모 길이 (초)
    val tags: List<String>? = null,           // 태그 목록
    val category: String? = null,             // 메모 카테고리
    val isSharedWithCompanion: Boolean = false // 동행자와 공유 여부
)

/**
 * Request to update an existing memo
 */
@Serializable
data class UpdateMemoRequest(
    val content: String? = null,              // 메모 내용
    val tags: List<String>? = null,           // 태그 목록
    val category: String? = null,             // 메모 카테고리
    val isSharedWithCompanion: Boolean? = null // 동행자와 공유 여부
)

/**
 * Memo response
 */
@Serializable
data class MemoResponse(
    val id: Int,
    val userId: Int,

    // Connected entities
    val artworkId: Int?,
    val docentSessionId: Int?,

    // Content
    val content: String,

    // Voice memo
    val voiceUrl: String?,
    val voiceDurationSeconds: Int?,

    // Input method
    val inputMethod: String,

    // Tags
    val tags: List<String>,

    // Category
    val category: String?,

    // Sharing
    val isSharedWithCompanion: Boolean,

    // Timestamps
    val createdAt: String,
    val updatedAt: String
)

/**
 * Memo list response with pagination
 */
@Serializable
data class MemoListResponse(
    val memos: List<MemoResponse>,
    val pagination: PaginationInfo
)

/**
 * Memo with artwork/session details
 */
@Serializable
data class MemoWithDetailsResponse(
    val memo: MemoResponse,
    val artwork: ArtworkResponse?,
    val docentSession: DocentSessionResponse?
)

/**
 * Memo statistics
 */
@Serializable
data class MemoStatistics(
    val totalMemos: Long,
    val textMemos: Long,
    val voiceMemos: Long,
    val memosWithArtwork: Long,
    val memosWithSession: Long,
    val sharedMemos: Long
)