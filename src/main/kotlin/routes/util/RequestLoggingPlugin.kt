package com.kevin.routes.util

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*

/**
 * Application-wide HTTP request/response logger using RequestLogger.
 *
 * Notes:
 * - Does NOT log request/response bodies to avoid consuming channels before route handlers.
 * - Logs method, URI, headers (Authorization masked by RequestLogger), query params, status, and duration.
 * - Errors are logged with stack information.
 */
fun Application.installRequestLogger() {
    intercept(ApplicationCallPipeline.Monitoring) {
        // Ensure we have a trace id available and expose it to clients
        RequestLogger.ensureTraceIdHeader(call)

        // Only log metadata here to avoid consuming the request body prematurely
        RequestLogger.logRequest(call, requestBody = "")
        val startTime = System.currentTimeMillis()
        try {
            proceed()
            val status = call.response.status() ?: HttpStatusCode.OK
            val duration = System.currentTimeMillis() - startTime
            // Expose trace-id header in case it wasn't added before response committed
            RequestLogger.ensureTraceIdHeader(call)
            RequestLogger.logResponse(call, statusCode = status, responseBody = "", durationMs = duration)
        } catch (e: Exception) {
            val status = HttpStatusCode.InternalServerError
            RequestLogger.ensureTraceIdHeader(call)
            RequestLogger.logError(call, statusCode = status, errorMessage = e.message ?: "Unhandled exception", exception = e)
            throw e
        }
    }
}
