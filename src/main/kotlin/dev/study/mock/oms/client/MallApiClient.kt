package dev.study.mock.oms.client

import dev.study.mock.oms.shipment.ShipmentStatusRequest
import dev.study.mock.oms.stock.StockSyncRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.Base64

@Component
class MallApiClient(
    @Value("\${mall.api.base-url}") private val baseUrl: String,
    @Value("\${mall.auth.username}") private val username: String,
    @Value("\${mall.auth.password}") private val password: String
) {
    private val restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()
            ).apply {
                setReadTimeout(Duration.ofSeconds(5))
            }
        )
        .defaultHeader("Authorization", createBasicAuthHeader())
        .build()

    private fun createBasicAuthHeader(): String {
        val credentials = "$username:$password"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encoded"
    }

    fun syncStock(request: StockSyncRequest) {
        restClient.post()
            .uri("/api/v1/stocks/sync")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }

    fun sendShipmentStatus(request: ShipmentStatusRequest) {
        restClient.post()
            .uri("/api/v1/shipments/status")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity()
    }
}
