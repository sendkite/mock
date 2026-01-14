# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0.1 + Kotlin 기반 OMS(Order Management System) Mock 서버. 자사몰 서버 테스트 용도로 WireMock 학습 프로젝트.

## Build Commands

```bash
./gradlew build          # 빌드
./gradlew bootRun        # 실행 (http://localhost:8080)
./gradlew test           # 전체 테스트

# 단일 테스트 실행
./gradlew test --tests "dev.study.mock.oms.client.MallApiClientWireMockTest"
./gradlew test --tests "dev.study.mock.oms.order.OrderServiceTest"
```

## Architecture

### Package Structure
```
dev.study.mock.oms/
├── order/      # 주문 (Order, OrderLine, OrderService, OrderController)
├── claim/      # 클레임 (Claim, ClaimLine, ClaimService, ClaimController)
├── stock/      # 재고 (Stock, StockService, StockController)
├── shipment/   # 배송 (Shipment, ShipmentService, ShipmentController)
├── client/     # 자사몰 API 호출 클라이언트 (MallApiClient)
└── config/     # 설정 (BasicAuthFilter, GlobalExceptionHandler)
```

### API Endpoints (OMS Mock Server)
| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/v1/orders | 주문 등록 |
| GET | /api/v1/orders/{orderId} | 주문 조회 |
| POST | /api/v1/claims | 클레임 등록 |
| PUT | /api/v1/stocks/{productId}/{optionId} | 재고 수정 |
| POST | /api/v1/shipments | 배송 생성 |

### 인증
- Basic Auth: `oms-user:oms-pass`

## Key Dependencies

- Spring Data JPA + H2 (인메모리 DB)
- WireMock 3.10 for HTTP API mocking
- Mockito Kotlin for unit tests

## WireMock 테스트 패턴

### 클라이언트 테스트 (MallApiClientWireMockTest)
OMS → 자사몰 API 호출을 WireMock으로 모킹:
```kotlin
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
```

### 학습 포인트 (테스트 코드 참조)
- **Stubbing**: `stubFor()`, `aResponse()`, HTTP 상태 코드
- **Request Matching**: `matchingJsonPath()`, `withHeader()`
- **Scenario**: 상태 기반 응답 (`inScenario()`, `whenScenarioStateIs()`)
- **Delay**: `withFixedDelay()` 타임아웃 테스트
- **Verification**: `verify()` 호출 횟수/본문 검증
- **Priority**: `atPriority()` stub 우선순위

## H2 Console

http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:omsdb`
- User: `sa`
