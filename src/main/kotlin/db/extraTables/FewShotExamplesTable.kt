package com.kevin.db.extraTables

import com.kevin.db.Artworks
import com.kevin.db.DocentFeedbacks
import com.kevin.db.DocentSessions
import com.kevin.db.jsonb
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// Custom column type for integer arrays
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.postgresql.util.PGobject

/**
 * Extended Few-Shot Examples Table
 *
 * This table stores high-quality docent examples for few-shot learning,
 * now enhanced with resource tracking (contexts, links, memos) and
 * effectiveness metrics.
 */
object FewShotExamplesTable : IntIdTable("few_shot_examples") {
    // Basic few-shot information
    val artworkId = reference("artwork_id", Artworks)
    val userContextSummary = text("user_context_summary")
    val exemplarText = text("exemplar_text")

    // Source references
    val sourceDocentSessionId = reference("source_docent_session_id", DocentSessions).nullable()
    val sourceFeedbackId = reference("source_feedback_id", DocentFeedbacks).nullable()

    // Resource IDs (arrays) - NEW in V2
    val userContextIds = array<Int>("user_context_ids", IntegerColumnType()).default(emptyList())
    val userLinkIds = array<Int>("user_link_ids", IntegerColumnType()).default(emptyList())
    val userMemoIds = array<Int>("user_memo_ids", IntegerColumnType()).default(emptyList())

    // Companion input - NEW in V2
    val companionInput = text("companion_input").nullable()

    // Prompt metadata (JSONB) - NEW in V2
    val promptMetadata = jsonb("prompt_metadata")

    // Quality metrics
    val qualityScore = decimal("quality_score", 3, 2)
    val userRating = integer("user_rating").nullable()

    // Effectiveness metrics - NEW in V2
    val effectivenessScore = decimal("effectiveness_score", 5, 2).nullable()
    val diversityScore = decimal("diversity_score", 5, 2).nullable()

    // Usage statistics
    val usageCount = integer("usage_count").default(0)
    val lastUsedAt = timestamp("last_used_at").nullable()

    // Category
    val category = varchar("category", 50).nullable()

    // Active status
    val isActive = bool("is_active").default(true)

    // Timestamps
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

class IntegerArrayColumnType : ColumnType<List<Int>>() {
    override fun sqlType(): String = "INTEGER[]"

    override fun valueFromDB(value: Any): List<Int> {
        return when (value) {
            is java.sql.Array -> {
                @Suppress("UNCHECKED_CAST")
                (value.array as Array<*>).mapNotNull { it as? Int }
            }
            is Array<*> -> value.mapNotNull { it as? Int }
            else -> emptyList()
        }
    }

    override fun notNullValueToDB(value: List<Int>): Any {
        return PGobject().apply {
            type = "integer[]"
            this.value = value.joinToString(",", "{", "}")
        }
    }
}

// Extension function for easier array column definition
fun <T : Table> T.array(name: String, columnType: ColumnType<Int>): Column<List<Int>> {
    return registerColumn(name, IntegerArrayColumnType())
}

// For compatibility with existing code
class IntegerColumnType : ColumnType<Int>() {
    override fun sqlType(): String = "INTEGER"

    override fun valueFromDB(value: Any): Int {
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            else -> throw IllegalArgumentException("Cannot convert $value to Int")
        }
    }

    override fun notNullValueToDB(value: Int): Any = value
}