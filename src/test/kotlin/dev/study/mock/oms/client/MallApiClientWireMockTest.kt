package dev.study.mock.oms.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import dev.study.mock.oms.shipment.ShipmentItem
import dev.study.mock.oms.shipment.ShipmentStatus
import dev.study.mock.oms.shipment.ShipmentStatusRequest
import dev.study.mock.oms.stock.StockItem
import dev.study.mock.oms.stock.StockSyncRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.HttpServerErrorException
import java.time.Instant

/**
 * MallApiClient WireMock 테스트
 *
 * OMS → 자사몰 API 호출 시나리오를 WireMock으로 테스트
 *
 * 학습 포인트:
 * 1. Basic Auth 헤더 검증
 * 2. Request body matching
 * 3. Response Templating (동적 응답)
 * 4. Scenario를 이용한 재시도 테스트
 * 5. Verification
 */
class MallApiClientWireMockTest {

    private lateinit var wireMockServer: WireMockServer
    private lateinit var mallApiClient: MallApiClient

    @BeforeEach
    fun setup() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()

        mallApiClient = MallApiClient(
            baseUrl = "http://localhost:${wireMockServer.port()}",
            username = "mall-user",
            password = "mall-pass"
        )
    }

    @AfterEach
    fun teardown() {
        wireMockServer.stop()
    }

    // ==================== 재고 전송 테스트 ====================

    @Test
    fun `재고 전송 성공`() {
        // given
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .withHeader("Authorization", equalTo("Basic bWFsbC11c2VyOm1hbGwtcGFzcw=="))
                .willReturn(aResponse().withStatus(200))
        )

        // when
        val request = StockSyncRequest(
            stocks = listOf(
                StockItem("PROD-001", "OPT-001", 100),
                StockItem("PROD-002", "OPT-001", 50)
            ),
            syncedAt = Instant.now()
        )
        mallApiClient.syncStock(request)

        // then: API가 호출되었는지 검증
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/v1/stocks/sync")))
    }

    @Test
    fun `재고 전송 시 요청 본문 검증`() {
        // given
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .willReturn(aResponse().withStatus(200))
        )

        // when
        val request = StockSyncRequest(
            stocks = listOf(StockItem("PROD-001", "OPT-001", 100)),
            syncedAt = Instant.parse("2024-01-15T12:00:00Z")
        )
        mallApiClient.syncStock(request)

        // then: 요청 본문 검증
        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/api/v1/stocks/sync"))
                .withRequestBody(matchingJsonPath("$.stocks[0].productId", equalTo("PROD-001")))
                .withRequestBody(matchingJsonPath("$.stocks[0].optionId", equalTo("OPT-001")))
                .withRequestBody(matchingJsonPath("$.stocks[0].availableQuantity", equalTo("100")))
        )
    }

    @Test
    fun `인증 실패 시 401 에러`() {
        // given: 잘못된 인증 정보로 클라이언트 생성
        val wrongAuthClient = MallApiClient(
            baseUrl = "http://localhost:${wireMockServer.port()}",
            username = "wrong-user",
            password = "wrong-pass"
        )

        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .withHeader("Authorization", equalTo("Basic bWFsbC11c2VyOm1hbGwtcGFzcw=="))
                .willReturn(aResponse().withStatus(200))
        )

        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .withHeader("Authorization", notMatching("Basic bWFsbC11c2VyOm1hbGwtcGFzcw=="))
                .willReturn(aResponse().withStatus(401))
        )

        // when & then
        assertThrows<Exception> {
            wrongAuthClient.syncStock(
                StockSyncRequest(
                    stocks = listOf(StockItem("PROD-001", "OPT-001", 100)),
                    syncedAt = Instant.now()
                )
            )
        }
    }

    // ==================== 배송 상태 전송 테스트 ====================

    @Test
    fun `배송 상태 전송 성공`() {
        // given
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/shipments/status"))
                .willReturn(aResponse().withStatus(200))
        )

        // when
        val request = ShipmentStatusRequest(
            orderId = "ORD-2024-001",
            shipments = listOf(
                ShipmentItem(
                    shipmentId = "SHP-001",
                    carrierCode = "CJ",
                    trackingNumber = "1234567890",
                    status = ShipmentStatus.OUT_FOR_DELIVERY,
                    statusUpdatedAt = Instant.now(),
                    orderLines = listOf("LINE-001", "LINE-002")
                )
            )
        )
        mallApiClient.sendShipmentStatus(request)

        // then
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/v1/shipments/status")))
    }

    @Test
    fun `배송 상태에 따른 다른 응답 - Priority 사용`() {
        // given: DELIVERED 상태는 추가 처리 응답
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/shipments/status"))
                .atPriority(1)
                .withRequestBody(matchingJsonPath("$.shipments[0].status", equalTo("DELIVERED")))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("X-Processing", "delivery-confirmed")
                )
        )

        // 기본 응답
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/shipments/status"))
                .atPriority(10)
                .willReturn(aResponse().withStatus(200))
        )

        // when: DELIVERED 상태 전송
        mallApiClient.sendShipmentStatus(
            ShipmentStatusRequest(
                orderId = "ORD-2024-001",
                shipments = listOf(
                    ShipmentItem(
                        shipmentId = "SHP-001",
                        carrierCode = "CJ",
                        trackingNumber = "1234567890",
                        status = ShipmentStatus.DELIVERED,
                        statusUpdatedAt = Instant.now(),
                        orderLines = listOf("LINE-001")
                    )
                )
            )
        )

        // then: DELIVERED 전용 stub이 매칭됨
        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/api/v1/shipments/status"))
                .withRequestBody(matchingJsonPath("$.shipments[0].status", equalTo("DELIVERED")))
        )
    }

    // ==================== Scenario 테스트 (상태 기반 응답) ====================

    @Test
    fun `재시도 시나리오 - 첫 번째 실패 후 두 번째 성공`() {
        // given: 첫 호출은 503, 두 번째는 200
        val scenarioName = "Retry Scenario"

        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("Service Recovered")
        )

        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("Service Recovered")
                .willReturn(aResponse().withStatus(200))
        )

        val request = StockSyncRequest(
            stocks = listOf(StockItem("PROD-001", "OPT-001", 100)),
            syncedAt = Instant.now()
        )

        // when: 첫 번째 호출 - 실패
        assertThrows<HttpServerErrorException.ServiceUnavailable> {
            mallApiClient.syncStock(request)
        }

        // when: 두 번째 호출 - 성공
        mallApiClient.syncStock(request)

        // then: 총 2번 호출됨
        wireMockServer.verify(2, postRequestedFor(urlEqualTo("/api/v1/stocks/sync")))
    }

    // ==================== 응답 지연 테스트 ====================

    @Test
    fun `응답 지연 시 타임아웃`() {
        // given: 10초 지연 (클라이언트 타임아웃 5초)
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withFixedDelay(10000)
                )
        )

        // when & then: 타임아웃 발생
        assertThrows<Exception> {
            mallApiClient.syncStock(
                StockSyncRequest(
                    stocks = listOf(StockItem("PROD-001", "OPT-001", 100)),
                    syncedAt = Instant.now()
                )
            )
        }
    }

    // ==================== Response Templating 테스트 ====================

    @Test
    fun `Response Templating - 요청 데이터를 응답에 포함`() {
        // given: Response Templating 활성화
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"received": "{{jsonPath request.body '$.stocks.length()'}}", "timestamp": "{{now}}"}""")
                        .withTransformers("response-template")
                )
        )

        // when
        mallApiClient.syncStock(
            StockSyncRequest(
                stocks = listOf(
                    StockItem("PROD-001", "OPT-001", 100),
                    StockItem("PROD-002", "OPT-001", 50)
                ),
                syncedAt = Instant.now()
            )
        )

        // then: API 호출됨
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/v1/stocks/sync")))
    }

    // ==================== 호출 횟수 검증 ====================

    @Test
    fun `API가 호출되지 않았음을 검증`() {
        // given
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .willReturn(aResponse().withStatus(200))
        )

        // when: 아무것도 호출하지 않음

        // then
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/api/v1/stocks/sync")))
    }

    @Test
    fun `여러 번 호출 후 횟수 검증`() {
        // given
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/stocks/sync"))
                .willReturn(aResponse().withStatus(200))
        )

        val request = StockSyncRequest(
            stocks = listOf(StockItem("PROD-001", "OPT-001", 100)),
            syncedAt = Instant.now()
        )

        // when: 3번 호출
        repeat(3) {
            mallApiClient.syncStock(request)
        }

        // then: 정확히 3번 호출됨
        wireMockServer.verify(3, postRequestedFor(urlEqualTo("/api/v1/stocks/sync")))
    }
}
