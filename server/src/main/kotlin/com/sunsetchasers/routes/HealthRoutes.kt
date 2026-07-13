package com.sunsetchasers.routes

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * Outside the rate limiter deliberately: used by Render's own health checks
 * and by an external keepalive pinger, neither of which should compete with
 * real forecast traffic for the 100 req/min budget.
 */
fun Route.healthRoutes() {
    get("/health") {
        call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
    }
}
