package com.kevin.routes.util

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import io.ktor.server.request.host

/**
 * RESTful API Request/Response Logger Utility
 *
 * Features:
 * - Colorful terminal output
 * - Request/Response body logging
 * - HTTP headers logging
 * - Timestamp and duration tracking
 * - JSON pretty-printing
 */
object RequestLogger {

    enum class LogMode { CONSOLE, JSON, BOTH }

    // Logging mode from env var; default BOTH
    private val mode: LogMode = when (System.getenv("AURA_HTTP_LOG_MODE")?.lowercase()) {
        "console" -> LogMode.CONSOLE
        "json" -> LogMode.JSON
        else -> LogMode.BOTH
    }

    // Public header name used for passing request id across services
    const val TRACE_ID_HEADER: String = "X-Request-ID"

    // Attribute key to store trace id per call
    val TraceIdAttr: AttributeKey<String> = AttributeKey("RequestLogger.TraceId")

    // ANSI Color Codes
    private const val RESET = "\u001B[0m"
    private const val BOLD = "\u001B[1m"
    private const val RED = "\u001B[31m"
    private const val GREEN = "\u001B[32m"
    private const val YELLOW = "\u001B[33m"
    private const val BLUE = "\u001B[34m"
    private const val MAGENTA = "\u001B[35m"
    private const val CYAN = "\u001B[36m"
    private const val WHITE = "\u001B[37m"

    // Box drawing characters
    private const val BOX_TOP_LEFT = "╔"
    private const val BOX_TOP_RIGHT = "╗"
    private const val BOX_BOTTOM_LEFT = "╚"
    private const val BOX_BOTTOM_RIGHT = "╝"
    private const val BOX_HORIZONTAL = "═"
    private const val BOX_VERTICAL = "║"
    private const val BOX_LEFT_T = "╠"
    private const val BOX_RIGHT_T = "╣"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val jsonCompact = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    // Helper: get or create a per-call trace id (from header or generated UUID)
    private fun getOrCreateTraceId(call: ApplicationCall): String {
        return if (call.attributes.contains(TraceIdAttr)) {
            call.attributes[TraceIdAttr]
        } else {
            val incoming = call.request.headers[TRACE_ID_HEADER]
            val id = incoming ?: UUID.randomUUID().toString()
            call.attributes.put(TraceIdAttr, id)
            id
        }
    }

    // Helper: Ensure trace id header is present on the response (for clients)
    fun ensureTraceIdHeader(call: ApplicationCall) {
        val id = getOrCreateTraceId(call)
        try {
            call.response.headers.append(TRACE_ID_HEADER, id, safeOnly = false)
        } catch (_: Throwable) {
            // ignore if headers already sent
        }
    }

    // Helper: mask Authorization header value
    private fun maskAuth(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return if (value.length > 16) value.take(8) + "...***" else "***"
    }

    // Helper: best-effort client IP from common proxy headers
    private fun getClientIp(call: ApplicationCall): String? {
        val h = call.request.headers
        val candidates = listOf(
            h["X-Forwarded-For"],
            h["X-Real-IP"],
            h["CF-Connecting-IP"],
            h["Fly-Client-IP"],
            h["True-Client-IP"]
        )
        val first = candidates.firstOrNull { !it.isNullOrBlank() }
        return first?.split(',')?.first()?.trim()
    }

    // Helper: parse JSON if possible, else wrap as string
    private fun safeJsonEcho(text: String): JsonElement {
        return try {
            Json.parseToJsonElement(text)
        } catch (_: Exception) {
            JsonPrimitive(text)
        }
    }

