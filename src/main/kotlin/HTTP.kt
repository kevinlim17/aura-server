package com.kevin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kborowy.authprovider.firebase.firebase
import com.kevin.routes.util.installRequestLogger
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import org.jetbrains.exposed.sql.*

fun Application.configureHTTP() {
    install(Compression)

    // Install ContentNegotiation for JSON serialization
    install(ContentNegotiation) {
        json()
    }

    // Install application-wide HTTP request/response logger
    installRequestLogger()
}
