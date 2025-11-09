package com.kevin.model.dto

import kotlinx.serialization.Serializable

/**
 * Link DTOs for User Links Management
 * Supports linking external resources to artworks and docent sessions
 */

/**
 * Link types
 */
enum class LinkType {
    ARTICLE,      // 기사/블로그 포스트
    VIDEO,        // 비디오 콘텐츠
    AUDIO,        // 오디오 콘텐츠
    REFERENCE,    // 참고 자료
    INSPIRATION,  // 영감을 준 콘텐츠
    OTHER         // 기타
}

/**
 * Request to add a new link
 */
@Serializable
data class AddLinkRequest(
    val url: String,                           // 필수: URL
    val title: String? = null,                 // 링크 제목
    val description: String? = null,           // 링크 설명
    val artworkId: Int? = null,                // 연결된 작품 ID
    val docentSessionId: Int? = null,          // 연결된 도슨트 세션 ID
    val linkType: String? = null,              // ARTICLE | VIDEO | AUDIO | REFERENCE | INSPIRATION | OTHER
    val thumbnailUrl: String? = null,          // 썸네일 URL
    val hasAudioDescription: Boolean = false,  // 오디오 설명 여부
    val hasSubtitles: Boolean = false,         // 자막 여부
    val metadata: Map<String, String>? = null  // Open Graph 메타데이터
)

/**
 * Link response
 */
@Serializable
data class LinkResponse(
    val id: Int,
    val userId: Int,
    val url: String,
    val title: String?,
    val description: String?,

    // Connected entities
    val artworkId: Int?,
    val docentSessionId: Int?,

    // Link classification
    val linkType: String?,

    // Metadata (Open Graph)
    val metadata: Map<String, String>,

    // Media
    val thumbnailUrl: String?,

    // Accessibility
    val hasAudioDescription: Boolean,
    val hasSubtitles: Boolean,

    // Timestamp
    val createdAt: String
)

/**
 * Link list response with pagination
 */
@Serializable
data class LinkListResponse(
    val links: List<LinkResponse>,
    val pagination: PaginationInfo
)

/**
 * Link with artwork/session details
 */
@Serializable
data class LinkWithDetailsResponse(
    val link: LinkResponse,
    val artwork: ArtworkResponse?,
    val docentSession: DocentSessionResponse?
)

/**
 * Open Graph metadata
 */
@Serializable
data class OpenGraphMetadata(
    val ogTitle: String?,
    val ogDescription: String?,
    val ogImage: String?,
    val siteName: String?,
    val url: String?
)