    /**
     * Log incoming request
     */
    fun logRequest(call: ApplicationCall, requestBody: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val method = call.request.httpMethod.value
        val uri = call.request.uri
        val contentType = call.request.contentType()
        val traceId = getOrCreateTraceId(call)
        val userAgent = call.request.headers[HttpHeaders.UserAgent]
        val ip = getClientIp(call) ?: call.request.host()
        val authHeader = call.request.headers["Authorization"]
        val maskedAuth = maskAuth(authHeader)
        val queryString = call.request.queryString()

        if (mode != LogMode.JSON) {
            println()
            printBox("INCOMING REQUEST", CYAN)
            println("$BOX_VERTICAL ${BOLD}Timestamp:${RESET} $timestamp")
            println("$BOX_VERTICAL ${BOLD}${getMethodColor(method)}${method}${RESET} ${BLUE}${uri}${RESET}")
            println("$BOX_VERTICAL ${BOLD}Trace-Id:${RESET} $traceId")
            println("$BOX_VERTICAL ${BOLD}Content-Type:${RESET} $contentType")

            // Log important headers
            if (maskedAuth != null) {
                println("$BOX_VERTICAL ${BOLD}Authorization:${RESET} $maskedAuth")
            }

            // Log query parameters
            val queryParams = call.request.queryParameters
            if (queryParams.entries().isNotEmpty()) {
                println("$BOX_VERTICAL ${BOLD}Query Parameters:${RESET}")
                queryParams.entries().forEach { (key, values) ->
                    println("$BOX_VERTICAL   ${YELLOW}${key}${RESET} = ${values.joinToString(", ")}")
                }
            }

            // Log request body
            if (requestBody.isNotBlank()) {
                println("${BOX_LEFT_T}${BOX_HORIZONTAL.repeat(60)}${BOX_RIGHT_T}")
                println("$BOX_VERTICAL ${BOLD}Request Body:${RESET}")
                val prettyBody = prettyPrintJson(requestBody)
                prettyBody.lines().forEach { line ->
                    println("$BOX_VERTICAL   $line")
                }
            }

            printBoxBottom()
        }

        if (mode != LogMode.CONSOLE) {
            val obj = buildJsonObject {
                put("timestamp", timestamp)
                put("level", "INFO")
                put("event", "request")
                put("traceId", traceId)
                put("method", method)
                put("uri", uri)
                put("query", queryString)
                put("contentType", contentType.toString())
                put("userAgent", userAgent)
                put("ip", ip)
                if (maskedAuth != null) put("authorizationMasked", maskedAuth)
                if (requestBody.isNotBlank()) put("body", safeJsonEcho(requestBody))
            }
            println(jsonCompact.encodeToString(JsonObject.serializer(), obj))
        }
    }

    /**
     * Log outgoing response
     */
    fun logResponse(call: ApplicationCall, statusCode: HttpStatusCode, responseBody: String, durationMs: Long? = null) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val method = call.request.httpMethod.value
        val uri = call.request.uri
        val traceId = getOrCreateTraceId(call)

        if (mode != LogMode.JSON) {
            println()
            printBox("OUTGOING RESPONSE", GREEN)
            println("$BOX_VERTICAL ${BOLD}Timestamp:${RESET} $timestamp")
            println("$BOX_VERTICAL ${BOLD}${getMethodColor(method)}${method}${RESET} ${BLUE}${uri}${RESET}")
            println("$BOX_VERTICAL ${BOLD}Trace-Id:${RESET} $traceId")
            println("$BOX_VERTICAL ${BOLD}Status:${RESET} ${getStatusColor(statusCode)}${statusCode.value} ${statusCode.description}${RESET}")

            if (durationMs != null) {
                println("$BOX_VERTICAL ${BOLD}Duration:${RESET} ${MAGENTA}${durationMs}ms${RESET}")
            }

            // Log response body
            if (responseBody.isNotBlank()) {
                println("${BOX_LEFT_T}${BOX_HORIZONTAL.repeat(60)}${BOX_RIGHT_T}")
                println("$BOX_VERTICAL ${BOLD}Response Body:${RESET}")
                val prettyBody = prettyPrintJson(responseBody)
                val lines = prettyBody.lines()

                // Truncate if too long
                if (lines.size > 50) {
                    lines.take(47).forEach { line ->
                        println("$BOX_VERTICAL   $line")
                    }
                    println("$BOX_VERTICAL   ${YELLOW}... (${lines.size - 47} more lines)${RESET}")
                } else {
                    lines.forEach { line ->
                        println("$BOX_VERTICAL   $line")
                    }
                }
            }

            printBoxBottom()
            println()
        }

