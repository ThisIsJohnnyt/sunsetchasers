package com.sunsetchasers

import com.sunsetchasers.plugins.installErrorHandling
import com.sunsetchasers.routes.forecastRoutes
import com.sunsetchasers.routes.healthRoutes
import com.sunsetchasers.services.WeatherService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    val httpClient = HttpClient(CIO) {
        // Open-Meteo's response includes fields we don't declare (latitude,
        // elevation, hourly_units, etc.) — ignoreUnknownKeys is required, not optional.
        install(ClientContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    val weatherService = WeatherService(httpClient)

    install(ContentNegotiation) {
        json(Json { encodeDefaults = true })
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(RateLimit) {
        register {
            rateLimiter(limit = 100, refillPeriod = 60.seconds)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }

    installErrorHandling()

    routing {
        healthRoutes()
        forecastRoutes(weatherService)
    }
}
