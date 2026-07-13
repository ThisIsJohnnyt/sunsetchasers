package com.sunsetchasers.plugins

import com.sunsetchasers.models.ErrorResponse
import com.sunsetchasers.routes.DateOutOfRangeException
import com.sunsetchasers.routes.InvalidRequestException
import com.sunsetchasers.services.AstronomyCalculationException
import com.sunsetchasers.services.WeatherApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun newRequestId(): String = "req_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)

/**
 * Maps the domain exceptions thrown by the forecast routes/services to the
 * error response shapes documented in API_SPEC.md.
 */
fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<InvalidRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "INVALID_REQUEST",
                    message = cause.message ?: "Invalid request",
                    details = buildJsonObject {
                        put("field", cause.field)
                        put("value", cause.value)
                    }
                )
            )
        }
        exception<DateOutOfRangeException> { call, cause ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                ErrorResponse(
                    code = "DATE_OUT_OF_RANGE",
                    message = cause.message ?: "Forecast date must be within 7 days of today",
                    details = buildJsonObject {
                        put("requested_date", cause.requestedDate)
                        put("max_forecast_date", cause.maxForecastDate)
                        put("days_ahead", cause.daysAhead)
                    }
                )
            )
        }
        exception<AstronomyCalculationException> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = "CALCULATION_ERROR",
                    message = "Failed to calculate sunrise/sunset times",
                    requestId = newRequestId()
                )
            )
        }
        exception<WeatherApiException> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = "CALCULATION_ERROR",
                    message = "Failed to retrieve weather data",
                    requestId = newRequestId()
                )
            )
        }
        exception<Throwable> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = "CALCULATION_ERROR",
                    message = "Unexpected server error",
                    requestId = newRequestId()
                )
            )
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            val retryAfter = call.response.headers["Retry-After"]?.toIntOrNull() ?: 60
            call.respond(
                status,
                ErrorResponse(
                    code = "RATE_LIMITED",
                    message = "Too many requests. Please try again in $retryAfter seconds.",
                    retryAfter = retryAfter
                )
            )
        }
    }
}