        if (mode != LogMode.CONSOLE) {
            val obj = buildJsonObject {
                put("timestamp", timestamp)
                put("level", "INFO")
                put("event", "response")
                put("traceId", traceId)
                put("method", method)
                put("uri", uri)
                put("status", statusCode.value)
                if (durationMs != null) put("durationMs", durationMs)
                if (responseBody.isNotBlank()) put("body", safeJsonEcho(responseBody))
            }
            println(jsonCompact.encodeToString(JsonObject.serializer(), obj))
        }
    }

    /**
     * Log error response
     */
    fun logError(call: ApplicationCall, statusCode: HttpStatusCode, errorMessage: String, exception: Exception? = null) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val method = call.request.httpMethod.value
        val uri = call.request.uri
        val traceId = getOrCreateTraceId(call)

        if (mode != LogMode.JSON) {
            println()
            printBox("ERROR RESPONSE", RED)
            println("$BOX_VERTICAL ${BOLD}Timestamp:${RESET} $timestamp")
            println("$BOX_VERTICAL ${BOLD}${getMethodColor(method)}${method}${RESET} ${BLUE}${uri}${RESET}")
            println("$BOX_VERTICAL ${BOLD}Trace-Id:${RESET} $traceId")
            println("$BOX_VERTICAL ${BOLD}Status:${RESET} ${RED}${statusCode.value} ${statusCode.description}${RESET}")
            println("${BOX_LEFT_T}${BOX_HORIZONTAL.repeat(60)}${BOX_RIGHT_T}")
            println("$BOX_VERTICAL ${BOLD}Error Message:${RESET}")
            println("$BOX_VERTICAL   ${RED}${errorMessage}${RESET}")

            if (exception != null) {
                println("$BOX_VERTICAL ${BOLD}Exception:${RESET}")
                println("$BOX_VERTICAL   ${RED}${exception.javaClass.simpleName}: ${exception.message}${RESET}")
            }

            printBoxBottom()
            println()
        }

        if (mode != LogMode.CONSOLE) {
            val obj = buildJsonObject {
                put("timestamp", timestamp)
                put("level", "ERROR")
                put("event", "error")
                put("traceId", traceId)
                put("method", method)
                put("uri", uri)
                put("status", statusCode.value)
                put("message", errorMessage)
                if (exception != null) {
                    putJsonObject("exception") {
                        put("type", exception.javaClass.simpleName)
                        put("message", exception.message ?: "")
                    }
                }
            }
            println(jsonCompact.encodeToString(JsonObject.serializer(), obj))
        }
    }

    /**
     * Pretty print JSON
     */
    private fun prettyPrintJson(jsonString: String): String {
        return try {
            val jsonElement = Json.parseToJsonElement(jsonString)
            json.encodeToString(JsonElement.serializer(), jsonElement)
        } catch (e: Exception) {
            // Not valid JSON, return as-is
            jsonString
        }
    }

    /**
     * Get color for HTTP method
     */
    private fun getMethodColor(method: String): String {
        return when (method.uppercase()) {
            "GET" -> GREEN
            "POST" -> CYAN
            "PUT" -> YELLOW
            "DELETE" -> RED
            "PATCH" -> MAGENTA
            else -> WHITE
        }
    }

    /**
     * Get color for HTTP status code
     */
    private fun getStatusColor(statusCode: HttpStatusCode): String {
        return when (statusCode.value) {
            in 200..299 -> GREEN
            in 300..399 -> CYAN
            in 400..499 -> YELLOW
            in 500..599 -> RED
            else -> WHITE
        }
    }

    /**
     * Print box top border with title
     */
    private fun printBox(title: String, color: String) {
        val titleText = " $title "
        val remainingWidth = 60 - titleText.length
        val leftPadding = remainingWidth / 2
        val rightPadding = remainingWidth - leftPadding

        println("${color}${BOLD}${BOX_TOP_LEFT}${BOX_HORIZONTAL.repeat(leftPadding)}${titleText}${BOX_HORIZONTAL.repeat(rightPadding)}${BOX_TOP_RIGHT}${RESET}")
    }

    /**
     * Print box bottom border
     */
    private fun printBoxBottom() {
        println("${BOX_BOTTOM_LEFT}${BOX_HORIZONTAL.repeat(60)}${BOX_BOTTOM_RIGHT}")
    }
}

/**
 * Extension function to easily log requests in routes
 */
fun PipelineContext<Unit, ApplicationCall>.logRequest(requestBody: String) {
    RequestLogger.logRequest(call, requestBody)
}

/**
 * Extension function to easily log responses in routes
 */
fun PipelineContext<Unit, ApplicationCall>.logResponse(statusCode: HttpStatusCode, responseBody: String, durationMs: Long? = null) {
    RequestLogger.logResponse(call, statusCode, responseBody, durationMs)
}

/**
 * Extension function to easily log errors in routes
 */
fun PipelineContext<Unit, ApplicationCall>.logError(statusCode: HttpStatusCode, errorMessage: String, exception: Exception? = null) {
    RequestLogger.logError(call, statusCode, errorMessage, exception)
}
