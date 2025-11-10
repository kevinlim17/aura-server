package com.kevin.db.extraTables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Few-Shot Example Resources Join Table
 *
 * This table links few-shot examples to their associated resources:
 * - User Contexts (memories, emotions, goals)
 * - User Links (web resources, references)
 * - User Memos (personal notes)
 *
 * Each resource has a relevance score indicating its contribution
 * to the quality of the few-shot example.
 */
object FewShotExampleResourcesTable : IntIdTable("few_shot_example_resources") {
    // Foreign key to few_shot_examples table
    val fewShotExampleId = reference("few_shot_example_id", FewShotExamplesTable)

    // Resource type: CONTEXT, LINK, or MEMO
    val resourceType = varchar("resource_type", 20)

    // Resource ID (polymorphic reference)
    // This ID refers to the primary key in:
    // - user_contexts table (if resourceType = 'CONTEXT')
    // - user_links table (if resourceType = 'LINK')
    // - user_memos table (if resourceType = 'MEMO')
    val resourceId = integer("resource_id")

    // Relevance score (0.0 - 1.0)
    // Indicates how much this resource contributed to the example quality
    val relevanceScore = decimal("relevance_score", 3, 2).default(0.0.toBigDecimal())

    // Timestamp
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    // Unique constraint: one resource can only be linked once per example
    init {
        uniqueIndex("unique_resource_per_example", fewShotExampleId, resourceType, resourceId)
    }
}

/**
 * Resource Type Enum
 */
enum class ResourceType(val value: String) {
    CONTEXT("CONTEXT"),
    LINK("LINK"),
    MEMO("MEMO");

    companion object {
        fun fromString(value: String): ResourceType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Invalid resource type: $value")
        }
    }
}