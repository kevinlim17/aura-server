package com.kevin.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.postgresql.util.PGobject

/**
 * Custom column types for PostgreSQL-specific types
 */

/**
 * JSONB column type for PostgreSQL
 */
class JsonbColumnType : ColumnType<String>() {
    override fun sqlType(): String = "JSONB"

    override fun valueFromDB(value: Any): String {
        return when (value) {
            is PGobject -> value.value ?: "{}"
            is String -> value
            else -> value.toString()
        }
    }

    override fun notNullValueToDB(value: String): Any {
        return PGobject().apply {
            type = "jsonb"
            this.value = value
        }
    }
}

/**
 * Extension function to add JSONB column to a table
 */
fun Table.jsonb(name: String): Column<String> = registerColumn(name, JsonbColumnType())