package com.kevin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

/**
 * Configure JWT authentication for the application
 */
fun Application.configureSecurity() {
    // Load environment variables
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    // JWT Configuration from environment or defaults
    val jwtSecret = dotenv["JWT_SECRET"] ?: "aura-secret-key-change-in-production"
    val jwtIssuer = dotenv["JWT_ISSUER"] ?: "aura-server"
    val jwtAudience = dotenv["JWT_AUDIENCE"] ?: "aura-client"
    val jwtRealm = dotenv["JWT_REALM"] ?: "Aura API"

    log.info("========================================")
    log.info("Configuring JWT Authentication...")
    log.info("JWT Issuer: $jwtIssuer")
    log.info("JWT Audience: $jwtAudience")
    log.info("JWT Realm: $jwtRealm")
    log.info("========================================")

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm

            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            )

            validate { credential ->
                // Validate the JWT token
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { defaultScheme, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "success" to false,
                        "error" to mapOf(
                            "code" to "UNAUTHORIZED",
                            "message" to "Token is not valid or has expired"
                        )
                    )
                )
            }
        }
    }

    log.info("JWT Authentication configured successfully")
}
