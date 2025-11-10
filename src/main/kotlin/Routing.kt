package com.kevin

import com.kevin.routes.artworkRoutes
import com.kevin.routes.authRoutes
import com.kevin.routes.companionRoutes
import com.kevin.routes.docentRoutes
import com.kevin.routes.feedbackRoutes
import com.kevin.routes.fewShotRoutes
import com.kevin.routes.linkRoutes
import com.kevin.routes.memoRoutes
import com.kevin.routes.userContextRoutes
import com.kevin.routes.userPreferencesRoutes
import com.kevin.routes.userProfileRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configure application routes
 */
fun Application.configureRouting() {
    routing {
        // Root endpoint
        get("/") {
            call.respondText("Aura Server API - Running")
        }

        // Health check endpoint
        get("/health") {
            call.respond(
                mapOf(
                    "status" to "healthy",
                    "service" to "aura-server",
                    "version" to "0.0.1"
                )
            )
        }

        // Authentication routes
        authRoutes()

        // User profile routes
        userProfileRoutes()

        // User context routes
        userContextRoutes()

        // User preferences routes
        userPreferencesRoutes()

        // Artwork routes
        artworkRoutes()

        // Docent routes
        docentRoutes()

        // Feedback routes
        feedbackRoutes()

        // Few-Shot routes
        fewShotRoutes()

        // Link routes
        linkRoutes()

        // Memo routes
        memoRoutes()

        // Companion routes
        companionRoutes()
    }
}
