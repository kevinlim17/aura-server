package com.kevin

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabases()  // Initialize database first
    configureHTTP()
    configureSecurity()   // Configure JWT authentication
    configureSerialization()
    configureRouting()
}
