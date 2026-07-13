package com.sunsetchasers.core.network

import com.sunsetchasers.core.model.ForecastRangeResult
import com.sunsetchasers.core.model.ForecastResult
import com.sunsetchasers.core.model.ForecastType
import com.sunsetchasers.core.network.dto.ErrorResponseDto
import com.sunsetchasers.core.network.dto.ForecastRangeRequestDto
import com.sunsetchasers.core.network.dto.ForecastRangeResponseDto
import com.sunsetchasers.core.network.dto.ForecastRequestDto
import com.sunsetchasers.core.network.dto.ForecastResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject

class ForecastApi @Inject constructor(
    private val httpClient: HttpClient,
    @param:BaseUrl private val baseUrl: String
) {
    suspend fun getForecast(
        latitude: Double,
        longitude: Double,
        date: String,
        type: ForecastType
    ): ForecastResult {
        return try {
            val response = httpClient.post("${baseUrl}api/forecast") {
                contentType(ContentType.Application.Json)
                setBody(
                    ForecastRequestDto(
                        latitude = latitude,
                        longitude = longitude,
                        date = date,
                        type = type.name.lowercase()
                    )
                )
            }

            if (response.status.isSuccess()) {
                ForecastResult.Success(response.body<ForecastResponseDto>().toDomain())
            } else {
                val error = response.body<ErrorResponseDto>()
                ForecastResult.Error(code = error.code, message = error.message)
            }
        } catch (e: Exception) {
            ForecastResult.Error(code = "NETWORK_ERROR", message = e.message ?: "Unable to reach the server")
        }
    }

    suspend fun getForecastRange(
        latitude: Double,
        longitude: Double,
        type: ForecastType
    ): ForecastRangeResult {
        return try {
            val response = httpClient.post("${baseUrl}api/forecast/range") {
                contentType(ContentType.Application.Json)
                setBody(
                    ForecastRangeRequestDto(
                        latitude = latitude,
                        longitude = longitude,
                        type = type.name.lowercase()
                    )
                )
            }

            if (response.status.isSuccess()) {
                ForecastRangeResult.Success(response.body<ForecastRangeResponseDto>().toDomain())
            } else {
                val error = response.body<ErrorResponseDto>()
                ForecastRangeResult.Error(code = error.code, message = error.message)
            }
        } catch (e: Exception) {
            ForecastRangeResult.Error(code = "NETWORK_ERROR", message = e.message ?: "Unable to reach the server")
        }
    }
}
